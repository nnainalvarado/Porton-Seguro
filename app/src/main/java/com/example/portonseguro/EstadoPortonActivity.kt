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

class EstadoPortonActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estado_porton)

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

        val navbar = findViewById<LinearLayout>(R.id.navbar)
        val iconPorton = navbar.findViewById<ImageView>(R.id.porton)
        val iconHome = navbar.findViewById<ImageView>(R.id.home)
        val iconRadar = navbar.findViewById<ImageView>(R.id.radar)

        val underlinePorton = navbar.findViewById<View>(R.id.underline_porton)
        val underlineHome = navbar.findViewById<View>(R.id.underline_home)
        val underlineRadar = navbar.findViewById<View>(R.id.underline_radar)

        fun setActive(tab: String) {
            underlinePorton.visibility = if (tab == "porton") View.VISIBLE else View.GONE
            underlineHome.visibility = if (tab == "home") View.VISIBLE else View.GONE
            underlineRadar.visibility = if (tab == "radar") View.VISIBLE else View.GONE
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

        composeView.setContent {
            PortonSeguroTheme {
                MensajeExtraComposable()
            }
        }
    }
}

annotation class preview

@Composable
fun MensajeExtraComposable() {
    Text(
        text = "Vista combinada: XML + Jetpack Compose",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary
    )
}
