package com.piecer.app

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import com.piecer.app.util.BreadcrumbLog
import com.piecer.app.util.CrashReporter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PiecerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED) {
            BreadcrumbLog.init(filesDir = filesDir, enabled = true)
            BreadcrumbLog.record(
                category = "PROCESS",
                message = "Cold start Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) ${Build.MANUFACTURER} ${Build.MODEL}"
            )
            CrashReporter.install(context = this, enabled = true)
            registerActivityLifecycleCallbacks(DebugLifecycleLogger())
        }
    }

    private class DebugLifecycleLogger : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            BreadcrumbLog.record(
                category = "ACTIVITY",
                message = "${activity.javaClass.simpleName}.onCreate(hasSavedState=${savedInstanceState != null})",
                syncWrite = true
            )
        }

        override fun onActivityStarted(activity: Activity) {
            BreadcrumbLog.record(
                category = "ACTIVITY",
                message = "${activity.javaClass.simpleName}.onStart",
                syncWrite = true
            )
        }

        override fun onActivityResumed(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) {
            BreadcrumbLog.record(
                category = "ACTIVITY",
                message = "${activity.javaClass.simpleName}.onStop(changingConfig=${activity.isChangingConfigurations}, finishing=${activity.isFinishing})",
                syncWrite = true
            )
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            BreadcrumbLog.record(
                category = "ACTIVITY",
                message = "${activity.javaClass.simpleName}.onSaveInstanceState",
                syncWrite = true
            )
        }

        override fun onActivityDestroyed(activity: Activity) {
            BreadcrumbLog.record(
                category = "ACTIVITY",
                message = "${activity.javaClass.simpleName}.onDestroy(changingConfig=${activity.isChangingConfigurations}, finishing=${activity.isFinishing})",
                syncWrite = true
            )
        }
    }
}
