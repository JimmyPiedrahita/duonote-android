package com.example.harmoninote

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseListenerService : Service() {
    private lateinit var databaseReference: DatabaseReference
    private val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            NotaWidget.updateWidget(applicationContext)
        }
        override fun onCancelled(error: DatabaseError) {
            Log.e("FirebaseService", "Error: ${error.message}")
        }
    }
    override fun onCreate() {
        super.onCreate()
        databaseReference = FirebaseDatabase.getInstance().getReference("notes")
        databaseReference.addValueEventListener(listener)
    }
    override fun onDestroy() {
        super.onDestroy()
        databaseReference.removeEventListener(listener)
    }
    override fun onBind(intent: Intent?): IBinder? = null
}