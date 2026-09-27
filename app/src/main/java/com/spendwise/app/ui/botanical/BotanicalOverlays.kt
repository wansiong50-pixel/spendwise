package com.spendwise.app.ui.botanical

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.app.ui.theme.LocalPerfMode
import kotlinx.coroutines.delay

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

/**
 * White bottom sheet over a navy scrim (`.b-overlay` + `.b-sheet`): handle,
 * Barlow title with a close button, then content. Enters over 240ms, leaves
 * over 150ms with a 12dp drop — quick enough that the next task isn't held
 * up. Back, the scrim and the close button all dismiss.
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
    val reduced = LocalPerfMode.current.reducedMotion
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(if (reduced) 0 else 240)),
        exit = fadeOut(tween(if (reduced) 0 else 150))
    ) {
        BackHandler(enabled = visible, onBack = onDismiss)
        val density = LocalDensity.current
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Bot.Scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .imePadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottomPad = maxOf(28.dp, navBottom)
            Column(
                modifier = modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * maxHeightFraction)
                    .animateEnterExit(
                        enter = slideInVertically(tween(if (reduced) 0 else 240)) {
                            with(density) { 24.dp.roundToPx() }
                        },
                        exit = slideOutVertically(tween(if (reduced) 0 else 150)) {
                            with(density) { 12.dp.roundToPx() }
                        }
                    )
                    .shadow(24.dp, SheetShape, clip = false, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
                    .clip(SheetShape)
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
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
    val reduced = LocalPerfMode.current.reducedMotion
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = state.message != null,
            enter = fadeIn(tween(if (reduced) 0 else 200)) +
                slideInVertically(tween(if (reduced) 0 else 200)) { it / 3 },
            exit = fadeOut(tween(if (reduced) 0 else 150)) +
                slideOutVertically(tween(if (reduced) 0 else 150)) { it / 3 },
            modifier = Modifier.padding(bottom = bottomOffset, start = 20.dp, end = 20.dp)
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
