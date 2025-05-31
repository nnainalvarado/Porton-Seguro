package com.example.portonseguro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class registrarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar)

        val emailEditText = findViewById<EditText>(R.id.editTextEmailRegister)
        val passwordEditText = findViewById<EditText>(R.id.editTextPasswordRegister)
        val confirmEditText = findViewById<EditText>(R.id.editTextConfirmPassword)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirm = confirmEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && password == confirm) {
                // Aquí va tu lógica de registro
                Toast.makeText(this, "Usuario registrado", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                Toast.makeText(this, "Verifica los datos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
