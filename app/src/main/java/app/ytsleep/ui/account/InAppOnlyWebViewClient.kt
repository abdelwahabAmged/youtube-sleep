package app.ytsleep.ui.account

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class InAppOnlyWebViewClient(
    private val onYouTubeReady: () -> Unit = {},
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url
        // Returning true cancels the navigation: anything else would leave the app (FR-020).
        return url.scheme !in setOf("https", "http") || !isAllowedHost(url.host)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        if (url.contains("youtube.com") && !url.contains("accounts.google.com")) {
            onYouTubeReady()
        }
    }
}

private val ALLOWED_DOMAINS = setOf("google.com", "youtube.com", "gstatic.com")

/** True for an allowed domain or one of its subdomains; look-alikes such as evilgoogle.com are rejected. */
internal fun isAllowedHost(host: String?): Boolean {
    if (host.isNullOrEmpty()) return false
    val h = host.lowercase()
    return ALLOWED_DOMAINS.any { h == it || h.endsWith(".$it") }
}
