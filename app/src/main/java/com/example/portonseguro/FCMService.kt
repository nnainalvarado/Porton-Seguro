// FCMService.kt
package com.example.portonseguro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Mensaje recibido: ${remoteMessage.data}")

        // Verificar si es una notificación de movimiento
        if (remoteMessage.data["tipo"] == "movimiento_detectado") {
            mostrarNotificacionMovimiento(remoteMessage.data)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token: $token")

        // Guardar el token en Firebase para este usuario
        guardarTokenEnFirebase(token)
    }

    private fun mostrarNotificacionMovimiento(data: Map<String, String>) {
        val hora = data["hora"] ?: "Ahora"
        val dispositivo = data["dispositivo"] ?: "Portón"

        // Crear intent para abrir la actividad
        val intent = Intent(this, MovimientoDetectadoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("desde_notificacion", true)
            putExtra("hora_deteccion", hora)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crear canal de notificación (Android 8+)
        createNotificationChannel()

        // Construir notificación
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.candado) // Cambia por tu icono
            .setContentTitle("🚨 Movimiento Detectado")
            .setContentText("Actividad frente al $dispositivo a las $hora")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Se ha detectado movimiento frente al $dispositivo a las $hora. Toca para ver las opciones de control."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Reproducir sonido de alarma
        reproducirAlarma()
    }

    private fun reproducirAlarma() {
        try {
            val mediaPlayer = MediaPlayer.create(this, R.raw.alarma)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { mp ->
                mp.release()
            }
        } catch (e: Exception) {
            Log.e("FCM", "Error reproduciendo alarma: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Detección de Movimiento",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando se detecta movimiento en el portón"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun guardarTokenEnFirebase(token: String) {
        // Implementar guardado del token FCM en Firebase
        // Esto permitirá enviar notificaciones específicas a este dispositivo
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            database.child("usuarios").child(currentUser.uid).child("fcm_token").setValue(token)
        }
    }

    companion object {
        private const val CHANNEL_ID = "movimiento_channel"
        private const val NOTIFICATION_ID = 1001
    }
}