package com.piecer.app.util.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.piecer.app.BuildConfig
import com.piecer.app.util.BreadcrumbLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

object InstallEligibility {

    fun evaluate(
        expectedPackageName: String,
        currentVersionCode: Long,
        currentSigningCertSha256s: Set<String>,
        apkPackageName: String?,
        apkVersionCode: Long,
        apkSigningCertSha256s: Set<String>,
        apkMinSdk: Int,
        deviceSdkInt: Int,
        actualSizeBytes: Long,
        expectedSizeBytes: Long,
        actualSha256: String,
        expectedSha256: String
    ): InstallDecision {
        if (actualSizeBytes <= 0L || actualSizeBytes != expectedSizeBytes) {
            return InstallDecision.Rejected(InstallRejectReason.SIZE_MISMATCH)
        }
        if (!actualSha256.equals(expectedSha256.trim(), ignoreCase = false)) {
            return InstallDecision.Rejected(InstallRejectReason.SHA256_MISMATCH)
        }
        if (apkPackageName.isNullOrBlank() || apkPackageName != expectedPackageName) {
            return InstallDecision.Rejected(InstallRejectReason.PACKAGE_MISMATCH)
        }
        if (apkVersionCode <= currentVersionCode) {
            return InstallDecision.Rejected(InstallRejectReason.NOT_NEWER_VERSION)
        }
        if (deviceSdkInt < apkMinSdk) {
            return InstallDecision.Rejected(InstallRejectReason.MIN_SDK_UNSUPPORTED)
        }
        if (currentSigningCertSha256s.isEmpty() || apkSigningCertSha256s.isEmpty()) {
            return InstallDecision.Rejected(InstallRejectReason.MISSING_SIGNATURE)
        }
        if (currentSigningCertSha256s != apkSigningCertSha256s) {
            return InstallDecision.Rejected(InstallRejectReason.SIGNATURE_MISMATCH)
        }
        return InstallDecision.Allowed
    }
}

@Singleton
class ApkVerifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun verifyDownloadedApk(file: File, metadata: UpdateMetadata): InstallDecision {
        return try {
            if (!file.isFile) {
                return InstallDecision.Rejected(InstallRejectReason.INVALID_APK)
            }

            val actualSize = file.length()
            if (actualSize != metadata.sizeBytes) {
                file.delete()
                return InstallDecision.Rejected(InstallRejectReason.SIZE_MISMATCH)
            }

            val actualSha256 = Sha256Verifier.computeSha256(file)
            if (actualSha256 != metadata.sha256.lowercase()) {
                file.delete()
                return InstallDecision.Rejected(InstallRejectReason.SHA256_MISMATCH)
            }

            val pm = context.packageManager
            val archiveInfo = getArchivePackageInfo(pm, file.absolutePath)
            if (archiveInfo == null) {
                file.delete()
                return InstallDecision.Rejected(InstallRejectReason.INVALID_APK)
            }

            val currentInfo = getInstalledPackageInfo(pm, context.packageName)
            if (currentInfo == null) {
                file.delete()
                return InstallDecision.Rejected(InstallRejectReason.MISSING_SIGNATURE)
            }

            val currentCerts = extractSigningCertSha256s(currentInfo)
            val archiveCerts = extractSigningCertSha256s(archiveInfo)
            val archiveVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo)
            val archiveMinSdk = archiveInfo.applicationInfo?.minSdkVersion ?: metadata.minSdk

            val decision = InstallEligibility.evaluate(
                expectedPackageName = context.packageName,
                currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
                currentSigningCertSha256s = currentCerts,
                apkPackageName = archiveInfo.packageName,
                apkVersionCode = archiveVersionCode,
                apkSigningCertSha256s = archiveCerts,
                apkMinSdk = maxOf(archiveMinSdk, metadata.minSdk),
                deviceSdkInt = Build.VERSION.SDK_INT,
                actualSizeBytes = actualSize,
                expectedSizeBytes = metadata.sizeBytes,
                actualSha256 = actualSha256,
                expectedSha256 = metadata.sha256.lowercase()
            )

            if (decision !is InstallDecision.Allowed) {
                file.delete()
            }
            decision
        } catch (t: Throwable) {
            runCatching { file.delete() }
            if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED) {
                BreadcrumbLog.record(
                    "UPDATE",
                    "ApkVerifier gagal (${t::class.java.simpleName}): ${t.message ?: "tanpa pesan"}",
                    syncWrite = true
                )
            }
            InstallDecision.Rejected(InstallRejectReason.INVALID_APK)
        }
    }

    @Suppress("DEPRECATION")
    private fun getArchivePackageInfo(pm: PackageManager, apkPath: String): PackageInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(
                apkPath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNATURES)
        }
    }

    @Suppress("DEPRECATION")
    private fun getInstalledPackageInfo(pm: PackageManager, packageName: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun extractSigningCertSha256s(info: PackageInfo): Set<String> {
        val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            info.signatures
        }
        if (signatures.isNullOrEmpty()) return emptySet()
        val digest = MessageDigest.getInstance("SHA-256")
        return signatures.map { sig ->
            digest.digest(sig.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        }.toSet()
    }
}
