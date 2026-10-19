package app.ytsleep.auth

import org.junit.Assert.*
import org.junit.Test

class CookieParserTest {

    @Test
    fun `name=value format is parsed`() {
        val input = "SAPISID=sapisid_value; SID=sid_value; __Secure-3PAPISID=apisid_val"
        val result = CookieParser.parse(input)
        assertEquals("sapisid_value", result["SAPISID"])
        assertEquals("sid_value", result["SID"])
        assertEquals("apisid_val", result["__Secure-3PAPISID"])
    }

    @Test
    fun `only allowed cookie names are kept`() {
        val input = "SAPISID=val; RANDOM_COOKIE=should_be_dropped; SID=sid"
        val result = CookieParser.parse(input)
        assertFalse(result.containsKey("RANDOM_COOKIE"))
        assertTrue(result.containsKey("SAPISID"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `input missing SAPISID is rejected`() {
        CookieParser.parse("SID=sid_value; HSID=hsid_value")
    }

    @Test
    fun `netscape format is parsed`() {
        val input = """
            # Netscape HTTP Cookie File
            .youtube.com	TRUE	/	TRUE	0	SAPISID	my_sapisid
            .youtube.com	TRUE	/	TRUE	0	SID	my_sid
        """.trimIndent()
        val result = CookieParser.parse(input)
        assertEquals("my_sapisid", result["SAPISID"])
        assertEquals("my_sid", result["SID"])
    }
}
