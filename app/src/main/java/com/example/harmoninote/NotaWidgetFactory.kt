package com.example.harmoninote

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NotaWidgetFactory(private val context: Context) : RemoteViewsFactory {
    private var listaNotas = mutableListOf<Nota>()
    override fun onCreate() {}
    override fun onDataSetChanged() {
        val dbRef = FirebaseDatabase.getInstance().getReference("notes")
        val latch = CountDownLatch(1)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaNotas.clear()
                for (notaSnapshot in snapshot.children) {
                    val texto = notaSnapshot.child("Text").getValue(String::class.java)
                    val id = notaSnapshot.key
                    if (texto != null && id != null) {
                        listaNotas.add(Nota(id, texto))
                    }
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
    override fun getCount(): Int = listaNotas.size
    override fun getViewAt(position: Int): RemoteViews? {
        val nota = listaNotas[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        views.setTextViewText(R.id.note_item_text, nota.texto)
        return views
    }
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
