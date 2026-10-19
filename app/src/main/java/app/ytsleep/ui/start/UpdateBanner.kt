package app.ytsleep.ui.start

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.ytsleep.YtSleepApp
import app.ytsleep.update.UpdateState

@Composable
fun UpdateBanner(state: UpdateState) {
    val context = LocalContext.current
    val checker = (context.applicationContext as YtSleepApp).container.updateChecker

    // Re-checked on every resume so the card disappears after the permission is granted.
    var canInstall by remember { mutableStateOf(context.packageManager.canRequestPackageInstalls()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            canInstall = context.packageManager.canRequestPackageInstalls()
        }
    }

    if (!canInstall) {
        BannerCard(
            title = "Allow updates",
            body = "Let YT Sleep install its own updates. This is a one-time step.",
            action = "Open settings",
        ) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            )
        }
    }

    when (state) {
        is UpdateState.DISMISSED ->
            BannerCard("Update ready — ${state.versionName}", state.notes, "Install") { checker.install() }
        is UpdateState.FAILED ->
            BannerCard("Update failed", state.versionName?.let { "Version $it could not be installed." }, "Retry") {
                checker.checkNow()
            }
        else -> Unit
    }
}

@Composable
private fun BannerCard(title: String, body: String?, action: String, onAction: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            body?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAction) { Text(action) }
        }
    }
}
