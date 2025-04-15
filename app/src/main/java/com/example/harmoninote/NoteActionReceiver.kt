package com.example.harmoninote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NoteActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("resultado", "${intent.getStringExtra("NOTE_ID")}")
        NoteWidget.updateWidget(context)
    }
}