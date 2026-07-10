package com.spendwise.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Screen size tier the dashboard scales its primary surfaces against. The
 * heatmap, recent-activity list, and category tiles already scale horizontally
 * via `weight(1f)`, so the visible difference between phones is concentrated
 * in the hero balance and greeting — both of which use fixed sp + dp values.
 * On a tall Pixel-style phone with default display zoom (~448dp × 997dp), the
 * 42sp hero amount and 26sp greeting end up consuming a much smaller share of
 * the screen than they do on a typical 360dp × 740dp mid-range phone, leaving
 * a band of empty space around the hero and heatmap. Bumping primary type +
 * padding one tier closes that gap without making compact phones feel cramped.
 */
enum class SizeTier { Compact, Medium, Tall }

data class SwSizing(
    val tier: SizeTier,
    /** Multiplier for hero text (`42f`, `26f`) and hero/greeting padding. */
    val scale: Float
)

val LocalSwSizing = compositionLocalOf { SwSizing(SizeTier.Compact, 1.0f) }

/** Resolves the current [SwSizing] from the active configuration. */
@Composable
fun rememberSwSizing(): SwSizing {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp
    val heightDp = config.screenHeightDp
    return remember(widthDp, heightDp) {
        val tier = when {
            heightDp >= 900 -> SizeTier.Tall
            widthDp >= 410 -> SizeTier.Medium
            else -> SizeTier.Compact
        }
        val scale = when (tier) {
            SizeTier.Tall -> 1.22f
            SizeTier.Medium -> 1.10f
            SizeTier.Compact -> 1.0f
        }
        SwSizing(tier, scale)
    }
}

/** Scale factor sourced from the surrounding [LocalSwSizing]. */
val swScale: Float
    @Composable @ReadOnlyComposable
    get() = LocalSwSizing.current.scale
