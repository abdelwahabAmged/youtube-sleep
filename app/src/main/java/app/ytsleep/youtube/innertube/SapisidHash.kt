package app.ytsleep.youtube.innertube

import java.security.MessageDigest

object SapisidHash {

    fun compute(timestampSeconds: Long, sapisid: String, origin: String): String {
        val input = "$timestampSeconds $sapisid $origin"
        val digest = MessageDigest.getInstance("SHA-1")
        val sha1 = digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH ${timestampSeconds}_$sha1"
    }

    fun header(sapisid: String, origin: String = "https://www.youtube.com"): String {
        val ts = System.currentTimeMillis() / 1000
        return compute(ts, sapisid, origin)
    }
}
