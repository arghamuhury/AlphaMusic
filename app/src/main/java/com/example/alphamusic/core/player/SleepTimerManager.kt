package com.example.alphamusic.core.player

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Singleton
class SleepTimerManager @Inject constructor(
    private val musicController: MusicController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timerJob: Job? = null

    fun start(durationMillis: Long = DEFAULT_DURATION_MILLIS) {
        timerJob?.cancel()
        timerJob = scope.launch {
            delay(durationMillis)
            musicController.pause()
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
    }

    private companion object {
        const val DEFAULT_DURATION_MILLIS = 30 * 60 * 1000L
    }
}
