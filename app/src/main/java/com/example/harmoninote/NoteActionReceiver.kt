package com.example.harmoninote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.database.FirebaseDatabase

class NoteActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra("NOTE_ID")
        if (noteId == null) return
        val noteRef = FirebaseDatabase.getInstance().getReference("notes").child(noteId)
        noteRef.child("IsCompleted").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val isCompleted = task.result?.getValue(Boolean::class.java) == true
                noteRef.child("IsCompleted").setValue(!isCompleted)
            }
        }
        NoteWidget.updateWidget(context)
    }
}