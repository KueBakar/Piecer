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
    fun `allowlist accepts github and github io HTTPS URLs and rejects others`() {
        assertTrue(UpdateUrlValidator.isAllowedUrl("https://github.com/user/repo/releases/download/v1/app.apk"))
        assertTrue(UpdateUrlValidator.isAllowedUrl("https://objects.githubusercontent.com/some/path"))
        assertTrue(UpdateUrlValidator.isAllowedUrl("https://githubusercontent.com/some/path"))
        assertTrue(UpdateUrlValidator.isAllowedUrl("https://myuser.github.io/piecer/update.json"))

        assertFalse(UpdateUrlValidator.isAllowedUrl("http://github.com/user/repo"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://evil.com/app.apk"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://github.com.evil.com/app.apk"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://github.io/update.json"))
        assertFalse(UpdateUrlValidator.isAllowedUrl("https://user@github.com/repo"))
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
