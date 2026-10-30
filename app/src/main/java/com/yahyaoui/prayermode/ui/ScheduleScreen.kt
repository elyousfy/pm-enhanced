package com.yahyaoui.prayermode.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import androidx.compose.foundation.selection.toggleable
import com.yahyaoui.prayermode.R
import com.yahyaoui.prayermode.core.AppConfig
import java.time.LocalTime

@Composable
fun TopBar(title: String) {
    Box(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
        Text(title, style = PmType.title, color = Pm.text)
    }
}

@Composable
fun ScreenColumn(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        TopBar(title)
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) { content() }
    }
}

/** Row whose whole surface toggles the switch. */
@Composable
fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = PmType.rowTitle, color = Pm.text)
            if (sub != null) Text(sub, style = PmType.rowSub, color = Pm.sub)
        }
        PmSwitch(checked, null)
    }
}

private fun minutes(t: LocalTime) = t.hour * 60 + t.minute
private fun ofMinutes(m: Int): LocalTime = LocalTime.of(m / 60, m % 60)

@Composable
fun ScheduleScreen(config: AppConfig, update: Update) {
    val ctx = LocalContext.current
    val res = LocalResources.current
    val locale = currentLocale()
    val s = config.settings
    ScreenColumn(stringResource(R.string.nav_schedule)) {
        Section(stringResource(R.string.active_days), aside = daysSummary(LocalResources.current, s.activeDays, config.weekStartsSunday, locale), help = stringResource(R.string.days_help)) {
            PmCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        weekOrder(config.weekStartsSunday).forEach { d ->
                            val on = d in s.activeDays
                            DayChip(dayShort(d, locale), dayFull(d, locale), on, Modifier.weight(1f)) {
                                update { c -> c.copy(settings = c.settings.copy(activeDays = if (on) c.settings.activeDays - d else c.settings.activeDays + d)) }
                            }
                        }
                    }
                    Text(stringResource(R.string.week_starts_on), style = PmType.rowSub, color = Pm.sub)
                    Segmented(
                        listOf(false to stringResource(R.string.monday), true to stringResource(R.string.sunday)),
                        config.weekStartsSunday,
                        { v -> update { it.copy(weekStartsSunday = v) } },
                    )
                }
            }
        }

        Section(stringResource(R.string.before_silence)) {
            PmCard {
                SwitchRow(stringResource(R.string.takbir_title), stringResource(R.string.takbir_sub), s.takbir) { v ->
                    update { it.copy(settings = it.settings.copy(takbir = v)) }
                }
            }
        }

        Section(stringResource(R.string.friday_jumua)) {
            PmCard {
                val j = s.jumua
                fun setJ(f: (com.yahyaoui.prayermode.core.JumuaConfig) -> com.yahyaoui.prayermode.core.JumuaConfig) =
                    update { it.copy(settings = it.settings.copy(jumua = f(it.settings.jumua))) }
                SwitchRow(stringResource(R.string.use_jumua), stringResource(R.string.use_jumua_sub), j.enabled) { v -> setJ { it.copy(enabled = v) } }
                Collapse(j.enabled) {
                    Column {
                        RowDivider()
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.jumua_time), style = PmType.rowTitle, color = Pm.text, modifier = Modifier.weight(1f))
                                if (config.location.countryCode == "AE" && j.fixedTime == LocalTime.of(12, 45)) StatusPill(stringResource(R.string.uae_fixed), PillTone.OK)
                            }
                            Segmented(
                                listOf(true to stringResource(R.string.jumua_fixed), false to stringResource(R.string.jumua_dhuhr)),
                                j.fixedTime != null,
                                { fixed -> setJ { it.copy(fixedTime = if (fixed) LocalTime.of(12, 45) else null) } },
                            )
                            Collapse(j.fixedTime != null) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Stepper(minutes(j.fixedTime ?: LocalTime.of(12, 45)), { v -> setJ { it.copy(fixedTime = ofMinutes(v)) } }, 600..900, 5) { ltr(hm(ofMinutes(it))) }
                                }
                            }
                            StepRow(stringResource(R.string.silence_before), j.beforeMin, 0..60, 5) { v -> setJ { it.copy(beforeMin = v) } }
                            StepRow(stringResource(R.string.silence_after), j.afterMin, 10..90, 5) { v -> setJ { it.copy(afterMin = v) } }
                            j.fixedTime?.let { t ->
                                Text(
                                    stringResource(R.string.fridays_summary, range(hm(t.minusMinutes(j.beforeMin.toLong())), hm(t.plusMinutes(j.afterMin.toLong())))),
                                    style = PmType.rowSub.copy(fontWeight = FontWeight.Medium), color = Pm.accText,
                                )
                            }
                        }
                    }
                }
            }
        }

        Section(stringResource(R.string.ramadan_eid), help = stringResource(R.string.ramadan_schedule_sub)) {
            PmCard {
                val r = s.ramadan
                fun setR(f: (com.yahyaoui.prayermode.core.RamadanConfig) -> com.yahyaoui.prayermode.core.RamadanConfig) =
                    update { it.copy(settings = it.settings.copy(ramadan = f(it.settings.ramadan))) }
                SwitchRow(stringResource(R.string.isha_taraweeh), stringResource(R.string.isha_taraweeh_sub), r.taraweehEnabled) { v -> setR { it.copy(taraweehEnabled = v) } }
                Collapse(r.taraweehEnabled) {
                    Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                        StepRow(stringResource(R.string.silence_length), r.taraweehDurationMin, 30..150, 15) { v -> setR { it.copy(taraweehDurationMin = v) } }
                    }
                }
                RowDivider()
                SwitchRow(stringResource(R.string.tahajjud), stringResource(R.string.tahajjud_sub), r.tahajjudBeforeMin > 0) { v -> setR { it.copy(tahajjudBeforeMin = if (v) 60 else 0) } }
                Collapse(r.tahajjudBeforeMin > 0) {
                    Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                        StepRow(stringResource(R.string.silence_length), r.tahajjudBeforeMin.coerceAtLeast(15), 15..90, 15) { v -> setR { it.copy(tahajjudBeforeMin = v) } }
                    }
                }
                RowDivider()
                val e = s.eid
                fun setE(f: (com.yahyaoui.prayermode.core.EidConfig) -> com.yahyaoui.prayermode.core.EidConfig) =
                    update { it.copy(settings = it.settings.copy(eid = f(it.settings.eid))) }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.eid_time_prayer), style = PmType.rowTitle, color = Pm.text)
                    Segmented(
                        listOf(false to stringResource(R.string.eid_after_sunrise), true to stringResource(R.string.jumua_fixed)),
                        e.fixedTime != null,
                        { fixed -> setE { it.copy(fixedTime = if (fixed) LocalTime.of(6, 30) else null) } },
                    )
                    Collapse(e.fixedTime != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Stepper(minutes(e.fixedTime ?: LocalTime.of(6, 30)), { v -> setE { it.copy(fixedTime = ofMinutes(v)) } }, 300..540, 5) { ltr(hm(ofMinutes(it))) }
                        }
                    }
                    StepRow(stringResource(R.string.eid_silence), e.durationMin, 15..90, 15) { v -> setE { it.copy(durationMin = v) } }
                }
            }
        }
    }
}

/** Day chip: filled green when on, springs on toggle. */
@Composable
private fun DayChip(label: String, full: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Pm.acc else Color.Transparent, tween(Pm.PRESS), label = "dayBg")
    val scale by animateFloatAsState(if (on) 1f else 0.94f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "dayScale")
    Box(
        modifier.scale(scale).heightIn(min = 40.dp).clip(Pm.pill).background(bg).border(1.dp, if (on) Pm.acc else Pm.line2, Pm.pill)
            .semantics { contentDescription = full }
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = PmType.rowSub.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium), color = if (on) Pm.onAcc else Pm.sub, maxLines = 1)
    }
}
