package com.example.harmoninote

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory

class NotaWidgetFactory(private val context: Context) : RemoteViewsFactory {

    private var listaNotas = listOf<Nota>()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        listaNotas = NotaRepository.obtenerNotas()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = listaNotas.size

    override fun getViewAt(position: Int): RemoteViews {
        val nota = listaNotas[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        views.setTextViewText(R.id.note_item_text, nota.texto)

        // Acción pa' eliminar nota
        val intent = Intent(context, NotaWidget::class.java).apply {
            action = "com.miapp.ELIMINAR_NOTA"
            putExtra("nota_id", nota.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, nota.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.note_item_text, pendingIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = listaNotas[position].id.toLong()
    override fun hasStableIds(): Boolean = true
}
