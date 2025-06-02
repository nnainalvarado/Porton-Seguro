package com.example.portonseguro

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class registrarActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar)

        auth = FirebaseAuth.getInstance()

        val emailRegister = findViewById<EditText>(R.id.emailRegister)
        val passwordRegister = findViewById<EditText>(R.id.passwordRegister)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val email = emailRegister.text.toString()
            val password = passwordRegister.text.toString()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Error al registrarse", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
