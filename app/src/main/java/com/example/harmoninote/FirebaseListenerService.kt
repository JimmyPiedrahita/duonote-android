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

    private val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            NoteWidget.updateWidget(applicationContext)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("FirebaseService", "Error: ${error.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        qrDataStore = QRDataStore(this)

        // Obtener el valor del QR desde DataStore
        serviceScope.launch {
            val contentQR = qrDataStore.qrContent.first() ?: "default"
            databaseReference =
                FirebaseDatabase.getInstance().getReference("Connections").child(contentQR)
                    .child("Notes")
            databaseReference.addValueEventListener(listener)
        }

        startForegroundServiceWithNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::databaseReference.isInitialized) {
            databaseReference.removeEventListener(listener)
        }
        serviceScope.cancel()
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
