package com.yahyaoui.prayermode.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Pm.sub) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Text(
        if (rtl) text else text.uppercase(),
        style = if (rtl) PmType.eyebrow.copy(letterSpacing = PmType.body.letterSpacing) else PmType.eyebrow,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun Section(title: String, aside: String? = null, help: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
            Eyebrow(title, Modifier.weight(1f))
            if (aside != null) Text(aside, style = PmType.rowSub, color = Pm.sub)
        }
        content()
        if (help != null) Text(help, style = PmType.rowSub, color = Pm.sub, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
fun PmCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(Pm.cardShape)
            .background(Pm.surface)
            .border(1.dp, Pm.cardBorder, Pm.cardShape),
        content = content,
    )
}

@Composable
fun RowDivider() = HorizontalDivider(thickness = 1.dp, color = Pm.line)

/** Standard list row: title, optional subtitle, trailing slot. */
@Composable
fun PmRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) Pm.accWash else Color.Transparent, tween(Pm.PRESS, easing = Pm.ease), label = "rowbg")
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(interaction, null, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = PmType.rowTitle, color = Pm.text)
            if (subtitle != null) Text(subtitle, style = PmType.rowSub, color = Pm.sub, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke(this)
    }
}

/** 46x26 switch, 20dp knob travelling 2→22dp in 160ms. */
@Composable
fun PmSwitch(checked: Boolean, onChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, tween(Pm.SWITCH, easing = Pm.ease), label = "knob")
    val track by animateColorAsState(if (checked) Pm.acc else Pm.line2, tween(Pm.SWITCH, easing = Pm.ease), label = "track")
    Box(
        modifier
            .size(46.dp, 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (enabled) track else track.copy(alpha = 0.45f))
            .then(
                if (onChange != null) Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
                else Modifier
            ),
    ) {
        Box(
            Modifier
                .offset(x = if (rtl) 24.dp - x else x, y = 3.dp)
                .size(20.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(if (checked) Pm.onAcc else if (LocalPalette.current.isLight) Color.White else Pm.text.copy(alpha = 0.92f)),
        )
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) Pm.accPress else Pm.acc, tween(Pm.PRESS), label = "btn")
    val s by animateFloatAsState(if (pressed) 0.98f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "btnScale")
    Box(
        modifier
            .scale(s)
            .heightIn(min = 48.dp)
            .clip(Pm.ctlShape)
            .background(if (enabled) bg else Pm.raised)
            .clickable(interaction, null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = PmType.button, color = if (enabled) Pm.onAcc else Pm.faint) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(Pm.ctlShape)
            .border(1.dp, Pm.line2, Pm.ctlShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = PmType.button, color = Pm.accText) }
}

/** Pill chip; on = filled green. Springs slightly on select. */
@Composable
fun PmChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) Pm.acc else Color.Transparent, tween(Pm.PRESS), label = "chipbg")
    val s by animateFloatAsState(
        if (selected) 1f else 0.96f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "chipScale",
    )
    Box(
        modifier
            .scale(s)
            .heightIn(min = 36.dp)
            .clip(Pm.pill)
            .background(bg)
            .border(1.dp, if (selected) Pm.acc else Pm.line2, Pm.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = PmType.button.copy(fontWeight = if (selected) PmType.button.fontWeight else androidx.compose.ui.text.font.FontWeight.Medium), color = if (selected) Pm.onAcc else Pm.sub)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipGroup(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (v, label) -> PmChip(label, v == selected, { onSelect(v) }) }
    }
}

/** Segmented control on a recessed well; the thumb slides between options. */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(Pm.ctlShape)
            .background(Pm.well)
            .border(1.dp, Pm.line, Pm.ctlShape)
            .padding(3.dp),
    ) {
        val w = maxWidth / options.size
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val x by animateDpAsState(w * index, tween(Pm.ENTER, easing = Pm.ease), label = "seg")
        Box(
            Modifier
                .offset(x = if (rtl) -x else x)
                .width(w)
                .fillMaxHeight()
                .clip(Pm.thumbShape)
                .background(Pm.raised)
                .border(1.dp, Pm.line2, Pm.thumbShape),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEach { (v, label) ->
                val on = v == selected
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(Pm.thumbShape).clickable { onSelect(v) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = PmType.button.copy(fontSize = PmType.rowSub.fontSize), color = if (on) Pm.text else Pm.sub, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** − value + stepper for minute values. */
@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, range: IntRange, step: Int = 1, format: (Int) -> String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StepButton("−", value - step >= range.first) { onChange((value - step).coerceIn(range)) }
        Text(format(value), style = PmType.stepValue, color = Pm.text, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 64.dp))
        StepButton("+", value + step <= range.last) { onChange((value + step).coerceIn(range)) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(Pm.ctlShape)
            .background(Pm.raised)
            .border(1.dp, Pm.line2, Pm.ctlShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = PmType.rowTitle.copy(fontSize = 18.sp), color = if (enabled) Pm.text else Pm.faint) }
}

enum class PillTone { OK, WARN, BAD, NEUTRAL }

@Composable
fun StatusPill(text: String, tone: PillTone) {
    val (fg, bg) = when (tone) {
        PillTone.OK -> Pm.accText to Pm.accWash
        PillTone.WARN -> Pm.warn to Pm.warnWash
        PillTone.BAD -> Pm.bad to Pm.badWash
        PillTone.NEUTRAL -> Pm.sub to Pm.raised
    }
    Box(Modifier.clip(Pm.pill).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, style = PmType.pill, color = fg)
    }
}

@Composable
fun Dot(color: Color, size: Int = 8) = Box(Modifier.size(size.dp).clip(CircleShape).background(color))

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))
