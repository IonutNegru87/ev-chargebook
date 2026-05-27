package io.github.inegru.chargebook.web.platform

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.round

private val monthNames = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

fun monthName(month1to12: Int): String =
    monthNames.getOrElse(month1to12 - 1) { month1to12.toString() }

/** `27 May 2026, 08:15` in the system timezone. */
fun Instant.formatDateTime(zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val dt = toLocalDateTime(zone)
    val hh = dt.hour.toString().padStart(2, '0')
    val mm = dt.minute.toString().padStart(2, '0')
    return "${dt.dayOfMonth} ${monthName(dt.monthNumber)} ${dt.year}, $hh:$mm"
}

/** Formats a minute count as `Xh Ym`, `Ym`, or `Xh`. */
fun formatDuration(minutes: Int): String {
    if (minutes <= 0) return "0m"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

/** Minutes between two instants (>= 0). */
fun minutesBetween(from: Instant, to: Instant): Int =
    ((to - from).inWholeSeconds / 60).toInt().coerceAtLeast(0)

fun Double.round1(): String = (round(this * 10) / 10).toString()

fun Double.round2(): String = (round(this * 100) / 100).toString()
