package edu.cuhk.csci3310.liftlog

import kotlin.time.DurationUnit
import kotlin.time.toDuration

// ── Time / Duration formatting ────────────────────────────────────────────────

/**
 * Formats a number of seconds as a `"M:SS"` countdown string (e.g. `"1:05"`, `"0:45"`).
 * Used by the rest-timer notification and the spotter-screen countdown display.
 */
fun Int.toTimerString(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * Formats a millisecond duration as `"MM minutes SS seconds"` (e.g. `"05 minutes 30 seconds"`).
 * Used on the Session Completed screen.
 */
fun Long.toVerboseDuration(): String =
    this.toDuration(DurationUnit.MILLISECONDS)
        .toComponents { minutes, seconds, _ ->
            "%02d minutes %02d seconds".format(minutes, seconds)
        }

/**
 * Formats a millisecond duration as `"MMmSSs"` (e.g. `"05m30s"`).
 * Used on session cards in the workout log.
 */
fun Long.toCompactDuration(): String =
    this.toDuration(DurationUnit.MILLISECONDS)
        .toComponents { minutes, seconds, _ ->
            "%02dm%02ds".format(minutes, seconds)
        }

/**
 * Formats a millisecond duration as `"X min"` (e.g. `"45 min"`), or `"—"` when zero.
 * Used on the Stats screen's Average Session Duration card.
 */
fun Long.toAverageMinutes(): String {
    val minutes = this / 60_000
    return if (minutes == 0L) "—" else "$minutes min"
}

// ─────────────────────────────────────────────────────────────────────────────

fun String.titlecase(): String {
    return this.split(' ').joinToString(" ") { word ->
        word.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }
}

fun Long.formatWithCommas(): String = "%,d".format(this)

fun Double.formatWeight(): String =
    if (this == kotlin.math.floor(this)) "%.0f".format(this) else "%.1f".format(this)
