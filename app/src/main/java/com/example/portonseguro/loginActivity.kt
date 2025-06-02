package com.example.portonseguro

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailLogin = findViewById<EditText>(R.id.emailLogin)
        val passwordLogin = findViewById<EditText>(R.id.passwordLogin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoToRegister = findViewById<Button>(R.id.btnGoToRegister)

        btnLogin.setOnClickListener {
            val email = emailLogin.text.toString()
            val password = passwordLogin.text.toString()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        startActivity(Intent(this, MovimientoDetectadoActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, registrarActivity::class.java))
        }
    }
}


