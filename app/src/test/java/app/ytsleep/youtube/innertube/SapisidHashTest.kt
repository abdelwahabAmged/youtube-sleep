package app.ytsleep.youtube.innertube

import app.ytsleep.youtube.innertube.SapisidHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class SapisidHashTest {

    @Test
    fun `header format is correct`() {
        val ts = 1700000000L
        val sapisid = "test_sapisid_value"
        val origin = "https://www.youtube.com"

        val expected = run {
            val input = "$ts $sapisid $origin"
            val sha1 = MessageDigest.getInstance("SHA-1")
                .digest(input.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            "SAPISIDHASH ${ts}_$sha1"
        }

        val result = SapisidHash.compute(ts, sapisid, origin)
        assertEquals(expected, result)
    }

    @Test
    fun `header starts with SAPISIDHASH prefix`() {
        val header = SapisidHash.compute(1700000000L, "sapisid", "https://www.youtube.com")
        assertTrue(header.startsWith("SAPISIDHASH "))
    }

    @Test
    fun `header contains timestamp and underscore separator`() {
        val ts = 1700000000L
        val header = SapisidHash.compute(ts, "sapisid", "https://www.youtube.com")
        assertTrue(header.contains("${ts}_"))
    }
}
