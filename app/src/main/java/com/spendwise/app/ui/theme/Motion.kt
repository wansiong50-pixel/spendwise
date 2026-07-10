package com.spendwise.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Pressable — universal press feedback ─────────────────────────────────────
//
// Buttons must feel responsive to press. Subtle scale on press confirms the
// interface heard the user. Use this in place of bare `clickable {}` on every
// interactive surface (FAB, cards, rows, chips, pills, icon buttons).

/**
 * Adds press-scale feedback (scales to [scale] while pressed, springs back on
 * release). Keeps Material ripple via `LocalIndication`. Uses `graphicsLayer`
 * so the transform runs hardware-accelerated, off the main thread.
 *
 * Pass [scale] = 0.985 for wide surfaces (cards, rows) where 0.97 reads too
 * aggressive; the default 0.97 fits buttons and small chips.
 *
 * Press feedback respects reduced motion by collapsing the duration to 0.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    scale: Float = SpendWiseMotion.PressScale,
    shape: Shape? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduced = reducedMotion()
    val target = if (isPressed && enabled) scale else 1f
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = if (reduced) 0 else SpendWiseMotion.Press,
            easing = SpendWiseMotion.EaseOut
        ),
        label = "pressableScale"
    )
    // Clip lives BETWEEN graphicsLayer and clickable so it both bounds the
    // ripple AND scales with the card on press — no rectangular ripple flash
    // outside the corners, no transparent halo where the press-scale shrinks
    // the card inside an unscaled clip mask.
    this
        .graphicsLayer {
            scaleX = animated
            scaleY = animated
        }
        .let { if (shape != null) it.clip(shape) else it }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Same as [pressable] but with no Material ripple — useful when a wrapping
 * Surface already paints the indication, or when the design calls for purely
 * the scale feedback.
 */
fun Modifier.pressableNoIndication(
    enabled: Boolean = true,
    scale: Float = SpendWiseMotion.PressScale,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduced = reducedMotion()
    val target = if (isPressed && enabled) scale else 1f
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = if (reduced) 0 else SpendWiseMotion.Press,
            easing = SpendWiseMotion.EaseOut
        ),
        label = "pressableScale"
    )
    this
        .graphicsLayer {
            scaleX = animated
            scaleY = animated
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Like [pressable] but also detects long-press. Use this on surfaces that have
 * a primary tap action (edit/open) and a secondary destructive action (delete)
 * — long-press surfaces the destructive option without cluttering the row with
 * always-visible affordances.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressableCombined(
    enabled: Boolean = true,
    scale: Float = SpendWiseMotion.PressScale,
    shape: Shape? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduced = reducedMotion()
    val target = if (isPressed && enabled) scale else 1f
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = if (reduced) 0 else SpendWiseMotion.Press,
            easing = SpendWiseMotion.EaseOut
        ),
        label = "pressableCombinedScale"
    )
    // See [pressable] — clip is placed between scale and clickable so the
    // ripple is shape-bounded AND scales with the card.
    this
        .graphicsLayer {
            scaleX = animated
            scaleY = animated
        }
        .let { if (shape != null) it.clip(shape) else it }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick
        )
}

// ── Sheet drag-to-dismiss — gesture-driven, interruptible bottom sheets ──────
//
// The single biggest "feels smooth" ingredient in the X/Linear tier of apps:
// sheets track the finger 1:1, velocity carries through the release (flick
// hard = fast dismiss, drag slowly = it follows), and an under-threshold
// release springs back instead of committing. Attach [sheetDragToDismiss] to
// the sheet surface and read [SheetDragState.progress] in the scrim's draw
// phase so the dim fades with the drag.

@Stable
class SheetDragState internal constructor(
    internal val offsetY: Animatable<Float, AnimationVector1D>
) {
    internal var sheetHeightPx: Float by mutableFloatStateOf(0f)

    /** Current downward displacement in px — feed into an `offset {}`. */
    val offset: Float get() = offsetY.value

    /** 0 = resting, 1 = dragged fully off-screen. Drives the scrim fade. */
    val progress: Float
        get() = if (sheetHeightPx <= 0f) 0f
            else (offsetY.value / sheetHeightPx).coerceIn(0f, 1f)
}

/** Remembers a [SheetDragState], re-zeroed each time the sheet re-opens. */
@Composable
fun rememberSheetDragState(visible: Boolean): SheetDragState {
    val state = remember { SheetDragState(Animatable(0f)) }
    LaunchedEffect(visible) {
        if (visible) state.offsetY.snapTo(0f)
    }
    return state
}

/**
 * Makes a bottom-sheet surface draggable-to-dismiss. Dismiss commits when the
 * release velocity is a real flick (>2200 px/s) or the sheet is past 35% of
 * its height; anything less springs back. Upward drag past rest is
 * rubber-banded to a third so the sheet feels anchored, not rigid.
 *
 * Order matters at the call site: apply this BEFORE any `clickable` so the
 * drag samples the raw gesture. Inner `verticalScroll` regions keep their own
 * gestures; the drag picks up everywhere else (handle, hero, action rows).
 */
fun Modifier.sheetDragToDismiss(
    state: SheetDragState,
    onDismiss: () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    this
        .onGloballyPositioned { state.sheetHeightPx = it.size.height.toFloat() }
        .offset { IntOffset(0, state.offsetY.value.roundToInt().coerceAtLeast(0)) }
        .draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { delta ->
                scope.launch {
                    val next = state.offsetY.value + delta
                    state.offsetY.snapTo(if (next >= 0f) next else next / 3f)
                }
            },
            onDragStopped = { velocity ->
                val height = state.sheetHeightPx
                val commit = height > 0f &&
                    (velocity > 2200f || state.offsetY.value > height * 0.35f)
                scope.launch {
                    if (commit) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        state.offsetY.animateTo(
                            targetValue = height,
                            animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
                            initialVelocity = velocity
                        )
                        onDismiss()
                    } else {
                        state.offsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
                            initialVelocity = velocity
                        )
                    }
                }
            }
        )
}

// ── Stagger entrance — content settling in, not "the page loaded" ────────────
//
// Per Emil: keep stagger steps short (30–80 ms). Long delays make the
// interface feel slow. Stagger is decorative — reduce-motion drops the
// transform but keeps the opacity fade so content doesn't pop in cold.

/**
 * Provides the wall-clock millis at which the current screen mounted. Screens
 * that use [staggerEnter] inside a `LazyColumn` should wrap their content with
 * `CompositionLocalProvider(LocalScreenMountMs provides rememberScreenMountMs())`
 * so items that compose late (e.g. when the user scrolls below the fold
 * before the entrance window expires) can skip the entrance animation and
 * appear immediately instead of popping in cold.
 */
val LocalScreenMountMs = staticCompositionLocalOf<Long?> { null }

/** Captures the screen's mount time once, for use with [LocalScreenMountMs]. */
@Composable
fun rememberScreenMountMs(): Long = remember { System.currentTimeMillis() }

/**
 * Fades + lifts the element from `translateY(8.dp)` into place on first
 * composition, with a delay proportional to [index] (index 0 starts
 * immediately, index 1 after one step, etc.). Cap [index] at the call site so
 * a 50-item list doesn't take 2.5 s to settle.
 *
 * When the ambient [LocalScreenMountMs] is set, this modifier becomes a no-op
 * once the screen's entrance window has elapsed — items composed late by a
 * `LazyColumn` (e.g. scrolled into view fast) appear immediately rather than
 * waiting their stagger slot and popping in.
 */
fun Modifier.staggerEnter(
    index: Int = 0,
    enabled: Boolean = true,
    durationMillis: Int = SpendWiseMotion.Modal,
    stepMillis: Int = SpendWiseMotion.StaggerStep
): Modifier = composed {
    if (!enabled) return@composed this
    val reduced = reducedMotion()
    val mountMs = LocalScreenMountMs.current
    val composedAt = remember { System.currentTimeMillis() }
    // Tolerance covers prefetch + measure jitter between sibling items.
    val scheduledDelay = index.toLong() * stepMillis.toLong()
    val elapsedSinceMount = if (mountMs != null) composedAt - mountMs else 0L
    val skipAnimation = mountMs != null && elapsedSinceMount > scheduledDelay + 80L
    if (skipAnimation) return@composed this
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val remaining = (scheduledDelay - elapsedSinceMount).coerceAtLeast(0L)
        if (remaining > 0L) delay(remaining)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (reduced) durationMillis / 2 else durationMillis,
            easing = SpendWiseMotion.EaseOut
        ),
        label = "staggerAlpha"
    )
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 8f, // 8dp converted in graphicsLayer scope
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = SpendWiseMotion.EaseOut
        ),
        label = "staggerTranslate"
    )
    this.graphicsLayer {
        this.alpha = alpha
        if (!reduced) {
            translationY = translateY.dp.toPx()
        }
    }
}
