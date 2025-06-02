package com.example.portonseguro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MovimientoDetectadoActivity : AppCompatActivity() {

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
        }

        btnIrEstado.setOnClickListener {
            startActivity(Intent(this, EstadoPortonActivity::class.java))
        }
    }
}
