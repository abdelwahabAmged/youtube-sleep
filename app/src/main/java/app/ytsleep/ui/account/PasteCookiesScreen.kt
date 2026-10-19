package app.ytsleep.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.ytsleep.YtSleepApp
import app.ytsleep.auth.CookieParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PasteCookiesScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as YtSleepApp).container
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Paste cookies", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Paste cookies exported from a desktop browser (Netscape format or name=value pairs). " +
                    "The SAPISID cookie is required.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; error = null },
            label = { Text("Cookies") },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            isError = error != null,
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            try {
                val cookies = CookieParser.parse(text)
                scope.launch(Dispatchers.IO) {
                    container.accountStore.save(cookies)
                }
                navController.popBackStack()
            } catch (e: Exception) {
                error = e.message ?: "Invalid cookies"
            }
        }) {
            Text("Save")
        }
    }
}
