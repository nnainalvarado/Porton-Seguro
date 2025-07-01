package com.example.portonseguro

import android.widget.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
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

class navbarActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.navbar)

        try {
            val porton = findViewById<ImageView>(R.id.porton)
            val home = findViewById<ImageView>(R.id.home)
            val radar = findViewById<ImageView>(R.id.radar)

            porton?.setOnClickListener {
                startActivity(Intent(this, EstadoPortonActivity::class.java))
            }
            home?.setOnClickListener {
                startActivity(Intent(this, ConexionPortonActivity::class.java))
            }
            radar?.setOnClickListener {
                startActivity(Intent(this, MovimientoDetectadoActivity::class.java))
            }

        } catch (e: Exception) {
            Log.e("navbarActivity", "Error al inicializar navbar: ${e.message}")
            e.printStackTrace()
        }
    }

}
