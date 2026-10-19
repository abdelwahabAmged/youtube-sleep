package app.ytsleep.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

@UnstableApi
@RunWith(AndroidJUnit4::class)
class PlaybackServiceSmokeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var controller: MediaController

    @Before
    fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
        // MediaController must be created and used on the main looper.
        instrumentation.runOnMainSync { future = MediaController.Builder(context, token).buildAsync() }
        controller = future.get(10, TimeUnit.SECONDS)
    }

    @After
    fun release() {
        instrumentation.runOnMainSync {
            controller.stop()
            controller.clearMediaItems()
            controller.release()
        }
    }

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }

    private fun waitUntil(timeoutMs: Long = 10_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!onMain(condition)) {
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for player" }
            Thread.sleep(100)
        }
    }

    private fun loadTestAudio() {
        val file = File(context.cacheDir, "test_audio.wav")
        instrumentation.context.assets.open("test_audio.wav").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        onMain {
            controller.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            controller.prepare()
        }
        waitUntil { controller.playbackState == Player.STATE_READY }
    }

    @Test
    fun seekIncrements_are15BackAnd30Forward() {
        assertEquals(15_000L, onMain { controller.seekBackIncrement })
        assertEquals(30_000L, onMain { controller.seekForwardIncrement })
    }

    @Test
    fun play_localAudio_isPlaying() {
        loadTestAudio()
        onMain { controller.play() }
        waitUntil { controller.isPlaying }
    }

    @Test
    fun seekForward_adds30Seconds() {
        loadTestAudio()
        val before = onMain { controller.currentPosition }
        onMain { controller.seekForward() }
        waitUntil { controller.currentPosition >= before + 29_000 }
        assertTrue(onMain { controller.currentPosition } <= before + 31_000)
    }

    @Test
    fun player_hasNoVideoTrack() {
        loadTestAudio()
        val tracks = onMain { controller.currentTracks }
        assertTrue(tracks.groups.isNotEmpty())
        assertTrue(tracks.groups.none { it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO })
    }
}
