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
        val noteId = intent.getStringExtra("NOTE_ID")

        when (intent.action) {
            "ACTION_OPEN_LINK" -> {
                val url = intent.getStringExtra("NOTE_URL")
                if (url != null) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(browserIntent)
                }
            }
            "ACTION_COPY_TEXT" -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val textNoteCopy = intent.getStringExtra("NOTE_TEXT")
                val clip = ClipData.newPlainText("Copied note", textNoteCopy)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied text", Toast.LENGTH_SHORT).show()
            }
            else -> {
                if (noteId == null) return
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
                    // Doble click detectado: ELIMINAR
                    // Marcar como pendiente de eliminación para evitar que el toggle lo resucite
                    prefs.edit { putBoolean("pending_delete_$noteId", true) }
                    
                    dbRef.removeValue().addOnCompleteListener {
                        // Limpiar banderas
                        prefs.edit { 
                            remove("last_click_time_$noteId")
                            remove("pending_delete_$noteId")
                        }
                        NoteWidget.updateWidget(context)
                    }
                } else {
                    // Click simple: MARCAR COMO COMPLETADA (o pendiente)
                    prefs.edit { 
                        putLong("last_click_time_$noteId", currentTime)
                        putBoolean("pending_delete_$noteId", false)
                    }

                    dbRef.child("IsCompleted").get().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Verificar si se solicitó eliminar mientras obteníamos el dato
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
        // NoteWidget.updateWidget(context) // Se llama dentro de los callbacks para asegurar consistencia
    }
}