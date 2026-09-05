package com.example.duonote

import android.content.Context
import android.content.Intent

object SecurityIntents {
    fun createPin(context: Context): Intent = Intent(context, AuthActivity::class.java).apply {
        putExtra(AuthActivity.EXTRA_SETUP, true)
        putExtra(AuthActivity.EXTRA_MODE, AuthActivity.MODE_WIDGET_REVEAL)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun revealWidget(context: Context): Intent = Intent(context, AuthActivity::class.java).apply {
        putExtra(AuthActivity.EXTRA_MODE, AuthActivity.MODE_WIDGET_REVEAL)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun unlockApp(context: Context): Intent = Intent(context, AuthActivity::class.java).apply {
        putExtra(AuthActivity.EXTRA_MODE, AuthActivity.MODE_APP_ENTRY)
    }
}
