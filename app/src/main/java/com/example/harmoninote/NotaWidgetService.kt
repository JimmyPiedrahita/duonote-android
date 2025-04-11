package com.example.harmoninote

import android.content.Intent
import android.widget.RemoteViewsService

class NotaWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NotaWidgetFactory(applicationContext)
    }
}
