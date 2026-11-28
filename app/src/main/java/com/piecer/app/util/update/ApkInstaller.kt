package com.piecer.app.util.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.piecer.app.BuildConfig
import com.piecer.app.util.BreadcrumbLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface InstallStatusEvent {
    data class PendingUserAction(val confirmIntent: Intent) : InstallStatusEvent
    data object Success : InstallStatusEvent
    data object Cancelled : InstallStatusEvent
    data class Failure(val message: String) : InstallStatusEvent
}

object UpdateInstallEvents {
    private val _events = MutableSharedFlow<InstallStatusEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<InstallStatusEvent> = _events.asSharedFlow()

    fun emit(event: InstallStatusEvent) {
        _events.tryEmit(event)
    }
}

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val status = intent.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
            )
            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_INTENT)
                    }
                    if (confirmIntent != null) {
                        UpdateInstallEvents.emit(InstallStatusEvent.PendingUserAction(confirmIntent))
                    } else {
                        UpdateInstallEvents.emit(InstallStatusEvent.Failure("Konfirmasi pemasangan tidak tersedia."))
                    }
                }

                PackageInstaller.STATUS_SUCCESS -> {
                    UpdateInstallEvents.emit(InstallStatusEvent.Success)
                }

                PackageInstaller.STATUS_FAILURE_ABORTED -> {
                    UpdateInstallEvents.emit(InstallStatusEvent.Cancelled)
                }

                else -> {
                    val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                        ?.takeIf { it.isNotBlank() }
                        ?: "Pemasangan pembaruan gagal."
                    UpdateInstallEvents.emit(InstallStatusEvent.Failure(msg))
                }
            }
        } catch (t: Throwable) {
            if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED) {
                BreadcrumbLog.record(
                    "UPDATE",
                    "UpdateInstallReceiver gagal (${t::class.java.simpleName}): ${t.message ?: "tanpa pesan"}",
                    syncWrite = true
                )
            }
            UpdateInstallEvents.emit(InstallStatusEvent.Failure("Pemasangan pembaruan gagal."))
        }
    }
}

@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun canRequestPackageInstalls(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }
        } catch (t: Throwable) {
            if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED) {
                BreadcrumbLog.record(
                    "UPDATE",
                    "canRequestPackageInstalls gagal (${t::class.java.simpleName}): ${t.message ?: "tanpa pesan"}",
                    syncWrite = true
                )
            }
            false
        }
    }

    fun createUnknownSourcesSettingsIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    suspend fun installApk(apkFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        if (!apkFile.isFile) {
            return@withContext Result.failure(IllegalStateException("Berkas APK tidak ditemukan."))
        }
        var session: PackageInstaller.Session? = null
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
            }
            val sessionId = packageInstaller.createSession(params)
            session = packageInstaller.openSession(sessionId)
            apkFile.inputStream().buffered().use { input ->
                session.openWrite("piecer_update", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            val callbackIntent = Intent(context, UpdateInstallReceiver::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                callbackIntent,
                flags
            )
            session.commit(pendingIntent.intentSender)
            Result.success(Unit)
        } catch (t: Throwable) {
            runCatching { session?.abandon() }
            if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED) {
                BreadcrumbLog.record(
                    "UPDATE",
                    "ApkInstaller.installApk gagal (${t::class.java.simpleName}): ${t.message ?: "tanpa pesan"}",
                    syncWrite = true
                )
            }
            Result.failure(t)
        } finally {
            runCatching { session?.close() }
        }
    }
}
