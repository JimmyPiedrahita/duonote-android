package com.example.duonote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val appContext = context.applicationContext
            val qrContent = runBlocking { QRDataStore(appContext).qrContent.first() }
            if (!qrContent.isNullOrBlank()) {
                // Android 15+ no permite iniciar un foreground service dataSync
                // desde BOOT_COMPLETED. WorkManager se encarga de la sincronización
                // periódica y de refrescar el widget.
                BackgroundSyncScheduler.schedule(appContext)
            }
        }
    }
}
