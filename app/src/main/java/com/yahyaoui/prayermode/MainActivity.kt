package com.yahyaoui.prayermode

import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yahyaoui.prayermode.core.Prayer
import com.yahyaoui.prayermode.data.SettingsStore
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.yahyaoui.prayermode.ui.Accent
import com.yahyaoui.prayermode.ui.LocationScreen
import com.yahyaoui.prayermode.ui.PmThemeId
import com.yahyaoui.prayermode.ui.TimeFmt
import com.yahyaoui.prayermode.ui.OnboardingScreen
import com.yahyaoui.prayermode.ui.Pm
import com.yahyaoui.prayermode.ui.PmIcons
import com.yahyaoui.prayermode.ui.PmTheme
import com.yahyaoui.prayermode.ui.PmType
import com.yahyaoui.prayermode.ui.PrayerSheet
import com.yahyaoui.prayermode.ui.ScheduleScreen
import com.yahyaoui.prayermode.ui.SettingsScreen
import com.yahyaoui.prayermode.ui.TodayScreen
import com.yahyaoui.prayermode.ui.TodayState
import com.yahyaoui.prayermode.ui.rememberToday
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent {
            val config by SettingsStore.config(this).collectAsState()
            var system24 by remember { mutableStateOf(DateFormat.is24HourFormat(this)) }
            // The phone's 24-hour switch can change while we are in the background.
            LifecycleResumeEffect(Unit) {
                system24 = DateFormat.is24HourFormat(this@MainActivity)
                onPauseOrDispose { }
            }
            TimeFmt.is24 = when (config.clock) { "24" -> true; "12" -> false; else -> system24 }
            val theme = PmThemeId.resolve(config.theme, isSystemInDarkTheme())
            // Status and navigation bar icons follow the active theme, not the phone's setting.
            val view = LocalView.current
            val windowBg = theme.palette(Accent.from(config.accent)).bg.toArgb()
            SideEffect {
                window.decorView.setBackgroundColor(windowBg)
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = theme == PmThemeId.LIGHT
                    isAppearanceLightNavigationBars = theme == PmThemeId.LIGHT
                }
            }
            PmTheme(theme, Accent.from(config.accent)) { App() }
        }
    }
}

private enum class Tab(val label: Int, val icon: ImageVector) {
    TODAY(R.string.nav_today, PmIcons.clock),
    SCHEDULE(R.string.nav_schedule, PmIcons.calendar),
    LOCATION(R.string.nav_location, PmIcons.pin),
    SETTINGS(R.string.nav_settings, PmIcons.sliders),
}

@Composable
private fun App() {
    val ctx = LocalContext.current
    val config by SettingsStore.config(ctx).collectAsState()
    val update: com.yahyaoui.prayermode.ui.Update = { change -> SettingsStore.update(ctx, change) }
    var reload by remember { mutableIntStateOf(0) }
    val today by rememberToday(config, reload)
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(msg) } }

    Box(Modifier.fillMaxSize().background(Pm.bg)) {
        if (!config.onboarded) {
            OnboardingScreen(config, today, update, onRefresh = { reload++ })
            return@Box
        }
        var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
        var sheet by rememberSaveable { mutableStateOf<Prayer?>(null) }
        BackHandler(tab != Tab.TODAY) { tab = Tab.TODAY }
        Column(Modifier.fillMaxSize()) {
            AnimatedContent(
                tab,
                transitionSpec = {
                    (fadeIn(tween(Pm.ENTER, easing = Pm.ease)) + slideInVertically(tween(Pm.ENTER, easing = Pm.ease)) { 12 }) togetherWith fadeOut(tween(90))
                },
                label = "tab",
                modifier = Modifier.weight(1f).statusBarsPadding(),
            ) { t ->
                when (t) {
                    Tab.TODAY -> TodayScreen(
                        config, update, reload, onRetry = { reload++ },
                        onOpenLocation = { tab = Tab.LOCATION }, onOpenSchedule = { tab = Tab.SCHEDULE },
                        onOpenPrayer = { sheet = it }, toast = toast,
                    )
                    Tab.SCHEDULE -> ScheduleScreen(config, update)
                    Tab.LOCATION -> LocationScreen(config, today, update, onRefresh = { reload++ })
                    Tab.SETTINGS -> SettingsScreen(config, update)
                }
            }
            BottomNav(tab) { tab = it }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 84.dp, start = 16.dp, end = 16.dp)) {
            Snackbar(it, containerColor = Pm.text, contentColor = Pm.bg, shape = RoundedCornerShape(8.dp))
        }
        sheet?.let { p -> PrayerSheet(p, config, (today as? TodayState.Ready)?.data, update) { sheet = null } }
    }
}

@Composable
private fun BottomNav(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Pm.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Pm.line))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(72.dp).padding(top = 8.dp)) {
            Tab.entries.forEach { t ->
                val on = t == selected
                val ind by animateFloatAsState(if (on) 1f else 0f, tween(Pm.ENTER, easing = Pm.ease), label = "ind")
                Column(
                    Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { onSelect(t) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(56.dp, 30.dp).graphicsLayer { scaleX = 0.4f + 0.6f * ind; alpha = ind }
                                .clip(RoundedCornerShape(15.dp)).background(Pm.accWash),
                        )
                        Icon(t.icon, null, tint = if (on) Pm.accText else Pm.sub, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        stringResource(t.label),
                        style = PmType.rowSub.copy(fontSize = 11.5.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (on) Pm.text else Pm.sub,
                    )
                }
            }
        }
    }
}

