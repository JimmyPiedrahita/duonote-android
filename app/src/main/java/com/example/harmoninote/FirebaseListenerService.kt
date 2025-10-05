package com.example.harmoninote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FirebaseListenerService : Service() {
    private lateinit var databaseReference: DatabaseReference
    private lateinit var qrDataStore: QRDataStore
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentQRContent: String? = null

    private val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d("FirebaseService", "Data changed - updating widget")
            NoteWidget.updateWidget(applicationContext)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("FirebaseService", "Error: ${error.message}")
            // Reintentar la conexión después de un error
            serviceScope.launch {
                android.os.Handler(mainLooper).postDelayed({
                    setupFirebaseListener()
                }, 3000) // Reintentar después de 3 segundos
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        qrDataStore = QRDataStore(this)

        // Habilitar persistencia de Firebase (mantiene datos locales sincronizados)
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.w("FirebaseService", "Persistence already enabled or failed: ${e.message}")
        }

        // Configurar listener de Firebase
        setupFirebaseListener()

        // Monitorear cambios en el QR guardado
        serviceScope.launch {
            qrDataStore.qrContent.collect { newQR ->
                if (newQR != null && newQR != currentQRContent) {
                    Log.d("FirebaseService", "QR changed from $currentQRContent to $newQR")
                    currentQRContent = newQR
                    // Remover listener anterior si existe
                    if (::databaseReference.isInitialized) {
                        databaseReference.removeEventListener(listener)
                    }
                    // Configurar nuevo listener
                    setupFirebaseListener()
                }
            }
        }

        startForegroundServiceWithNotification()
    }

    private fun setupFirebaseListener() {
        serviceScope.launch {
            val contentQR = qrDataStore.qrContent.first() ?: "default"
            Log.d("FirebaseService", "Setting up listener for QR: $contentQR")

            val database = FirebaseDatabase.getInstance()
            // Mantener la conexión activa
            database.goOnline()

            databaseReference = database.getReference("Connections")
                .child(contentQR)
                .child("Notes")

            // Mantener sincronización local activa
            databaseReference.keepSynced(true)

            // Agregar listener
            databaseReference.addValueEventListener(listener)

            Log.d("FirebaseService", "Listener attached successfully")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("FirebaseService", "Service destroying")
        if (::databaseReference.isInitialized) {
            databaseReference.removeEventListener(listener)
        }
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Asegurar que el servicio se reinicie si es eliminado por el sistema
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceWithNotification() {
        val channelId = "notes_channel"
        val channelName = "Notes in the background"

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Active notes widget")
            .setContentText("Synchronizing notes in real time...")
            .setSmallIcon(R.drawable.ic_add_note)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }
}
