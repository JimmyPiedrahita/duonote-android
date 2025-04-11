package com.example.harmoninote

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlin.jvm.java
import androidx.core.net.toUri

class NotaWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val intent = Intent(context, NotaWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = this.toUri(Intent.URI_INTENT_SCHEME).toUri()
            }

            val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
                setRemoteAdapter(R.id.widget_list_view, intent)
                setEmptyView(R.id.widget_list_view, android.R.id.empty)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        // Aquí después metemos lo de eliminar nota o actualizar si querés
        if (intent?.action == "com.miapp.ELIMINAR_NOTA") {
            val notaId = intent.getIntExtra("nota_id", -1)
            if (notaId != -1) {
                NotaRepository.eliminarNotaPorId(notaId)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context!!, NotaWidget::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(component)
                appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_list_view)
            }
        }
    }
}
