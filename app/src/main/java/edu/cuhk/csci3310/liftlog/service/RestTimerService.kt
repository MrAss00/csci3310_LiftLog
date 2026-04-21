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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        const val ACTION_STOP  = "edu.cuhk.csci3310.liftlog.REST_TIMER_STOP"
        const val ACTION_SKIP  = "edu.cuhk.csci3310.liftlog.REST_TIMER_SKIP"

        const val EXTRA_DURATION_SECONDS = "duration_seconds"

        const val NOTIFICATION_CHANNEL_ID = "rest_timer"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var countdownJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

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
        serviceScope.cancel()
        RestTimerState.setRunning(false)
        RestTimerState.setTimeRemaining(0)
    }

    // ------------------------------------------------------------------
    // Timer logic
    // ------------------------------------------------------------------

    private fun startCountdown(durationSeconds: Int) {
        countdownJob?.cancel()

        RestTimerState.setTimeRemaining(durationSeconds)
        RestTimerState.setRunning(true)

        // Promote to foreground immediately so Android doesn't kill us
        startForeground(NOTIFICATION_ID, buildNotification(durationSeconds))

        countdownJob = serviceScope.launch {
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
        countdownJob?.cancel()
        onCountdownFinished()
    }

    private fun stopTimer() {
        countdownJob?.cancel()
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

    // ------------------------------------------------------------------
    // Notification helpers
    // ------------------------------------------------------------------

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
            .setContentText("Rest: ${formatTime(secondsRemaining)} remaining")
            .setContentIntent(tapPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
