package com.example.portonseguro

import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import com.example.portonseguro.ui.theme.PortonSeguroTheme

class EstadoPortonActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estado_porton) // Asegúrate que este nombre coincida

        // Referencias a los elementos XML
        val imgPorton = findViewById<ImageView>(R.id.imgEstadoPorton)
        val textEstado = findViewById<TextView>(R.id.textEstado)
        val btnCambiar = findViewById<Button>(R.id.btnCambiarEstado)
        val composeView = findViewById<ComposeView>(R.id.composeView)

        var portonAbierto = true

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

        // Inserta el Composable en el ComposeView
        composeView.setContent {
            PortonSeguroTheme {
                MensajeExtraComposable()
            }
        }
    }
}

@Composable
fun MensajeExtraComposable() {
    Text(
        text = "Vista combinada: XML + Jetpack Compose",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary
    )
}
