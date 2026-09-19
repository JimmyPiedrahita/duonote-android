package com.example.duonote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val appContext = context.applicationContext
            val qrContent = runBlocking { QRDataStore(appContext).qrContent.first() }
            if (!qrContent.isNullOrBlank()) {
                BackgroundSyncScheduler.schedule(appContext)
                val serviceIntent = Intent(appContext, FirebaseListenerService::class.java)
                ContextCompat.startForegroundService(appContext, serviceIntent)
            }
        }
    }
}
