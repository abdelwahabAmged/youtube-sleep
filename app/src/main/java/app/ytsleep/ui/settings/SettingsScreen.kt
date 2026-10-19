package app.ytsleep.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.content.Intent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.ytsleep.BuildConfig
import app.ytsleep.youtube.AudioQuality

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(navController: NavController, vm: SettingsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))

        // Account section
        Text("Account", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Status: ${state.accountStatus}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { navController.navigate("account/connect") }) {
                Text(if (state.accountStatus == "EXPIRED") "Reconnect" else "Connect")
            }
            OutlinedButton(onClick = { navController.navigate("account/paste") }) {
                Text("Paste cookies")
            }
            if (state.accountStatus == "CONNECTED") {
                OutlinedButton(onClick = vm::signOut) { Text("Sign out") }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Audio quality", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Chosen automatically by network when each video starts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        QualityRow("On Wi-Fi", state.wifiQuality, vm::setWifiQuality)
        Spacer(Modifier.height(12.dp))
        QualityRow("On mobile data", state.mobileQuality, vm::setMobileQuality)

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        BatteryOptimizationRow()

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))


        Text("About", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = vm::checkForUpdates) { Text("Check for updates") }
    }
}

@Composable
private fun BatteryOptimizationRow() {
    val context = LocalContext.current
    fun isExempt() = context.getSystemService<PowerManager>()
        ?.isIgnoringBatteryOptimizations(context.packageName) == true

    var exempt by remember { mutableStateOf(isExempt()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { exempt = isExempt() }
    }

    Text("Battery", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Text(
        if (exempt) "Battery optimization is off for YT Sleep, so overnight playback won't be stopped."
        else "Battery optimization may stop playback while the screen is off.",
        style = MaterialTheme.typography.bodyMedium,
    )
    if (!exempt) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
            )
        }) { Text("Allow background playback") }
    }
}

@Composable
private fun QualityRow(label: String, selected: AudioQuality, onSelect: (AudioQuality) -> Unit) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AudioQuality.entries.forEach { quality ->
            FilterChip(
                selected = quality == selected,
                onClick = { onSelect(quality) },
                label = { Text(quality.label) },
            )
        }
    }
    Text(
        "About ${selected.mbPerHour} MB per hour",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
