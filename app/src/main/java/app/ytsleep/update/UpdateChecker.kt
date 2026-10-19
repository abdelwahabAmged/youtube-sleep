package app.ytsleep.update

import android.content.Context
import android.content.pm.PackageInstaller
import app.ytsleep.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/**
 * Downloads a newer release in the background. Once verified, [maybeAutoInstall] opens the system
 * install prompt while the app is in the foreground and nothing is playing (contracts/update-manifest.md).
 */
class UpdateChecker(
    private val context: Context,
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.NONE)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences("update", Context.MODE_PRIVATE)
    private val installer = UpdateInstaller(context)

    private var manifest: UpdateManifest? = null

    private val manifestUrl =
        "https://github.com/${BuildConfig.RELEASE_REPO}/releases/latest/download/update.json"

    fun checkNow() {
        if (_state.value == UpdateState.DOWNLOADING || _state.value == UpdateState.INSTALLING) return
        scope.launch(Dispatchers.IO) {
            val latest = try {
                fetchManifest()
            } catch (_: Exception) {
                null // Offline or not published yet: the current version keeps working.
            } ?: return@launch
            if (!latest.isOfferable(BuildConfig.VERSION_CODE, android.os.Build.VERSION.SDK_INT)) return@launch

            manifest = latest
            _state.value = UpdateState.DOWNLOADING
            _state.value = try {
                download(latest)
                if (prefs.getInt(KEY_DISMISSED, 0) == latest.versionCode) {
                    UpdateState.DISMISSED(latest.versionName, latest.notes)
                } else {
                    UpdateState.READY(latest.versionName, latest.notes)
                }
            } catch (_: Exception) {
                UpdateState.FAILED(latest.versionName)
            }
        }
    }

    /** Called from the foreground UI whenever the update or playback state changes. */
    fun maybeAutoInstall(isPlaying: Boolean) {
        if (_state.value is UpdateState.READY && !isPlaying) install()
    }

    /** Opens the system install prompt (auto-prompt, or the banner's "Install" button). */
    fun install() {
        val m = manifest ?: return
        val apk = apkFile(m)
        _state.value = UpdateState.INSTALLING
        scope.launch {
            try {
                if (!installer.install(apk)) markDismissed(m)
            } catch (_: Exception) {
                _state.value = UpdateState.FAILED(m.versionName)
            }
        }
    }

    fun onInstallResult(status: Int) {
        val m = manifest ?: return
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> Unit // The process is replaced by the new version.
            PackageInstaller.STATUS_FAILURE_ABORTED -> markDismissed(m)
            else -> _state.value = UpdateState.FAILED(m.versionName)
        }
    }

    private fun markDismissed(m: UpdateManifest) {
        prefs.edit().putInt(KEY_DISMISSED, m.versionCode).apply()
        _state.value = UpdateState.DISMISSED(m.versionName, m.notes)
    }

    private fun fetchManifest(): UpdateManifest? {
        client.newCall(Request.Builder().url(manifestUrl).build()).execute().use { response ->
            if (!response.isSuccessful) return null
            return json.decodeFromString<UpdateManifest>(response.body?.string() ?: return null)
        }
    }

    private fun apkFile(m: UpdateManifest) = File(context.cacheDir, "updates/${m.versionCode}.apk")

    private suspend fun download(m: UpdateManifest) = withContext(Dispatchers.IO) {
        val target = apkFile(m)
        if (target.exists() && sha256(target) == m.sha256) return@withContext

        target.parentFile?.listFiles()?.forEach { it.delete() }
        target.parentFile?.mkdirs()
        val partial = File(target.path + ".part")
        client.newCall(Request.Builder().url(m.apkUrl).build()).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("HTTP ${response.code}")
            val body = response.body ?: throw java.io.IOException("Empty body")
            partial.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
        if (sha256(partial) != m.sha256) {
            partial.delete()
            throw SecurityException("APK checksum mismatch")
        }
        partial.renameTo(target)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val KEY_DISMISSED = "dismissedVersionCode"
    }
}
