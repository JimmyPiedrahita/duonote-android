package com.example.harmoninote

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NoteWidgetFactory(private val context: Context) : RemoteViewsFactory {
    private var listNotes = mutableListOf<Note>()
    override fun onCreate() {}
    override fun onDataSetChanged() {
        val dbRef = FirebaseDatabase.getInstance().getReference("notes")
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
        val fillInIntent = Intent().apply {
            putExtra("NOTE_ID", note.id)
        }
        views.setOnClickFillInIntent(R.id.note_item_text, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}