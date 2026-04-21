package edu.cuhk.csci3310.liftlog.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton bridge between [RestTimerService] and [SpotterViewModel].
 *
 * The service writes to these flows; the ViewModel reads from them.
 * Using a singleton avoids the need to bind the service and keeps the
 * existing StateFlow-based architecture intact.
 */
object RestTimerState {
    private val _timeRemaining = MutableStateFlow(0)
    val timeRemaining = _timeRemaining.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    /** Emits one Unit when the countdown reaches zero. */
    private val _timerFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val timerFinished = _timerFinished.asSharedFlow()

    // --- written by RestTimerService only ---

    internal fun setTimeRemaining(seconds: Int) {
        _timeRemaining.value = seconds
    }

    internal fun setRunning(running: Boolean) {
        _isRunning.value = running
    }

    internal fun emitFinished() {
        _timerFinished.tryEmit(Unit)
    }
}
