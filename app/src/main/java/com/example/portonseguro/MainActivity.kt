package com.example.portonseguro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PortonSeguroApp()
        }
    }
}

@Composable
fun PortonSeguroApp() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "login") {
        composable("login") { loginActivity(navController) }
        composable("registrar") { RegistrarActivity(navController) }
    }
}
