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
            
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val isDarkTheme = prefs.getBoolean("is_dark_theme", true)
            
            if (isDarkTheme) {
                views.setInt(R.id.header_widget, "setBackgroundResource", R.drawable.bg_widget_item_dark)
                views.setImageViewResource(R.id.themeButton, R.drawable.ic_theme_dark)
                views.setInt(R.id.themeButton, "setColorFilter", android.graphics.Color.WHITE)
                views.setInt(R.id.visibilityButton, "setColorFilter", android.graphics.Color.WHITE)
                views.setInt(R.id.refreshButton, "setColorFilter", android.graphics.Color.WHITE)
                views.setInt(R.id.imageButton, "setColorFilter", android.graphics.Color.WHITE)
            } else {
                views.setInt(R.id.header_widget, "setBackgroundResource", R.drawable.bg_widget_item_light)
                views.setImageViewResource(R.id.themeButton, R.drawable.ic_theme_light)
                val iconColorLight = android.graphics.Color.parseColor("#333333") // Menu icons same as item icons
                views.setInt(R.id.themeButton, "setColorFilter", iconColorLight)
                views.setInt(R.id.visibilityButton, "setColorFilter", iconColorLight)
                views.setInt(R.id.refreshButton, "setColorFilter", iconColorLight)
                views.setInt(R.id.imageButton, "setColorFilter", iconColorLight)
            }
            val dialogIntent = Intent(context, DialogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                dialogIntent,
                pendingIntentFlags
            )
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

            // Theme button setup
            val themeIntent = Intent(context, NoteWidget::class.java).apply {
                action = "ACTION_TOGGLE_THEME"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
            }
            val themePendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId,
                themeIntent,
                broadcastRefreshFlags
            )
            views.setOnClickPendingIntent(R.id.themeButton, themePendingIntent)

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
            
            val isVisible = prefs.getBoolean("notes_visible", true)
            views.setImageViewResource(
                R.id.visibilityButton,
                if (isVisible) R.drawable.ic_visibility_on else R.drawable.ic_visibility_off
            )

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
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val isVisible = prefs.getBoolean("notes_visible", true)
            prefs.edit().putBoolean("notes_visible", !isVisible).apply()
            
            updateWidget(context)
        } else if (intent?.action == "ACTION_TOGGLE_THEME" && context != null) {
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val isDarkTheme = prefs.getBoolean("is_dark_theme", true)
            prefs.edit().putBoolean("is_dark_theme", !isDarkTheme).apply()
            
            updateWidget(context)
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
