package com.example.duonote

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager

class DuoNoteApplication : Application() {
    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                if (startedActivities == 0 && shouldLockApp(activity)) {
                    AppSecuritySession.appUnlocked = false
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun shouldLockApp(activity: Activity): Boolean {
        val securityStore = SecurityStore(activity)
        return securityStore.hasPin() && !securityStore.isWidgetRevealed()
    }
}
