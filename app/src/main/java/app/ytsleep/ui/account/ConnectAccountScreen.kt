package app.ytsleep.ui.account

import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import app.ytsleep.YtSleepApp
import app.ytsleep.auth.CookieParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ConnectAccountScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as YtSleepApp).container

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    // The phone's real WebView user agent minus the "; wv" marker, so Google
                    // sees a current mobile Chrome instead of an embedded browser.
                    userAgentString = WebSettings.getDefaultUserAgent(ctx).replace("; wv", "")
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                webViewClient = InAppOnlyWebViewClient(
                    onYouTubeReady = {
                        val rawCookies = cookieManager.getCookie("https://www.youtube.com") ?: return@InAppOnlyWebViewClient
                        try {
                            val parsed = CookieParser.parse(rawCookies)
                            scope.launch(Dispatchers.IO) {
                                container.accountStore.save(parsed)
                            }
                            navController.popBackStack()
                        } catch (_: Exception) {
                            // SAPISID missing — stay on sign-in page
                        }
                    }
                )
                loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&continue=https://www.youtube.com/")
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
