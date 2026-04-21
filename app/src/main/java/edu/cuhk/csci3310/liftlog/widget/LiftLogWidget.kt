package edu.cuhk.csci3310.liftlog.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

class LiftLogWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // read daily goal from SharedPreferences
        val prefs = context.getSharedPreferences("liftlog_prefs", Context.MODE_PRIVATE)
        val dailyGoal = prefs.getLong("daily_goal_kg", 2000L)

        // read today's volume from Room
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val db = LiftLogDatabase.getInstance(context)
        val todayVolume = db.sessionDao().getTodayVolume(startOfDay, endOfDay).first()

        // build launch intent: open app at Log tab with picker auto-opened
        val launchIntent = Intent(
            Intent.ACTION_VIEW,
            "liftlog://log?openPicker=true".toUri(),
            context,
            edu.cuhk.csci3310.liftlog.MainActivity::class.java,
        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    todayVolume = todayVolume,
                    dailyGoal = dailyGoal,
                    launchIntent = launchIntent,
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    todayVolume: Long,
    dailyGoal: Long,
    launchIntent: Intent,
) {
    val progress =
        if (dailyGoal > 0) (todayVolume.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f) else 0f
    val goalReached = todayVolume >= dailyGoal

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.Top,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = "LiftLog",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (goalReached) {
                Text(
                    text = "Goal reached!",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = "Daily Goal",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp,
            ),
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = "$todayVolume / $dailyGoal kg",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier.fillMaxWidth(),
            color = if (goalReached) GlanceTheme.colors.tertiary else GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.surfaceVariant,
        )
        Spacer(GlanceModifier.defaultWeight())
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.primaryContainer)
                .clickable(actionStartActivity(launchIntent))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = "Start Logging",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}
