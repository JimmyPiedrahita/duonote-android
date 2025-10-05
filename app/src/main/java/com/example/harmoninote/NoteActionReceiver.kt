package com.example.harmoninote

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import androidx.core.content.edit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

class NoteActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra("NOTE_ID") ?: return

        when (intent.action) {
            "ACTION_COPY_TEXT" -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val textNoteCopy = intent.getStringExtra("NOTE_TEXT")
                val clip = ClipData.newPlainText("Copied note", textNoteCopy)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied text", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Obtener el valor del QR desde DataStore
                val qrDataStore = QRDataStore(context)
                val contentQR = runBlocking {
                    qrDataStore.qrContent.first() ?: "default"
                }

                val dbRef = FirebaseDatabase.getInstance().getReference("Connections").child(contentQR).child("Notes").child(noteId)
                val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                val lastClickTime = prefs.getLong("last_click_time_$noteId", 0L)
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastClickTime < 500) {
                    dbRef.removeValue()
                } else {
                    dbRef.child("IsCompleted").get().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val isCompleted = task.result?.getValue(Boolean::class.java) == true
                            dbRef.child("IsCompleted").setValue(!isCompleted)
                        }
                    }
                    prefs.edit { putLong("last_click_time_$noteId", currentTime) }
                }
            }
        }
        NoteWidget.updateWidget(context)
    }
}