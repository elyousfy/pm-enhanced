package com.yahyaoui.prayermode.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.yahyaoui.prayermode.DndController
import com.yahyaoui.prayermode.R
import com.yahyaoui.prayermode.core.AladhanApi
import com.yahyaoui.prayermode.core.AppConfig
import com.yahyaoui.prayermode.core.LocationConfig
import com.yahyaoui.prayermode.core.Prayer
import com.yahyaoui.prayermode.core.TimeSource
import com.yahyaoui.prayermode.core.UaeCities
import com.yahyaoui.prayermode.data.TimesRepository
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Locale

/** Aladhan ids for the upstream `calculation_methods` array, in array order. */
val METHOD_IDS = listOf(4, 3, 2, 5, 1, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 7, 0)

@Composable
fun methodName(id: Int): String {
    val names = stringArrayResource(R.array.calculation_methods)
    return METHOD_IDS.indexOf(id).takeIf { it >= 0 }?.let { names[it] } ?: "Method $id"
}

fun countryName(code: String, locale: Locale): String = Locale("", code).getDisplayCountry(locale).ifBlank { code }

@Composable
fun LocationScreen(config: AppConfig, today: TodayState, update: Update, onRefresh: () -> Unit, footer: (@Composable () -> Unit)? = null) {
    val locale = currentLocale()
    var picker by remember { mutableStateOf<String?>(null) }
    val loc = config.location
    val isUae = loc.countryCode == "AE"
    val ready = (today as? TodayState.Ready)?.data

    ScreenColumn(stringResource(R.string.nav_location)) {
        Section(stringResource(R.string.location), help = stringResource(R.string.location_help)) {
            PmCard {
                PmRow(stringResource(R.string.country), countryName(loc.countryCode, locale), onClick = { picker = "country" }, trailing = { Chevron() })
                RowDivider()
                if (isUae) {
                    PmRow(stringResource(R.string.city), loc.city, onClick = { picker = "city" }, trailing = { Chevron() })
                } else {
                    CityField(config, update)
                }
                RowDivider()
                val zone = ready?.zone ?: com.yahyaoui.prayermode.PrayerScheduler.zoneOf(config)
                val offset = zone.rules.getOffset(java.time.Instant.now()).id.let { if (it == "Z") "GMT" else "GMT$it" }
                PmRow(stringResource(R.string.time_zone), "${zone.id} · $offset")
            }
        }

        Section(stringResource(R.string.time_source)) {
            PmCard {
                SourceOption(stringResource(R.string.aladhan_title), stringResource(R.string.aladhan_sub), null, config.source == TimeSource.ALADHAN, true) {
                    update { it.copy(source = TimeSource.ALADHAN) }
                }
                RowDivider()
                SourceOption(stringResource(R.string.awqaf_title), stringResource(R.string.awqaf_sub), stringResource(R.string.experimental), config.source == TimeSource.AWQAF, isUae) {
                    update { it.copy(source = TimeSource.AWQAF) }
                }
                Collapse(config.source == TimeSource.AWQAF && isUae) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Eyebrow(stringResource(R.string.awqaf_key))
                        var key by remember(config.awqafApiKey) { mutableStateOf(config.awqafApiKey) }
                        PmTextField(key, { key = it }, "x-api-key", password = true, onDone = { update { it.copy(awqafApiKey = key.trim()) } })
                        Text(stringResource(R.string.awqaf_key_help), style = PmType.rowSub, color = Pm.sub)
                    }
                }
            }
            if (ready?.fellBack == true) Text(stringResource(R.string.fallback_note), style = PmType.rowSub, color = Pm.warn, modifier = Modifier.padding(horizontal = 4.dp))
        }

        Section(stringResource(R.string.section_calculation)) {
            PmCard {
                // Whatever actually served the times decides; Awqaf without a key falls back to Aladhan.
                val aladhan = (ready?.source ?: config.source) == TimeSource.ALADHAN || !isUae
                Collapse(aladhan) {
                    Column {
                        val id = config.effectiveMethod
                        val sub = if (config.method == null) stringResource(R.string.method_auto_for, countryName(loc.countryCode, locale), id)
                        else stringResource(R.string.method_chosen, id)
                        PmRow(stringResource(R.string.method) + " · " + methodName(id), sub, onClick = { picker = "method" }, trailing = { Chevron() })
                        RowDivider()
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.asr_time), style = PmType.rowTitle, color = Pm.text)
                            Segmented(
                                listOf(0 to stringResource(R.string.asr_standard), 1 to stringResource(R.string.asr_hanafi)),
                                config.asrSchool, { v -> update { it.copy(asrSchool = v) } },
                            )
                        }
                    }
                }
                Collapse(!aladhan) {
                    Text(stringResource(R.string.awqaf_note), style = PmType.body, color = Pm.sub, modifier = Modifier.padding(16.dp))
                }
            }
        }

        Section(stringResource(R.string.match_mosque), help = stringResource(R.string.match_mosque_help)) {
            PmCard {
                Prayer.entries.forEachIndexed { i, p ->
                    if (i > 0) RowDivider()
                    val pc = config.settings.prayer(p)
                    val preview = ready?.let { d ->
                        val base = d.day.time(p)
                        range(hm(base), hm(base.plusMinutes(pc.tuneMin.toLong())), " → ")
                    }
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        StepRow(DndController.prayerLabel(LocalContext.current, p.key), pc.tuneMin, -30..30, 1, signed = true, sub = preview) { v ->
                            update { it.withPrayer(p) { c -> c.copy(tuneMin = v) } }
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(stringResource(R.string.refresh_times), onRefresh)
            when (today) {
                is TodayState.Loading -> Text(stringResource(R.string.loading_times), style = PmType.rowSub, color = Pm.sub)
                is TodayState.Failed -> Text(today.message ?: stringResource(R.string.load_failed), style = PmType.rowSub, color = Pm.bad)
                is TodayState.Ready -> StatusPill(stringResource(if (today.data.source == TimeSource.AWQAF) R.string.source_awqaf_short else R.string.source_aladhan_short), PillTone.OK)
            }
        }
        footer?.invoke()
    }

    when (picker) {
        "country" -> PickerSheet(
            stringResource(R.string.country),
            Locale.getISOCountries().map { it to countryName(it, locale) }.sortedBy { it.second },
            loc.countryCode,
            search = { (code, name), q -> name.contains(q, true) || countryName(code, Locale.ENGLISH).contains(q, true) || code.equals(q, true) },
            onDismiss = { picker = null },
        ) { code ->
            if (code != loc.countryCode) update {
                it.copy(
                    location = if (code == "AE") UaeCities.default else LocationConfig(code, "", null, null, null, null),
                    method = null,
                    source = if (code == "AE") it.source else TimeSource.ALADHAN,
                    // UAE holds Jumuʿah at a fixed 12:45 nationwide; elsewhere it follows Dhuhr.
                    settings = it.settings.copy(jumua = it.settings.jumua.copy(fixedTime = if (code == "AE") java.time.LocalTime.of(12, 45) else null)),
                )
            }
        }
        "city" -> PickerSheet(
            stringResource(R.string.city), UaeCities.all.map { it.city to it.city }, loc.city,
            search = { (c, _), q -> c.contains(q, true) }, onDismiss = { picker = null },
        ) { name -> update { it.copy(location = UaeCities.all.first { c -> c.city == name }) } }
        "method" -> {
            val auto = AladhanApi.defaultMethodFor(loc.countryCode)
            val options = listOf(-1 to stringResource(R.string.method_auto) + " · $auto") +
                stringArrayResource(R.array.calculation_methods).mapIndexed { i, n -> METHOD_IDS[i] to "$n · ${METHOD_IDS[i]}" }
            PickerSheet(stringResource(R.string.method), options, config.method ?: -1, search = { (_, n), q -> n.contains(q, true) }, onDismiss = { picker = null }) { id ->
                update { it.copy(method = id.takeIf { m -> m >= 0 }) }
            }
        }
    }
}

@Composable
private fun Chevron() = Icon(PmIcons.chevron, null, tint = Pm.faint, modifier = Modifier.size(18.dp))

/** Free-text city outside the UAE, checked against Aladhan before it is saved. */
@Composable
private fun CityField(config: AppConfig, update: Update) {
    val ctx = LocalContext.current
    val res = LocalResources.current
    val scope = rememberCoroutineScope()
    var text by remember(config.location.countryCode) { mutableStateOf(config.location.city) }
    var status by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun check() {
        val city = text.trim()
        if (city.isEmpty() || busy) return
        busy = true
        scope.launch {
            val candidate = config.copy(location = LocationConfig(config.location.countryCode, city, null, null, null, null), source = TimeSource.ALADHAN)
            status = try {
                val m = TimesRepository(ctx).month(candidate, YearMonth.now(), forceRefresh = true)
                update { it.copy(location = candidate.location.copy(timeZone = m.month.zone.id)) }
                true to res.getString(R.string.city_found, m.month.zone.id)
            } catch (e: Exception) {
                false to res.getString(R.string.city_not_found)
            }
            busy = false
        }
    }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(stringResource(R.string.city))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { PmTextField(text, { text = it; status = null }, stringResource(R.string.city_hint), onDone = ::check) }
            SecondaryButton(if (busy) "…" else stringResource(R.string.check_city), ::check)
        }
        status?.let { (ok, msg) -> Text(msg, style = PmType.rowSub, color = if (ok) Pm.accText else Pm.bad) }
    }
}

@Composable
fun PmTextField(value: String, onChange: (String) -> Unit, placeholder: String, password: Boolean = false, leading: Boolean = false, onDone: () -> Unit = {}) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) Pm.accText else Pm.line2, tween(Pm.PRESS), label = "field")
    BasicTextField(
        value, onChange,
        singleLine = true,
        textStyle = PmType.rowTitle.copy(color = Pm.text),
        cursorBrush = SolidColor(Pm.accText),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Text, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().height(44.dp).clip(Pm.ctlShape).background(Pm.bg).border(1.dp, border, Pm.ctlShape).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (leading) Icon(PmIcons.search, null, tint = Pm.sub, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = PmType.rowTitle, color = Pm.faint)
                    inner()
                }
            }
        },
    )
}

@Composable
private fun SourceOption(title: String, sub: String, pill: String?, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Pm.accWashLt else Color.Transparent, tween(Pm.PRESS), label = "src")
    Row(
        Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.45f).background(bg).clickable(enabled = enabled, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(18.dp).clip(CircleShape).border(2.dp, if (selected) Pm.accText else Pm.line2, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Dot(Pm.accText, 8)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = PmType.button.copy(fontSize = PmType.rowTitle.fontSize), color = Pm.text)
                if (pill != null) StatusPill(pill, PillTone.WARN)
            }
            Text(sub, style = PmType.rowSub, color = Pm.sub)
        }
    }
}

/** Searchable single-choice list in a bottom sheet. */
@Composable
fun <T> PickerSheet(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    search: (Pair<T, String>, String) -> Boolean,
    onDismiss: () -> Unit,
    onPick: (T) -> Unit,
) {
    var q by remember { mutableStateOf("") }
    PmSheet(onDismiss) { close ->
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = PmType.heading, color = Pm.text, modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clip(Pm.ctlShape).clickable(onClick = close), contentAlignment = Alignment.Center) {
                    Icon(PmIcons.close, null, tint = Pm.sub, modifier = Modifier.size(20.dp))
                }
            }
            if (options.size > 8) PmTextField(q, { q = it }, stringResource(R.string.search), leading = true)
            val shown = options.filter { q.isBlank() || search(it, q.trim()) }
            if (shown.isEmpty()) Text(stringResource(R.string.no_matches), style = PmType.body, color = Pm.sub, modifier = Modifier.padding(vertical = 16.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(bottom = 16.dp)) {
                items(shown, key = { it.first.toString() }) { (v, label) ->
                    val on = v == selected
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(Pm.ctlShape).background(if (on) Pm.accWashLt else Color.Transparent)
                            .clickable { onPick(v); close() }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = PmType.rowTitle, color = if (on) Pm.accText else Pm.text, modifier = Modifier.weight(1f))
                        if (on) Icon(PmIcons.check, null, tint = Pm.accText, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
