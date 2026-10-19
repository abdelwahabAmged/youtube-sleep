package app.ytsleep.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateManifest(
    val schema: Int,
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val minSdk: Int,
    val publishedAt: String? = null,
    val notes: String? = null,
)

fun UpdateManifest.isOfferable(installedVersionCode: Int, deviceSdk: Int): Boolean {
    if (schema != 1) return false
    if (versionCode <= installedVersionCode) return false
    if (minSdk > deviceSdk) return false
    if (!apkUrl.startsWith("https://")) return false
    val host = java.net.URL(apkUrl).host
    if (!host.endsWith("github.com") && !host.endsWith("objects.githubusercontent.com")) return false
    if (sha256.length != 64 || !sha256.all { it.isDigit() || it in 'a'..'f' }) return false
    return true
}
