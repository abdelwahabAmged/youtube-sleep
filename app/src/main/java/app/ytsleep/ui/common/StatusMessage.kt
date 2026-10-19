package app.ytsleep.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.ytsleep.youtube.ContentError

@Composable
fun StatusMessage(
    error: ContentError,
    onAction: (() -> Unit)? = null,
    actionLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = errorMessage(error),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        if (onAction != null && actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

fun errorMessage(error: ContentError): String = when (error) {
    is ContentError.Offline -> "You're offline."
    is ContentError.NotSignedIn -> "Connect your YouTube account to see your feed and playlists. Search still works."
    is ContentError.SessionExpired -> "Your YouTube session has expired. Search still works."
    is ContentError.Unplayable -> "This can't be played (${error.reason})."
    is ContentError.SourceChanged -> "YouTube changed something. An app update is needed."
}

/** The sign-in action for errors that need an account, or null for other errors. */
fun accountAction(error: ContentError, navController: NavController): Pair<String, () -> Unit>? = when (error) {
    is ContentError.NotSignedIn -> "Connect account" to { navController.navigate("account/connect") }
    is ContentError.SessionExpired -> "Reconnect account" to { navController.navigate("account/connect") }
    else -> null
}
