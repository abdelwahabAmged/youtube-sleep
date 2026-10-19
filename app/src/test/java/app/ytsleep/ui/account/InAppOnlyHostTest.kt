package app.ytsleep.ui.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppOnlyHostTest {

    @Test
    fun `google and youtube hosts and their subdomains are allowed`() {
        listOf(
            "accounts.google.com", "google.com", "www.youtube.com", "accounts.youtube.com", "ssl.gstatic.com",
            "accounts.google.com.eg", "www.google.co.uk", "accounts.google.de", "lh3.googleusercontent.com",
        )
            .forEach { assertTrue(it, isAllowedHost(it)) }
    }

    @Test
    fun `look-alike and unrelated hosts are rejected`() {
        listOf(
            "evilgoogle.com", "google.com.evil.net", "notyoutube.com", "example.com", "evilgoogle.com.eg",
            "google.evil.com", "", null,
        )
            .forEach { assertFalse(it.toString(), isAllowedHost(it)) }
    }
}
