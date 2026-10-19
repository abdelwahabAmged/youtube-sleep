package app.ytsleep.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {

    private class FakeTarget : FadeTarget {
        val volumes = mutableListOf<Float>()
        var paused = false
        override fun setVolume(volume: Float) { volumes += volume }
        override fun pause() { paused = true }
    }

    private fun TestScope.timer(durationMs: Long) = SleepTimer(
        kind = SleepTimerKind.DURATION,
        endsAtElapsedMs = durationMs,
        fadeMs = 10_000L,
        clock = { testScheduler.currentTime },
    )

    @Test
    fun `30 minute timer pauses at 30 minutes, not before`() = runTest {
        val target = FakeTarget()
        var finished = false
        val t = timer(30 * 60_000L)
        t.run(target, this, onFinished = { finished = true })

        advanceTimeBy(30 * 60_000L - 1)
        runCurrent()
        assertFalse(target.paused)

        advanceTimeBy(300)
        runCurrent()
        assertTrue(target.paused)
        assertTrue(finished)
    }

    @Test
    fun `volume fades during the last 10 seconds and is restored after pausing`() = runTest {
        val target = FakeTarget()
        val t = timer(60_000L)
        t.run(target, this, onFinished = {})

        advanceTimeBy(45_000L); runCurrent()
        assertTrue("no fade before the last 10 s", target.volumes.isEmpty())

        advanceTimeBy(10_000L); runCurrent()
        val fading = target.volumes.last()
        assertTrue("fading mid-way, was $fading", fading in 0.4f..0.6f)

        advanceTimeBy(6_000L); runCurrent()
        assertTrue(target.paused)
        assertEquals(1f, target.volumes.last())
    }

    @Test
    fun `remaining time counts down`() = runTest {
        val t = timer(60_000L)
        t.run(FakeTarget(), this, onFinished = {})
        runCurrent()
        assertEquals(60_000L, t.remainingMs.value)
        advanceTimeBy(20_000L); runCurrent()
        assertEquals(40_000L, t.remainingMs.value)
        t.cancel()
    }

    @Test
    fun `cancel stops the timer without pausing`() = runTest {
        val target = FakeTarget()
        val t = timer(60_000L)
        t.run(target, this, onFinished = {})
        advanceTimeBy(10_000L)
        t.cancel()
        advanceTimeBy(120_000L); runCurrent()
        assertFalse(target.paused)
        assertEquals(0L, t.remainingMs.value)
    }

    @Test
    fun `end-of-item fade volume`() {
        assertEquals(1f, SleepTimer.fadeVolume(60_000L, 10_000L))
        assertEquals(0.5f, SleepTimer.fadeVolume(5_000L, 10_000L))
        assertEquals(0f, SleepTimer.fadeVolume(-5L, 10_000L))
    }
}
