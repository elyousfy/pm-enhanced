package com.yahyaoui.prayermode.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yahyaoui.prayermode.R
import com.yahyaoui.prayermode.core.AppConfig

/** First run: step 1 permissions, step 2 city and source. Ends by turning Prayer Mode on. */
@Composable
fun OnboardingScreen(config: AppConfig, today: TodayState, update: Update, onRefresh: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    BackHandler(step == 2) { step = 1 }
    AnimatedContent(
        step,
        transitionSpec = { (fadeIn(tween(Pm.ENTER)) + slideInHorizontally(tween(Pm.SHEET, easing = Pm.ease)) { it / 6 }) togetherWith fadeOut(tween(120)) },
        label = "onboard",
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
    ) { s ->
        if (s == 1) PermissionsStep { step = 2 }
        else Column {
            StepBars(2)
            LocationScreen(config, today, update, onRefresh) {
                PrimaryButton(
                    stringResource(R.string.start_pm),
                    { update { it.copy(onboarded = true, settings = it.settings.copy(masterOn = true)) } },
                    Modifier.fillMaxWidth(),
                    enabled = today is TodayState.Ready,
                )
            }
        }
    }
}

@Composable
private fun StepBars(step: Int) {
    Row(Modifier.fillMaxWidth().background(Pm.bg).padding(start = 20.dp, top = 16.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..2).forEach { i ->
            Box(Modifier.size(24.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= step) Pm.accText else Pm.line2))
        }
    }
}

@Composable
private fun PermissionsStep(onContinue: () -> Unit) {
    val perms = rememberPermissions()
    val required = perms[Perm.DND] == true && perms[Perm.ALARMS] == true
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        StepBars(1)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow(stringResource(R.string.step_permissions))
                Text(stringResource(R.string.onboard_title), style = PmType.onboard, color = Pm.text)
                Text(stringResource(R.string.onboard_lead), style = PmType.body.copy(lineHeight = PmType.body.fontSize * 1.5), color = Pm.sub)
            }
            PmCard { PermissionRows(perms, onboarding = true) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.n_of_4, perms.values.count { it }), style = PmType.rowSub, color = Pm.sub, modifier = Modifier.padding(horizontal = 4.dp))
                PrimaryButton(stringResource(R.string.continue_city), onContinue, Modifier.fillMaxWidth(), enabled = required)
                if (!required) Text(stringResource(R.string.continue_hint), style = PmType.rowSub, color = Pm.faint, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}
