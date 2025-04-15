package com.example.harmoninote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.database.FirebaseDatabase

class NoteActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra("NOTE_ID")
        if (noteId == null) return
        val dbRef = FirebaseDatabase.getInstance().getReference("notes").child(noteId)
        dbRef.removeValue()
        NoteWidget.updateWidget(context)
    }
}