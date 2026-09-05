package com.example.duonote

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.net.toUri

class NoteWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val intent = Intent(context, NoteWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = this.toUri(Intent.URI_INTENT_SCHEME).toUri()
            }
            val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
                setRemoteAdapter(R.id.widget_list_view, intent)
                setEmptyView(R.id.widget_list_view, android.R.id.empty)
            }
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }

            val dialogIntent = Intent(context, DialogActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, widgetId, dialogIntent, pendingIntentFlags)
            views.setOnClickPendingIntent(R.id.imageButton, pendingIntent)

            // Refresh button setup
            val refreshIntent = Intent(context, NoteWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
            }
            val broadcastRefreshFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId,
                refreshIntent,
                broadcastRefreshFlags
            )
            views.setOnClickPendingIntent(R.id.refreshButton, refreshPendingIntent)

            // Visibility button setup
            val visibilityIntent = Intent(context, NoteWidget::class.java).apply {
                action = "ACTION_TOGGLE_VISIBILITY"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
            }
            val visibilityPendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId,
                visibilityIntent,
                broadcastRefreshFlags
            )
            views.setOnClickPendingIntent(R.id.visibilityButton, visibilityPendingIntent)
            
            val isVisible = SecurityStore(context).isWidgetRevealed()
            views.setImageViewResource(
                R.id.visibilityButton,
                if (isVisible) R.drawable.ic_visibility_on else R.drawable.ic_visibility_off
            )
            views.setViewVisibility(R.id.imageButton, android.view.View.VISIBLE)

            val clickIntent = Intent(context, NoteActionReceiver::class.java)

            val broadcastFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntentTemplate = PendingIntent.getBroadcast(
                context,
                0,
                clickIntent,
                broadcastFlags
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, pendingIntentTemplate)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == "ACTION_TOGGLE_VISIBILITY" && context != null) {
            val securityStore = SecurityStore(context)
            if (securityStore.isWidgetRevealed()) {
                if (securityStore.hasPin()) {
                    securityStore.setWidgetRevealed(false)
                    AppSecuritySession.appUnlocked = false
                    updateWidget(context)
                } else {
                    context.startActivity(SecurityIntents.createPin(context))
                }
            } else {
                context.startActivity(SecurityIntents.revealWidget(context))
            }
        } else if (intent?.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE && context != null) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, NoteWidget::class.java))
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view)
        }
    }

    companion object {
        fun updateWidget(context: Context) {
            val intent = Intent(context, NoteWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, NoteWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
