package com.spendwise.app.ui.theme

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

/**
 * Captures the device's animation budget. Two independent signals:
 *
 * - [lowSpec] — `ActivityManager.isLowRamDevice` returns true on Android Go
 *   tier devices (typically <2 GB RAM, slow eMMC, Mali-G31/PowerVR-class GPU).
 *   When true we skip blur, drop secondary animation transforms, and shorten
 *   non-essential durations. These devices physically can't render the full
 *   visual treatment at 60 fps.
 * - [reducedMotion] — the user has explicitly set the system animator scale to
 *   0 (Developer Options → Animator duration scale, or
 *   Settings → Accessibility → Remove animations). Independent of hardware;
 *   honors the user's preference regardless of device class. Reduced motion
 *   keeps opacity fades but drops transforms (slide, scale).
 */
data class PerfMode(
    val lowSpec: Boolean,
    val reducedMotion: Boolean
) {
    /** Combined: true if either signal asks us to skip a transform animation. */
    val shouldSkipTransforms: Boolean get() = lowSpec || reducedMotion
}

val LocalPerfMode = compositionLocalOf { PerfMode(lowSpec = false, reducedMotion = false) }

/** Resolves the current [PerfMode] from the device + system settings. */
@Composable
fun rememberPerfMode(): PerfMode {
    val context = LocalContext.current
    val reduced = reducedMotion()
    val lowSpec = remember(context) {
        runCatching {
            val am = context.getSystemService<ActivityManager>() ?: return@runCatching false
            // isLowRamDevice only flags Android Go hardware. The devices that
            // actually struggle with blur/large shadows are the cheap-but-not-Go
            // tier (3–4 GB RAM, Mali-G5x/PowerVR GPUs), so also treat a small
            // per-app heap budget or ≤4 GB total RAM as low-spec. totalMem
            // reports slightly under the marketed size (a "4 GB" phone shows
            // ~3.7 GB), so the 4 GB threshold catches that whole tier.
            val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            am.isLowRamDevice ||
                am.memoryClass <= 192 ||
                memInfo.totalMem <= 4_000_000_000L
        }.getOrDefault(false)
    }
    return remember(lowSpec, reduced) { PerfMode(lowSpec, reduced) }
}

/** [PerfMode] sourced from the ambient [LocalPerfMode]. */
val perfMode: PerfMode
    @Composable @ReadOnlyComposable
    get() = LocalPerfMode.current

/**
 * Returns a [tween] of [durationMillis], or a zero-duration [snap] when the
 * caller is in reduced-motion mode. Use for animations that drive *transforms*
 * (position, size, blur radius) — those should snap to their target when the
 * user has asked the system to stop animating.
 *
 * Opacity crossfades should generally keep their tween even in reduced motion,
 * since fading is what the accessibility setting expects ("reduced motion ≠
 * no motion"). Use [tween] directly for those.
 */
@Composable
fun <T> tweenOrSnap(
    durationMillis: Int,
    easing: Easing = FastOutSlowInEasing
): AnimationSpec<T> {
    val reduced = LocalPerfMode.current.reducedMotion
    return remember(reduced, durationMillis, easing) {
        if (reduced) snap() else tween(durationMillis = durationMillis, easing = easing)
    }
}
