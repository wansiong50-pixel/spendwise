package com.spendwise.app.ui.botanical

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.foundation.layout.offset
import com.spendwise.app.ui.theme.LocalPerfMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Motion, the iOS way: everything that moves is a spring, described like
 * SwiftUI's — a response (roughly how long the move takes, in seconds) and a
 * damping ratio (1 arrives without overshoot). Springs keep their velocity
 * when retargeted, so an interrupted animation turns around instead of
 * restarting, and gestures hand their release speed to the spring that
 * finishes the move. The system "remove animations" setting still applies:
 * Compose scales every spring by the animator duration scale.
 */
object BotMotion {
    /** Large surfaces — pages, sheets — arrive without wobbling. */
    fun <T> smooth(response: Float = 0.42f, visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 1f, stiffness = stiffnessFor(response), visibilityThreshold = visibilityThreshold)

    /** Controls settling into place (segments, the tab pill) overshoot a hair. */
    fun <T> snappy(response: Float = 0.34f, visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.82f, stiffness = stiffnessFor(response), visibilityThreshold = visibilityThreshold)

    /** Confirmations — a check mark, a released button — with some life in them. */
    fun <T> bouncy(response: Float = 0.36f, visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.62f, stiffness = stiffnessFor(response), visibilityThreshold = visibilityThreshold)

    /** Quick and firm: tracks a finger or a press. */
    fun <T> interactive(response: Float = 0.16f, visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = stiffnessFor(response), visibilityThreshold = visibilityThreshold)

    /** Page pushes and pops. */
    val PageOffset: SpringSpec<IntOffset> = smooth(0.42f, IntOffset.VisibilityThreshold)
    val PageFade: SpringSpec<Float> = smooth(0.42f)

    /** Crossfades: tab switches, swapped labels. */
    val Fade: SpringSpec<Float> = smooth(0.3f)

    /** Selection colour changes. */
    val ColorShift: SpringSpec<Color> = smooth(0.3f)

    /** Layout growing or shrinking around changed content. */
    val Resize: SpringSpec<IntSize> = smooth(0.38f, IntSize.VisibilityThreshold)

    /** Sheets rise a little slower than they leave. */
    val SheetOpen: SpringSpec<Float> = smooth(0.46f)
    val SheetClose: SpringSpec<Float> = smooth(0.36f)
}

/** SwiftUI's spring `response` as a Compose stiffness (unit mass): k = (2π / response)². */
fun stiffnessFor(response: Float): Float {
    val omega = 2f * PI.toFloat() / response
    return omega * omega
}

// ── Rubber band ──────────────────────────────────────────────────────────────

/** UIScrollView's coefficient: near the edge the content follows at 55% of the finger. */
internal const val RubberCoefficient = 0.55f

/**
 * UIScrollView's rubber band: how far content shows past an edge when the
 * finger has pulled [distance] beyond it, within a view [dimension] long.
 * The further the pull, the less the content follows; it never reaches
 * [dimension].
 */
internal fun rubberBand(distance: Float, dimension: Float): Float {
    if (dimension <= 0f || distance <= 0f) return 0f
    return (1f - 1f / (distance * RubberCoefficient / dimension + 1f)) * dimension
}

/** The finger distance that shows content [offset] past the edge — [rubberBand] reversed. */
internal fun rubberBandInverse(offset: Float, dimension: Float): Float {
    if (dimension <= 0f || offset <= 0f) return 0f
    val shown = offset.coerceAtMost(dimension * 0.999f)
    return dimension / RubberCoefficient * (shown / (dimension - shown))
}

// ── Overscroll ───────────────────────────────────────────────────────────────

/**
 * iOS-style overscroll. Past an edge the content follows the finger with
 * rubber-band resistance and springs home on release; a fling that runs into
 * an edge bounces with the speed it had left. A touch catches a bounce in
 * flight. [offsetY]/[offsetX] expose the displacement so a header can
 * stretch to fill the gap it leaves.
 */
@Stable
class BounceOverscroll internal constructor(
    private val scope: CoroutineScope,
    private val maxExtent: Float,
    private val density: Float
) : OverscrollEffect {
    /** How far the content shows past its edge, px; positive = pulled down / right. */
    var offsetX by mutableFloatStateOf(0f)
        private set
    var offsetY by mutableFloatStateOf(0f)
        private set

    private var extentX = maxExtent
    private var extentY = maxExtent
    private var settleJob: Job? = null

    override val isInProgress: Boolean
        get() = offsetX != 0f || offsetY != 0f

    override val node: DelegatableNode = BounceNode(this)

    internal fun onViewport(width: Int, height: Int) {
        extentX = min(width.toFloat(), maxExtent).coerceAtLeast(1f)
        extentY = min(height.toFloat(), maxExtent).coerceAtLeast(1f)
    }

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        if (source != NestedScrollSource.UserInput) return performScroll(delta)
        settleJob?.cancel()
        // A stretched edge gives back first, before the content scrolls away from it.
        val usedX = relax(delta.x, horizontal = true)
        val usedY = relax(delta.y, horizontal = false)
        val remaining = Offset(delta.x - usedX, delta.y - usedY)
        val left = remaining - performScroll(remaining)
        if (left.x != 0f) offsetX = pulled(offsetX, left.x, extentX)
        if (left.y != 0f) offsetY = pulled(offsetY, left.y, extentY)
        return delta
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        if (offsetX != 0f || offsetY != 0f) {
            // Let go while stretched: spring home, carrying the finger's speed
            // as the stretched content felt it.
            settle(Velocity(velocity.x * followRate(offsetX, extentX), velocity.y * followRate(offsetY, extentY)))
            return
        }
        val left = performFling(velocity)
        val bounce = Velocity(edgeBounce(left.x), edgeBounce(left.y))
        if (bounce != Velocity.Zero) settle(bounce)
    }

    /** Returns how much of [delta] went into relaxing an existing stretch. */
    private fun relax(delta: Float, horizontal: Boolean): Float {
        val offset = if (horizontal) offsetX else offsetY
        if (offset == 0f || delta == 0f || sign(delta) == sign(offset)) return 0f
        val extent = if (horizontal) extentX else extentY
        val pull = sign(offset) * rubberBandInverse(abs(offset), extent)
        val next = pull + delta
        val (used, shown) = if (next == 0f || sign(next) != sign(pull)) {
            -pull to 0f
        } else {
            delta to sign(next) * rubberBand(abs(next), extent)
        }
        if (horizontal) offsetX = shown else offsetY = shown
        return used
    }

    private fun pulled(offset: Float, extra: Float, extent: Float): Float {
        val pull = sign(offset) * rubberBandInverse(abs(offset), extent) + extra
        return sign(pull) * rubberBand(abs(pull), extent)
    }

    /** Slope of the rubber band at the current stretch: how fast the content moves per finger px. */
    private fun followRate(offset: Float, extent: Float): Float {
        val t = rubberBandInverse(abs(offset), extent) * RubberCoefficient / extent + 1f
        return RubberCoefficient / (t * t)
    }

    /** A fling that reached an edge keeps half its speed as the bounce, within reason. */
    private fun edgeBounce(left: Float): Float {
        if (abs(left) < MinBounceVelocity * density) return 0f
        val cap = MaxBounceVelocity * density
        return (left * 0.5f).coerceIn(-cap, cap)
    }

    private fun settle(velocity: Velocity) {
        settleJob?.cancel()
        settleJob = scope.launch {
            coroutineScope {
                launch { animate(offsetX, 0f, velocity.x, SettleSpring) { value, _ -> offsetX = value } }
                launch { animate(offsetY, 0f, velocity.y, SettleSpring) { value, _ -> offsetY = value } }
            }
        }
    }

    private companion object {
        val SettleSpring = spring<Float>(dampingRatio = 1f, stiffness = stiffnessFor(0.42f))
        const val MinBounceVelocity = 250f // dp/s
        const val MaxBounceVelocity = 2600f // dp/s
    }
}

/** Moves the scrolled content by the effect's offset, in the layer (no relayout). */
private class BounceNode(private val effect: BounceOverscroll) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        effect.onViewport(placeable.width, placeable.height)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                translationX = effect.offsetX
                translationY = effect.offsetY
            }
        }
    }
}

private class BounceOverscrollFactory(
    private val scope: CoroutineScope,
    private val maxExtent: Float,
    private val density: Float
) : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = BounceOverscroll(scope, maxExtent, density)

    override fun equals(other: Any?): Boolean =
        other is BounceOverscrollFactory &&
            other.scope == scope && other.maxExtent == maxExtent && other.density == density

    override fun hashCode(): Int = (scope.hashCode() * 31 + maxExtent.hashCode()) * 31 + density.hashCode()
}

/** The app-wide overscroll: every scrollable bounces instead of stretching. */
@Composable
internal fun rememberBounceOverscrollFactory(): OverscrollFactory {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val extent = LocalConfiguration.current.screenHeightDp * density
    return remember(scope, extent, density) { BounceOverscrollFactory(scope, extent, density) }
}

/** A bounce effect whose offset the caller can read (Home's stretching painting). */
@Composable
fun rememberBounceOverscroll(): BounceOverscroll {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val extent = LocalConfiguration.current.screenHeightDp * density
    return remember(scope, extent, density) { BounceOverscroll(scope, extent, density) }
}

// ── Layout motion ────────────────────────────────────────────────────────────

/**
 * Glides a child to its new place in its parent instead of jumping there —
 * for rows whose children shift as content grows (the amount's digits, the
 * reordered top-spending tiles). A child placed for the first time appears
 * where it lands.
 */
fun Modifier.animatePlacement(
    spec: FiniteAnimationSpec<IntOffset> = BotMotion.snappy(0.34f, IntOffset.VisibilityThreshold)
): Modifier = composed {
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<IntOffset?>(null) }
    var placement by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    this
        .onPlaced { coordinates ->
            val position = coordinates.positionInParent().round()
            if (placement == null) placement = Animatable(position, IntOffset.VectorConverter)
            target = position
        }
        .offset {
            val anim = placement
            val goal = target
            if (anim == null || goal == null) {
                IntOffset.Zero
            } else {
                if (anim.targetValue != goal) scope.launch { anim.animateTo(goal, spec) }
                anim.value - goal
            }
        }
}

/**
 * A chart bar standing [fraction] of the height it's given. It grows from
 * nothing the first time it appears — [order] staggers a row of bars left
 * to right — and eases to new values after that. [minHeight] keeps an
 * empty bar visible.
 */
fun Modifier.growingBar(fraction: Float, order: Int = 0, minHeight: Dp = 0.dp): Modifier = composed {
    val reduced = LocalPerfMode.current.reducedMotion
    val grown = remember { Animatable(0f) }
    val appeared = remember { BooleanArray(1) }
    LaunchedEffect(fraction) {
        if (!appeared[0]) {
            appeared[0] = true
            if (!reduced) delay(order * 28L)
        }
        grown.animateTo(fraction.coerceIn(0f, 1f), BotMotion.smooth(0.55f))
    }
    layout { measurable, constraints ->
        if (!constraints.hasBoundedHeight) {
            val placeable = measurable.measure(constraints)
            return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        val floor = minHeight.roundToPx().coerceAtMost(constraints.maxHeight)
        val height = (constraints.maxHeight * grown.value).roundToInt().coerceIn(floor, constraints.maxHeight)
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        layout(placeable.width, height) { placeable.place(0, 0) }
    }
}

/** A colour that eases to its new value (selection states). */
@Composable
fun animatedColor(target: Color, label: String = "color"): Color =
    animateColorAsState(target, BotMotion.ColorShift, label = label).value

/**
 * Content that unfolds into place and folds away, pushing what's below it
 * smoothly instead of popping in (notes, errors, form sections).
 */
@Composable
fun Reveal(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(BotMotion.smooth(0.32f)) +
            expandVertically(BotMotion.smooth(0.38f, IntSize.VisibilityThreshold), expandFrom = Alignment.Top),
        exit = fadeOut(BotMotion.smooth(0.22f)) +
            shrinkVertically(BotMotion.smooth(0.32f, IntSize.VisibilityThreshold), shrinkTowards = Alignment.Top),
        content = content
    )
}

/** [ErrorBox] that unfolds when an error appears and keeps its words while it folds away. */
@Composable
fun AnimatedError(text: String?, modifier: Modifier = Modifier) {
    val current = text?.takeIf { it.isNotBlank() }
    val shown = rememberRetained(current)
    Reveal(visible = current != null) {
        ErrorBox(shown.orEmpty(), modifier)
    }
}

/** List rows fade in and out and glide when their neighbours come and go. */
fun LazyItemScope.itemMotion(): Modifier = Modifier.animateItem(
    fadeInSpec = BotMotion.smooth(0.34f),
    placementSpec = BotMotion.smooth(0.4f, IntOffset.VisibilityThreshold),
    fadeOutSpec = BotMotion.smooth(0.22f)
)
