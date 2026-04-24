package edu.cuhk.csci3310.liftlog.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import edu.cuhk.csci3310.liftlog.formatWithCommas
import edu.cuhk.csci3310.liftlog.toAverageMinutes
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

class LiftLogWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val dao = LiftLogDatabase.getInstance(context).sessionDao()

        val todayVolume = dao.getTodayVolume(startOfDay, endOfDay).first()
        val monthlyVolume = dao.getMonthlyVolume(startOfMonth).first()
        val monthlySessions = dao.getMonthlySessionCount(startOfMonth).first()
        val averageDuration = dao.getAverageSessionDuration(startOfMonth).first()

        val monthLabel = today.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())

        // open the routine picker dialog instead of directly launching the app
        val pickerIntent = Intent(context, RoutinePickerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    monthLabel = monthLabel,
                    todayVolume = todayVolume,
                    monthlyVolume = monthlyVolume,
                    monthlySessions = monthlySessions,
                    averageDuration = averageDuration,
                    pickerIntent = pickerIntent,
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    monthLabel: String,
    todayVolume: Long,
    monthlyVolume: Long,
    monthlySessions: Int,
    averageDuration: Long,
    pickerIntent: Intent,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp),
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = monthLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
            )
        }
        Spacer(GlanceModifier.height(10.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            StatCell(
                label = "Today's Volume",
                value = "${todayVolume.formatWithCommas()} kg",
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(8.dp))
            StatCell(
                label = "Monthly Sessions",
                value = monthlySessions.toString(),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            StatCell(
                label = "Monthly Volume",
                value = "${monthlyVolume.formatWithCommas()} kg",
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(8.dp))
            StatCell(
                label = "Average Duration",
                value = averageDuration.toAverageMinutes(),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.primaryContainer)
                .clickable(actionStartActivity(pickerIntent))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = "Start Logging",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier
            .background(GlanceTheme.colors.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 10.sp,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = value,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}
