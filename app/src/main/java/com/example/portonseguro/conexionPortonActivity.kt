package com.example.portonseguro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.portonseguro.ui.theme.PortonSeguroTheme
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class conexionPortonActivity : AppCompatActivity() {
    private var conectado = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conexion_porton)

        val btnConectar = findViewById<Button>(R.id.btnConectar)
        val btnDesconectar = findViewById<Button>(R.id.btnDesconectar)
        val estadoConexion = findViewById<TextView>(R.id.estadoConexion)

        actualizarEstado(estadoConexion, btnConectar, btnDesconectar)

        btnConectar.setOnClickListener {
            conectado = true
            actualizarEstado(estadoConexion, btnConectar, btnDesconectar)
        }

        btnDesconectar.setOnClickListener {
            conectado = false
            actualizarEstado(estadoConexion, btnConectar, btnDesconectar)
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

        setActive("home")

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

    private fun actualizarEstado(estadoConexion: TextView, btnConectar: Button, btnDesconectar: Button) {
        if (conectado) {
            estadoConexion.text = "Portón conectado"
            btnConectar.visibility = View.GONE
            btnDesconectar.visibility = View.VISIBLE
        } else {
            estadoConexion.text = "Portón seguro desconectado"
            btnConectar.visibility = View.VISIBLE
            btnDesconectar.visibility = View.GONE
        }
    }
}
