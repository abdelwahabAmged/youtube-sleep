package app.ytsleep.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SleepTimerKind { DURATION, END_OF_ITEM }

interface FadeTarget {
    fun setVolume(volume: Float)
    fun pause()
}

class SleepTimer(
    val kind: SleepTimerKind,
    /** For DURATION: deadline on the [clock] timeline (elapsed-realtime ms). */
    val endsAtElapsedMs: Long?,
    val fadeMs: Long = 10_000L,
    private val clock: () -> Long = { android.os.SystemClock.elapsedRealtime() },
) {
    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private var job: Job? = null

    /** Runs a DURATION timer: fades over the last [fadeMs], then pauses and calls [onFinished]. */
    fun run(target: FadeTarget, scope: CoroutineScope, onFinished: () -> Unit) {
        job?.cancel()
        val endAt = endsAtElapsedMs ?: return
        job = scope.launch {
            while (isActive) {
                val remaining = endAt - clock()
                _remainingMs.value = remaining.coerceAtLeast(0L)
                if (remaining <= 0L) {
                    target.pause()
                    target.setVolume(1f)
                    onFinished()
                    return@launch
                }
                if (remaining <= fadeMs) target.setVolume(remaining.toFloat() / fadeMs)
                delay(minOf(250L, remaining))
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = 0L
    }

    companion object {
        /** Volume for an END_OF_ITEM fade given the time left in the current item. */
        fun fadeVolume(timeLeftMs: Long, fadeMs: Long): Float =
            if (timeLeftMs >= fadeMs) 1f else (timeLeftMs.coerceAtLeast(0L).toFloat() / fadeMs)
    }
}
