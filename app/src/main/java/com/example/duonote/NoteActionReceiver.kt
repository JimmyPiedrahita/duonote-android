package com.example.duonote

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
        val noteId = intent.getStringExtra("NOTE_ID")

        when (intent.action) {
            "ACTION_OPEN_LINK" -> {
                var url = intent.getStringExtra("NOTE_URL")
                if (!url.isNullOrEmpty()) {
                    // Ensure URL has a scheme
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "ACTION_COPY_TEXT" -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val textNoteCopy = intent.getStringExtra("NOTE_TEXT")
                val clip = ClipData.newPlainText("Copied note", textNoteCopy)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Texto copiado", Toast.LENGTH_SHORT).show()
            }
            else -> {
                if (noteId == null) return
                val qrDataStore = QRDataStore(context)
                val contentQR = runBlocking {
                    qrDataStore.qrContent.first() ?: "default"
                }

                val dbRef = FirebaseDatabase.getInstance().getReference("Connections").child(contentQR).child("Notes").child(noteId)
                val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                val lastClickTime = prefs.getLong("last_click_time_$noteId", 0L)
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastClickTime < 500) {
                    prefs.edit { putBoolean("pending_delete_$noteId", true) }
                    
                    dbRef.removeValue().addOnCompleteListener {
                        prefs.edit { 
                            remove("last_click_time_$noteId")
                            remove("pending_delete_$noteId")
                        }
                        NoteWidget.updateWidget(context)
                    }
                } else {
                    prefs.edit { 
                        putLong("last_click_time_$noteId", currentTime)
                        putBoolean("pending_delete_$noteId", false)
                    }

                    dbRef.child("IsCompleted").get().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val isPendingDelete = prefs.getBoolean("pending_delete_$noteId", false)
                            
                            if (!isPendingDelete) {
                                val isCompleted = task.result?.getValue(Boolean::class.java) == true
                                dbRef.child("IsCompleted").setValue(!isCompleted).addOnCompleteListener {
                                    NoteWidget.updateWidget(context)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
