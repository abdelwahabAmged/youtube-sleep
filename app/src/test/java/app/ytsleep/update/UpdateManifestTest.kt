package app.ytsleep.update

import org.junit.Assert.*
import org.junit.Test

class UpdateManifestTest {

    private fun manifest(
        schema: Int = 1,
        versionCode: Int = 2,
        versionName: String = "0.2.0",
        apkUrl: String = "https://github.com/owner/repo/releases/download/v0.2.0/app.apk",
        sha256: String = "a".repeat(64),
        sizeBytes: Long = 1_000_000,
        minSdk: Int = 29,
        notes: String? = null,
        publishedAt: String? = null,
    ) = UpdateManifest(
        schema = schema,
        versionCode = versionCode,
        versionName = versionName,
        apkUrl = apkUrl,
        sha256 = sha256,
        sizeBytes = sizeBytes,
        minSdk = minSdk,
        notes = notes,
        publishedAt = publishedAt,
    )

    @Test
    fun `schema 1 with higher version code is offerable`() {
        assertTrue(manifest().isOfferable(installedVersionCode = 1, deviceSdk = 30))
    }

    @Test
    fun `unknown schema is not offerable`() {
        assertFalse(manifest(schema = 2).isOfferable(installedVersionCode = 1, deviceSdk = 30))
    }

    @Test
    fun `equal version code is not offerable`() {
        assertFalse(manifest(versionCode = 1).isOfferable(installedVersionCode = 1, deviceSdk = 30))
    }

    @Test
    fun `lower version code is not offerable`() {
        assertFalse(manifest(versionCode = 1).isOfferable(installedVersionCode = 5, deviceSdk = 30))
    }

    @Test
    fun `apkUrl must be https on github com`() {
        assertTrue(manifest(apkUrl = "https://github.com/o/r/releases/download/v1/app.apk")
            .isOfferable(1, 30))
        assertFalse(manifest(apkUrl = "http://github.com/o/r/releases/download/v1/app.apk")
            .isOfferable(1, 30))
        assertFalse(manifest(apkUrl = "https://evil.com/app.apk")
            .isOfferable(1, 30))
    }

    @Test
    fun `apkUrl on objects githubusercontent com is offerable`() {
        assertTrue(manifest(apkUrl = "https://objects.githubusercontent.com/repos/o/r/app.apk")
            .isOfferable(1, 30))
    }

    @Test
    fun `sha256 must be 64 lowercase hex chars`() {
        assertTrue(manifest(sha256 = "a".repeat(64)).isOfferable(1, 30))
        assertFalse(manifest(sha256 = "A".repeat(64)).isOfferable(1, 30))
        assertFalse(manifest(sha256 = "a".repeat(63)).isOfferable(1, 30))
        assertFalse(manifest(sha256 = "a".repeat(65)).isOfferable(1, 30))
        assertFalse(manifest(sha256 = "g".repeat(64)).isOfferable(1, 30)) // 'g' not in a-f
    }

    @Test
    fun `minSdk higher than device sdk is not offerable`() {
        assertFalse(manifest(minSdk = 35).isOfferable(1, deviceSdk = 30))
    }

    @Test
    fun `minSdk equal to device sdk is offerable`() {
        assertTrue(manifest(minSdk = 30).isOfferable(1, deviceSdk = 30))
    }

    @Test
    fun `optional fields may be absent`() {
        assertTrue(manifest(notes = null, publishedAt = null).isOfferable(1, 30))
        assertTrue(manifest(notes = "some note", publishedAt = "2026-01-01T00:00:00Z").isOfferable(1, 30))
    }
}
