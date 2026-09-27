package com.spendwise.app.ui.botanical

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.spendwise.app.R
import com.spendwise.app.ui.theme.LocalPerfMode
import com.spendwise.app.ui.theme.rememberPerfMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
//  Botanical — the September 2026 redesign (redesign-local/SpendWise.html).
//
//  Painted floral hero on warm paper for Home; midnight-navy pages for
//  everything else; white data cards; Barlow Condensed for headlines and
//  money, DM Sans for supporting text. Values below trace to the prototype's
//  stylesheets (scratch/redesign-complete/work/src/components/generated/
//  Botanical*Styles.ts). CSS px map 1:1 to dp/sp — the prototype was laid out
//  at 402px, the width class of the phones this app targets.
// ═════════════════════════════════════════════════════════════════════════════

/** Colour tokens. The redesign has a single appearance — no light/dark split. */
object Bot {
    val Ink = Color(0xFF080C18)
    val Navy = Color(0xFF101C3A)
    val Paper = Color(0xFFE2E2E2)
    val Muted = Color(0xFF697184)
    val Blue = Color(0xFF0088FF)
    /** Selected controls: white text on this measures 5.83:1. */
    val ActionSolid = Color(0xFF0063C6)
    val Pink = Color(0xFFC50FC5)
    val Orchid = Color(0xFFD989D9)

    // Dark pages
    val PageInk = Color.White
    val PageMuted = Color(0xFFB6C0D7)
    val PageDeep = Color(0xFF05080E)

    // White surfaces
    val Surface = Color.White
    val SurfaceInk = Color(0xFF080C18)
    val SurfaceMuted = Color(0xFF505B70)
    val Caption = Color(0xFF71798B)
    val Rule = Color(0xFFECEEF2)
    val RuleSoft = Color(0x33697184)
    val Track = Color(0xFFEFF0F4)
    val TileBg = Color(0xFFF1F1F4)
    val ChipBg = Color(0xFFEDF2F8)
    val ChipInk = Color(0xFF345078)
    val SecondaryBg = Color(0xFFEDF0F5)
    val SecondaryInk = Color(0xFF182444)
    val DangerBg = Color(0xFFFFF0F2)
    val DangerInk = Color(0xFFA32442)
    val FieldBg = Color(0xFFF6F7F9)
    val FieldBorder = Color(0xFFE2E6ED)
    val FieldInk = Color(0xFF172137)
    val FieldLabel = Color(0xFF505B70)
    val SheetMuted = Color(0xFF677084)
    val Scrim = Color(0x8C030815)

    // Money
    val Positive = Color(0xFF217050)
    val Negative = Color(0xFFB33A55)
    val PositiveOnDark = Color(0xFFA5DEC4)
    val NegativeOnDark = Color(0xFFFFB1C2)

    // Budgets
    val BudgetOk = Color(0xFF217050)
    val BudgetNear = Color(0xFF8A5200)
    val BudgetOver = Color(0xFFA32442)

    // Charts
    val IncomeBar = Color(0xFF1C375D)
    val ExpenseBar = Color(0xFFD989D9)
    val ChartLabel = Color(0xFF56647B)
    val TrendGood = Color(0xFF0D6B45)
    val TrendCaution = Color(0xFF8F1D6B)

    /** Spending heatmap ramp, lightest to darkest. */
    val Heat = listOf(
        Color(0xFFF7EDF7),
        Color(0xFFEDD4ED),
        Color(0xFFD989D9),
        Color(0xFFC50FC5),
        Color(0xFF6A016A)
    )

    // Navigation pill
    val NavBg = Color(0xFFF4F4F0)
    val NavBorder = Color(0x24080C18)
    val NavInk = Color(0xFF28344B)
    val NavActive = Color(0xFF080B13)

    /** `linear-gradient(165deg,#05080e 0%,#142342 65%,#38467a 100%)` */
    val PageGradient: Brush = CssLinearGradient(
        angleDegrees = 165f,
        stops = listOf(0f to Color(0xFF05080E), 0.65f to Color(0xFF142342), 1f to Color(0xFF38467A))
    )
}

/**
 * CSS `linear-gradient(<angle>, …)`: 0° points up, angles turn clockwise, and
 * the gradient line is long enough that the first and last stops land exactly
 * on the box's corners. Compose's linearGradient takes explicit points, so the
 * line is derived from the size at draw time.
 */
@Immutable
class CssLinearGradient(
    private val angleDegrees: Float,
    private val stops: List<Pair<Float, Color>>
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(radians).toFloat()
        val dy = -cos(radians).toFloat()
        val length = abs(size.width * dx) + abs(size.height * dy)
        val center = Offset(size.width / 2f, size.height / 2f)
        val half = Offset(dx * length / 2f, dy * length / 2f)
        return LinearGradientShader(
            from = center - half,
            to = center + half,
            colors = stops.map { it.second },
            colorStops = stops.map { it.first }
        )
    }

    override fun equals(other: Any?): Boolean =
        other is CssLinearGradient && other.angleDegrees == angleDegrees && other.stops == stops

    override fun hashCode(): Int = 31 * angleDegrees.hashCode() + stops.hashCode()
}

/** Headlines, money and row titles. */
val Barlow = FontFamily(
    Font(R.font.barlow_condensed_extralight, FontWeight.ExtraLight),
    Font(R.font.barlow_condensed_light, FontWeight.Light),
    Font(R.font.barlow_condensed_regular, FontWeight.Normal),
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold)
)

/** Supporting text, labels and controls. */
val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold)
)

private val NoPadding = PlatformTextStyle(includeFontPadding = false)
private val CenteredLines = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

/**
 * Barlow Condensed style. [lineHeight] is a multiple of the size (CSS
 * unitless line-height); null keeps the font's own metrics.
 */
fun display(
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Float = 0f,
    lineHeight: Float? = null,
    color: Color = Color.Unspecified
): TextStyle = TextStyle(
    fontFamily = Barlow,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = letterSpacing.sp,
    lineHeight = lineHeight?.let { (size * it).sp } ?: TextStyle.Default.lineHeight,
    lineHeightStyle = if (lineHeight != null) CenteredLines else null,
    platformStyle = NoPadding,
    color = color
)

/** DM Sans style; same conventions as [display]. */
fun body(
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Float? = null,
    letterSpacing: Float = 0f,
    color: Color = Color.Unspecified
): TextStyle = TextStyle(
    fontFamily = DmSans,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = letterSpacing.sp,
    lineHeight = lineHeight?.let { (size * it).sp } ?: TextStyle.Default.lineHeight,
    lineHeightStyle = if (lineHeight != null) CenteredLines else null,
    platformStyle = NoPadding,
    color = color
)

/** `.b-kicker`: 11px uppercase eyebrow with 1.8px tracking. */
fun kickerStyle(color: Color = Bot.PageMuted): TextStyle =
    body(11f, FontWeight.Medium, letterSpacing = 1.8f, color = color)

/** Tabular figures for amounts that line up in columns. */
internal val TabularNums = "tnum"

private val BotanicalColorScheme = lightColorScheme(
    primary = Bot.ActionSolid,
    onPrimary = Color.White,
    secondary = Bot.Navy,
    onSecondary = Color.White,
    background = Bot.PageDeep,
    onBackground = Color.White,
    surface = Color.White,
    onSurface = Bot.Ink,
    surfaceVariant = Bot.FieldBg,
    onSurfaceVariant = Bot.SurfaceMuted,
    outline = Bot.FieldBorder,
    error = Bot.Negative,
    onError = Color.White
)

/**
 * App theme. Keeps the existing font-scale clamp (0.85–1.3): every layout
 * pairs sp text with dp containers, and an unbounded accessibility scale
 * pushes labels out of their frames. Scrollables bounce past their edges
 * (iOS) instead of stretching.
 */
@Composable
fun BotanicalTheme(content: @Composable () -> Unit) {
    val perf = rememberPerfMode()
    val baseDensity = LocalDensity.current
    val clampedDensity = remember(baseDensity) {
        Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale.coerceIn(0.85f, 1.3f)
        )
    }
    CompositionLocalProvider(
        LocalPerfMode provides perf,
        LocalDensity provides clampedDensity,
        LocalOverscrollFactory provides rememberBounceOverscrollFactory()
    ) {
        MaterialTheme(colorScheme = BotanicalColorScheme, content = content)
    }
}

/** Letter spacing in em, for text that is later scaled to fit. */
internal fun emSpacing(px: Float, size: Float) = (px / size).em
