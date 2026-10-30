package com.yahyaoui.prayermode.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yahyaoui.prayermode.R

/** One theme's colours. Accent-derived washes are mixed over the theme's surface. */
data class Palette(
    val bg: Color, val surface: Color, val raised: Color, val well: Color,
    val line: Color, val line2: Color, val cardBorder: Color,
    val text: Color, val sub: Color, val faint: Color,
    val acc: Color, val accText: Color, val onAcc: Color,
    /** Fully round buttons and controls instead of 8 dp corners. */
    val round: Boolean = false,
    val isLight: Boolean = false,
    val warn: Color = Color(0xFFD9A54A),
    val bad: Color = Color(0xFFE07A6E),
) {
    val accPress = lerp(acc, Color.Black, 0.15f)
    val accWash = accText.copy(alpha = 0.16f).compositeOver(surface)
    val accWashLt = accText.copy(alpha = 0.08f).compositeOver(surface)
    val silentBorder = accText.copy(alpha = 0.35f).compositeOver(surface)
    val warnWash = warn.copy(alpha = 0.14f).compositeOver(surface)
    val badWash = bad.copy(alpha = 0.14f).compositeOver(surface)
}

/**
 * Accent presets. Dark: fill (button text >= 4.5:1) and text colour (>= 7.5:1 on dark surfaces).
 * Light: darker text colour (>= 5.4:1 on light surfaces); Mono flips from white to black buttons.
 */
enum class Accent(
    val id: String,
    val fill: Color, val text: Color, val on: Color,
    val lightFill: Color, val lightText: Color, val lightOn: Color,
) {
    GREEN("green", Color(0xFF2A7A56), Color(0xFF7BBF9A), Color.White, Color(0xFF2A7A56), Color(0xFF1E6847), Color.White),
    BLUE("blue", Color(0xFF2F67AD), Color(0xFF8DB5EE), Color.White, Color(0xFF2F67AD), Color(0xFF24569A), Color.White),
    MONO("mono", Color(0xFFEDEDED), Color(0xFFF2F2F2), Color.Black, Color(0xFF111111), Color(0xFF111111), Color.White);

    companion object { fun from(id: String) = entries.firstOrNull { it.id == id } ?: GREEN }
}

enum class PmThemeId(val id: String) {
    LIGHT("light"), MIDNIGHT("midnight"), FOREST("forest");

    fun palette(a: Accent) = when (this) {
        // Dull green-tinted dark with hairline borders (the original design).
        FOREST -> Palette(
            bg = Color(0xFF121413), surface = Color(0xFF181B19), raised = Color(0xFF1F2321), well = Color(0xFF0E100F),
            line = Color(0xFF2A2F2C), line2 = Color(0xFF3A413D), cardBorder = Color(0xFF2A2F2C),
            text = Color(0xFFF2F4F3), sub = Color(0xFFA9B0AC), faint = Color(0xFF838B86),
            acc = a.fill, accText = a.text, onAcc = a.on,
        )
        // White, neutral greys, borderless cards.
        LIGHT -> Palette(
            bg = Color.White, surface = Color(0xFFF3F3F3), raised = Color(0xFFE8E8E8), well = Color(0xFFEDEDED),
            line = Color(0xFFE6E6E6), line2 = Color(0xFFD4D4D4), cardBorder = Color.Transparent,
            text = Color(0xFF111111), sub = Color(0xFF555555), faint = Color(0xFF666666),
            acc = a.lightFill, accText = a.lightText, onAcc = a.lightOn, round = true, isLight = true,
            warn = Color(0xFF8A5A00), bad = Color(0xFFB3261E),
        )
        // True black, neutral greys, borderless cards.
        MIDNIGHT -> Palette(
            bg = Color.Black, surface = Color(0xFF0D0D0D), raised = Color(0xFF1A1A1A), well = Color.Black,
            line = Color(0xFF161616), line2 = Color(0xFF2A2A2A), cardBorder = Color.Transparent,
            text = Color(0xFFF5F5F5), sub = Color(0xFFA3A3A3), faint = Color(0xFF808080),
            acc = a.fill, accText = a.text, onAcc = a.on, round = true,
        )
    }

    companion object {
        /** "system" (the default) follows the phone's dark-mode setting. */
        fun resolve(id: String, systemDark: Boolean) =
            entries.firstOrNull { it.id == id } ?: if (systemDark) MIDNIGHT else LIGHT
    }
}

val LocalPalette = staticCompositionLocalOf { PmThemeId.FOREST.palette(Accent.GREEN) }

/** Design tokens from docs/design. Colours come from the active [Palette]. */
object Pm {
    val bg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bg
    val surface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface
    val raised: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.raised
    val well: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.well
    val line: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.line
    val line2: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.line2
    val cardBorder: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.cardBorder
    val text: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.text
    val sub: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.sub
    val faint: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.faint
    val acc: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.acc
    val accPress: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accPress
    val accText: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accText
    val accWash: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accWash
    val accWashLt: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accWashLt
    val onAcc: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onAcc
    val warnWash: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.warnWash
    val badWash: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.badWash
    val silentBorder: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.silentBorder
    val warn: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.warn
    val bad: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bad

    val ease = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val PRESS = 120
    const val SWITCH = 160
    const val ENTER = 200
    const val COLLAPSE = 220
    const val SHEET = 300

    val cardShape = RoundedCornerShape(12.dp)
    val ctlShape: RoundedCornerShape @Composable @ReadOnlyComposable get() = if (LocalPalette.current.round) pill else RoundedCornerShape(8.dp)
    /** Inner selection inside a control (segmented thumb). */
    val thumbShape: RoundedCornerShape @Composable @ReadOnlyComposable get() = if (LocalPalette.current.round) pill else RoundedCornerShape(6.dp)
    val pill = RoundedCornerShape(999.dp)
}

val PublicSans = FontFamily(
    Font(R.font.public_sans_400, FontWeight.Normal),
    Font(R.font.public_sans_500, FontWeight.Medium),
    Font(R.font.public_sans_600, FontWeight.SemiBold),
    Font(R.font.public_sans_700, FontWeight.Bold),
)

val SourceSerif = FontFamily(
    Font(R.font.source_serif_400, FontWeight.Normal),
    Font(R.font.source_serif_600, FontWeight.SemiBold),
    Font(R.font.source_serif_700, FontWeight.Bold),
)

private const val TNUM = "tnum"

object PmType {
    val display = TextStyle(fontFamily = SourceSerif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.01).em)
    val title = TextStyle(fontFamily = SourceSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 29.sp, letterSpacing = (-0.01).em)
    val heading = TextStyle(fontFamily = SourceSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 30.sp)
    val body = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val rowTitle = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp)
    val rowSub = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp)
    val time = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, fontFeatureSettings = TNUM)
    val button = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    val eyebrow = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.08.em)
    val pill = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.04.em)
    val countdown = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp, fontFeatureSettings = TNUM)
    val prayerName = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp, lineHeight = 20.sp)
    val onboard = TextStyle(fontFamily = SourceSerif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 35.sp, letterSpacing = (-0.01).em)
    val stepValue = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TNUM)
}

@Composable
fun PmTheme(theme: PmThemeId, accent: Accent, content: @Composable () -> Unit) {
    val target = theme.palette(accent)
    // Theme and accent switches cross-fade instead of snapping.
    @Composable fun anim(c: Color) = animateColorAsState(c, tween(Pm.SHEET, easing = Pm.ease), label = "palette").value
    val p = target.copy(
        bg = anim(target.bg), surface = anim(target.surface), raised = anim(target.raised), well = anim(target.well),
        line = anim(target.line), line2 = anim(target.line2), cardBorder = anim(target.cardBorder),
        text = anim(target.text), sub = anim(target.sub), faint = anim(target.faint),
        acc = anim(target.acc), accText = anim(target.accText), onAcc = anim(target.onAcc),
        warn = anim(target.warn), bad = anim(target.bad),
    )
    val colors = (if (p.isLight) lightColorScheme(
        primary = p.acc, onPrimary = p.onAcc, primaryContainer = p.accWash, onPrimaryContainer = p.accText,
        secondary = p.accText, onSecondary = p.bg, background = p.bg, onBackground = p.text,
        surface = p.surface, onSurface = p.text, surfaceVariant = p.raised, onSurfaceVariant = p.sub,
        surfaceContainerLowest = p.well, surfaceContainerLow = p.bg, surfaceContainer = p.surface,
        surfaceContainerHigh = p.raised, surfaceContainerHighest = p.raised,
        outline = p.line2, outlineVariant = p.line,
        error = p.bad, onError = p.bg, errorContainer = p.badWash, onErrorContainer = p.bad,
        scrim = Color(0x66000000),
    ) else darkColorScheme(
        primary = p.acc, onPrimary = p.onAcc, primaryContainer = p.accWash, onPrimaryContainer = p.accText,
        secondary = p.accText, onSecondary = p.bg, background = p.bg, onBackground = p.text,
        surface = p.surface, onSurface = p.text, surfaceVariant = p.raised, onSurfaceVariant = p.sub,
        surfaceContainerLowest = p.well, surfaceContainerLow = p.bg, surfaceContainer = p.surface,
        surfaceContainerHigh = p.raised, surfaceContainerHighest = p.raised,
        outline = p.line2, outlineVariant = p.line,
        error = p.bad, onError = p.bg, errorContainer = p.badWash, onErrorContainer = p.bad,
        scrim = Color(0x99000000),
    ))
    CompositionLocalProvider(LocalPalette provides p) { MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            bodyLarge = PmType.body.copy(fontSize = 15.sp),
            bodyMedium = PmType.body,
            bodySmall = PmType.rowSub,
            titleLarge = PmType.title,
            titleMedium = PmType.rowTitle,
            labelLarge = PmType.button,
            labelSmall = PmType.eyebrow,
            displaySmall = PmType.display,
            headlineSmall = PmType.heading,
        ),
        shapes = Shapes(small = Pm.ctlShape, medium = Pm.cardShape, large = Pm.cardShape),
        content = content,
    ) }
}
