package com.piecer.app.util.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateModuleTest {

    private val validSha256 = "a".repeat(64)

    private fun makeJson(
        versionCode: Int = 2,
        versionName: String = "1.1.0",
        apkUrl: String = "https://github.com/user/repo/releases/download/v1.1.0/app.apk",
        sha256: String = validSha256,
        sizeBytes: Long = 5_000_000L,
        minSdk: Int = 26,
        releaseNotes: String = "Perbaikan bug"
    ): String {
        return """
            {
              "versionCode": $versionCode,
              "versionName": "$versionName",
              "apkUrl": "$apkUrl",
              "sha256": "$sha256",
              "sizeBytes": $sizeBytes,
              "minSdk": $minSdk,
              "releaseNotes": "$releaseNotes"
            }
        """.trimIndent()
    }

    @Test
    fun `parseAndValidate returns UpdateAvailable when versionCode is newer`() {
        val result = UpdateJsonParser.parseAndValidate(
            rawJson = makeJson(versionCode = 5),
            currentVersionCode = 4,
            currentSdkInt = 34
        )
        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val meta = (result as UpdateCheckResult.UpdateAvailable).metadata
        assertEquals(5, meta.versionCode)
        assertEquals("1.1.0", meta.versionName)
    }

    @Test
    fun `parseAndValidate returns UpToDate when versionCode is same`() {
        val result = UpdateJsonParser.parseAndValidate(
            rawJson = makeJson(versionCode = 4),
            currentVersionCode = 4,
            currentSdkInt = 34
        )
        assertEquals(UpdateCheckResult.UpToDate, result)
    }

    @Test
    fun `parseAndValidate returns UpToDate when versionCode is older`() {
        val result = UpdateJsonParser.parseAndValidate(
            rawJson = makeJson(versionCode = 3),
            currentVersionCode = 4,
            currentSdkInt = 34
        )
        assertEquals(UpdateCheckResult.UpToDate, result)
    }

    @Test
    fun `parseAndValidate returns Error on malformed JSON`() {
        val result = UpdateJsonParser.parseAndValidate(
            rawJson = "{ broken json",
            currentVersionCode = 1,
            currentSdkInt = 34
        )
        assertTrue(result is UpdateCheckResult.Error)
    }

    @Test
    fun `parseAndValidate returns Error when JSON exceeds size limit`() {
        val oversized = makeJson(releaseNotes = "x".repeat(UpdateJsonParser.MAX_JSON_BYTES + 100))
        val result = UpdateJsonParser.parseAndValidate(
            rawJson = oversized,
            currentVersionCode = 1,
            currentSdkInt = 34
        )
        assertTrue(result is UpdateCheckResult.Error)
    }

    @Test
    fun `parseAndValidate returns Error when apk sizeBytes exceeds cap`() {
        val result = UpdateJsonParser.parseAndValidate(
            rawJson = makeJson(sizeBytes = UpdateJsonParser.MAX_APK_BYTES + 1L),
            currentVersionCode = 1,
            currentSdkInt = 34
        )
        assertTrue(result is UpdateCheckResult.Error)
    }

    @Test
    fun `allowlist accepts github and githubusercontent HTTPS URLs and rejects others`() {
        assertTrue(UpdateUrlValidator.isAllowedUrl("https://github.com/user/repo/releases/download/v1/app.apk"))
        assertTrue(UpdateUrlValidator.isAllowedUrl("https://release-assets.githubusercontent.com/github-production-release-asset/123/app.apk"))
        assertTrue(UpdateUrlValidator.isAllowedUrl("https://objects.githubusercontent.com/some/path"))
        assertTrue(UpdateUrlValidator.isAllowedUrl("https://raw.githubusercontent.com/user/repo/main/file"))
        assertTrue(UpdateUrlValidator.isAllowedUrl("https://githubusercontent.com/some/path"))
        assertTrue(UpdateUrlValidator.isAllowedUrl("https://myuser.github.io/piecer/update.json"))
        assertTrue(UpdateUrlValidator.isAllowedUrl("https://github.com:443/user/repo/releases/download/v1/app.apk"))

        // URL panjang (~1000 karakter) dengan parameter bertanda tangan sementara harus diizinkan
        val longSignedQuery = "sig=" + "a".repeat(950)
        assertTrue(
            UpdateUrlValidator.isAllowedUrl(
                "https://release-assets.githubusercontent.com/asset/app.apk?$longSignedQuery",
                allowGithubIo = false
            )
        )

        // github.io hanya untuk update.json, bukan unduhan APK
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://myuser.github.io/piecer/app.apk", allowGithubIo = false))

        // Skema HTTP, host mirip/palsu, userinfo, port aneh, dan IP literal harus ditolak
        assertFalse(UpdateUrlValidator.isAllowedUrl("http://github.com/user/repo"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("http://objects.githubusercontent.com/some/path"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://evil.com/app.apk"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://github.com.evil.com/app.apk"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://evilgithubusercontent.com/app.apk"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://sub.github.com/app.apk"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://github.io/update.json"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://user@github.com/repo"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://user:pass@github.com/repo"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://@github.com/repo"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://github.com:8443/user/repo"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://objects.githubusercontent.com:8080/path"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://127.0.0.1/app.apk"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://192.168.1.10/app.apk"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://[::1]/app.apk"))
    }

    @Test
    fun `redirect validation allows trusted githubusercontent hop and rejects foreign or http downgrade`() {
        val initial = "https://github.com/KueBakar/Piecer/releases/download/v1.0.1/PIECER-v1.0.1.apk"
        val signedTarget = "https://release-assets.githubusercontent.com/github-production-release-asset/1/PIECER-v1.0.1.apk?token=" + "x".repeat(900)

        val allowed = UpdateUrlValidator.resolveAndValidateRedirect(initial, signedTarget, allowGithubIo = false)
        assertEquals(UpdateUrlValidator.RedirectValidation.Allowed(signedTarget), allowed)

        val foreignRedirect = UpdateUrlValidator.resolveAndValidateRedirect(
            initial,
            "https://cdn.evil.com/malware.apk?secret=123",
            allowGithubIo = false
        )
        assertEquals(UpdateUrlValidator.RedirectValidation.Rejected("cdn.evil.com"), foreignRedirect)

        val downgradeRedirect = UpdateUrlValidator.resolveAndValidateRedirect(
            initial,
            "http://objects.githubusercontent.com/some/path",
            allowGithubIo = false
        )
        assertEquals(UpdateUrlValidator.RedirectValidation.Rejected("objects.githubusercontent.com"), downgradeRedirect)
    }

    @Test
    fun `sha256 verifier computes and matches expected lowercase digest`() {
        val tempFile = File.createTempFile("piecer-sha-test", ".bin")
        try {
            tempFile.writeText("piecer-update-payload", Charsets.UTF_8)
            val digest = Sha256Verifier.computeSha256(tempFile)
            assertEquals(64, digest.length)
            assertTrue(Sha256Verifier.matchesSha256(tempFile, digest))
            assertFalse(Sha256Verifier.matchesSha256(tempFile, "0".repeat(64)))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `install eligibility allows valid newer signed apk and rejects mismatches`() {
        val sigA = setOf("11".repeat(32))
        val sigB = setOf("22".repeat(32))

        val allowed = InstallEligibility.evaluate(
            expectedPackageName = "com.piecer.app",
            currentVersionCode = 1L,
            currentSigningCertSha256s = sigA,
            apkPackageName = "com.piecer.app",
            apkVersionCode = 2L,
            apkSigningCertSha256s = sigA,
            apkMinSdk = 26,
            deviceSdkInt = 34,
            actualSizeBytes = 1000L,
            expectedSizeBytes = 1000L,
            actualSha256 = validSha256,
            expectedSha256 = validSha256
        )
        assertEquals(InstallDecision.Allowed, allowed)

        val wrongSig = InstallEligibility.evaluate(
            expectedPackageName = "com.piecer.app",
            currentVersionCode = 1L,
            currentSigningCertSha256s = sigA,
            apkPackageName = "com.piecer.app",
            apkVersionCode = 2L,
            apkSigningCertSha256s = sigB,
            apkMinSdk = 26,
            deviceSdkInt = 34,
            actualSizeBytes = 1000L,
            expectedSizeBytes = 1000L,
            actualSha256 = validSha256,
            expectedSha256 = validSha256
        )
        assertEquals(InstallDecision.Rejected(InstallRejectReason.SIGNATURE_MISMATCH), wrongSig)

        val notNewer = InstallEligibility.evaluate(
            expectedPackageName = "com.piecer.app",
            currentVersionCode = 2L,
            currentSigningCertSha256s = sigA,
            apkPackageName = "com.piecer.app",
            apkVersionCode = 2L,
            apkSigningCertSha256s = sigA,
            apkMinSdk = 26,
            deviceSdkInt = 34,
            actualSizeBytes = 1000L,
            expectedSizeBytes = 1000L,
            actualSha256 = validSha256,
            expectedSha256 = validSha256
        )
        assertEquals(InstallDecision.Rejected(InstallRejectReason.NOT_NEWER_VERSION), notNewer)

        val wrongPkg = InstallEligibility.evaluate(
            expectedPackageName = "com.piecer.app",
            currentVersionCode = 1L,
            currentSigningCertSha256s = sigA,
            apkPackageName = "com.other.app",
            apkVersionCode = 2L,
            apkSigningCertSha256s = sigA,
            apkMinSdk = 26,
            deviceSdkInt = 34,
            actualSizeBytes = 1000L,
            expectedSizeBytes = 1000L,
            actualSha256 = validSha256,
            expectedSha256 = validSha256
        )
        assertEquals(InstallDecision.Rejected(InstallRejectReason.PACKAGE_MISMATCH), wrongPkg)
    }
}
