@file:Suppress("BidiSpoofing") // intentional LTR isolates around time ranges

package com.yahyaoui.prayermode.ui

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale

/** Clock style for every time on screen; set from AppConfig.clock (or the phone's setting). */
object TimeFmt {
    var is24 by mutableStateOf(true)
}

/** HH:MM or h:mm AM/PM, always with Latin digits. */
fun hm(t: LocalTime): String =
    if (TimeFmt.is24) String.format(Locale.US, "%02d:%02d", t.hour, t.minute)
    else DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).withDecimalStyle(DecimalStyle.STANDARD).format(t)
fun hm(i: Instant, zone: ZoneId): String = hm(i.atZone(zone).toLocalTime())

/** Keeps "a – b" in order inside right-to-left text. */
fun ltr(s: String) = "⁦$s⁩"
fun range(a: String, b: String, sep: String = " – ") = ltr("$a$sep$b")

fun countdown(d: Duration): String {
    val s = d.seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec) else String.format(Locale.US, "%02d:%02d", m, sec)
}

fun signedMin(v: Int) = when {
    v > 0 -> "+$v"
    v < 0 -> "−${-v}"
    else -> "0"
}
