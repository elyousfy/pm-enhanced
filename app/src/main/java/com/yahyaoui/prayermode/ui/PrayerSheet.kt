package com.yahyaoui.prayermode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yahyaoui.prayermode.DndController
import com.yahyaoui.prayermode.R
import com.yahyaoui.prayermode.core.AppConfig
import com.yahyaoui.prayermode.core.Prayer
import kotlinx.coroutines.launch

private enum class StartMode { ADHAN, ABLUTION, IQAMA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PmSheet(onDismiss: () -> Unit, content: @Composable (close: () -> Unit) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Pm.surface,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(32.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Pm.line2))
        },
    ) { content(close) }
}

/** Per-prayer settings: enable, start mode, iqama, length, tune. Edits save at once. */
@Composable
fun PrayerSheet(prayer: Prayer, config: AppConfig, today: TodayData?, update: Update, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val res = LocalResources.current
    val pc = config.settings.prayer(prayer)
    val mode = when {
        pc.ablution -> StartMode.ABLUTION
        pc.iqamaMin > 0 -> StartMode.IQAMA
        else -> StartMode.ADHAN
    }
    fun change(f: (com.yahyaoui.prayermode.core.PrayerConfig) -> com.yahyaoui.prayermode.core.PrayerConfig) = update { it.withPrayer(prayer, f) }
    // Preview on a weekday plan so Dhuhr shows its own window, not Jumuʿah.
    val slot = today?.let { d ->
        val day = if (d.day.date.dayOfWeek == java.time.DayOfWeek.FRIDAY && prayer == Prayer.DHUHR) d.day.copy(date = d.day.date.minusDays(1)) else d.day
        val shifted = dayPlan(day, null, config.copy(settings = config.settings.copy(activeDays = java.time.DayOfWeek.entries.toSet())), d.zone).slots.first { it.prayer == prayer }
        shifted to d.zone
    }

    PmSheet(onDismiss) { close ->
        Column(Modifier.navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow(stringResource(R.string.prayer))
                    Text(DndController.prayerLabel(ctx, prayer.key), style = PmType.heading, color = Pm.text)
                    if (slot != null) Text(stringResource(R.string.adhan) + " " + ltr(hm(slot.first.at, slot.second)), style = PmType.body, color = Pm.sub)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow(stringResource(R.string.silence))
                    PmSwitch(pc.enabled, { on -> change { it.copy(enabled = on) } })
                }
            }
            RowDivider()
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Collapse(pc.enabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        val w = slot?.first?.window
                        if (w != null) {
                            Row(
                                Modifier.fillMaxWidth().clip(Pm.ctlShape).background(Pm.bg).border(1.dp, Pm.line, Pm.ctlShape).padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Eyebrow(stringResource(R.string.todays_silence), Modifier.weight(1f))
                                Text(range(hm(w.start, slot.second), hm(w.end, slot.second), " → "), style = PmType.time.copy(fontSize = 18.sp), color = Pm.text)
                            }
                        }
                        Field(stringResource(R.string.silence_starts)) {
                            Segmented(
                                listOf(StartMode.ADHAN to stringResource(R.string.mode_adhan), StartMode.ABLUTION to stringResource(R.string.mode_ablution), StartMode.IQAMA to stringResource(R.string.mode_iqama)),
                                mode,
                                { m ->
                                    change {
                                        when (m) {
                                            StartMode.ADHAN -> it.copy(ablution = false, iqamaMin = 0)
                                            StartMode.ABLUTION -> it.copy(ablution = true, iqamaMin = 0)
                                            StartMode.IQAMA -> it.copy(ablution = false, iqamaMin = if (it.iqamaMin > 0) it.iqamaMin else 10)
                                        }
                                    }
                                },
                            )
                            Text(
                                stringResource(when (mode) { StartMode.ADHAN -> R.string.help_adhan; StartMode.ABLUTION -> R.string.help_ablution; StartMode.IQAMA -> R.string.help_iqama }),
                                style = PmType.rowSub, color = Pm.sub,
                            )
                        }
                        Collapse(mode == StartMode.IQAMA) {
                            StepRow(stringResource(R.string.iqama_after), pc.iqamaMin, 5..30, 5) { v -> change { it.copy(iqamaMin = v) } }
                        }
                        StepRow(stringResource(R.string.silence_length), pc.durationMin, 15..60, 5) { v -> change { it.copy(durationMin = v) } }
                        StepRow(stringResource(R.string.adjust_adhan), pc.tuneMin, -30..30, 1, signed = true) { v -> change { it.copy(tuneMin = v) } }
                        if (prayer == Prayer.DHUHR) Text(stringResource(R.string.friday_note), style = PmType.rowSub, color = Pm.sub)
                    }
                }
                PrimaryButton(stringResource(R.string.done), close, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun Field(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(label)
        content()
    }
}

/** Label on the start side, minute stepper on the end side. */
@Composable
fun StepRow(label: String, value: Int, range: IntRange, step: Int, signed: Boolean = false, sub: String? = null, onChange: (Int) -> Unit) {
    val ctx = LocalContext.current
    val res = LocalResources.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = PmType.rowTitle, color = Pm.text)
            if (sub != null) Text(sub, style = PmType.rowSub, color = Pm.sub)
        }
        Stepper(value, onChange, range, step) { res.getString(R.string.min_value, if (signed) signedMin(it) else it.toString()) }
    }
}
