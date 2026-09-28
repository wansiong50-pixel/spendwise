package com.spendwise.app.ui.botanical

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Holds the last non-null [value] so an overlay keeps its content while it
 * animates out after the model has already been cleared.
 */
@Composable
fun <T : Any> rememberRetained(value: T?): T? {
    val holder = remember { mutableStateOf(value) }
    if (value != null && holder.value != value) holder.value = value
    return value ?: holder.value
}

private val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/** A fling faster than this dismisses a card whatever its position (dp/s). */
private const val DismissVelocity = 1000f

/** A card lowered past this share of its height dismisses when let go. */
private const val DismissFraction = 0.3f

/**
 * Drags a presented card. [shown] is its presentation: 1 at rest, 0 fully
 * below the screen. The finger moves the card 1:1 downward; upward it meets
 * UIScrollView's rubber band. On release the card either settles back or
 * keeps going and dismisses, handing the finger's speed to the spring. Also
 * a nested-scroll parent, so content scrolled to its top passes a downward
 * pull to the card, and pushing back up lifts the card before the content
 * scrolls again.
 */
private class CardDrag(
    private val shown: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
    private val density: Density
) {
    var height = 0f
    var onDismiss: () -> Unit = {}
    var isVisible: () -> Boolean = { true }

    /** Finger travel below the resting position, before any rubber band. */
    private var travel = 0f
    private var dragging = false

    fun start() {
        dragging = true
        val px = (1f - shown.value) * height
        travel = if (px >= 0f) px else -rubberBandInverse(-px, height)
    }

    fun by(delta: Float) {
        if (height <= 0f) return
        if (!dragging) start()
        travel += delta
        val px = if (travel >= 0f) travel else -rubberBand(-travel, height)
        val target = 1f - px / height
        scope.launch { shown.snapTo(target) }
    }

    fun release(velocity: Float) {
        if (!dragging) return
        dragging = false
        if (height <= 0f) return
        val lowered = 1f - shown.value
        val flung = velocity > DismissVelocity * density.density
        val far = lowered > DismissFraction && velocity > -DismissVelocity * 0.4f * density.density
        if (flung || far) {
            scope.launch {
                shown.animateTo(0f, BotMotion.SheetClose, initialVelocity = -velocity / height)
                if (isVisible()) shown.animateTo(1f, BotMotion.SheetOpen)
            }
            onDismiss()
        } else {
            scope.launch { shown.animateTo(1f, BotMotion.SheetOpen, initialVelocity = -velocity / height) }
        }
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && dragging && available.y < 0f && travel > 0f) {
                val used = max(available.y, -travel)
                by(used)
                return Offset(0f, used)
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && available.y > 0f) {
                by(available.y)
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!dragging) return Velocity.Zero
            release(available.y)
            return available
        }
    }
}

/**
 * Presents a card from the bottom of the screen the way iOS sheets move: it
 * rises from below the screen on a critically damped spring while the scrim
 * fades up with it, can be dragged down and flicked away (see [CardDrag]),
 * and follows a predictive back gesture down a little before dismissing.
 * Back, the scrim and a fast or far drag all call [onDismiss]; the card
 * stays composed until it has left the screen. [card] receives the modifier
 * that moves and drags it.
 */
@Composable
internal fun BottomCardHost(
    visible: Boolean,
    onDismiss: () -> Unit,
    scrim: Color,
    card: @Composable BoxWithConstraintsScope.(Modifier) -> Unit
) {
    val shown = remember { Animatable(0f) }
    var present by remember { mutableStateOf(visible) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(visible) {
        if (visible) {
            // The card takes over from any field being typed in beneath it: the
            // keyboard goes, so it can't crowd the card (a calendar's month
            // arrows) or keep typing into a field the card now hides.
            focusManager.clearFocus()
            present = true
            shown.animateTo(1f, BotMotion.SheetOpen)
        } else if (present) {
            shown.animateTo(0f, BotMotion.SheetClose)
            present = false
        }
    }
    if (!present) return

    val latestVisible by rememberUpdatedState(visible)
    val latestDismiss by rememberUpdatedState(onDismiss)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val drag = remember(shown, scope, density) { CardDrag(shown, scope, density) }
    drag.onDismiss = { latestDismiss() }
    drag.isVisible = { latestVisible }

    PredictiveBackHandler(enabled = visible) { progress ->
        try {
            progress.collect { event -> shown.snapTo(1f - 0.12f * event.progress) }
            latestDismiss()
        } catch (cancelled: CancellationException) {
            scope.launch { shown.animateTo(1f, BotMotion.SheetOpen) }
            throw cancelled
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(scrim, alpha = shown.value.coerceIn(0f, 1f)) }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { latestDismiss() }
            )
            .imePadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val cardModifier = Modifier
            .onSizeChanged { drag.height = it.height.toFloat() }
            .graphicsLayer { translationY = (1f - shown.value) * size.height }
            .draggable(
                state = rememberDraggableState { delta -> drag.by(delta) },
                orientation = Orientation.Vertical,
                onDragStarted = { drag.start() },
                onDragStopped = { velocity -> drag.release(velocity) }
            )
            .nestedScroll(drag.connection)
        card(cardModifier)
    }
}

/**
 * White bottom sheet over a navy scrim (`.b-overlay` + `.b-sheet`): handle,
 * Barlow title with a close button, then content. It rises and leaves like
 * an iOS sheet ([BottomCardHost]) — drag it down or flick it away — and
 * grows or shrinks smoothly when its content changes. Back, the scrim and
 * the close button all dismiss.
 */
@Composable
fun BotSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    maxHeightFraction: Float = 0.92f,
    horizontalPadding: Dp = 24.dp,
    scrollable: Boolean = true,
    header: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BottomCardHost(visible = visible, onDismiss = onDismiss, scrim = Bot.Scrim) { cardModifier ->
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomPad = maxOf(28.dp, navBottom)
        Column(
            modifier = modifier
                .then(cardModifier)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .heightIn(max = maxHeight * maxHeightFraction)
                .shadow(24.dp, SheetShape, clip = false, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
                .clip(SheetShape)
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .animateContentSize(BotMotion.Resize)
                .semantics { paneTitle = title }
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFFD9DCE3))
                )
            }
            if (header) {
                SheetHeader(title, onDismiss, Modifier.padding(horizontal = horizontalPadding))
            }
            val body = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = horizontalPadding)
                .padding(bottom = if (footer == null) bottomPad else 16.dp)
            Column(body) { content() }
            if (footer != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Bot.Rule))
                    Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = maxOf(20.dp, navBottom))) {
                        footer()
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            style = display(32f, lineHeight = 1.1f),
            color = Bot.Ink,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        CircleIconButton(
            icon = BotIcons.Close,
            contentDescription = "Close",
            onClick = onClose,
            size = 46.dp,
            tone = IconButtonTone.OnLight
        )
    }
}

/** Sheet body copy (`.b-sheet > p`). */
@Composable
fun SheetText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = body(13f, lineHeight = 1.7f),
        color = Bot.SheetMuted,
        modifier = modifier.padding(bottom = 15.dp)
    )
}

/** Two buttons side by side (`.b-button-row`). */
@Composable
fun ButtonRow(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    // Intrinsic height so a label that wraps (e.g. "Move & delete category")
    // grows both buttons together instead of leaving them uneven.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

/** A destructive or replacing action, confirmed in place. */
data class ConfirmRequest(
    val title: String,
    val body: String,
    val action: String,
    val destructive: Boolean = true,
    val cancel: String = "Cancel",
    val onConfirm: () -> Unit
)

@Composable
fun ConfirmSheet(request: ConfirmRequest?, onDismiss: () -> Unit) {
    val shown = rememberRetained(request)
    BotSheet(visible = request != null, onDismiss = onDismiss, title = shown?.title.orEmpty()) {
        if (shown != null) {
            SheetText(shown.body)
            ButtonRow {
                BotButton(shown.cancel, onClick = onDismiss, type = ButtonType.Secondary, modifier = Modifier.weight(1f).fillMaxHeight())
                BotButton(
                    shown.action,
                    onClick = {
                        onDismiss()
                        shown.onConfirm()
                    },
                    type = if (shown.destructive) ButtonType.Danger else ButtonType.Primary,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

// ── Toast ────────────────────────────────────────────────────────────────────

/** Short status line above the navigation (`.b-toast`), 3.2s. */
@Stable
class BotToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var serial by mutableStateOf(0)
        private set

    fun show(text: String) {
        message = text
        serial++
    }

    fun clear() {
        message = null
    }
}

val LocalToast = staticCompositionLocalOf { BotToastState() }

@Composable
fun BotToastHost(state: BotToastState, bottomOffset: Dp, modifier: Modifier = Modifier) {
    LaunchedEffect(state.serial) {
        if (state.message != null) {
            delay(3200)
            state.clear()
        }
    }
    val shown = rememberRetained(state.message)
    val offset by animateDpAsState(bottomOffset, BotMotion.smooth(0.4f, Dp.VisibilityThreshold), label = "toastOffset")
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // Rises in with a little spring and a slight grow, like an iOS banner; sinks away.
        AnimatedVisibility(
            visible = state.message != null,
            enter = fadeIn(BotMotion.smooth(0.24f)) +
                slideInVertically(spring(dampingRatio = 0.74f, stiffness = stiffnessFor(0.42f), visibilityThreshold = IntOffset.VisibilityThreshold)) { it } +
                scaleIn(BotMotion.snappy(0.4f), initialScale = 0.9f),
            exit = fadeOut(BotMotion.smooth(0.22f)) +
                slideOutVertically(BotMotion.smooth(0.32f, IntOffset.VisibilityThreshold)) { it / 2 } +
                scaleOut(BotMotion.smooth(0.3f), targetScale = 0.94f),
            modifier = Modifier.padding(bottom = offset, start = 20.dp, end = 20.dp)
        ) {
            Text(
                shown.orEmpty(),
                style = body(12f, FontWeight.Medium, lineHeight = 1.4f),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(99.dp), ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFF101626))
                    .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(99.dp))
                    .padding(horizontal = 22.dp, vertical = 13.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@Composable
fun VerticalGap(height: Dp) = Spacer(Modifier.height(height))
