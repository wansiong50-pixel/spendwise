package com.spendwise.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * SpendWise v2 ("Soft Fintech") color system.
 *
 * Source of truth: the SpendWise Redesign May 2026 handoff. Canvas is a tinted
 * lavender wash that wraps everything in one calm temperature; pebble surfaces
 * are true white floating on it. Ink (#15121F) and violet (#6E5BF2) carry all
 * confident contrast — every other accent is a pastel category tint.
 *
 * Field naming preserves the legacy `plum*`, `coral*`, `tertiary*` slots so
 * existing call sites keep compiling, but the values they resolve to are
 * remapped to the v2 system (e.g. `plumContainer` → violet, `coralContainer`
 * → peach). Treat the `sw*` and `cat*` fields as the canonical tokens going
 * forward; the legacy aliases are kept only so we can migrate file-by-file.
 */
data class SpendWiseColors(
    // ── Legacy slots (remapped to v2 hues; keep field names for call sites) ──
    val plumPrimary: Color,
    val plumContainer: Color,
    val plumFixed: Color,
    val coralSecondary: Color,
    val coralContainer: Color,
    val coralFixed: Color,
    val tertiaryDark: Color,
    val tertiaryContainer: Color,
    val tertiaryFixed: Color,
    // ── Surface hierarchy ────────────────────────────────────────────────
    /** In-phone app background. The design canvas outside the phone is
     *  lavender, but the app screen itself is the lighter #F4F1FA. */
    val appBackground: Color,
    /** White pebble surface — the dominant card material. */
    val appSurface: Color,
    /** Tinted lavender low surface — inactive segmented controls, input
     *  fills, recessed wells. */
    val appSurfaceLow: Color,
    /** Slightly darker lavender — dividers, grabbers, sheet handles. */
    val appSurfaceContainer: Color,
    /** Strongest lavender hairline — chart baselines, input outlines. */
    val appSurfaceContainerHigh: Color,
    val appOnSurface: Color,
    val appOnSurfaceVariant: Color,
    val appOutline: Color,
    val appError: Color,
    val appErrorContainer: Color,
    val appAmber: Color,
    // ── v2 canonical tokens ─────────────────────────────────────────────
    /** Confident ink — buttons, headlines, selected chips, FAB ring. */
    val swInk: Color,
    /** Brand violet — FAB fill, primary chart bar, highlighted indicator. */
    val swViolet: Color,
    /** Lavender pastel — primary category, soft buttons, hero gradient end. */
    val swVioletSoft: Color,
    /** Warm peach — Food/Eating Out category, hero gradient start. */
    val swPeach: Color,
    /** Light peach — gradient mid-tones, soft tints. */
    val swPeachSoft: Color,
    /** Mint — income / Groceries / positive accents. */
    val swMint: Color,
    /** Pink — Entertainment / Fun. */
    val swPink: Color,
    /** Sky blue — Transport. */
    val swSky: Color,
    /** Butter yellow — Bills / Utilities. */
    val swButter: Color,
    /** Coral — Gifts. */
    val swCoral: Color,
    /** Positive semantic — income, net positive, % keeping. */
    val swPos: Color,
    /** Negative semantic — negative balance, expense red. */
    val swNeg: Color,
    // ── Heatmap (5-step lavender ramp, light to dark) ───────────────────
    val swHeat0: Color,
    val swHeat1: Color,
    val swHeat2: Color,
    val swHeat3: Color,
    val swHeat4: Color,
    // ── Legacy-name semantic tokens kept for call sites ─────────────────
    val swPrimary: Color,
    val onCoral: Color,
    val cream: Color,
    val ctaBg: Color,
    val ctaFg: Color,
    val heroBg: Color,
    val green: Color,
    val plumMuted: Color,
    val plum1: Color,
    val plum2: Color,
    /** Navigation bar — floating black pill silhouette. */
    val navBg: Color,
    val isDark: Boolean
)

private val LightAppColors = SpendWiseColors(
    // Legacy slots remapped to v2 hues
    plumPrimary        = Color(0xFF15121F),
    plumContainer      = Color(0xFF6E5BF2),
    plumFixed          = Color(0xFFC5BCF8),
    coralSecondary     = Color(0xFFF8C99B),
    coralContainer     = Color(0xFFF8C99B),
    coralFixed         = Color(0xFFFCE0BC),
    tertiaryDark       = Color(0xFF15121F),
    tertiaryContainer  = Color(0xFFE8E4F2),
    tertiaryFixed      = Color(0xFFF1EEF7),
    // Surfaces — light in-phone screen, white pebble cards
    appBackground      = Color(0xFFF4F1FA),
    appSurface         = Color(0xFFFFFFFF),
    appSurfaceLow      = Color(0xFFF1EEF7),
    appSurfaceContainer      = Color(0xFFE8E4F2),
    appSurfaceContainerHigh  = Color(0xFFD6CBF1),
    appOnSurface       = Color(0xFF15121F),
    appOnSurfaceVariant= Color(0xFF8E8AA0),
    appOutline         = Color(0xFFB8AFCF),
    appError           = Color(0xFFE54B5B),
    appErrorContainer  = Color(0xFFFFE1E5),
    appAmber           = Color(0xFFE89B3C),
    // v2 canonical
    swInk              = Color(0xFF15121F),
    swViolet           = Color(0xFF6E5BF2),
    swVioletSoft       = Color(0xFFC5BCF8),
    swPeach            = Color(0xFFF8C99B),
    swPeachSoft        = Color(0xFFFCE0BC),
    swMint             = Color(0xFF9FE3C5),
    swPink             = Color(0xFFF8C0D6),
    swSky              = Color(0xFFB8D5FF),
    swButter           = Color(0xFFFFE08A),
    swCoral            = Color(0xFFF4A39B),
    swPos              = Color(0xFF1FA971),
    swNeg              = Color(0xFFE54B5B),
    swHeat0            = Color(0xFFEDE9F5),
    swHeat1            = Color(0xFFD6CBF1),
    swHeat2            = Color(0xFFB8A6EA),
    swHeat3            = Color(0xFF8E72E3),
    swHeat4            = Color(0xFF5B40D6),
    // Legacy semantic
    swPrimary          = Color(0xFF15121F),
    onCoral            = Color(0xFF15121F),
    cream              = Color(0xFFFFFFFF),
    ctaBg              = Color(0xFF15121F),
    ctaFg              = Color(0xFFFFFFFF),
    heroBg             = Color(0xFFC5BCF8),
    green              = Color(0xFF1FA971),
    plumMuted          = Color(0xFF8E8AA0),
    plum1              = Color(0xFF6E5BF2),
    plum2              = Color(0xFFB8D5FF),
    navBg              = Color(0xFF15121F),
    isDark             = false
)

// Dark mode — "Deep aubergine, luminous accents, same tabular money."
//
// Not an inversion. The lavender canvas swaps for a near-black aubergine
// (#0F0D1A) that keeps the brand's purple temperature, then cards live on a
// single elevated tier on top of it. We don't lean on drop shadows in dark —
// the brightness step from canvas (#0F0D1A) → surface (#1A1727) → surface2
// (#221E33) does all the depth work, which is both cheaper to render and reads
// cleaner under low ambient light. Accents are tuned ~15–20% brighter so they
// survive on the dark canvas; income green moves from a muted #1FA971 in light
// to a bioluminescent #4EE3A8 in dark. The heatmap inverts its semantic —
// "more spent" gets *brighter* lavender, not darker — because dark cells on a
// dark canvas would just disappear into the background.
private val DarkAppColors = SpendWiseColors(
    // Legacy slots — remapped to the v2 dark hues so anything still using the
    // old names continues to read correctly.
    plumPrimary        = Color(0xFFF4F1FA),
    plumContainer      = Color(0xFF8C7BFF),
    plumFixed          = Color(0xFF221E33),
    coralSecondary     = Color(0xFFF4A874),
    coralContainer     = Color(0xFFF4A874),
    coralFixed         = Color(0xFF221E33),
    tertiaryDark       = Color(0xFFF4F1FA),
    tertiaryContainer  = Color(0xFF221E33),
    tertiaryFixed      = Color(0xFF1A1727),
    // Surface hierarchy — aubergine canvas + single elevation tier
    appBackground      = Color(0xFF0F0D1A),
    appSurface         = Color(0xFF1A1727),
    appSurfaceLow      = Color(0xFF221E33),
    appSurfaceContainer      = Color(0xFF2D2842),
    appSurfaceContainerHigh  = Color(0xFF3A3354),
    appOnSurface       = Color(0xFFF4F1FA),
    appOnSurfaceVariant= Color(0xFF7F7A98),
    appOutline         = Color(0xFF7F7A98),
    appError           = Color(0xFFFF7088),
    appErrorContainer  = Color(0xFF4A1D28),
    appAmber           = Color(0xFFFFD062),
    // v2 canonical — accents pushed 15–20% brighter than their light siblings
    swInk              = Color(0xFFF4F1FA),
    swViolet           = Color(0xFF8C7BFF),
    swVioletSoft       = Color(0xFF3A2F66),
    swPeach            = Color(0xFFF4A874),
    swPeachSoft        = Color(0xFF4A3424),
    swMint             = Color(0xFF4EE3A8),
    swPink             = Color(0xFFF4A0C2),
    swSky              = Color(0xFF9ABFFF),
    swButter           = Color(0xFFFFD062),
    swCoral            = Color(0xFFFF8B7E),
    swPos              = Color(0xFF4EE3A8),
    swNeg              = Color(0xFFFF7088),
    // Heatmap — inverted semantic: heat0 sits at surface tone (almost
    // invisible by design — "no spend that day") and heat4 blooms to a near-
    // white violet.
    swHeat0            = Color(0xFF1A1727),
    swHeat1            = Color(0xFF2A2447),
    swHeat2            = Color(0xFF473B7F),
    swHeat3            = Color(0xFF7560D9),
    swHeat4            = Color(0xFFB6A4FF),
    swPrimary          = Color(0xFFF4F1FA),
    onCoral            = Color(0xFF0F0D1A),
    cream              = Color(0xFF1A1727),
    // Dark CTA flips to violet (not ink) so it stays the brightest pixel and
    // can carry the FAB-style glow we apply on tap targets in dark.
    ctaBg              = Color(0xFF8C7BFF),
    ctaFg              = Color(0xFFFFFFFF),
    heroBg             = Color(0xFF2A1F4D),
    green              = Color(0xFF4EE3A8),
    plumMuted          = Color(0xFF7F7A98),
    plum1              = Color(0xFF8C7BFF),
    plum2              = Color(0xFF9ABFFF),
    // Bottom nav silhouette is translucent surface (rgba 0.85) in the
    // composable — this is the fallback solid tone for places that paint nav
    // with a single opaque color.
    navBg              = Color(0xFF1A1727),
    isDark             = true
)

private val LocalSpendWiseColors = staticCompositionLocalOf { LightAppColors }

val MaterialTheme.spendWiseColors: SpendWiseColors
    @Composable @ReadOnlyComposable
    get() = LocalSpendWiseColors.current

// ── Primary palette ──────────────────────────────────────────────────────────

val PlumPrimary: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.plumPrimary

val PlumContainer: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.plumContainer

val PlumFixed: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.plumFixed

val CoralSecondary: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.coralSecondary

val CoralContainer: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.coralContainer

val CoralFixed: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.coralFixed

val TertiaryDark: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.tertiaryDark

val TertiaryContainer: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.tertiaryContainer

val TertiaryFixed: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.tertiaryFixed

// ── Surface hierarchy ────────────────────────────────────────────────────────

val AppBackground: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.appBackground

val AppSurface: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.appSurface

val AppSurfaceLow: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.appSurfaceLow

val AppSurfaceContainer: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.appSurfaceContainer

val AppSurfaceContainerHigh: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.appSurfaceContainerHigh

// ── Content colours ──────────────────────────────────────────────────────────

val AppOnSurface: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.appOnSurface

val AppOnSurfaceVariant: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.appOnSurfaceVariant

val AppOutline: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.appOutline

val AppError: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.appError

val AppErrorContainer: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.appErrorContainer

val AppAmber: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.appAmber

// ── Semantic / design-system tokens ──────────────────────────────────────────

/** Primary app accent. */
val AppSWPrimary: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.swPrimary

/** Readable text on clay accent surfaces. */
val OnCoral: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.onCoral

/** Warm neutral surface for selected-model cards. */
val AppCream: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.cream

/** CTA background. */
val AppCtaBg: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.ctaBg

/** CTA foreground. */
val AppCtaFg: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.ctaFg

/** Hero card background. */
val AppHeroBg: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.heroBg

/** Positive change indicator green. */
val AppGreen: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.green

/** Muted text for secondary copy on hero surfaces. */
val AppPlumMuted: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.plumMuted

/** Category accent 1. */
val AppPlum1: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.plum1

/** Category accent 2. */
val AppPlum2: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.plum2

/** Navigation bar background. */
val AppNavBg: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.navBg

/** Whether dark mode is currently active. */
val AppIsDark: Boolean
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.spendWiseColors.isDark

// ── v2 canonical tokens ──────────────────────────────────────────────────────
//
// These are the names new code should use. They map to the same field-backed
// values as the legacy aliases below, just expressed in the v2 vocabulary
// (ink/violet/pastel) so call sites read like the design reference.

/** Confident ink — the one loud thing on every screen. */
val SwInk: Color         @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swInk
/** Brand violet — FAB, primary chart highlight, selected indicators. */
val SwViolet: Color      @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swViolet
/** Lavender pastel — soft buttons, hero gradient end, primary category. */
val SwVioletSoft: Color  @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swVioletSoft
/** Warm peach — Food, hero gradient start. */
val SwPeach: Color       @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swPeach
/** Light peach — gradient mid-tones. */
val SwPeachSoft: Color   @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swPeachSoft
/** Mint — income tile, positive accents. */
val SwMint: Color        @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swMint
/** Pink — Entertainment. */
val SwPink: Color        @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swPink
/** Sky — Transport. */
val SwSky: Color         @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swSky
/** Butter — Bills. */
val SwButter: Color      @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swButter
/** Coral — Gifts. */
val SwCoral: Color       @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swCoral
/** Positive semantic. */
val SwPos: Color         @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swPos
/** Negative semantic. */
val SwNeg: Color         @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swNeg
/** Heatmap step 0 — coolest. */
val SwHeat0: Color       @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swHeat0
val SwHeat1: Color       @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swHeat1
val SwHeat2: Color       @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swHeat2
val SwHeat3: Color       @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swHeat3
/** Heatmap step 4 — hottest. */
val SwHeat4: Color       @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.swHeat4

/**
 * SpendWise pebble-shadow stack — the one shadow recipe used by every floating
 * white surface (hero, recent-activity card, account row…). Apply via
 * `Modifier.shadow(elevation = SwPebbleShadow, shape = RoundedCornerShape(24.dp))`
 * — the soft drop is what sells "pebble floating on lavender".
 */
val SwPebbleShadow: Dp = 14.dp

// ── Legacy aliases (kept for backward compat) ────────────────────────────────

val AppInk: Color      @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.plumPrimary
val AppMuted: Color    @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.appOnSurfaceVariant
val AppMist: Color     @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.appSurfaceLow
val AppLine: Color     @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.appSurfaceContainerHigh
val AppTeal: Color     @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.coralSecondary
val AppCoral: Color    @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.coralSecondary
val AppJadeDark: Color @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.plumContainer
val AppJadeSoft: Color @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.coralFixed
val AppSaffron: Color  @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.coralContainer
val AppMint: Color     @Composable @ReadOnlyComposable get() = MaterialTheme.spendWiseColors.tertiaryFixed

// ── Material colour schemes ───────────────────────────────────────────────────

private val LightColors = lightColorScheme(
    primary              = LightAppColors.plumPrimary,
    onPrimary            = LightAppColors.ctaFg,
    primaryContainer     = LightAppColors.plumContainer,
    onPrimaryContainer   = LightAppColors.ctaFg,
    secondary            = LightAppColors.coralSecondary,
    onSecondary          = LightAppColors.ctaFg,
    secondaryContainer   = LightAppColors.coralFixed,
    onSecondaryContainer = Color(0xFF5D3C34),
    tertiary             = LightAppColors.tertiaryDark,
    onTertiary           = LightAppColors.ctaFg,
    tertiaryContainer    = LightAppColors.tertiaryContainer,
    onTertiaryContainer  = Color(0xFF4C4039),
    background           = LightAppColors.appBackground,
    onBackground         = LightAppColors.appOnSurface,
    surface              = LightAppColors.appSurface,
    onSurface            = LightAppColors.appOnSurface,
    surfaceVariant       = LightAppColors.appSurfaceContainerHigh,
    onSurfaceVariant     = LightAppColors.appOnSurfaceVariant,
    error                = LightAppColors.appError,
    onError              = LightAppColors.ctaFg,
    errorContainer       = LightAppColors.appErrorContainer,
    onErrorContainer     = Color(0xFF93000A),
    outline              = LightAppColors.appOutline,
    outlineVariant       = Color(0xFFD2C9BE),
)

private val DarkColors = darkColorScheme(
    primary              = DarkAppColors.plumPrimary,
    onPrimary            = Color(0xFF211C18),
    primaryContainer     = DarkAppColors.plumContainer,
    onPrimaryContainer   = Color(0xFFF0E9DF),
    secondary            = DarkAppColors.coralSecondary,
    onSecondary          = Color(0xFF241A17),
    secondaryContainer   = DarkAppColors.coralContainer,
    onSecondaryContainer = Color(0xFF241A17),
    tertiary             = Color(0xFFE1D7CA),
    onTertiary           = Color(0xFF211C18),
    tertiaryContainer    = DarkAppColors.tertiaryContainer,
    onTertiaryContainer  = Color(0xFFE1D7CA),
    background           = DarkAppColors.appBackground,
    onBackground         = DarkAppColors.appOnSurface,
    surface              = DarkAppColors.appSurface,
    onSurface            = DarkAppColors.appOnSurface,
    surfaceVariant       = DarkAppColors.appSurfaceContainerHigh,
    onSurfaceVariant     = DarkAppColors.appOnSurfaceVariant,
    error                = DarkAppColors.appError,
    onError              = Color(0xFF690005),
    errorContainer       = DarkAppColors.appErrorContainer,
    onErrorContainer     = Color(0xFFFFDAD6),
    outline              = DarkAppColors.appOutline,
    outlineVariant       = Color(0xFF50473F),
)

/**
 * OpenType feature flag for tabular figures. Apply to any Text rendering money,
 * percentages, or any numeric column so digits keep equal advance widths and
 * "RM 1,234.50" / "RM 9,876.00" align cleanly under each other instead of
 * shifting around as the values change.
 *
 * Supported by Roboto (Android default) and most modern UI typefaces. Use as:
 * `style = TextStyle(..., fontFeatureSettings = FinanceNumFeatures)`.
 */
const val FinanceNumFeatures: String = "tnum"

// Shared text-bounds-tightening settings — applied to every typography entry
// so the measured bounds match the visible glyph ink. Without these, the
// default line-height padding makes text sit slightly above the geometric
// center of its layout box; any icon placed next to it via `Row(
// CenterVertically)` reads as misaligned. See HeaderBlock for the canonical
// case (May ⌄ vs the theme-toggle moon icon).
/**
 * Trims ascender + descender padding so a Text's measured bounds match its
 * visible glyph extent. Apply to inline `TextStyle(...)` declarations whenever
 * the Text shares a `Row(CenterVertically)` with an Icon/Button — otherwise
 * the icon reads as floating above the text.
 */
val TrimBoth = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both
)
/** Removes Android's legacy `includeFontPadding` — pairs with [TrimBoth]. */
val NoFontPadding = PlatformTextStyle(includeFontPadding = false)

// v2 type system — Plus Jakarta Sans inheritance (system fallback on Android);
// money lives at ExtraBold with tight tracking, body sits at Medium, eyebrows
// are loud 700 with letterspacing. RM prefix handling happens at the call site
// (Amount composable in SpendWisePrimitives.kt) — 55% opacity, 55% size.
private val SpendWiseTypography = Typography(
    // Display · hero amounts ("RM 5,192.50"). 42sp, weight 800, tight tracking.
    headlineLarge = TextStyle(
        fontSize = 42.sp, lineHeight = 46.sp,
        fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp,
        lineHeightStyle = TrimBoth, platformStyle = NoFontPadding
    ),
    // Greeting / "Here's where your money is" — 26sp, 700.
    headlineMedium = TextStyle(
        fontSize = 26.sp, lineHeight = 30.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp,
        lineHeightStyle = TrimBoth, platformStyle = NoFontPadding
    ),
    // Section / card titles — 15sp, 700, slight tightening.
    titleLarge = TextStyle(
        fontSize = 15.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.15).sp,
        lineHeightStyle = TrimBoth, platformStyle = NoFontPadding
    ),
    // Row title — merchant names, account names — 14.5sp, 600.
    titleMedium = TextStyle(
        fontSize = 14.5.sp, lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp,
        lineHeightStyle = TrimBoth, platformStyle = NoFontPadding
    ),
    // Body — supporting copy, descriptions — 14sp, 500.
    bodyLarge = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 0.sp,
        lineHeightStyle = TrimBoth, platformStyle = NoFontPadding
    ),
    // Secondary body — category, time, sublabel — 12.5sp, 500, muted.
    bodyMedium = TextStyle(
        fontSize = 12.5.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 0.sp,
        lineHeightStyle = TrimBoth, platformStyle = NoFontPadding
    ),
    // Chip / pill labels — 12.5sp, 700.
    labelLarge = TextStyle(
        fontSize = 12.5.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.sp,
        lineHeightStyle = TrimBoth, platformStyle = NoFontPadding
    ),
    // Subtle row metadata.
    labelMedium = TextStyle(
        fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp,
        lineHeightStyle = TrimBoth, platformStyle = NoFontPadding
    ),
    // Eyebrow — uppercase TOTAL BALANCE / NET YTD with wide tracking.
    labelSmall = TextStyle(
        fontSize = 11.sp, lineHeight = 14.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
        lineHeightStyle = TrimBoth, platformStyle = NoFontPadding
    )
)

// v2 radii — r10 inputs, r14 chips, r18 buttons, r24 cards, r30 hero, 999 pills.
// Map to Material's slot vocabulary so component defaults pick up the right
// shape automatically.
private val SpendWiseShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small      = RoundedCornerShape(14.dp),
    medium     = RoundedCornerShape(18.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

// ── Motion tokens — Emil Kowalski's animation philosophy in Compose ──────────
//
// Custom easings (the built-in CSS / Compose easings are too weak for UI),
// asymmetric durations (release always snappier than press), and Apple-style
// springs that stay easier to reason about than mass/stiffness/damping.

object SpendWiseMotion {
    // Strong ease-out — UI interactions and entrances. Starts fast (the moment
    // the user is watching most closely), then settles.
    val EaseOut: CubicBezierEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    // Strong ease-in-out — for elements moving on-screen (not in or out of view).
    val EaseInOut: CubicBezierEasing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

    // iOS-like drawer curve — purpose-built for sheet/drawer motion paths.
    val EaseDrawer: CubicBezierEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    // Standard ease — color and other non-positional changes.
    val EaseStandard = FastOutSlowInEasing

    // Durations (ms). UI animations stay under 300; charts and rare moments
    // can breathe a bit longer.
    const val Press: Int     = 140
    const val Tooltip: Int   = 160
    const val Dropdown: Int  = 200
    const val Modal: Int     = 240   // enter
    const val ModalExit: Int = 180   // exit faster than enter — release is always snappy
    const val Sheet: Int     = 280
    const val Chart: Int     = 600   // first-time / rare

    // Press scale — never below 0.95. Subtle but readable on press.
    const val PressScale: Float = 0.97f
    // Wider surfaces (cards, rows) — even gentler scale still reads.
    const val PressScaleSurface: Float = 0.985f
    // Entry scale — 0.96, never 0. Nothing in the real world appears from nothing.
    const val EntryScale: Float = 0.96f

    // Stagger step between sibling list items, in ms. Range 30–80.
    const val StaggerStep: Int = 50

    // Apple-style springs — define by visible bounce + duration, not raw physics.
    val SpringSubtle: SpringSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow
    )
    val SpringPlayful: SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessLow
    )
}

/** True when the user has disabled animations system-wide (Developer Options or
 *  accessibility settings). Reduced-motion-aware code should drop transforms
 *  but keep opacity/color crossfades — reduced motion ≠ no motion. */
@Composable
fun reducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val appColors      = if (darkTheme) DarkAppColors else LightAppColors
    val materialColors = if (darkTheme) DarkColors    else LightColors
    val sizing         = rememberSwSizing()
    val perf           = rememberPerfMode()

    // Clamp the system font scale. Every layout in the app pairs sp text with
    // fixed-dp containers (segmented pills, nav bar, donut labels, stat
    // tiles…), so an unbounded accessibility font scale (up to 2.0 on some
    // OEMs) inflates text past its frame and breaks rows. 1.3 keeps a
    // meaningful accessibility bump while staying within what the fixed-dp
    // frames were designed to absorb; the floor guards against sub-1.0
    // display setups shrinking labels below legibility.
    val baseDensity = LocalDensity.current
    val clampedDensity = remember(baseDensity) {
        Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale.coerceIn(0.85f, 1.3f)
        )
    }

    CompositionLocalProvider(
        LocalSpendWiseColors provides appColors,
        LocalSwSizing provides sizing,
        LocalPerfMode provides perf,
        LocalDensity provides clampedDensity
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography  = SpendWiseTypography,
            shapes      = SpendWiseShapes,
            content     = content
        )
    }
}
