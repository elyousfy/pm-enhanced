package com.yahyaoui.prayermode.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.yahyaoui.prayermode.DndController
import com.yahyaoui.prayermode.R
import com.yahyaoui.prayermode.core.AppConfig
import com.yahyaoui.prayermode.core.Prayer
import com.yahyaoui.prayermode.core.TimeSource
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun TodayScreen(
    config: AppConfig,
    update: Update,
    reload: Int,
    onRetry: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenPrayer: (Prayer) -> Unit,
    toast: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val res = LocalResources.current
    val locale = currentLocale()
    val state by rememberToday(config, reload)
    val now by rememberNow()
    val perms = rememberPermissions()
    val s = config.settings
    val ready = state as? TodayState.Ready
    val zone = ready?.data?.zone ?: com.yahyaoui.prayermode.PrayerScheduler.zoneOf(config)
    val date = LocalDate.ofInstant(now, zone)

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        // Top bar
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow(DateTimeFormatter.ofPattern("EEEE d MMMM", locale).format(date))
                Text(stringResource(R.string.pm_title), style = PmType.title, color = Pm.text)
            }
            PmSwitch(s.masterOn, { on ->
                setMaster(ctx, on)
                toast(res.getString(if (on) R.string.pm_on_toast else R.string.pm_off_toast))
            })
        }

        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Banners(perms)

            val hero = ready?.let { heroInfo(it.data, config, now) }
            StatusRow(config, perms[Perm.DND] == true, date, hero?.silentNow == true, ready?.data, onOpenLocation)

            when (val st = state) {
                is TodayState.Loading -> PmCard { Text(stringResource(R.string.loading_times), style = PmType.body, color = Pm.sub, modifier = Modifier.padding(18.dp)) }
                is TodayState.Failed -> PmCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.load_failed), style = PmType.body, color = Pm.text)
                        if (st.message != null) Text(st.message, style = PmType.rowSub, color = Pm.faint)
                        SecondaryButton(stringResource(R.string.retry), onRetry)
                    }
                }
                is TodayState.Ready -> {
                    Hero(hero!!, config, now, st.data.zone)
                    TimesSection(st.data, config, now, hero, update, onOpenPrayer)
                }
            }

            DaysCard(config, date, onOpenSchedule, locale)
        }
    }
}

@Composable
private fun Banners(perms: Map<Perm, Boolean>) {
    val ctx = LocalContext.current
    val res = LocalResources.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Collapse(perms[Perm.DND] == false) {
            WarnBanner(stringResource(R.string.dnd_banner_title), stringResource(R.string.dnd_banner_body)) {
                ctx.startActivity(Permissions.settingsIntent(ctx, Perm.DND))
            }
        }
        Collapse(perms[Perm.DND] == true && perms[Perm.ALARMS] == false) {
            WarnBanner(stringResource(R.string.alarm_banner_title), stringResource(R.string.alarm_banner_body)) {
                ctx.startActivity(Permissions.settingsIntent(ctx, Perm.ALARMS))
            }
        }
    }
}

@Composable
fun Collapse(visible: Boolean, content: @Composable () -> Unit) = AnimatedVisibility(
    visible,
    enter = expandVertically(tween(Pm.COLLAPSE, easing = Pm.ease)) + fadeIn(tween(Pm.COLLAPSE)) +
        slideInVertically(tween(Pm.COLLAPSE, easing = Pm.ease)) { -it / 8 },
    exit = shrinkVertically(tween(Pm.COLLAPSE, easing = Pm.ease)) + fadeOut(tween(Pm.COLLAPSE)),
) { content() }

@Composable
fun WarnBanner(title: String, body: String, onAllow: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Pm.cardShape).background(Pm.warnWash).border(1.dp, Pm.warn.copy(alpha = 0.35f), Pm.cardShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PmIcons.warning, null, tint = Pm.warn, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = PmType.button, color = Pm.text)
            Text(body, style = PmType.rowSub, color = Pm.sub)
        }
        SecondaryButton(stringResource(R.string.allow), onAllow)
    }
}

@Composable
private fun StatusRow(config: AppConfig, dnd: Boolean, date: LocalDate, silent: Boolean, data: TodayData?, onOpenLocation: () -> Unit) {
    val s = config.settings
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            !s.masterOn -> StatusPill(stringResource(R.string.pm_off), PillTone.NEUTRAL)
            !dnd -> StatusPill(stringResource(R.string.needed_dnd), PillTone.WARN)
            date.dayOfWeek !in s.activeDays -> StatusPill(stringResource(R.string.off_today), PillTone.NEUTRAL)
            silent -> BreathingPill(stringResource(R.string.silent_now))
            else -> StatusPill(stringResource(R.string.sound_on), PillTone.NEUTRAL)
        }
        Spacer(Modifier.weight(1f))
        val source = if ((data?.source ?: config.source) == TimeSource.AWQAF) R.string.source_awqaf_short else R.string.source_aladhan_short
        Row(
            Modifier.clip(Pm.ctlShape).clickable(onClick = onOpenLocation).padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(PmIcons.pin, null, tint = Pm.sub, modifier = Modifier.size(16.dp))
            Text(
                "${config.location.city} · ${stringResource(source)}" + if (data?.fellBack == true) " ⚠" else "",
                style = PmType.rowSub, color = Pm.sub, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Ok pill with a breathing halo behind its dot. */
@Composable
fun BreathingPill(text: String) {
    val t = rememberInfiniteTransition(label = "breath")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    val bg by t.animateColor(Pm.accWash, Pm.accText.copy(alpha = 0.26f).compositeOver(Pm.surface), 2400)
    val k = (phase / 0.7f).coerceAtMost(1f)
    Row(
        Modifier.clip(Pm.pill).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(7.dp).scale(1f + 1.6f * k).alpha(0.55f * (1f - k)).clip(CircleShape).background(Pm.accText))
            Dot(Pm.accText, 7)
        }
        Text(if (LocalLayoutDirection.current == LayoutDirection.Rtl) text else text.uppercase(), style = PmType.pill, color = Pm.accText)
    }
}

@Composable
private fun androidx.compose.animation.core.InfiniteTransition.animateColor(a: Color, b: Color, ms: Int): androidx.compose.runtime.State<Color> {
    val f = animateFloat(0f, 1f, infiniteRepeatable(tween(ms), RepeatMode.Reverse), label = "breathColor")
    return remember(f) { androidx.compose.runtime.derivedStateOf { androidx.compose.ui.graphics.lerp(a, b, f.value) } }
}

@Composable
private fun Hero(h: HeroInfo, config: AppConfig, now: Instant, zone: java.time.ZoneId) {
    val ctx = LocalContext.current
    val res = LocalResources.current
    val locale = currentLocale()
    val s = config.settings
    val bg by animateColorAsState(if (h.silentNow) Pm.accWashLt else Pm.surface, tween(Pm.SHEET), label = "heroBg")
    val border by animateColorAsState(if (h.silentNow) Pm.silentBorder else Pm.cardBorder, tween(Pm.SHEET), label = "heroBorder")
    val cardAlpha by animateFloatAsState(if (s.masterOn) 1f else 0.6f, tween(Pm.SHEET), label = "heroAlpha")
    Row(
        Modifier.fillMaxWidth().alpha(cardAlpha).clip(Pm.cardShape).background(bg).border(1.dp, border, Pm.cardShape).padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        val isJumua = h.key == "Jumua"
        Ring(
            h.progress,
            countdown(Duration.between(now, h.target)),
            stringResource(if (h.silentNow) R.string.until_sound else if (isJumua) R.string.until_prayer else R.string.until_adhan),
        )
        AnimatedContent(
            targetState = h,
            contentKey = { Triple(it.key, it.silentNow, it.tomorrow) },
            transitionSpec = {
                (fadeIn(tween(220, delayMillis = 140, easing = Pm.ease)) + slideInVertically(tween(220, delayMillis = 140, easing = Pm.ease)) { it / 6 }) togetherWith
                    (fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 8 }) using SizeTransform(clip = false)
            },
            label = "hero",
            modifier = Modifier.weight(1f),
        ) { hh ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val eyebrow = when {
                    !s.masterOn -> stringResource(R.string.pm_off)
                    hh.silentNow -> stringResource(R.string.silent_now)
                    hh.tomorrow -> stringResource(R.string.next_prayer) + " · " + stringResource(R.string.tomorrow)
                    else -> stringResource(R.string.next_prayer)
                }
                Eyebrow(eyebrow, color = if (hh.silentNow) Pm.accText else Pm.sub)
                Text(DndController.prayerLabel(ctx, hh.key), style = PmType.display, color = Pm.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row {
                    Text(stringResource(if (hh.key == "Jumua") R.string.prayer else R.string.adhan) + " ", style = PmType.body.copy(fontSize = 15.sp), color = Pm.sub)
                    Text(ltr(hm(hh.adhan, zone)), style = PmType.time.copy(fontSize = 15.sp), color = Pm.text)
                }
                val day = hh.adhan.atZone(zone).dayOfWeek
                val line = when {
                    !s.masterOn -> stringResource(R.string.turn_on_hint)
                    day !in s.activeDays -> stringResource(R.string.not_active_on, dayFull(day, locale))
                    hh.window == null -> stringResource(R.string.no_silence)
                    else -> stringResource(R.string.silent_range, range(hm(hh.window.start, zone), hm(hh.window.end, zone)))
                }
                Text(line, style = PmType.rowSub.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = Pm.accText)
            }
        }
    }
}

/** Countdown ring: 100dp track, accText arc from 12 o'clock, mirrored in RTL. */
@Composable
private fun Ring(progress: Float, center: String, label: String) {
    val anim = remember { Animatable(progress) }
    LaunchedEffect(progress) {
        val jump = abs(progress - anim.value) > 0.04f
        anim.animateTo(progress, if (jump) tween(600, easing = Pm.ease) else tween(1000, easing = LinearEasing))
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
        val track = Pm.line
        val arc = Pm.accText
        Canvas(Modifier.size(100.dp)) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val sweep = 360f * anim.value
            drawArc(arc, -90f, if (rtl) -sweep else sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(ltr(center), style = if (center.length > 5) PmType.countdown.copy(fontSize = 18.sp) else PmType.countdown, color = Pm.text)
            Text(label, style = PmType.rowSub.copy(fontSize = 10.5.sp, lineHeight = 13.sp), color = Pm.sub, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 80.dp))
        }
    }
}

@Composable
private fun TimesSection(data: TodayData, config: AppConfig, now: Instant, hero: HeroInfo, update: Update, onOpenPrayer: (Prayer) -> Unit) {
    val ctx = LocalContext.current
    val res = LocalResources.current
    val months = stringArrayResource(R.array.hijri_months)
    val d = data.day
    val hijri = if (d.hijriMonth in 1..12) stringResource(R.string.hijri_ah, d.hijriDay, months[d.hijriMonth - 1], d.hijriYear) else null
    val plan = remember(data, config) { dayPlan(data.day, data.tomorrow, config, data.zone) }
    val s = config.settings
    val dim by animateFloatAsState(if (s.masterOn && d.date.dayOfWeek in s.activeDays) 1f else 0.55f, tween(Pm.SHEET), label = "dim")
    Section(stringResource(R.string.todays_times), aside = hijri) {
        PmCard(Modifier.alpha(dim)) {
            val sunrise = d.date.atTime(d.sunrise).atZone(data.zone).toInstant()
            val rows = plan.slots.map { it as Any }.toMutableList().apply { add(1, sunrise) }
            rows.forEachIndexed { i, r ->
                if (i > 0) RowDivider()
                Staggered(i) {
                    if (r is PrayerSlot) {
                        PrayerRow(r, config, now, data.zone, hero, s.masterOn,
                            onToggle = { on -> update { it.withPrayer(r.prayer) { p -> p.copy(enabled = on) } } },
                            onClick = { onOpenPrayer(r.prayer) })
                    } else {
                        SunriseRow(res.getString(R.string.sunrise), hm(r as Instant, data.zone))
                    }
                }
            }
        }
    }
}

@Composable
private fun Staggered(index: Int, content: @Composable () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visible,
        enter = fadeIn(tween(Pm.ENTER, delayMillis = 60 + 40 * index, easing = Pm.ease)) +
            slideInVertically(tween(Pm.ENTER, delayMillis = 60 + 40 * index, easing = Pm.ease)) { it / 10 },
    ) { content() }
}

@Composable
private fun PrayerRow(
    slot: PrayerSlot, config: AppConfig, now: Instant, zone: java.time.ZoneId, hero: HeroInfo, masterOn: Boolean,
    onToggle: (Boolean) -> Unit, onClick: () -> Unit,
) {
    val ctx = LocalContext.current
    val res = LocalResources.current
    val pc = config.settings.prayer(slot.prayer)
    val w = slot.window
    val isNow = masterOn && w != null && !now.isBefore(w.start) && now.isBefore(w.end)
    val isNext = !hero.silentNow && !hero.tomorrow && hero.key == slot.key && hero.adhan == slot.at
    val past = !slot.at.isAfter(now)
    val off = !pc.enabled
    val bg by animateColorAsState(if (isNow) Pm.accWash else if (isNext) Pm.accWashLt else Color.Transparent, tween(Pm.ENTER), label = "rowBg")
    val bar by animateFloatAsState(if (isNow || isNext) 1f else 0f, tween(Pm.ENTER, easing = Pm.ease), label = "bar")
    val sub = when {
        off -> res.getString(R.string.silence_off)
        slot.key == "Jumua" && w != null -> config.settings.jumua.fixedTime
            ?.let { res.getString(R.string.sub_fixed, range(hm(w.start, zone), hm(w.end, zone))) }
            ?: range(hm(w.start, zone), hm(w.end, zone))
        pc.ablution -> res.getString(R.string.sub_ablution, pc.durationMin)
        pc.iqamaMin > 0 -> res.getString(R.string.sub_iqama, pc.iqamaMin, pc.durationMin)
        else -> res.getString(R.string.sub_at_adhan, pc.durationMin)
    }
    Box(Modifier.fillMaxWidth().background(bg)) {
        Box(Modifier.align(Alignment.CenterStart).width(2.dp).heightIn(min = 64.dp).scale(1f, bar).background(Pm.accText))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(DndController.prayerLabel(ctx, slot.key), style = PmType.prayerName, color = if (off) Pm.faint else Pm.text)
                    if (isNow) BreathingPill(stringResource(R.string.now_pill))
                }
                Text(sub, style = PmType.rowSub, color = if (off) Pm.faint else Pm.sub)
            }
            Text(
                ltr(hm(slot.at, zone)), style = PmType.time, textAlign = TextAlign.End,
                color = if (off) Pm.faint else if (past) Pm.sub else Pm.text, modifier = Modifier.widthIn(min = 52.dp),
            )
            PmSwitch(pc.enabled, onToggle)
        }
    }
}

@Composable
private fun SunriseRow(label: String, time: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = PmType.rowSub.copy(fontSize = 13.sp), color = Pm.sub, modifier = Modifier.weight(1f))
        Text(ltr(time), style = PmType.time.copy(fontSize = 14.sp), color = Pm.sub, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 52.dp))
        Spacer(Modifier.width(46.dp))
    }
}

@Composable
private fun DaysCard(config: AppConfig, today: LocalDate, onEdit: () -> Unit, locale: java.util.Locale) {
    val ctx = LocalContext.current
    val res = LocalResources.current
    val days = config.settings.activeDays
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(stringResource(R.string.active_days), Modifier.weight(1f))
            Text(stringResource(R.string.edit), style = PmType.button.copy(fontSize = 13.sp), color = Pm.accText,
                modifier = Modifier.clip(Pm.ctlShape).clickable(onClick = onEdit).padding(horizontal = 6.dp, vertical = 4.dp))
        }
        PmCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    weekOrder(config.weekStartsSunday).forEach { d ->
                        val on = d in days
                        Box(
                            Modifier.weight(1f).clip(Pm.pill)
                                .background(if (on) Pm.accWash else Color.Transparent)
                                .border(1.dp, if (d == today.dayOfWeek) Pm.accText else if (on) Color.Transparent else Pm.line, Pm.pill)
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(dayShort(d, locale), style = PmType.pill.copy(letterSpacing = 0.sp), color = if (on) Pm.accText else Pm.faint, maxLines = 1) }
                    }
                }
                Text(daysSummary(LocalResources.current, days, config.weekStartsSunday, locale), style = PmType.rowSub, color = Pm.sub)
            }
        }
    }
}
