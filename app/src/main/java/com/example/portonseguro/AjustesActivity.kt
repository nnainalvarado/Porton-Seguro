package com.example.portonseguro

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import java.util.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.portonseguro.R.id.navbar

class AjustesActivity : AppCompatActivity() {

    private lateinit var temporizadorInput: EditText
    private lateinit var temporizadorActual: TextView
    private lateinit var guardarButton: Button

    companion object {
        const val PREFS_NAME = "PortonPrefs"
        const val TEMPORIZADOR_KEY = "temporizador"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        temporizadorInput = findViewById(R.id.temporizadorInput)
        temporizadorActual = findViewById(R.id.temporizadorActual)
        guardarButton = findViewById(R.id.guardarButton)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val temporizadorGuardado = prefs.getInt(TEMPORIZADOR_KEY, -1)

        if (temporizadorGuardado > 0) {
            temporizadorActual.text = "Temporizador actual: $temporizadorGuardado segundos"
        }

        guardarButton.setOnClickListener {
            val input = temporizadorInput.text.toString()
            if (input.isNotEmpty()) {
                val nuevoValor = input.toIntOrNull()
                if (nuevoValor != null && nuevoValor > 0) {

                    prefs.edit().putInt(TEMPORIZADOR_KEY, nuevoValor).apply()
                    temporizadorActual.text = "Temporizador actual: $nuevoValor segundos"
                    Toast.makeText(this, "Temporizador guardado correctamente", Toast.LENGTH_SHORT).show()
                    temporizadorInput.text.clear()
                } else {
                    Toast.makeText(this, "Ingresa un número válido mayor a 0", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor ingresa un número", Toast.LENGTH_SHORT).show()
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
}
