package edu.cuhk.csci3310.liftlog

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    fun showGoalAchievementNotification(context: Context, goalType: String) {
        val notificationId = if (goalType == "daily") 1001 else 1002
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(context, "liftlog_goal_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🎉 Goal Achieved!")
            .setContentText("Congratulations! You reached your $goalType goal in LiftLog 💪")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Congratulations! You reached your $goalType goal in LiftLog 💪")) // bigger text
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (areNotificationsEnabled()) {
                notify(notificationId, builder.build())
            }
        }
    }
}