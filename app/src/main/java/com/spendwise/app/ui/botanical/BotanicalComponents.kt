package com.spendwise.app.ui.botanical

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.spendwise.app.ui.theme.LocalPerfMode
import java.time.YearMonth
import kotlin.math.max

/** `cubic-bezier(0.2, 0, 0, 1)` — the prototype's control easing. */
val BotEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// ── Press feedback ───────────────────────────────────────────────────────────

/**
 * `button:active { transform: scale(.96) }` over 150ms. No ripple — the
 * prototype's controls answer with scale alone. Reduced motion keeps the tap
 * and drops the transform.
 */
fun Modifier.botPress(
    enabled: Boolean = true,
    scale: Float = 0.96f,
    role: Role? = Role.Button,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduced = LocalPerfMode.current.reducedMotion
    val current by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduced) scale else 1f,
        animationSpec = tween(150, easing = BotEase),
        label = "botPress"
    )
    this
        .graphicsLayer {
            scaleX = current
            scaleY = current
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

/**
 * Rows keep their size and tint instead (`.b-tx:active { background: #e4e9f1 }`),
 * so the reading target never moves under the finger.
 */
fun Modifier.botRowPress(
    pressedColor: Color = Color(0xFFE4E9F1),
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val tint by animateColorAsState(
        targetValue = if (pressed) pressedColor else pressedColor.copy(alpha = 0f),
        animationSpec = tween(120),
        label = "rowPress"
    )
    this
        .drawBehind { drawRect(tint) }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
}

@Composable
fun BotIcon(icon: ImageVector, size: Dp = 22.dp, tint: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = modifier.size(size))
}

// ── Money ────────────────────────────────────────────────────────────────────

/** Money type: Barlow Condensed with em tracking (it scales with the figure) and tabular digits. */
fun moneyStyle(size: Float, weight: FontWeight = FontWeight.Medium, letterSpacingPx: Float = 0f): TextStyle =
    display(size, weight).copy(
        letterSpacing = emSpacing(letterSpacingPx, size),
        fontFeatureSettings = TabularNums
    )

private fun moneyText(
    cents: Long,
    plusSign: Boolean,
    centsScale: Float,
    currencyWeight: FontWeight,
    short: Boolean
): AnnotatedString = buildAnnotatedString {
    append(
        when {
            cents < 0 -> "−"
            plusSign -> "+"
            else -> ""
        }
    )
    withStyle(SpanStyle(fontWeight = currencyWeight)) { append("RM ") }
    if (short) {
        append(shortAmount(cents))
    } else {
        val amount = formatAmount(cents)
        append(amount.substringBefore('.'))
        withStyle(SpanStyle(fontSize = centsScale.em)) { append("." + amount.substringAfter('.')) }
    }
}

/**
 * A money figure. It never wraps and is never cut off: when it is wider than
 * its space it shrinks uniformly (iOS minimumScaleFactor / the prototype's
 * MoneyFit), and where [abbreviate] allows, a figure that would shrink below
 * 60% switches to the short form ("RM 888.89M") first. Screen readers always
 * hear the exact amount.
 */
@Composable
fun Money(
    cents: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    plusSign: Boolean = false,
    centsScale: Float = 0.65f,
    currencyWeight: FontWeight = FontWeight.Light,
    abbreviate: Boolean = false,
    minScale: Float = 0.35f,
    align: Alignment.Horizontal = Alignment.Start
) {
    val full = remember(cents, plusSign, centsScale, currencyWeight) {
        moneyText(cents, plusSign, centsScale, currencyWeight, short = false)
    }
    val short = remember(cents, plusSign, abbreviate) {
        if (abbreviate) moneyText(cents, plusSign, 1f, currencyWeight, short = true) else null
    }
    val resolved = if (color == Color.Unspecified) style else style.copy(color = color)
    val spoken = formatRm(cents, plusSign)
    SubcomposeLayout(modifier.semantics { contentDescription = spoken }) { constraints ->
        fun line(slot: String, text: AnnotatedString, textStyle: TextStyle) =
            subcompose(slot) { Text(text = text, style = textStyle, maxLines = 1, softWrap = false) }
                .first()
                .measure(Constraints())

        var placeable = line("full", full, resolved)
        if (constraints.hasBoundedWidth && placeable.width > constraints.maxWidth && placeable.width > 0) {
            var text = full
            if (short != null && constraints.maxWidth.toFloat() / placeable.width < 0.6f) {
                text = short
                placeable = line("short", short, resolved)
            }
            val ratio = constraints.maxWidth.toFloat() / placeable.width
            if (ratio < 1f) {
                val scaled = resolved.copy(fontSize = resolved.fontSize * max(ratio * 0.98f, minScale))
                placeable = line("scaled", text, scaled)
            }
        }
        val width = placeable.width.coerceIn(constraints.minWidth, max(constraints.maxWidth, constraints.minWidth))
        val height = max(placeable.height, constraints.minHeight)
        layout(width, height) {
            val x = when (align) {
                Alignment.CenterHorizontally -> (width - placeable.width) / 2
                Alignment.End -> width - placeable.width
                else -> 0
            }
            placeable.place(x, (height - placeable.height) / 2)
        }
    }
}

/** Colour and sign for an entry amount on a white surface. */
@Composable
fun EntryMoney(
    cents: Long,
    kind: EntryKind,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    centsScale: Float = 0.8f,
    align: Alignment.Horizontal = Alignment.Start
) {
    Money(
        cents = if (kind == EntryKind.Expense) -cents else cents,
        style = style,
        modifier = modifier,
        color = when (kind) {
            EntryKind.Expense -> if (onDark) Bot.NegativeOnDark else Bot.Negative
            EntryKind.Income -> if (onDark) Bot.PositiveOnDark else Bot.Positive
            EntryKind.Transfer -> if (onDark) Color.White else Bot.Ink
        },
        plusSign = kind == EntryKind.Income,
        centsScale = centsScale,
        align = align
    )
}

enum class EntryKind(val label: String) { Expense("Expense"), Income("Income"), Transfer("Transfer") }

// ── Text atoms ───────────────────────────────────────────────────────────────

/** Uppercase eyebrow (`.b-kicker`). */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color = Bot.PageMuted) {
    Text(text.uppercase(), style = kickerStyle(color), modifier = modifier)
}

@Composable
fun Subtitle(text: String, modifier: Modifier = Modifier, color: Color = Bot.PageMuted) {
    Text(text, style = body(14f, lineHeight = 1.5f, color = color), modifier = modifier)
}

/** `.b-note`: quiet footnote on dark pages. */
@Composable
fun Note(text: String, modifier: Modifier = Modifier, color: Color = Bot.PageMuted) {
    Text(text, style = body(14f, lineHeight = 1.5f, color = color), modifier = modifier)
}

/** `.b-support`: secondary line inside white surfaces. */
@Composable
fun Support(text: String, modifier: Modifier = Modifier, color: Color = Bot.SurfaceMuted) {
    Text(text, style = body(14f, lineHeight = 1.5f, color = color), modifier = modifier)
}

// ── Buttons ──────────────────────────────────────────────────────────────────

enum class IconButtonTone { OnDark, OnLight, Plain }

/** 44–46dp circular icon button (`.b-icon`). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tone: IconButtonTone = IconButtonTone.OnDark,
    enabled: Boolean = true,
    tint: Color? = null
) {
    val (bg, border, ink) = when (tone) {
        IconButtonTone.OnDark -> Triple(Color(0x18FFFFFF), Color(0x40FFFFFF), Color.White)
        IconButtonTone.OnLight -> Triple(Color(0xFFF0F1F3), Color(0xFFE4E6E9), Bot.Ink)
        IconButtonTone.Plain -> Triple(Color.Transparent, Color.Transparent, Color.White)
    }
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .botPress(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        BotIcon(icon, size = iconSize, tint = tint ?: ink)
    }
}

enum class ButtonType { Primary, Secondary, Danger }

/**
 * `.b-primary` / `.b-secondary` / `.b-danger`: 99px pills, 48dp tall. The
 * default label is Barlow Condensed 22; flows that sit next to form text
 * (Entry, Filters) pass [dmSans].
 */
@Composable
fun BotButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ButtonType = ButtonType.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    dmSans: Boolean = false,
    color: Color? = null
) {
    val (bg, ink) = when (type) {
        ButtonType.Primary -> (color ?: Bot.Blue) to Color.White
        ButtonType.Secondary -> Bot.SecondaryBg to Bot.SecondaryInk
        ButtonType.Danger -> Bot.DangerBg to Bot.DangerInk
    }
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .botPress(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            BotIcon(icon, size = 18.dp, tint = ink)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = if (dmSans) body(16f, FontWeight.Medium, lineHeight = 1.3f) else display(22f, FontWeight.Medium),
            color = ink,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/** `.b-text-button`: bare Barlow label, 44dp touch height. */
@Composable
fun TextLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF737889),
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .botPress(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = display(19f), color = color)
    }
}

/** Section-head action ("View insights", "Budgets"): quiet tinted pill. */
@Composable
fun ChipButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Bot.ChipBg)
            .botPress(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = body(13f, FontWeight.Medium, lineHeight = 1.4f), color = Bot.ChipInk)
    }
}

// ── Surfaces ─────────────────────────────────────────────────────────────────

/** White data card (`.b-panel`): 28dp radius, 20dp padding. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Bot.Surface)
            .padding(padding)
    ) { content() }
}

/** Card title with an optional trailing action (`.b-section-head`). */
@Composable
fun SectionHead(
    title: String,
    modifier: Modifier = Modifier,
    titleSize: Float = 27f,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            style = display(titleSize, FontWeight.Medium, letterSpacing = -0.5f, lineHeight = 1.15f),
            color = Bot.SurfaceInk,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke(this)
    }
}

/** Budget / category share bar (`.b-track`). */
@Composable
fun Track(ratio: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 5.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(Bot.Track)
    ) {
        if (ratio > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .height(height)
                    .clip(RoundedCornerShape(99.dp))
                    .background(color)
            )
        }
    }
}

/** Illustrated category/account disc on a quiet rounded tile. */
@Composable
fun ArtTile(
    @DrawableRes art: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    imageSize: Dp = 46.dp,
    background: Color = Bot.TileBg,
    radius: Dp = 17.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (art != null) {
            Image(painterResource(art), contentDescription = null, modifier = Modifier.size(imageSize))
        } else {
            BotIcon(BotIcons.Transfer, size = 24.dp, tint = Bot.Ink)
        }
    }
}

@Composable
fun ArtImage(@DrawableRes art: Int, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painterResource(art),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

/**
 * One ledger line (`.b-tx`): illustrated tile, name + context, signed amount.
 * The amount may take up to 62% of the row; the name keeps at least 88dp and
 * truncates first — exact amounts are never shortened here.
 */
@Composable
fun TransactionRow(
    title: String,
    meta: String,
    cents: Long,
    kind: EntryKind,
    @DrawableRes art: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
    showDivider: Boolean = false
) {
    Column(modifier.fillMaxWidth().background(Bot.Surface)) {
        BoxWithConstraints(Modifier.fillMaxWidth().botRowPress(onClick = onClick)) {
            val maxAmount = maxWidth * 0.62f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                ArtTile(art)
                Column(Modifier.weight(1f).widthIn(min = 88.dp)) {
                    Text(
                        title,
                        style = display(23f, lineHeight = 1.2f),
                        color = Bot.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        meta,
                        style = body(13f, lineHeight = 1.5f),
                        color = Bot.Caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                EntryMoney(
                    cents = cents,
                    kind = kind,
                    style = moneyStyle(25f, letterSpacingPx = -0.8f),
                    modifier = Modifier.widthIn(max = maxAmount)
                )
            }
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Bot.Rule))
        }
    }
}

/**
 * Equal-width cells that all take the tallest cell's height, like a CSS grid
 * row. Measures twice via subcomposition because the cells hold [Money],
 * which can't answer intrinsic-size queries.
 */
@Composable
fun EqualHeightRow(
    cells: List<@Composable () -> Unit>,
    columns: Int,
    gap: Dp,
    modifier: Modifier = Modifier
) {
    SubcomposeLayout(modifier.fillMaxWidth()) { constraints ->
        val gapPx = gap.roundToPx()
        val cellWidth = ((constraints.maxWidth - gapPx * (columns - 1)) / columns).coerceAtLeast(0)
        val natural = cells.mapIndexed { index, cell ->
            subcompose("measure$index", cell).first().measure(Constraints(minWidth = cellWidth, maxWidth = cellWidth))
        }
        val height = natural.maxOfOrNull { it.height } ?: 0
        val placeables = cells.mapIndexed { index, cell ->
            subcompose("place$index", cell).first().measure(Constraints.fixed(cellWidth, height))
        }
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable -> placeable.place(index * (cellWidth + gapPx), 0) }
        }
    }
}

// ── Header controls ──────────────────────────────────────────────────────────

/**
 * Month control (`.b-period`): previous, the month pill that opens the month
 * picker, next. Months after the current one are unreachable — the ledger
 * has no future entries to show.
 */
@Composable
fun PeriodSelector(
    month: YearMonth,
    onChange: (YearMonth) -> Unit,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val current = YearMonth.from(todayKl())
    Row(
        modifier = modifier.offset(x = (-14).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        PeriodArrow(BotIcons.ChevronLeft, "Previous month", enabled = true) { onChange(month.minusMonths(1)) }
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0x33D9D9D9))
                .botPress(onClick = onOpenPicker)
                .semantics { contentDescription = "${monthLabel(month)}, choose month" }
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                monthLabel(month),
                style = body(15f, FontWeight.Medium),
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
            BotIcon(BotIcons.ChevronDown, size = 18.dp, tint = Color.White)
        }
        PeriodArrow(BotIcons.ChevronRight, "Next month", enabled = month < current) {
            if (month < current) onChange(month.plusMonths(1))
        }
    }
}

@Composable
private fun PeriodArrow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .botPress(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        BotIcon(icon, size = 18.dp, tint = Color.White)
    }
}

// ── Segments ─────────────────────────────────────────────────────────────────

enum class SegmentTone { OnDark, OnLight, Entry }

/** Pill segmented control (`.b-segment`). */
@Composable
fun <T> PillSegment(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    tone: SegmentTone = SegmentTone.OnDark,
    isEnabled: (T) -> Boolean = { true }
) {
    val containerBg: Color
    val containerBorder: Color
    when (tone) {
        SegmentTone.OnDark -> { containerBg = Color(0x15FFFFFF); containerBorder = Color(0x35FFFFFF) }
        SegmentTone.OnLight -> { containerBg = Color(0xFFEDF0F4); containerBorder = Color(0xFFE1E5ED) }
        SegmentTone.Entry -> { containerBg = Color(0x80FFFFFF); containerBorder = Color(0x90FFFFFF) }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(containerBg)
            .border(1.dp, containerBorder, RoundedCornerShape(99.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val enabled = isEnabled(option)
            val (bg, ink, border) = when (tone) {
                SegmentTone.OnDark ->
                    if (isSelected) Triple(Color.White, Bot.Navy, Color.Transparent)
                    else Triple(Color.Transparent, Color.White, Color.Transparent)
                SegmentTone.OnLight ->
                    if (isSelected) Triple(Color.White, Bot.Navy, Color.Transparent)
                    else Triple(Color.Transparent, Bot.Ink, Color.Transparent)
                SegmentTone.Entry ->
                    if (isSelected) Triple(Bot.ActionSolid, Color.White, Bot.ActionSolid)
                    else Triple(Color(0x40FFFFFF), Color(0xFF24344F), Color(0x50FFFFFF))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = if (tone == SegmentTone.Entry) 44.dp else 42.dp)
                    .alpha(if (enabled) 1f else 0.4f)
                    .then(
                        if (isSelected && tone != SegmentTone.Entry) {
                            Modifier.shadow(3.dp, RoundedCornerShape(99.dp), clip = false, ambientColor = Color(0x11000000), spotColor = Color(0x11000000))
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(99.dp))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(99.dp))
                    .botPress(enabled = enabled && !isSelected, role = Role.Tab) { onSelect(option) }
                    .semantics { this.selected = isSelected }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option),
                    style = body(14f, FontWeight.Medium, lineHeight = 1.4f),
                    color = ink,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Data tabs (Activity type, Insights scope, By category): a rule with the
 * selected tab underlined, so they read as views of the data rather than
 * as more buttons.
 */
@Composable
fun <T> TabSegment(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    inPanel: Boolean = false,
    fontSize: Float = 14f
) {
    val rule = if (inPanel) Color(0x38101C3A) else Color(0x38FFFFFF)
    val accent = if (inPanel) Bot.Pink else Bot.Orchid
    val activeInk = if (inPanel) Bot.SurfaceInk else Color.White
    val quietInk = if (inPanel) Bot.SurfaceMuted else Bot.PageMuted
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRect(rule, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - stroke), size = androidx.compose.ui.geometry.Size(size.width, stroke))
            }
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab
                    ) { onSelect(option) }
                    .semantics { this.selected = isSelected }
                    .drawBehind {
                        if (isSelected) {
                            val stroke = 2.dp.toPx()
                            drawRect(accent, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - stroke), size = androidx.compose.ui.geometry.Size(size.width, stroke))
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option),
                    style = body(fontSize, if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (isSelected) activeInk else quietInk,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

// ── Toggle ───────────────────────────────────────────────────────────────────

/** 44×27 switch (`.b-toggle`); [offColor] differs on navy pages and white cards. */
@Composable
fun BotToggle(
    checked: Boolean,
    onToggle: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    offColor: Color = Color(0x28FFFFFF),
    enabled: Boolean = true
) {
    val reduced = LocalPerfMode.current.reducedMotion
    val shift by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(if (reduced) 0 else 150, easing = BotEase),
        label = "toggle"
    )
    val track by animateColorAsState(
        targetValue = if (checked) Bot.Blue else offColor,
        animationSpec = tween(if (reduced) 0 else 150),
        label = "toggleTrack"
    )
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 27.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(99.dp))
            .background(track)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onClick = onToggle
            )
            .semantics {
                contentDescription = label
                stateDescription = if (checked) "On" else "Off"
            }
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .graphicsLayer { translationX = with(density) { 17.dp.toPx() } * shift }
                .size(21.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// ── Empty state ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    onDark: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (onDark) Color(0x17FFFFFF) else Color(0xFFF0F2F6)),
            contentAlignment = Alignment.Center
        ) {
            BotIcon(BotIcons.Wallet, size = 32.dp, tint = if (onDark) Color.White else Bot.Navy)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = display(29f, lineHeight = 1.15f),
            color = if (onDark) Color.White else Bot.Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            body,
            style = body(12f, lineHeight = 1.7f),
            color = Color(0xFF8D97AC),
            textAlign = TextAlign.Center
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            BotButton(action, onClick = onAction, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ── Form parts ───────────────────────────────────────────────────────────────

enum class FieldTone { Sheet, Entry }

@Composable
fun FieldLabel(text: String, tone: FieldTone = FieldTone.Sheet, modifier: Modifier = Modifier) {
    Text(
        text,
        style = if (tone == FieldTone.Entry) body(13f, FontWeight.SemiBold, lineHeight = 1.4f)
        else body(13f, FontWeight.Medium, lineHeight = 1.4f),
        color = if (tone == FieldTone.Entry) Color(0xFF0B1740) else Bot.FieldLabel,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

/**
 * Text input. Sheet fields are soft grey boxes in DM Sans; Entry fields are
 * white pills set in Barlow Condensed so what you type reads like the rest
 * of the ledger.
 */
@Composable
fun BotTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    tone: FieldTone = FieldTone.Sheet,
    singleLine: Boolean = true,
    minHeight: Dp = if (tone == FieldTone.Entry) 48.dp else 49.dp,
    maxLength: Int = Int.MAX_VALUE,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    focusRequester: FocusRequester? = null,
    onFocusChange: (Boolean) -> Unit = {},
    contentLabel: String? = null
) {
    val entry = tone == FieldTone.Entry
    val textStyle = if (entry) display(24f, lineHeight = 1.15f, color = Bot.FieldInk)
    else body(14f, lineHeight = 1.4f, color = Bot.FieldInk)
    val shape = RoundedCornerShape(if (entry) 24.dp else 17.dp)
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        singleLine = singleLine,
        textStyle = textStyle,
        cursorBrush = SolidColor(Bot.ActionSolid),
        keyboardOptions = keyboardOptions,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .semantics {
                if (contentLabel != null) contentDescription = contentLabel
                if (isError) error("Check this field")
            },
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(shape)
                    .background(if (entry) Color.White else Bot.FieldBg)
                    .border(
                        BorderStroke(1.dp, if (isError) Color(0xFFD9788C) else if (entry) Color.Transparent else Bot.FieldBorder),
                        shape
                    )
                    .padding(
                        horizontal = if (entry) 19.dp else 14.dp,
                        vertical = if (entry) 7.dp else 12.dp
                    ),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        placeholder,
                        style = textStyle,
                        color = if (entry) Color(0xFF897A7A) else Color(0xFF8D94A4),
                        maxLines = if (singleLine) 1 else Int.MAX_VALUE
                    )
                }
                inner()
            }
        }
    )
}

/** Dropdown styled as a sheet field (`<select>` in the prototype). */
@Composable
fun <T> SelectField(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose",
    isEnabled: (T) -> Boolean = { true },
    isError: Boolean = false,
    contentLabel: String
) {
    var open by remember { mutableStateOf(false) }
    // The menu matches the field's width, like the native picker it stands in for.
    var fieldWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { fieldWidth = it.width }
                .heightIn(min = 49.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Bot.FieldBg)
                .border(1.dp, if (isError) Color(0xFFD9788C) else Bot.FieldBorder, RoundedCornerShape(17.dp))
                .clickable(role = Role.DropdownList) { open = true }
                .semantics { contentDescription = "$contentLabel: ${selected?.let(label) ?: placeholder}" }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                selected?.let(label) ?: placeholder,
                style = body(14f, lineHeight = 1.4f),
                color = if (selected == null) Color(0xFF8D94A4) else Bot.FieldInk,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BotIcon(BotIcons.ChevronDown, size = 18.dp, tint = Bot.FieldInk)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(with(density) { fieldWidth.toDp() })
        ) {
            options.forEach { option ->
                val enabled = isEnabled(option)
                DropdownMenuItem(
                    text = {
                        Text(
                            label(option),
                            style = body(14f, if (option == selected) FontWeight.SemiBold else FontWeight.Normal),
                            color = if (enabled) Bot.FieldInk else Bot.FieldInk.copy(alpha = 0.4f)
                        )
                    },
                    enabled = enabled,
                    onClick = {
                        onSelect(option)
                        open = false
                    }
                )
            }
        }
    }
}

/** `.b-error`: soft rose box with the fix in plain words. */
@Composable
fun ErrorBox(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = body(12f, lineHeight = 1.5f),
        color = Color(0xFFA12D46),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Bot.DangerBg)
            .border(1.dp, Color(0xFFEFC3CD), RoundedCornerShape(15.dp))
            .padding(12.dp)
            .semantics { error(text) }
    )
}

/** `.b-inline-note`: explanatory aside inside a form. */
@Composable
fun InlineNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = body(12f, lineHeight = 1.6f),
        color = Color(0xFF5F6F8D),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEEF2F9))
            .padding(horizontal = 15.dp, vertical = 13.dp)
    )
}

@Composable
fun FormCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = body(13f, FontWeight.Medium, lineHeight = 1.4f),
        color = Bot.FieldLabel,
        modifier = modifier
    )
}

/**
 * Fixed-sen amount (`AmountInput`): digits fill from the right — typing 1, 2,
 * 3 reads 0.01, 0.12, 1.23 — and the decimal is never typed. The figure
 * shrinks as it grows once it no longer fits, then returns to full size.
 * Capped at RM 999,999,999.99 (11 digits), as the prototype.
 */
@Composable
fun AmountInput(
    cents: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    entry: Boolean = false,
    label: String = "Amount",
    error: String? = null,
    focusRequester: FocusRequester? = null
) {
    val display = formatAmount(cents)
    var field by remember { mutableStateOf(TextFieldValue(display, TextRange(display.length))) }
    if (field.text != display) field = TextFieldValue(display, TextRange(display.length))

    val baseSize = if (entry) 64f else 56f
    val weight = if (entry) FontWeight.Normal else FontWeight.Medium
    val color = when {
        entry -> Color.White
        cents == 0L -> Color(0xFF8D94A4)
        else -> Color(0xFF101626)
    }
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(if (entry) Modifier.height(112.dp) else Modifier.padding(vertical = 20.dp)),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val available = with(density) { maxWidth.toPx() }
        val numberStyle = moneyStyle(baseSize, weight, letterSpacingPx = if (entry) -1.5f else -2f)
        val prefixStyle = numberStyle.copy(fontWeight = FontWeight.Light)
        val natural = remember(display, numberStyle) {
            measurer.measure(display, numberStyle).size.width +
                measurer.measure("RM", prefixStyle).size.width +
                with(density) { 8.dp.toPx() }
        }
        val scale = if (natural > available && natural > 0) max(available / natural * 0.98f, 0.4f) else 1f
        val sized = numberStyle.copy(fontSize = numberStyle.fontSize * scale, color = color)
        val prefixSized = prefixStyle.copy(fontSize = prefixStyle.fontSize * scale, color = color)
        val textWidth = remember(display, sized) {
            with(density) { (measurer.measure(display, sized).size.width + 4).toDp() }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp * scale)) {
            Text("RM", style = prefixSized, maxLines = 1, softWrap = false)
            BasicTextField(
                value = field,
                onValueChange = { next ->
                    val digits = next.text.filter { it.isDigit() }.trimStart('0').take(11)
                    val newCents = digits.toLongOrNull() ?: 0L
                    val shown = formatAmount(newCents)
                    field = TextFieldValue(shown, TextRange(shown.length))
                    if (newCents != cents) onChange(newCents)
                },
                singleLine = true,
                textStyle = sized,
                cursorBrush = SolidColor(if (entry) Color.White else Bot.ActionSolid),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(textWidth)
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .onFocusChanged {
                        if (it.isFocused) field = field.copy(selection = TextRange(field.text.length))
                    }
                    .semantics {
                        contentDescription = "$label, RM $display"
                        if (error != null) error(error)
                    }
            )
        }
    }
}
