package com.example.portonseguro

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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class conexionPortonActivity : ComponentActivity() {
    private var conectado = false

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
