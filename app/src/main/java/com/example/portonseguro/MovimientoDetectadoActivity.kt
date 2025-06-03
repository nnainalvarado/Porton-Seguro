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

class MovimientoDetectadoActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movimiento_detectado)

        val btnSimular = findViewById<Button>(R.id.btnSimularMovimiento)
        val mensaje = findViewById<TextView>(R.id.textMensaje)
        val btnIrEstado = findViewById<Button>(R.id.btnIrEstado)

        btnSimular.setOnClickListener {
            val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
            formatoHora.timeZone = TimeZone.getTimeZone("America/Santiago")
            val horaActual = formatoHora.format(Date())

            mensaje.text = "Movimiento detectado frente al portón a las $horaActual.\nPresiona para abrir."
            btnIrEstado.visibility = View.VISIBLE

            mediaPlayer = MediaPlayer.create(this, R.raw.alarma)
            mediaPlayer.start()
            btnSimular.postDelayed({
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                    mediaPlayer.release()
                }
            }, 2000)

            val intent = Intent(this, EstadoPortonActivity::class.java)
            intent.putExtra("abrirPorton", true)
            startActivity(intent)
        }

        btnIrEstado.setOnClickListener {
            startActivity(Intent(this, EstadoPortonActivity::class.java))
        }

        val navbar = findViewById<LinearLayout>(R.id.navbar)
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

        setActive("radar")

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
}
