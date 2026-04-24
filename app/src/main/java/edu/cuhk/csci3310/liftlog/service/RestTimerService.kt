package edu.cuhk.csci3310.liftlog.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import edu.cuhk.csci3310.liftlog.MainActivity
import edu.cuhk.csci3310.liftlog.R
import edu.cuhk.csci3310.liftlog.toTimerString
import edu.cuhk.csci3310.liftlog.service.RestTimerService.Companion.ACTION_SKIP
import edu.cuhk.csci3310.liftlog.service.RestTimerService.Companion.ACTION_START
import edu.cuhk.csci3310.liftlog.service.RestTimerService.Companion.ACTION_STOP
import edu.cuhk.csci3310.liftlog.service.RestTimerService.Companion.EXTRA_DURATION_SECONDS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val _timerFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val timerFinished = _timerFinished.asSharedFlow()

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

/**
 * Foreground service that drives the rest-period countdown timer.
 *
 * Commands are delivered via [Intent] extras on [onStartCommand]:
 *   - [ACTION_START] with [EXTRA_DURATION_SECONDS] — begin a new countdown
 *   - [ACTION_STOP]  — cancel the countdown and stop the service
 *   - [ACTION_SKIP]  — treat the timer as finished immediately
 *
 * Timer state is published through the [RestTimerState] singleton so that
 * [edu.cuhk.csci3310.liftlog.ui.viewmodel.SpotterViewModel] can observe it
 * without needing to bind to this service.
 */
class RestTimerService : Service() {

    companion object {
        const val ACTION_START = "edu.cuhk.csci3310.liftlog.REST_TIMER_START"
        const val ACTION_STOP = "edu.cuhk.csci3310.liftlog.REST_TIMER_STOP"
        const val ACTION_SKIP = "edu.cuhk.csci3310.liftlog.REST_TIMER_SKIP"

        const val EXTRA_DURATION_SECONDS = "duration_seconds"

        const val NOTIFICATION_CHANNEL_ID = "rest_timer"
        private const val NOTIFICATION_ID = 1001
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var job: Job? = null

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val duration = intent.getIntExtra(EXTRA_DURATION_SECONDS, 0)
                if (duration > 0) startCountdown(duration)
            }

            ACTION_STOP -> stopTimer()
            ACTION_SKIP -> skipTimer()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        RestTimerState.setRunning(false)
        RestTimerState.setTimeRemaining(0)
    }

    private fun startCountdown(durationSeconds: Int) {
        job?.cancel()

        RestTimerState.setTimeRemaining(durationSeconds)
        RestTimerState.setRunning(true)

        // Promote to foreground immediately so Android doesn't kill us
        startForeground(NOTIFICATION_ID, buildNotification(durationSeconds))

        job = scope.launch {
            var remaining = durationSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                RestTimerState.setTimeRemaining(remaining)
                notificationManager.notify(NOTIFICATION_ID, buildNotification(remaining))
            }
            onCountdownFinished()
        }
    }

    private fun skipTimer() {
        job?.cancel()
        onCountdownFinished()
    }

    private fun stopTimer() {
        job?.cancel()
        RestTimerState.setRunning(false)
        RestTimerState.setTimeRemaining(0)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onCountdownFinished() {
        RestTimerState.setRunning(false)
        RestTimerState.setTimeRemaining(0)
        RestTimerState.emitFinished()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(secondsRemaining: Int): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Rest Timer")
            .setContentText("Rest: ${secondsRemaining.toTimerString()} remaining")
            .setContentIntent(tapPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
