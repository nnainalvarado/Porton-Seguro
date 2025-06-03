package com.example.portonseguro
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import com.example.portonseguro.ui.theme.PortonSeguroTheme
import android.os.CountDownTimer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager




class EstadoPortonActivity : ComponentActivity() {

    private var portonAbierto = false

    private fun abrirPortonUI() {
        portonAbierto = true

        val imgPorton = findViewById<ImageView>(R.id.imgEstadoPorton)
        val textEstado = findViewById<TextView>(R.id.textEstado)
        val btnCambiar = findViewById<Button>(R.id.btnCambiarEstado)

        textEstado.text = "ABIERTO"
        textEstado.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        imgPorton.setImageResource(R.drawable.porton_abierto)
        btnCambiar.text = "Cerrar portón"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estado_porton)

        val imgPorton = findViewById<ImageView>(R.id.imgEstadoPorton)
        val textEstado = findViewById<TextView>(R.id.textEstado)
        val btnCambiar = findViewById<Button>(R.id.btnCambiarEstado)

        val localBroadcastManager = LocalBroadcastManager.getInstance(this)

        portonAbierto = intent.getBooleanExtra("abrirPorton", false)
        if (portonAbierto) {
            abrirPortonUI()

            val prefs = getSharedPreferences("PortonPrefs", MODE_PRIVATE)
            prefs.edit().putInt("temporizador_restante", 30).apply()

            object : CountDownTimer(30000, 1000) {
                var segundosRestantes = 30
                override fun onTick(millisUntilFinished: Long) {
                    segundosRestantes--
                    prefs.edit().putInt("temporizador_restante", segundosRestantes).apply()
                }

                override fun onFinish() {
                    prefs.edit().putInt("temporizador_restante", 0).apply()
                    val intentCerrar = Intent("CERRAR_PORTON_AUTO")
                    localBroadcastManager.sendBroadcast(intentCerrar)
                    Toast.makeText(this@EstadoPortonActivity, "Portón cerrado automáticamente", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }

        btnCambiar.setOnClickListener {
            portonAbierto = !portonAbierto

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
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                portonAbierto = false
                textEstado.text = "CERRADO"
                textEstado.setTextColor(ContextCompat.getColor(this@EstadoPortonActivity, android.R.color.holo_red_dark))
                imgPorton.setImageResource(R.drawable.porton_cerrado)
                btnCambiar.text = "Abrir portón"
                Toast.makeText(this@EstadoPortonActivity, "Temporizador finalizado, portón cerrado automáticamente", Toast.LENGTH_SHORT).show()
            }
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

        setActive("porton")

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
