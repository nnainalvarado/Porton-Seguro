package com.example.portonseguro

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.json.JSONObject

class EstadoPortonActivity : ComponentActivity() {

    private var portonAbierto = false
    private var conectadoWemos = false
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private var wemosIP = "192.168.1.100"

    private var temporizadorActual: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var estadoListener: ValueEventListener? = null

    // Referencias UI
    private lateinit var imgPorton: ImageView
    private lateinit var textEstado: TextView
    private lateinit var btnCambiar: Button
    private lateinit var textTitulo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estado_porton)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        sharedPreferences = getSharedPreferences("PortonConfig", MODE_PRIVATE)
        wemosIP = sharedPreferences.getString("wemos_ip", "192.168.1.100") ?: "192.168.1.100"

        initializeViews()
        setupNavigation()

        // Verificar si se recibió comando para abrir desde otra actividad
        val abrirPorton = intent.getBooleanExtra("abrirPorton", false)
        if (abrirPorton) {
            abrirPortonConTemporizador()
        } else {
            // Cargar estado actual
            cargarEstadoActual()
        }

        setupFirebaseListener()
        verificarConexionWemos()
    }

    private fun initializeViews() {
        imgPorton = findViewById(R.id.imgEstadoPorton)
        textEstado = findViewById(R.id.textEstado)
        btnCambiar = findViewById(R.id.btnCambiarEstado)
        textTitulo = findViewById(R.id.textTitulo)

        btnCambiar.setOnClickListener {
            if (conectadoWemos) {
                cambiarEstadoPorton()
            } else {
                Toast.makeText(this, "Debe conectarse al Wemos primero", Toast.LENGTH_SHORT).show()
                // Ir a pantalla de conexión
                startActivity(Intent(this, ConexionPortonActivity::class.java))
            }
        }
    }

    private fun cargarEstadoActual() {
        // Cargar estado desde SharedPreferences primero
        portonAbierto = sharedPreferences.getBoolean("porton_abierto", false)
        conectadoWemos = sharedPreferences.getBoolean("wemos_conectado", false)

        actualizarUI()

        // Si está conectado, verificar estado real del Wemos
        if (conectadoWemos) {
            obtenerEstadoWemos()
        }
    }

    private fun verificarConexionWemos() {
        val url = "http://$wemosIP/ping"
        val requestQueue = Volley.newRequestQueue(this)

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                conectadoWemos = true
                sharedPreferences.edit().putBoolean("wemos_conectado", true).apply()
                textTitulo.text = "Estado del Portón ✓"
                textTitulo.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                obtenerEstadoWemos()
                Log.d("EstadoPorton", "Wemos conectado")
            },
            { error ->
                conectadoWemos = false
                sharedPreferences.edit().putBoolean("wemos_conectado", false).apply()
                textTitulo.text = "Estado del Portón (Sin conexión)"
                textTitulo.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                Log.w("EstadoPorton", "Wemos no conectado: ${error.message}")
            }
        )

        requestQueue.add(request)
    }

    private fun obtenerEstadoWemos() {
        val url = "http://$wemosIP/estado"
        val requestQueue = Volley.newRequestQueue(this)

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val estadoServo = jsonResponse.optBoolean("porton_abierto", false)
                    val anguloServo = jsonResponse.optInt("angulo_servo", 0)

                    portonAbierto = estadoServo
                    sharedPreferences.edit().putBoolean("porton_abierto", portonAbierto).apply()

                    actualizarUI()
                    Log.d("EstadoPorton", "Estado obtenido del Wemos: abierto=$estadoServo, ángulo=$anguloServo")

                } catch (e: Exception) {
                    Log.e("EstadoPorton", "Error parsing estado del Wemos", e)
                    Toast.makeText(this, "Error al obtener estado del Wemos", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("EstadoPorton", "Error obteniendo estado: ${error.message}")
                Toast.makeText(this, "No se pudo obtener el estado del portón", Toast.LENGTH_SHORT).show()
            }
        )

        requestQueue.add(request)
    }

    private fun cambiarEstadoPorton() {
        if (!conectadoWemos) {
            Toast.makeText(this, "Wemos no conectado", Toast.LENGTH_SHORT).show()
            return
        }

        // Deshabilitar botón mientras se procesa
        btnCambiar.isEnabled = false
        btnCambiar.text = if (portonAbierto) "Cerrando..." else "Abriendo..."

        val nuevoEstado = !portonAbierto
        val url = "http://$wemosIP/servo"
        val requestQueue = Volley.newRequestQueue(this)

        val jsonRequest = JSONObject().apply {
            put("accion", if (nuevoEstado) "abrir" else "cerrar")
            put("usuario", auth.currentUser?.uid ?: "unknown")
            put("timestamp", System.currentTimeMillis())
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonRequest,
            { response ->
                try {
                    val status = response.optString("status", "error")
                    val mensaje = response.optString("mensaje", "Comando ejecutado")
                    val angulo = response.optInt("angulo", 0)

                    if (status == "ok" || status == "success") {
                        portonAbierto = nuevoEstado
                        sharedPreferences.edit().putBoolean("porton_abierto", portonAbierto).apply()

                        actualizarUI()
                        registrarAccionEnFirebase(nuevoEstado)

                        Toast.makeText(this, "$mensaje (Ángulo: ${angulo}°)", Toast.LENGTH_SHORT).show()
                        Log.d("EstadoPorton", "Estado cambiado exitosamente: $nuevoEstado")

                        // Si se abrió, iniciar temporizador de cierre automático
                        if (nuevoEstado) {
                            iniciarTemporizadorCierre()
                        }

                    } else {
                        Toast.makeText(this, "Error del Wemos: $mensaje", Toast.LENGTH_SHORT).show()
                        btnCambiar.isEnabled = true
                        actualizarUI()
                    }
                } catch (e: Exception) {
                    Log.e("EstadoPorton", "Error parsing respuesta servo", e)
                    Toast.makeText(this, "Respuesta del Wemos no válida", Toast.LENGTH_SHORT).show()
                    btnCambiar.isEnabled = true
                    actualizarUI()
                }
            },
            { error ->
                Log.e("EstadoPorton", "Error cambiando estado: ${error.message}")
                Toast.makeText(this, "Error de comunicación con el Wemos", Toast.LENGTH_SHORT).show()
                btnCambiar.isEnabled = true
                actualizarUI()
            }
        )

        requestQueue.add(request)
    }

    private fun abrirPortonConTemporizador() {
        if (conectadoWemos) {
            // Abrir vía Wemos
            portonAbierto = false // Para que cambiarEstadoPorton() lo abra
            cambiarEstadoPorton()
        } else {
            // Abrir solo en UI (modo simulación)
            portonAbierto = true
            actualizarUI()
            iniciarTemporizadorCierre()
            Toast.makeText(this, "Portón abierto (modo simulación)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun iniciarTemporizadorCierre() {
        // Cancelar temporizador anterior si existe
        temporizadorActual?.cancel()

        val tiempoAbierto = 30 // 30 segundos

        temporizadorActual = object : CountDownTimer((tiempoAbierto * 1000).toLong(), 1000) {
            var segundosRestantes = tiempoAbierto

            override fun onTick(millisUntilFinished: Long) {
                segundosRestantes = (millisUntilFinished / 1000).toInt()
                textEstado.text = "ABIERTO (${segundosRestantes}s)"
            }

            override fun onFinish() {
                cerrarPortonAutomaticamente()
            }
        }

        temporizadorActual?.start()
        Log.d("EstadoPorton", "Temporizador de cierre iniciado: ${tiempoAbierto}s")
    }

    private fun cerrarPortonAutomaticamente() {
        if (portonAbierto) {
            if (conectadoWemos) {
                // Cerrar vía Wemos
                cambiarEstadoPorton()
            } else {
                // Cerrar solo en UI
                portonAbierto = false
                actualizarUI()
            }

            Toast.makeText(this, "⏰ Portón cerrado automáticamente", Toast.LENGTH_LONG).show()
            Log.d("EstadoPorton", "Portón cerrado automáticamente por temporizador")
        }
    }

    private fun registrarAccionEnFirebase(abierto: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        val accionData = mapOf(
            "accion" to if (abierto) "abrir" else "cerrar",
            "timestamp" to ServerValue.TIMESTAMP,
            "usuario" to userId,
            "metodo" to "manual",
            "ip_wemos" to wemosIP
        )

        database.child("acciones_porton").child(userId).push().setValue(accionData)
            .addOnSuccessListener {
                Log.d("EstadoPorton", "Acción registrada en Firebase")
            }
            .addOnFailureListener { error ->
                Log.e("EstadoPorton", "Error registrando acción: ${error.message}")
            }
    }

    private fun setupFirebaseListener() {
        val userId = auth.currentUser?.uid ?: return

        estadoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val comandoRemoto = snapshot.child("comando").getValue(String::class.java)
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0

                    // Solo procesar comandos recientes (últimos 10 segundos)
                    val ahora = System.currentTimeMillis()
                    if (ahora - timestamp < 10000 && comandoRemoto != null) {
                        when (comandoRemoto) {
                            "abrir" -> if (!portonAbierto) cambiarEstadoPorton()
                            "cerrar" -> if (portonAbierto) cambiarEstadoPorton()
                        }

                        // Limpiar comando procesado
                        snapshot.ref.removeValue()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EstadoPorton", "Error escuchando comandos remotos: ${error.message}")
            }
        }

        database.child("comandos_remotos").child(userId)
            .addValueEventListener(estadoListener!!)
    }

    private fun actualizarUI() {
        runOnUiThread {
            btnCambiar.isEnabled = true

            if (portonAbierto) {
                textEstado.text = "ABIERTO"
                textEstado.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                imgPorton.setImageResource(R.drawable.porton_abierto)
                btnCambiar.text = "Cerrar portón"
            } else {
                textEstado.text = "CERRADO"
                textEstado.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                imgPorton.setImageResource(R.drawable.porton_cerrado)
                btnCambiar.text = "Abrir portón"
            }

            // Mostrar estado de conexión en el título
            if (conectadoWemos) {
                textTitulo.text = "Estado del Portón ✓"
                textTitulo.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            } else {
                textTitulo.text = "Estado del Portón (Desconectado)"
                textTitulo.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
        }
    }

    private fun setupNavigation() {
        val navbar = findViewById<LinearLayout>(R.id.navbar)

        try {
            val iconPorton = navbar.findViewById<ImageView>(R.id.porton)
            val iconHome = navbar.findViewById<ImageView>(R.id.home)
            val iconRadar = navbar.findViewById<ImageView>(R.id.radar)
            val iconAjuste = navbar.findViewById<ImageView>(R.id.ajustes)

            // Configurar underlines si existen
            try {
                val underlinePorton = navbar.findViewById<View>(R.id.underline_porton)
                val underlineHome = navbar.findViewById<View>(R.id.underline_home)
                val underlineRadar = navbar.findViewById<View>(R.id.underline_radar)
                val underlineAjuste = navbar.findViewById<View>(R.id.underline_ajuste)

                // Activar tab actual (portón)
                underlinePorton?.visibility = View.VISIBLE
                underlineHome?.visibility = View.GONE
                underlineRadar?.visibility = View.GONE
                underlineAjuste?.visibility = View.GONE
            } catch (e: Exception) {
                Log.w("EstadoPorton", "Underlines no encontrados")
            }

            iconHome?.setOnClickListener {
                startActivity(Intent(this, ConexionPortonActivity::class.java))
            }

            iconRadar?.setOnClickListener {
                startActivity(Intent(this, MovimientoDetectadoActivity::class.java))
            }

            iconAjuste?.setOnClickListener {
                startActivity(Intent(this, AjustesActivity::class.java))
            }

        } catch (e: Exception) {
            Log.w("EstadoPorton", "Error configurando navegación: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        // Verificar conexión y estado al volver a la actividad
        verificarConexionWemos()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar recursos
        temporizadorActual?.cancel()
        estadoListener?.let {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                database.child("comandos_remotos").child(userId).removeEventListener(it)
            }
        }
        Log.d("EstadoPorton", "Activity destruida, recursos liberados")
    }
}