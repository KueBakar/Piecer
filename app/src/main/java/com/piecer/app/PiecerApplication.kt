package com.piecer.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.piecer.app.util.BreadcrumbLog
import com.piecer.app.util.CrashReporter
import com.piecer.app.util.NotificationChannelHelper
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class PiecerApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (!isCrashProcess()) {
            CrashReporter.install(context = this)
            BreadcrumbLog.init(filesDir = filesDir, enabled = true)
            BreadcrumbLog.record(
                category = "PROCESS",
                message = "Cold start Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) ${Build.MANUFACTURER} ${Build.MODEL}"
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!isCrashProcess()) {
            NotificationChannelHelper.ensureAllChannels(this)
            registerActivityLifecycleCallbacks(DebugLifecycleLogger())
        }
    }

    private fun isCrashProcess(): Boolean {
        val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            runCatching {
                File("/proc/self/cmdline").readText().trim('\u0000', ' ', '\n', '\r')
            }.getOrNull()
        }
        return processName?.endsWith(":crash") == true
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
