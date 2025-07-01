// index.js para Cloud Functions
        const functions = require("firebase-functions");
        const admin = require("firebase-admin");
        admin.initializeApp();

        exports.sendMovementNotification = functions.database
            .ref("/movimientos/{userId}/{pushId}")
            .onCreate(async (snapshot, context) => {
                const userId = context.params.userId;
                const movimientoData = snapshot.val();

                // Obtener el token FCM del usuario
                const userTokenSnapshot = await admin.database()
                    .ref(`/users/${userId}/fcmToken`).once("value");
                if (!userTokenSnapshot.exists()) {
                    console.log("No FCM token found for