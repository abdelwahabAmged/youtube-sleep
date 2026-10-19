package app.ytsleep.auth

private val ALLOWED_COOKIES = setOf(
    "SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID",
    "SID", "HSID", "SSID", "APISID", "LOGIN_INFO",
    "__Secure-3PSID", "__Secure-1PSID",
)

object CookieParser {

    fun parse(input: String): Map<String, String> {
        val result = mutableMapOf<String, String>()

        if (input.lines().any { it.startsWith("#") || it.contains("\t") }) {
            // Netscape format
            for (line in input.lines()) {
                if (line.isBlank() || line.startsWith("#")) continue
                val parts = line.split("\t")
                if (parts.size >= 7) {
                    val name = parts[5].trim()
                    val value = parts[6].trim()
                    if (name in ALLOWED_COOKIES) result[name] = value
                }
            }
        } else {
            // name=value; name2=value2 format
            for (part in input.split(";")) {
                val idx = part.indexOf('=')
                if (idx < 0) continue
                val name = part.substring(0, idx).trim()
                val value = part.substring(idx + 1).trim()
                if (name in ALLOWED_COOKIES) result[name] = value
            }
        }

        require(result.containsKey("SAPISID")) { "Required cookie SAPISID is missing" }
        return result
    }
}
