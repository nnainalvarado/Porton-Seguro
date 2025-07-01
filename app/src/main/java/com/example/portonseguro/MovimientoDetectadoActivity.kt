package com.example.portonseguro

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*
import android.os.CountDownTimer
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.util.Log
import android.os.Handler
import android.os.Looper

class MovimientoDetectadoActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private var countDownTimer: CountDownTimer? = null
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var movimientoListener: ValueEventListener? = null
    private val handler = Handler(Looper.getMainLooper())

    // Referencias a las vistas
    private lateinit var textMensaje: TextView
    private lateinit var textUltimaDeteccion: TextView
    private lateinit var textEstadoConexion: TextView
    private lateinit var imageEstadoMovimiento: ImageView
    private lateinit var layoutOpcionesRespuesta: LinearLayout
    private lateinit var btnSimular: Button
    private lateinit var btnIrEstado: Button
    private lateinit var btnConfirmarMovimiento: Button
    private lateinit var btnIgnorarMovimiento: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movimiento_detectado)

        // Inicializar Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        initializeViews()
        setupClickListeners()
        checkIfFromNotification()
        escucharMovimientosEnTiempoReal()
        actualizarEstadoConexion()
    }

    private fun initializeViews() {
        textMensaje = findViewById(R.id.textMensaje)
        textUltimaDeteccion = findViewById(R.id.textUltimaDeteccion)
        textEstadoConexion = findViewById(R.id.textEstadoConexion)
        imageEstadoMovimiento = findViewById(R.id.imageEstadoMovimiento)
        layoutOpcionesRespuesta = findViewById(R.id.layoutOpcionesRespuesta)
        btnSimular = findViewById(R.id.btnSimularMovimiento)
        btnIrEstado = findViewById(R.id.btnIrEstado)
        btnConfirmarMovimiento = findViewById(R.id.btnConfirmarMovimiento)
        btnIgnorarMovimiento = findViewById(R.id.btnIgnorarMovimiento)
    }

    private fun setupClickListeners() {
        btnSimular.setOnClickListener {
            simularMovimiento()
        }

        btnIrEstado.setOnClickListener {
            startActivity(Intent(this, EstadoPortonActivity::class.java))
        }

        btnConfirmarMovimiento.setOnClickListener {
            confirmarMovimiento()
        }

        btnIgnorarMovimiento.setOnClickListener {
            ignorarMovimiento()
        }
    }

    private fun checkIfFromNotification() {
        // Verificar si se abrió desde una notificación
        if (intent.getBooleanExtra("desde_notificacion", false)) {
            val horaDeteccion = intent.getStringExtra("hora_deteccion") ?: "Ahora"
            mostrarDeteccionMovimiento("Notificación recibida a las $horaDeteccion", horaDeteccion)
        }
    }

    private fun simularMovimiento() {
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        formatoHora.timeZone = TimeZone.getTimeZone("America/Santiago")
        val horaActual = formatoHora.format(Date())

        mostrarDeteccionMovimiento("🚨 Movimiento simulado detectado", horaActual)
        reproducirAlarma()
        enviarMovimientoAFirebase("simulado")
    }

    private fun mostrarDeteccionMovimiento(mensaje: String, hora: String) {
        // Actualizar mensaje principal
        textMensaje.text = "$mensaje\na las $hora"
        textMensaje.setTextColor(resources.getColor(android.R.color.holo_orange_dark))

        // Mostrar imagen de estado
        imageEstadoMovimiento.visibility = View.VISIBLE
        imageEstadoMovimiento.setImageResource(R.drawable.candado) // Cambia por tu icono de alerta

        // Actualizar última detección
        textUltimaDeteccion.text = "Última detección: $hora"

        // Mostrar opciones de respuesta
        mostrarOpcionesRespuesta()

        // Mostrar botón para ir al estado
        btnIrEstado.visibility = View.VISIBLE

        // Cambiar texto del botón de simular
        btnSimular.text = "🔄 Simular Otro Movimiento"
    }

    private fun mostrarOpcionesRespuesta() {
        layoutOpcionesRespuesta.visibility = View.VISIBLE

        // Animación simple para mostrar las opciones
        layoutOpcionesRespuesta.alpha = 0f
        layoutOpcionesRespuesta.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun ocultarOpcionesRespuesta() {
        layoutOpcionesRespuesta.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                layoutOpcionesRespuesta.visibility = View.GONE
            }
            .start()
    }

    private fun confirmarMovimiento() {
        Toast.makeText(this, "✅ Movimiento confirmado. Abriendo portón...", Toast.LENGTH_SHORT).show()

        // Cambiar mensaje
        textMensaje.text = "Movimiento confirmado. Redirigiendo..."
        textMensaje.setTextColor(resources.getColor(android.R.color.holo_green_dark))

        // Registrar confirmación en Firebase
        registrarRespuestaMovimiento("confirmado")

        // Ocultar opciones
        ocultarOpcionesRespuesta()

        // Ir a EstadoPortonActivity después de un pequeño delay
        handler.postDelayed({
            val intent = Intent(this, EstadoPortonActivity::class.java)
            intent.putExtra("abrirPorton", true)
            startActivity(intent)
            finish()
        }, 1500)
    }

    private fun ignorarMovimiento() {
        Toast.makeText(this, "❌ Movimiento ignorado", Toast.LENGTH_SHORT).show()

        // Cambiar mensaje
        textMensaje.text = "Movimiento ignorado. Sistema en espera..."
        textMensaje.setTextColor(resources.getColor(android.R.color.darker_gray))

        // Registrar que se ignoró en Firebase
        registrarRespuestaMovimiento("ignorado")

        // Ocultar opciones
        ocultarOpcionesRespuesta()

        // Ocultar imagen de estado
        imageEstadoMovimiento.visibility = View.GONE

        // Resetear botón de simular
        btnSimular.text = "🔄 Simular Movimiento"
    }

    private fun escucharMovimientosEnTiempoReal() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w("MovimientoDetectado", "Usuario no autenticado")
            textEstadoConexion.text = "🔴 Sin autenticar"
            return
        }

        textEstadoConexion.text = "🟡 Conectando..."

        movimientoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val movimiento = snapshot.child("movimiento").getValue(Boolean::class.java) ?: false
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0
                val tipo = snapshot.child("tipo").getValue(String::class.java) ?: "desconocido"

                textEstadoConexion.text = "🟢 Conectado"

                if (movimiento && timestamp > 0) {
                    val fecha = Date(timestamp)
                    val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
                    formatoHora.timeZone = TimeZone.getTimeZone("America/Santiago")
                    val horaDeteccion = formatoHora.format(fecha)

                    // Solo mostrar si no es el mismo movimiento que ya se está mostrando
                    if (tipo != "simulado") {
                        mostrarDeteccionEnTiempoReal(horaDeteccion)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("MovimientoDetectado", "Error escuchando movimientos: ${error.message}")
                textEstadoConexion.text = "🔴 Error conexión"
            }
        }

        database.child("dispositivos").child("porton_principal").child("ultimo_movimiento")
            .addValueEventListener(movimientoListener!!)
    }

    private fun mostrarDeteccionEnTiempoReal(hora: String) {
        mostrarDeteccionMovimiento("🚨 Movimiento detectado en tiempo real", hora)
        reproducirAlarma()
    }

    private fun reproducirAlarma() {
        try {
            // Liberar MediaPlayer anterior si existe
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.release()
            }

            mediaPlayer = MediaPlayer.create(this, R.raw.alarma)
            mediaPlayer?.start()

            // Parar automáticamente después de 3 segundos
            handler.postDelayed({
                try {
                    if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                        mediaPlayer.release()
                    }
                } catch (e: Exception) {
                    Log.e("MovimientoDetectado", "Error liberando MediaPlayer: ${e.message}")
                }
            }, 3000)

        } catch (e: Exception) {
            Log.e("MovimientoDetectado", "Error reproduciendo alarma: ${e.message}")
        }
    }

    private fun enviarMovimientoAFirebase(tipo: String) {
        val currentUser = auth.currentUser ?: return

        val movimientoData = mapOf(
            "movimiento" to true,
            "timestamp" to System.currentTimeMillis(),
            "tipo" to tipo,
            "usuario" to currentUser.uid
        )

        database.child("dispositivos").child("porton_principal").child("ultimo_movimiento")
            .setValue(movimientoData)
            .addOnSuccessListener {
                Log.d("MovimientoDetectado", "Movimiento enviado a Firebase")
            }
            .addOnFailureListener { error ->
                Log.e("MovimientoDetectado", "Error enviando movimiento: ${error.message}")
                textEstadoConexion.text = "🔴 Error envío"
            }
    }

    private fun registrarRespuestaMovimiento(respuesta: String) {
        val currentUser = auth.currentUser ?: return

        val respuestaData = mapOf(
            "respuesta" to respuesta,
            "timestamp" to System.currentTimeMillis(),
            "usuario" to currentUser.uid
        )

        database.child("respuestas_movimiento").push().setValue(respuestaData)
            .addOnSuccessListener {
                Log.d("MovimientoDetectado", "Respuesta registrada: $respuesta")
            }
            .addOnFailureListener { error ->
                Log.e("MovimientoDetectado", "Error registrando respuesta: ${error.message}")
            }
    }

    private fun actualizarEstadoConexion() {
        // Verificar conexión con Firebase
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    textEstadoConexion.text = "🟢 Firebase conectado"
                } else {
                    textEstadoConexion.text = "🔴 Sin conexión"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                textEstadoConexion.text = "🔴 Error conexión"
            }
        })
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

    override fun onDestroy() {
        super.onDestroy()

        // Limpiar listener
        movimientoListener?.let {
            database.child("dispositivos").child("porton_principal").child("ultimo_movimiento")
                .removeEventListener(it)
        }

        // Limpiar MediaPlayer
        if (::mediaPlayer.isInitialized) {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                Log.e("MovimientoDetectado", "Error liberando MediaPlayer: ${e.message}")
            }
        }

        countDownTimer?.cancel()
    }
}