package com.example.harmoninote

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

class NoteWidgetFactory(private val context: Context) : RemoteViewsFactory {
    private var listNotes = mutableListOf<Note>()
    private lateinit var qrDataStore: QRDataStore

    override fun onCreate() {
        qrDataStore = QRDataStore(context)
    }

    override fun onDataSetChanged() {
        // Obtener el valor del QR desde DataStore
        val contentQR = runBlocking {
            qrDataStore.qrContent.first() ?: "default"
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("Connections").child(contentQR).child("Notes")
        val latch = CountDownLatch(1)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listNotes.clear()
                for (notaSnapshot in snapshot.children) {
                    val text = notaSnapshot.child("Text").getValue(String::class.java)
                    val id = notaSnapshot.key
                    val timestamp = notaSnapshot.child("TimeStamp").getValue(Long::class.java)
                    val isCompleted =
                        notaSnapshot.child("IsCompleted").getValue(Boolean::class.java)
                    listNotes.add(Note(id, text, timestamp, isCompleted))
                }
                latch.countDown()
            }

            override fun onCancelled(error: DatabaseError) {
                latch.countDown()
            }
        })
        latch.await(1, TimeUnit.SECONDS)
    }

    override fun onDestroy() {}
    override fun getCount(): Int = listNotes.size
    override fun getViewAt(position: Int): RemoteViews? {
        val note = listNotes[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        views.setTextViewText(R.id.note_item_text, note.text)
        val backgroundResId = if (note.isCompleted == true){
            R.drawable.bg_ripple_note_completed
        }else{
            R.drawable.bg_ripple_note_pending
        }
        views.setInt(R.id.note_item_text, "setBackgroundResource", backgroundResId)
        if (note.isCompleted == true) {
            views.setInt(R.id.note_item_text, "setTextColor", android.graphics.Color.GRAY)
            views.setInt(
                R.id.note_item_text,
                "setPaintFlags",
                android.graphics.Paint.STRIKE_THRU_TEXT_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG
            )
        } else {
            views.setInt(R.id.note_item_text, "setTextColor", android.graphics.Color.WHITE)
            views.setInt(
                R.id.note_item_text,
                "setPaintFlags",
                android.graphics.Paint.ANTI_ALIAS_FLAG
            )
        }
        val fillInIntent = Intent().apply {
            putExtra("NOTE_ID", note.id)
        }

        val fillInIntentButton = Intent().apply {
            putExtra("NOTE_ID", note.id)
            putExtra("NOTE_TEXT",note.text)
            action = "ACTION_COPY_TEXT"
        }

        views.setOnClickFillInIntent(R.id.note_item_button, fillInIntentButton)
        views.setOnClickFillInIntent(R.id.note_item_text, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}