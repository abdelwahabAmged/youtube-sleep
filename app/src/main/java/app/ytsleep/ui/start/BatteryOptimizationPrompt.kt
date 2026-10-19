package app.ytsleep.ui.start

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import app.ytsleep.YtSleepApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Asks once for the battery-optimization exemption so overnight playback isn't killed. */
@Composable
fun BatteryOptimizationPrompt() {
    val context = LocalContext.current
    val settings = (context.applicationContext as YtSleepApp).container.settingsStore
    val scope = rememberCoroutineScope()
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val exempt = context.getSystemService<PowerManager>()
            ?.isIgnoringBatteryOptimizations(context.packageName) == true
        show = !exempt && !settings.batteryPromptShown.first()
    }

    if (!show) return

    fun close(openSettings: Boolean) {
        show = false
        scope.launch { settings.setBatteryPromptShown(true) }
        if (openSettings) {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}"),
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = { close(openSettings = false) },
        title = { Text("Keep audio playing overnight") },
        text = { Text("Allow YT Sleep to ignore battery optimization so audio keeps playing with the screen off.") },
        confirmButton = { TextButton(onClick = { close(openSettings = true) }) { Text("Allow") } },
        dismissButton = { TextButton(onClick = { close(openSettings = false) }) { Text("Not now") } },
    )
}
