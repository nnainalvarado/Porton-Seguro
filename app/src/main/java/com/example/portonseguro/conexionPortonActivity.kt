package com.example.portonseguro

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ConexionPortonActivity : AppCompatActivity() {
    private var conectado = false
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private var wemosIP = "192.168.1.100:5000"
    private val handler = Handler(Looper.getMainLooper())
    private var checkConnectionRunnable: Runnable? = null
    private var movementListener: ValueEventListener? = null

    // Referencias de UI
    private lateinit var btnConectar: Button
    private lateinit var btnDesconectar: Button
    private lateinit var estadoConexion: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conexion_porton)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("PortonConfig", MODE_PRIVATE)
        // CORRECCIÓN: Cargar IP con puerto por defecto
        wemosIP = sharedPreferences.getString("wemos_ip", "192.168.1.100:5000") ?: "192.168.1.100:5000"

        // Verificar si el usuario está autenticado
        if (auth.currentUser == null) {
            // Redirigir al login si no está autenticado
            Toast.makeText(this, "Debe iniciar sesión primero", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupNavigation()
        setupFirebaseListener()
        actualizarEstado()
        startConnectionMonitoring()
    }

    private fun initializeViews() {
        btnConectar = findViewById(R.id.btnConectar)
        btnDesconectar = findViewById(R.id.btnDesconectar)
        estadoConexion = findViewById(R.id.estadoConexion)

        btnConectar.setOnClickListener {
            showConnectionOptions()
        }
        btnDesconectar.setOnClickListener {
            desconectarWemos()
        }

        // Long click para configurar IP
        btnConectar.setOnLongClickListener {
            configurarIP()
            true
        }
    }

    private fun showConnectionOptions() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Opciones de Conexión")
        builder.setMessage("IP actual: $wemosIP")

        builder.setPositiveButton("Conectar") { _, _ ->
            conectarWemos()
        }

        builder.setNeutralButton("Cambiar IP") { _, _ ->
            configurarIP()
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun conectarWemos() {
        Log.d("ConexionPorton", "🔗 Iniciando conexión a: $wemosIP")

        btnConectar.isEnabled = false
        btnConectar.text = "Conectando..."

        val url = "http://$wemosIP/conectar"
        Log.d("ConexionPorton", "📡 URL completa: $url")

        val requestQueue = Volley.newRequestQueue(this)

        val jsonRequest = JSONObject().apply {
            put("usuario", auth.currentUser?.uid ?: "unknown")
            put("timestamp", System.currentTimeMillis())
            put("app_version", "1.0")
        }

        Log.d("ConexionPorton", "📤 Enviando datos: $jsonRequest")

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonRequest,
            { response ->
                Log.d("ConexionPorton", "📥 Respuesta recibida: $response")

                try {
                    val status = response.optString("status", "error")
                    val mensaje = response.optString("mensaje", "Conexión establecida")

                    if (status == "ok" || status == "success") {
                        conectado = true
                        guardarUltimaConexion()
                        actualizarEstado()
                        actualizarEstadoEnFirebase(true)
                        Toast.makeText(this, "✅ $mensaje", Toast.LENGTH_SHORT).show()
                        Log.d("ConexionPorton", "✅ Conectado exitosamente al Wemos")
                    } else {
                        conectado = false
                        actualizarEstado()
                        Toast.makeText(this, "❌ Error del Wemos: $mensaje", Toast.LENGTH_SHORT).show()
                        Log.e("ConexionPorton", "❌ Error en respuesta del Wemos: $mensaje")
                    }
                } catch (e: Exception) {
                    conectado = false
                    actualizarEstado()
                    Log.e("ConexionPorton", "❌ Error parsing response", e)
                    Toast.makeText(this, "❌ Respuesta del Wemos no válida", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                conectado = false
                actualizarEstado()
                Log.e("ConexionPorton", "❌ Error conectando: ${error.message}")
                Log.e("ConexionPorton", "❌ NetworkResponse: ${error.networkResponse}")

                val errorMsg = when {
                    error.networkResponse?.statusCode == 404 -> "❌ Endpoint /conectar no encontrado en el Wemos"
                    error.networkResponse?.statusCode == 500 -> "❌ Error interno del servidor Wemos"
                    error.message?.contains("timeout", true) == true -> "⏰ Timeout: El Wemos no responde"
                    error.message?.contains("ConnectException", true) == true -> "🔌 No se puede conectar al Wemos en IP: $wemosIP"
                    error.message?.contains("UnknownHostException", true) == true -> "🌐 IP no encontrada: $wemosIP"
                    else -> "❌ Error de conexión: ${error.message}"
                }

                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        )

        request.setShouldCache(false)
        requestQueue.add(request)
    }

    private fun desconectarWemos() {
        btnDesconectar.isEnabled = false
        btnDesconectar.text = "Desconectando..."

        val url = "http://$wemosIP/desconectar"
        val requestQueue = Volley.newRequestQueue(this)

        val jsonRequest = JSONObject().apply {
            put("usuario", auth.currentUser?.uid ?: "unknown")
            put("timestamp", System.currentTimeMillis())
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonRequest,
            { response ->
                conectado = false
                actualizarEstado()
                actualizarEstadoEnFirebase(false)
                Toast.makeText(this, "Desconectado del Wemos", Toast.LENGTH_SHORT).show()
                Log.d("ConexionPorton", "Desconectado exitosamente del Wemos")
            },
            { error ->
                // Desconectar localmente aunque falle la comunicación
                conectado = false
                actualizarEstado()
                actualizarEstadoEnFirebase(false)
                Log.w("ConexionPorton", "Error al desconectar, desconectando localmente: ${error.message}")
                Toast.makeText(this, "Desconectado localmente", Toast.LENGTH_SHORT).show()
            }
        )

        request.setShouldCache(false)
        requestQueue.add(request)
    }

    private fun configurarIP() {
        val builder = AlertDialog.Builder(this)
        val input = EditText(this)
        input.setText(wemosIP)
        input.hint = "Ej: 192.168.1.6:5000"
        input.setPadding(50, 30, 50, 30)

        builder.setTitle("Configurar IP del Wemos")
        builder.setMessage("Ingrese la dirección IP:PUERTO del Wemos D1 Mini:")
        builder.setView(input)

        builder.setPositiveButton("Guardar") { _, _ ->
            val nuevaIP = input.text.toString().trim()
            if (isValidIP(nuevaIP)) {
                wemosIP = nuevaIP
                sharedPreferences.edit().putString("wemos_ip", wemosIP).apply()
                Toast.makeText(this, "IP configurada: $wemosIP", Toast.LENGTH_SHORT).show()
                Log.d("ConexionPorton", "IP actualizada a: $wemosIP")
            } else {
                Toast.makeText(this, "IP no válida. Formato: xxx.xxx.xxx.xxx:puerto", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar", null)

        builder.setNeutralButton("Test Completo") { _, _ ->
            val nuevaIP = input.text.toString().trim()
            if (isValidIP(nuevaIP)) {
                wemosIP = nuevaIP // Temporalmente para el test
                testearConexionCompleta()
            } else {
                Toast.makeText(this, "IP no válida para probar", Toast.LENGTH_SHORT).show()
            }
        }

        builder.show()
    }

    // NUEVA FUNCIÓN: Test completo de conexión
    private fun testearConexionCompleta() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🧪 Test de Conexión")
        builder.setMessage("Probando todos los endpoints del Wemos...")

        val dialog = builder.create()
        dialog.show()

        val requestQueue = Volley.newRequestQueue(this)

        // Test 1: Ping
        val pingUrl = "http://$wemosIP/ping"
        Log.d("ConexionPorton", "🏓 Probando ping: $pingUrl")

        val pingRequest = StringRequest(
            Request.Method.GET, pingUrl,
            { response ->
                Log.d("ConexionPorton", "✅ Ping exitoso: $response")

                // Test 2: Status
                val statusUrl = "http://$wemosIP/status"
                val statusRequest = StringRequest(
                    Request.Method.GET, statusUrl,
                    { statusResponse ->
                        Log.d("ConexionPorton", "✅ Status obtenido: $statusResponse")

                        // Test 3: Sensor
                        val sensorUrl = "http://$wemosIP/sensor"
                        val sensorRequest = StringRequest(
                            Request.Method.GET, sensorUrl,
                            { sensorResponse ->
                                Log.d("ConexionPorton", "✅ Sensor respondió: $sensorResponse")

                                dialog.dismiss()
                                Toast.makeText(this, "✅ Todos los tests exitosos!\nWemos funcionando correctamente", Toast.LENGTH_LONG).show()
                            },
                            { error ->
                                dialog.dismiss()
                                Log.e("ConexionPorton", "❌ Error en sensor: ${error.message}")
                                Toast.makeText(this, "❌ Error en endpoint /sensor", Toast.LENGTH_SHORT).show()
                            }
                        )
                        requestQueue.add(sensorRequest)
                    },
                    { error ->
                        dialog.dismiss()
                        Log.e("ConexionPorton", "❌ Error en status: ${error.message}")
                        Toast.makeText(this, "❌ Error en endpoint /status", Toast.LENGTH_SHORT).show()
                    }
                )
                requestQueue.add(statusRequest)
            },
            { error ->
                dialog.dismiss()
                Log.e("ConexionPorton", "❌ Error en ping: ${error.message}")
                Toast.makeText(this, "❌ Error: No se puede conectar al Wemos\nVerifica IP: $wemosIP", Toast.LENGTH_LONG).show()
            }
        )

        requestQueue.add(pingRequest)
    }

    private fun probarConexionIP(ip: String) {
        val url = "http://$ip/ping"
        val requestQueue = Volley.newRequestQueue(this)

        Toast.makeText(this, "Probando conexión con $ip...", Toast.LENGTH_SHORT).show()

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                Toast.makeText(this, "✓ Conexión exitosa con $ip", Toast.LENGTH_LONG).show()
                Log.d("ConexionPorton", "Ping exitoso a $ip: $response")
            },
            { error ->
                Toast.makeText(this, "✗ No se pudo conectar con $ip", Toast.LENGTH_LONG).show()
                Log.e("ConexionPorton", "Error en ping a $ip: ${error.message}")
            }
        )

        requestQueue.add(request)
    }

    // FUNCIÓN MEJORADA: Validación de IP con soporte para puerto
    private fun isValidIP(ip: String): Boolean {
        if (ip.isBlank()) return false

        // Separar IP y puerto si existe
        val parts = if (ip.contains(":")) {
            val ipPort = ip.split(":")
            if (ipPort.size != 2) return false

            // Validar puerto
            try {
                val puerto = ipPort[1].toInt()
                if (puerto !in 1..65535) return false
            } catch (e: NumberFormatException) {
                return false
            }

            ipPort[0].split(".")
        } else {
            ip.split(".")
        }

        if (parts.size != 4) return false

        return parts.all { part ->
            try {
                val num = part.toInt()
                num in 0..255
            } catch (e: NumberFormatException) {
                false
            }
        }
    }

    private fun actualizarEstadoEnFirebase(conectado: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        val dispositivoData = mapOf(
            "conectado" to conectado,
            "timestamp" to ServerValue.TIMESTAMP,
            "ip" to wemosIP,
            "usuario" to userId,
            "dispositivo" to "wemos_d1"
        )

        database.child("dispositivos").child(userId).child("wemos")
            .setValue(dispositivoData)
            .addOnSuccessListener {
                Log.d("ConexionPorton", "Estado actualizado en Firebase: $conectado")
            }
            .addOnFailureListener { error ->
                Log.e("ConexionPorton", "Error actualizando Firebase: ${error.message}")
            }
    }

    private fun setupFirebaseListener() {
        val userId = auth.currentUser?.uid ?: return

        movementListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (movimientoSnapshot in snapshot.children) {
                    val timestamp = movimientoSnapshot.child("timestamp").getValue(Long::class.java) ?: 0
                    val distancia = movimientoSnapshot.child("distancia").getValue(Double::class.java) ?: 0.0
                    val tipo = movimientoSnapshot.child("tipo").getValue(String::class.java) ?: "unknown"

                    // Solo mostrar notificaciones de movimientos recientes (últimos 30 segundos)
                    val ahora = System.currentTimeMillis()
                    if (ahora - timestamp < 30000) {
                        mostrarNotificacionMovimiento(distancia, tipo)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ConexionPorton", "Error escuchando movimientos: ${error.message}")
            }
        }

        database.child("movimientos").child(userId)
            .orderByChild("timestamp")
            .limitToLast(5)
            .addValueEventListener(movementListener!!)
    }

    private fun mostrarNotificacionMovimiento(distancia: Double, tipo: String) {
        runOnUiThread {
            val mensaje = when (tipo) {
                "deteccion_automatica" -> "¡Movimiento detectado! Dist: ${distancia.toInt()}cm"
                else -> "¡Actividad en el portón!"
            }

            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            Log.d("ConexionPorton", "Notificación mostrada: $mensaje")
        }
    }

    private fun startConnectionMonitoring() {
        checkConnectionRunnable = object : Runnable {
            override fun run() {
                if (conectado) {
                    verificarConexionWemos()
                }
                // Verificar cada 30 segundos
                handler.postDelayed(this, 30000)
            }
        }
        handler.post(checkConnectionRunnable!!)
    }

    private fun verificarConexionWemos() {
        val url = "http://$wemosIP/ping"
        val requestQueue = Volley.newRequestQueue(this)

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                // Conexión OK
                Log.d("ConexionPorton", "Ping OK al Wemos")
            },
            { error ->
                // Conexión perdida
                if (conectado) {
                    conectado = false
                    actualizarEstado()
                    actualizarEstadoEnFirebase(false)
                    Toast.makeText(this, "⚠️ Conexión perdida con el Wemos", Toast.LENGTH_SHORT).show()
                    Log.w("ConexionPorton", "Conexión perdida con Wemos: ${error.message}")
                }
            }
        )

        requestQueue.add(request)
    }

    private fun guardarUltimaConexion() {
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        sharedPreferences.edit().putString("ultima_conexion", timestamp).apply()
        Log.d("ConexionPorton", "Última conexión guardada: $timestamp")
    }

    private fun actualizarEstado() {
        runOnUiThread {
            if (conectado) {
                estadoConexion.text = "✓ Portón conectado (IP: $wemosIP)"
                estadoConexion.setTextColor(getColor(android.R.color.holo_green_dark))
                btnConectar.visibility = View.GONE
                btnDesconectar.visibility = View.VISIBLE
                btnDesconectar.isEnabled = true
                btnDesconectar.text = "Desconectar portón"
            } else {
                estadoConexion.text = "✗ Portón desconectado"
                estadoConexion.setTextColor(getColor(android.R.color.holo_red_dark))
                btnConectar.visibility = View.VISIBLE
                btnDesconectar.visibility = View.GONE
                btnConectar.isEnabled = true
                btnConectar.text = "Conectar portón"
            }
        }
    }

    private fun setupNavigation() {
        val navbar = findViewById<LinearLayout>(R.id.navbar)

        // Buscar los elementos de navegación si existen
        try {
            val iconPorton = navbar.findViewById<ImageView>(R.id.porton)
            val iconHome = navbar.findViewById<ImageView>(R.id.home)
            val iconRadar = navbar.findViewById<ImageView>(R.id.radar)
            val iconAjuste = navbar.findViewById<ImageView>(R.id.ajustes)

            // Configurar active state si existen los underlines
            try {
                val underlinePorton = navbar.findViewById<View>(R.id.underline_porton)
                val underlineHome = navbar.findViewById<View>(R.id.underline_home)
                val underlineRadar = navbar.findViewById<View>(R.id.underline_radar)
                val underlineAjuste = navbar.findViewById<View>(R.id.underline_ajuste)

                // Activar tab actual
                underlineHome?.visibility = View.VISIBLE
                underlinePorton?.visibility = View.GONE
                underlineRadar?.visibility = View.GONE
                underlineAjuste?.visibility = View.GONE
            } catch (e: Exception) {
                Log.w("ConexionPorton", "Underlines no encontrados en navbar")
            }

            iconPorton?.setOnClickListener {
                startActivity(Intent(this, EstadoPortonActivity::class.java))
            }

            iconRadar?.setOnClickListener {
                startActivity(Intent(this, MovimientoDetectadoActivity::class.java))
            }

            iconAjuste?.setOnClickListener {
                startActivity(Intent(this, AjustesActivity::class.java))
            }

        } catch (e: Exception) {
            Log.w("ConexionPorton", "Error configurando navegación: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar listeners y handlers
        checkConnectionRunnable?.let { handler.removeCallbacks(it) }
        movementListener?.let {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                database.child("movimientos").child(userId).removeEventListener(it)
            }
        }
        Log.d("ConexionPorton", "Activity destruida, listeners removidos")
    }

    override fun onResume() {
        super.onResume()
        // Actualizar estado al volver a la actividad
        actualizarEstado()
    }
}