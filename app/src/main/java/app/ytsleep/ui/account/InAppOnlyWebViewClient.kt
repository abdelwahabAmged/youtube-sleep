package app.ytsleep.ui.account

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class InAppOnlyWebViewClient(
    private val onYouTubeReady: () -> Unit = {},
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url
        // Returning true cancels the navigation: anything else would leave the app (FR-020).
        val blocked = url.scheme !in setOf("https", "http") || !isAllowedHost(url.host)
        if (blocked) Log.w(TAG, "Blocked sign-in navigation to ${url.scheme}://${url.host}")
        return blocked
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        if (url.contains("youtube.com") && !url.contains("accounts.google.com")) {
            onYouTubeReady()
        }
    }
}

private const val TAG = "InAppOnlyWebView"

private val ALLOWED_DOMAINS = setOf("youtube.com", "gstatic.com", "googleusercontent.com")

/**
 * Google, including its country domains (google.com.eg, google.co.uk, google.de, ...): Google
 * finishes sign-in by setting cookies on the user's country domain.
 */
private val GOOGLE_DOMAIN = Regex("""(^|\.)google(\.com|\.co)?\.[a-z]{2,3}$""")

/** True for an allowed domain or one of its subdomains; look-alikes such as evilgoogle.com are rejected. */
internal fun isAllowedHost(host: String?): Boolean {
    if (host.isNullOrEmpty()) return false
    val h = host.lowercase()
    return GOOGLE_DOMAIN.containsMatchIn(h) || ALLOWED_DOMAINS.any { h == it || h.endsWith(".$it") }
}
