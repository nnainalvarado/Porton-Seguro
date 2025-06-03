package com.example.portonseguro

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.portonseguro.R.id.navbar
import android.os.CountDownTimer
import android.os.Handler



class AjustesActivity : AppCompatActivity() {

    private var countDownTimer: CountDownTimer? = null
    private lateinit var temporizadorInput: EditText
    private lateinit var temporizadorActual: TextView
    private lateinit var guardarButton: Button

    companion object {
        const val PREFS_NAME = "PortonPrefs"
        const val TEMPORIZADOR_KEY = "temporizador"
        const val CANAL_ID = "canal_temporizador"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        crearCanalNotificaciones()

        temporizadorInput = findViewById(R.id.temporizadorInput)
        temporizadorActual = findViewById(R.id.temporizadorActual)
        guardarButton = findViewById(R.id.guardarButton)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val temporizadorGuardado = prefs.getInt(TEMPORIZADOR_KEY, -1)

        if (temporizadorGuardado > 0) {
            temporizadorActual.text = "Temporizador actual:\n$temporizadorGuardado segundos"
        }

        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                val restante = prefs.getInt("temporizador_restante", -1)
                if (restante > 0) {
                    temporizadorActual.text = "Temporizador actual: $restante segundos"
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)


        guardarButton.setOnClickListener {
            val input = temporizadorInput.text.toString()
            if (input.isNotEmpty()) {
                val nuevoValor = input.toIntOrNull()
                if (nuevoValor != null && nuevoValor > 0) {
                    prefs.edit().putInt(TEMPORIZADOR_KEY, nuevoValor).apply()
                    temporizadorActual.text = "Temporizador actual: $nuevoValor segundos"
                    Toast.makeText(this, "Temporizador guardado correctamente", Toast.LENGTH_SHORT).show()

                    temporizadorInput.text.clear()

                    countDownTimer?.cancel()

                    countDownTimer = object : CountDownTimer(nuevoValor * 1000L, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            val segundosRestantes = millisUntilFinished / 1000
                            temporizadorActual.text = "Temporizador actual: $segundosRestantes segundos"
                        }

                        override fun onFinish() {
                            temporizadorActual.text = "Temporizador finalizado"
                            Toast.makeText(applicationContext, "¡Se ha cumplido el tiempo configurado!", Toast.LENGTH_LONG).show()
                        }
                    }.start()
                } else {
                    Toast.makeText(this, "Ingresa un número válido mayor a 0", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor ingresa un número", Toast.LENGTH_SHORT).show()
            }
        }


        val navbar = findViewById<LinearLayout>(navbar)
        val iconPorton = navbar.findViewById<ImageView>(R.id.porton)
        val iconHome = navbar.findViewById<ImageView>(R.id.home)
        val iconRadar = navbar.findViewById<ImageView>(R.id.radar)
        val iconAjuste = navbar.findViewById<ImageView>(R.id.ajustes)

        val underlinePorton = navbar.findViewById<View>(R.id.underline_porton)
        val underlineHome = navbar.findViewById<View>(R.id.underline_home)
        val underlineRadar = navbar.findViewById<View>(R.id.underline_radar)
        val underlineAjuste = navbar.findViewById<View>(R.id.underline_ajuste)

        fun setActive(tab: String) {
            underlinePorton.visibility = if (tab == "porton") View.VISIBLE else View.GONE
            underlineHome.visibility = if (tab == "home") View.VISIBLE else View.GONE
            underlineRadar.visibility = if (tab == "radar") View.VISIBLE else View.GONE
            underlineAjuste.visibility = if (tab == "ajustes") View.VISIBLE else View.GONE
        }

        setActive("ajustes")

        iconPorton.setOnClickListener {
            startActivity(Intent(this, EstadoPortonActivity::class.java))
        }

        iconHome.setOnClickListener {
            startActivity(Intent(this, conexionPortonActivity::class.java))
        }

        iconRadar.setOnClickListener {
            startActivity(Intent(this, MovimientoDetectadoActivity::class.java))
        }

        iconAjuste.setOnClickListener {
            startActivity(Intent(this, AjustesActivity::class.java))
        }
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nombre = "Canal Temporizador"
            val descripcion = "Notifica al terminar el temporizador"
            val importancia = NotificationManager.IMPORTANCE_DEFAULT
            val canal = NotificationChannel(CANAL_ID, nombre, importancia).apply {
                description = descripcion
            }
            val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }

    private fun mostrarNotificacion() {
        val builder = NotificationCompat.Builder(this, CANAL_ID)
            .setContentTitle("Temporizador terminado")
            .setContentText("¡Se ha cumplido el tiempo configurado!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build())
        }
    }
}
