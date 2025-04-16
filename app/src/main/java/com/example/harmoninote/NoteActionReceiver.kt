package com.example.harmoninote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.database.FirebaseDatabase
import androidx.core.content.edit

class NoteActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra("NOTE_ID")
        if (noteId == null) return
        val dbRef = FirebaseDatabase.getInstance().getReference("notes").child(noteId)
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val lastClickTime = prefs.getLong("last_click_time_$noteId", 0L)
        val currentTime = System.currentTimeMillis()
        if ( currentTime - lastClickTime < 500){
            dbRef.removeValue()
            return
        }
        dbRef.child("IsCompleted").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val isCompleted = task.result?.getValue(Boolean::class.java) == true
                dbRef.child("IsCompleted").setValue(!isCompleted)
            }
        }
        prefs.edit() { putLong("last_click_time_$noteId", currentTime) }
        NoteWidget.updateWidget(context)
    }
}