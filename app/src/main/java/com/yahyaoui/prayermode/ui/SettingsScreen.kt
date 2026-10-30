package com.yahyaoui.prayermode.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import com.yahyaoui.prayermode.BuildConfig
import com.yahyaoui.prayermode.R
import com.yahyaoui.prayermode.core.AppConfig

private val LANGUAGES = listOf(
    "" to "System", "en" to "English", "ar" to "العربية", "ur" to "اردو", "fr" to "Français", "es" to "Español",
    "pt" to "Português", "tr" to "Türkçe", "id" to "Bahasa Indonesia", "ms" to "Bahasa Melayu", "bn" to "বাংলা", "zh" to "中文",
)

data class PermInfo(val perm: Perm, val icon: ImageVector, val title: Int, val sub: Int, val required: Boolean)

val PERMS = listOf(
    PermInfo(Perm.DND, PmIcons.dnd, R.string.perm_dnd, R.string.perm_dnd_sub, true),
    PermInfo(Perm.ALARMS, PmIcons.alarm, R.string.perm_alarms, R.string.perm_alarms_sub, true),
    PermInfo(Perm.NOTIFICATIONS, PmIcons.bell, R.string.perm_notifications, R.string.perm_notifications_sub, false),
    PermInfo(Perm.BATTERY, PmIcons.battery, R.string.perm_battery, R.string.perm_battery_sub, false),
)

/** Opens the system screen for [p], or asks for notifications at runtime on Android 13+. */
@Composable
fun rememberPermissionAction(): (Perm) -> Unit {
    val ctx = LocalContext.current
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return { p ->
        if (p == Perm.NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notif.launch(Manifest.permission.POST_NOTIFICATIONS)
        else runCatching { ctx.startActivity(Permissions.settingsIntent(ctx, p)) }
    }
}

@Composable
fun PermissionRows(perms: Map<Perm, Boolean>, onboarding: Boolean) {
    val act = rememberPermissionAction()
    PERMS.forEachIndexed { i, info ->
        if (i > 0) RowDivider()
        val ok = perms[info.perm] == true
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (ok) Pm.accWash else Pm.raised), contentAlignment = Alignment.Center) {
                Icon(info.icon, null, tint = if (ok) Pm.accText else Pm.sub, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (onboarding) Text(stringResource(if (info.required) R.string.required else R.string.recommended).uppercase(), style = PmType.pill.copy(fontSize = 10.5.sp), color = Pm.faint)
                Text(stringResource(info.title), style = PmType.button.copy(fontSize = 14.5.sp), color = Pm.text)
                Text(stringResource(info.sub), style = PmType.rowSub, color = Pm.sub)
            }
            if (ok) {
                val pop by animateFloatAsState(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "grant")
                Box(Modifier.scale(pop)) { StatusPill("✓ " + stringResource(R.string.granted), PillTone.OK) }
            } else {
                SecondaryButton(stringResource(if (onboarding) R.string.allow else R.string.fix), { act(info.perm) })
            }
        }
    }
}

@Composable
fun SettingsScreen(config: AppConfig, update: Update) {
    val ctx = LocalContext.current
    val perms = rememberPermissions()
    var langPicker by remember { mutableStateOf(false) }
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')

    ScreenColumn(stringResource(R.string.nav_settings)) {
        Section(stringResource(R.string.appearance)) {
            PmCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Field(stringResource(R.string.theme)) {
                        Segmented(
                            listOf(
                                "system" to stringResource(R.string.clock_system),
                                "light" to stringResource(R.string.theme_light),
                                "midnight" to stringResource(R.string.theme_dark),
                                "forest" to stringResource(R.string.theme_forest),
                            ),
                            config.theme,
                            // Forest is designed around green; the others around black-and-white buttons.
                            { v -> update { it.copy(theme = v, accent = if (v == "forest") Accent.GREEN.id else Accent.MONO.id) } },
                        )
                    }
                    Field(stringResource(R.string.accent)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Accent.entries.forEach { a -> Swatch(a, Accent.from(config.accent) == a) { update { it.copy(accent = a.id) } } }
                        }
                    }
                    Field(stringResource(R.string.time_format)) {
                        Segmented(
                            listOf("system" to stringResource(R.string.clock_system), "24" to stringResource(R.string.clock_24), "12" to stringResource(R.string.clock_12)),
                            config.clock, { v -> update { it.copy(clock = v) } },
                        )
                    }
                }
            }
        }
        Section(stringResource(R.string.notifications)) {
            PmCard { SwitchRow(stringResource(R.string.notify_silence), null, config.notifySilence) { v -> update { it.copy(notifySilence = v) } } }
        }
        Section(stringResource(R.string.language)) {
            PmCard {
                PmRow(LANGUAGES.firstOrNull { it.first == current }?.second ?: "System", onClick = { langPicker = true }, trailing = {
                    Icon(PmIcons.chevron, null, tint = Pm.faint, modifier = Modifier.size(18.dp))
                })
            }
        }
        Section(stringResource(R.string.permissions)) { PmCard { PermissionRows(perms, onboarding = false) } }
        Section(stringResource(R.string.about), help = stringResource(R.string.about_help)) {
            PmCard {
                PmRow(stringResource(R.string.version), BuildConfig.VERSION_NAME)
                RowDivider()
                PmRow(stringResource(R.string.licence), "GPL-3.0", onClick = { open(ctx, "https://www.gnu.org/licenses/gpl-3.0.html") })
                RowDivider()
                PmRow(stringResource(R.string.source_code), "github.com/elyousfy/pm-enhanced", onClick = { open(ctx, "https://github.com/elyousfy/pm-enhanced") })
                RowDivider()
                PmRow(stringResource(R.string.upstream_project), "github.com/prayermode/prayermode", onClick = { open(ctx, "https://github.com/prayermode/prayermode") })
            }
        }
    }

    if (langPicker) PickerSheet(stringResource(R.string.language), LANGUAGES, current, search = { (_, n), q -> n.contains(q, true) }, onDismiss = { langPicker = false }) { tag ->
        AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }
}

private fun open(ctx: android.content.Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}

/** Accent colour dot; the selected one gets a ring that springs in. */
@Composable
private fun Swatch(a: Accent, selected: Boolean, onClick: () -> Unit) {
    val ring by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "swatch")
    Box(
        Modifier.size(40.dp).clip(CircleShape).border(2.dp, Pm.accText.copy(alpha = ring), CircleShape)
            .clickable(role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick).padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        val light = LocalPalette.current.isLight
        Box(Modifier.fillMaxSize().clip(CircleShape).background(if (light) a.lightFill else a.fill), contentAlignment = Alignment.Center) {
            if (selected) Icon(PmIcons.check, null, tint = if (light) a.lightOn else a.on, modifier = Modifier.size(16.dp).scale(ring))
        }
    }
}
