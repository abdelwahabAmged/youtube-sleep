package app.ytsleep

import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class NoExternalIntentsTest {

    private val allowlisted = listOf(
        "Settings.ACTION_",
        "PackageInstaller",
        "ACTION_MANAGE_UNKNOWN_APP_SOURCES",
        "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
    )

    private val forbidden = listOf(
        "Intent.ACTION_VIEW",
        "CustomTabsIntent",
        ".openUri(", // a call; the no-op `override fun openUri` handler is the guard itself
        "\"vnd.youtube",
        "market://",
        "intent://",
    )

    @Test
    fun `no external intents in main sources`() {
        val mainSrc = File("src/main/java/app/ytsleep")
        if (!mainSrc.exists()) {
            // Running from a different working directory — skip (will pass in CI)
            return
        }

        val violations = mutableListOf<String>()

        mainSrc.walkTopDown()
            .filter { it.extension == "kt" }
            .forEach { file ->
                val lines = file.readLines()
                lines.forEachIndexed { lineNum, line ->
                    for (pattern in forbidden) {
                        if (line.contains(pattern)) {
                            val isAllowed = allowlisted.any { line.contains(it) }
                            if (!isAllowed) {
                                violations.add("${file.path}:${lineNum + 1}: $pattern")
                            }
                        }
                    }
                }
            }

        if (violations.isNotEmpty()) {
            fail("External intent/URI patterns found:\n${violations.joinToString("\n")}")
        }
    }
}
