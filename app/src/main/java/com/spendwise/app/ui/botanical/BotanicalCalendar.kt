package com.spendwise.app.ui.botanical

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.spendwise.app.R
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarMode { Date, Month }

private val Glass = Color(0x94122A69)
private val GlassEdge = Color(0x47FFFFFF)
private val CardShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
private val CardColor = Color(0xFF102B85)

/**
 * The first date of each of [month]'s calendar cells, Sunday first; null for
 * the blank lead-in and tail. Always whole weeks, so six-week months (31 days
 * starting on a Friday or Saturday) get their sixth row.
 */
fun calendarCells(month: YearMonth): List<LocalDate?> {
    val offset = month.atDay(1).dayOfWeek.value % 7
    val days = month.lengthOfMonth()
    val count = (offset + days + 6) / 7 * 7
    return List(count) { index ->
        val day = index - offset + 1
        if (day in 1..days) month.atDay(day) else null
    }
}

/**
 * Moves the staged date to [target]'s month, keeping its day where the month
 * allows (Jan 31 → Feb 28) and never past [maxDate].
 */
fun clampToMonth(selected: LocalDate, target: YearMonth, maxDate: LocalDate?): LocalDate {
    val maxMonth = maxDate?.let(YearMonth::from)
    val month = if (maxMonth != null && target > maxMonth) maxMonth else target
    var day = minOf(selected.dayOfMonth, month.lengthOfMonth())
    if (maxDate != null && month == maxMonth) day = minOf(day, maxDate.dayOfMonth)
    return month.atDay(day)
}

/**
 * The illustrated calendar card (Figma "Calender" / "Calender with Month
 * Picker"). Changes are staged until ✓; ✕, Back, the scrim and a downward
 * flick discard them. It rises like a sheet ([BottomCardHost]); months page
 * sideways under the finger. [CalendarMode.Month] shows only month/year
 * choices — no day pretends to filter anything — and confirms the first of
 * the chosen month.
 */
@Composable
fun CalendarPicker(
    visible: Boolean,
    mode: CalendarMode,
    initial: LocalDate,
    onCancel: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    maxDate: LocalDate? = todayKl()
) {
    BottomCardHost(visible = visible, onDismiss = onCancel, scrim = Color(0x33000000), fill = CardColor) { cardModifier ->
        val bounded = if (maxDate != null && initial > maxDate) maxDate else initial
        var selected by remember(initial) { mutableStateOf(bounded) }
        var visibleMonth by remember(initial) { mutableStateOf(YearMonth.from(bounded)) }
        var monthsOpen by remember(initial, mode) { mutableStateOf(mode == CalendarMode.Month) }
        val maxMonth = maxDate?.let(YearMonth::from)
        val lastYear = maxMonth?.year ?: (todayKl().year + 10)
        val yearRange = (lastYear - 110)..lastYear
        val changeMonth: (YearMonth) -> Unit = { target ->
            selected = clampToMonth(selected, target, maxDate)
            visibleMonth = YearMonth.from(selected)
        }
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = Modifier
                .then(cardModifier)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.94f)
                .shadow(24.dp, CardShape, clip = false)
                .clip(CardShape)
                .background(CardColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .animateContentSize(BotMotion.Resize)
                .semantics { paneTitle = if (mode == CalendarMode.Month) "Choose month" else "Choose entry date" }
        ) {
            Image(
                painter = painterResource(R.drawable.calendar_art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 22.dp + navBottom)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    GlassCircle(BotIcons.Close, "Cancel", size = 48, onClick = onCancel)
                    GlassCircle(BotIcons.Check, "Confirm", size = 48) {
                        onConfirm(if (mode == CalendarMode.Month) visibleMonth.atDay(1) else selected)
                    }
                }
                Spacer(Modifier.height(24.dp))
                // Days and months swap with a gentle zoom, the card easing to the new height.
                AnimatedContent(
                    targetState = monthsOpen,
                    transitionSpec = {
                        (fadeIn(BotMotion.smooth(0.3f)) + scaleIn(BotMotion.smooth(0.38f), initialScale = 0.94f))
                            .togetherWith(fadeOut(BotMotion.smooth(0.2f)) + scaleOut(BotMotion.smooth(0.3f), targetScale = 1.04f))
                            .using(SizeTransform(clip = false) { _, _ -> BotMotion.Resize })
                    },
                    label = "calendarView"
                ) { months ->
                    Column(Modifier.fillMaxWidth()) {
                        if (!months) {
                            DateView(
                                visibleMonth = visibleMonth,
                                selected = selected,
                                maxDate = maxDate,
                                maxMonth = maxMonth,
                                firstMonth = YearMonth.of(yearRange.first, 1),
                                lastMonth = maxMonth ?: YearMonth.of(yearRange.last, 12),
                                onOpenMonths = { monthsOpen = true },
                                onSelect = { selected = it },
                                onChangeMonth = changeMonth
                            )
                        } else {
                            MonthView(
                                mode = mode,
                                visibleMonth = visibleMonth,
                                maxMonth = maxMonth,
                                yearRange = yearRange,
                                onBack = { monthsOpen = false },
                                onPick = { month ->
                                    changeMonth(month)
                                    if (mode == CalendarMode.Date) monthsOpen = false
                                },
                                onYear = { year -> changeMonth(YearMonth.of(year, visibleMonth.monthValue)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateView(
    visibleMonth: YearMonth,
    selected: LocalDate,
    maxDate: LocalDate?,
    maxMonth: YearMonth?,
    firstMonth: YearMonth,
    lastMonth: YearMonth,
    onOpenMonths: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    onChangeMonth: (YearMonth) -> Unit
) {
    val today = todayKl()
    val pageCount = monthsBetween(firstMonth, lastMonth) + 1
    val pageOf: (YearMonth) -> Int = { month -> monthsBetween(firstMonth, month).coerceIn(0, pageCount - 1) }
    val pager = rememberPagerState(initialPage = pageOf(visibleMonth)) { pageCount }
    val shownMonth by rememberUpdatedState(visibleMonth)
    val changeMonth by rememberUpdatedState(onChangeMonth)
    // A swipe that settles on a month makes it the staged one…
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val month = firstMonth.plusMonths(page.toLong())
            if (month != shownMonth) changeMonth(month)
        }
    }
    // …and the arrows (or a month picked from the list) glide the pages along.
    LaunchedEffect(visibleMonth) {
        val target = pageOf(visibleMonth)
        if (pager.currentPage != target && !pager.isScrollInProgress) {
            pager.animateScrollToPage(target, animationSpec = BotMotion.smooth(0.42f))
        }
    }
    GlassPill(
        onClick = onOpenMonths,
        contentDescription = "${monthLabel(visibleMonth)}, choose month"
    ) {
        AnimatedContent(
            targetState = visibleMonth,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInVertically(BotMotion.smooth(0.32f, IntOffset.VisibilityThreshold)) { h -> direction * h / 2 } + fadeIn(BotMotion.smooth(0.24f)))
                    .togetherWith(slideOutVertically(BotMotion.smooth(0.32f, IntOffset.VisibilityThreshold)) { h -> -direction * h / 2 } + fadeOut(BotMotion.smooth(0.18f)))
                    .using(SizeTransform(clip = false) { _, _ -> BotMotion.Resize })
            },
            label = "calendarMonthName"
        ) { month ->
            Text(monthShort(month.monthValue), style = body(22f, FontWeight.SemiBold), color = Color.White)
        }
        Spacer(Modifier.size(12.dp))
        BotIcon(BotIcons.Next, size = 18.dp, tint = Color(0xFFA6B9F3))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        "SMTWTFS".forEach { letter ->
            Box(Modifier.weight(1f).height(28.dp), contentAlignment = Alignment.Center) {
                Text(letter.toString(), style = display(17f), color = Color(0xFFE6EDFF))
            }
        }
    }
    HorizontalPager(
        state = pager,
        modifier = Modifier.fillMaxWidth(),
        beyondViewportPageCount = 1,
        verticalAlignment = Alignment.Top
    ) { page ->
        MonthGrid(
            month = firstMonth.plusMonths(page.toLong()),
            selected = selected,
            today = today,
            maxDate = maxDate,
            onSelect = onSelect
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        GlassCircle(
            BotIcons.Back,
            "Previous calendar month",
            size = 44,
            enabled = visibleMonth > firstMonth
        ) { onChangeMonth(visibleMonth.minusMonths(1)) }
        Text(
            monthLabel(visibleMonth),
            style = body(12f, FontWeight.Medium).copy(shadow = Shadow(Color.Black, Offset(0f, 1f), 4f)),
            color = Color.White
        )
        GlassCircle(
            BotIcons.Next,
            "Next calendar month",
            size = 44,
            enabled = maxMonth == null || visibleMonth < maxMonth
        ) { onChangeMonth(visibleMonth.plusMonths(1)) }
    }
}

private fun monthsBetween(from: YearMonth, to: YearMonth): Int =
    (to.year - from.year) * 12 + (to.monthValue - from.monthValue)

/**
 * One month's days. Always six weeks tall, so paging between months never
 * changes the card's height under the finger.
 */
@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    maxDate: LocalDate?,
    onSelect: (LocalDate) -> Unit
) {
    val cells = remember(month) { calendarCells(month).let { it + List(42 - it.size) { null } } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 6.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                selected = date == selected,
                                isToday = date == today,
                                enabled = maxDate == null || date <= maxDate,
                                onClick = { onSelect(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: Boolean, isToday: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val fill = animatedColor(if (selected) Bot.ActionSolid else Glass, "dayFill")
    val edge = animatedColor(if (selected) Color.White else GlassEdge, "dayEdge")
    val edgeWidth by animateDpAsState(if (selected) 2.dp else 1.dp, BotMotion.smooth(0.3f, Dp.VisibilityThreshold), label = "dayEdgeWidth")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(fill)
            .border(edgeWidth, edge, shape)
            .drawBehind {
                if (isToday && !selected) {
                    val line = 3.dp.toPx()
                    drawRect(Color.White, topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
                }
            }
            .botPress(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = dateLabel(date) + if (isToday) ", today" else ""
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        Text(date.dayOfMonth.toString(), style = display(26f), color = Color.White)
    }
}

@Composable
private fun MonthView(
    mode: CalendarMode,
    visibleMonth: YearMonth,
    maxMonth: YearMonth?,
    yearRange: IntRange,
    onBack: () -> Unit,
    onPick: (YearMonth) -> Unit,
    onYear: (Int) -> Unit
) {
    var yearsOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (mode == CalendarMode.Month) {
                Text("Choose month", style = body(18f, FontWeight.SemiBold, lineHeight = 1.3f), color = Color.White)
            } else {
                GlassPill(onClick = onBack, contentDescription = "Back to calendar") {
                    Text(monthShort(visibleMonth.monthValue), style = body(18f, FontWeight.Medium), color = Color.White)
                    Spacer(Modifier.size(8.dp))
                    BotIcon(BotIcons.Back, size = 18.dp, tint = Color.White)
                }
            }
            Box {
                GlassPill(onClick = { yearsOpen = true }, contentDescription = "Year ${visibleMonth.year}, choose year") {
                    Text(visibleMonth.year.toString(), style = body(18f, FontWeight.Medium), color = Color.White)
                    Spacer(Modifier.size(10.dp))
                    BotIcon(BotIcons.ChevronDown, size = 18.dp, tint = Color.White)
                }
                DropdownMenu(
                    expanded = yearsOpen,
                    onDismissRequest = { yearsOpen = false },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    yearRange.reversed().forEach { year ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    year.toString(),
                                    style = body(15f, if (year == visibleMonth.year) FontWeight.SemiBold else FontWeight.Normal),
                                    color = Bot.FieldInk
                                )
                            },
                            onClick = {
                                onYear(year)
                                yearsOpen = false
                            }
                        )
                    }
                }
            }
        }
        (1..12).chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { monthNumber ->
                    val month = YearMonth.of(visibleMonth.year, monthNumber)
                    val isSelected = monthNumber == visibleMonth.monthValue
                    val enabled = maxMonth == null || month <= maxMonth
                    val shape = RoundedCornerShape(100.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .alpha(if (enabled) 1f else 0.4f)
                            .clip(shape)
                            .background(animatedColor(if (isSelected) Bot.ActionSolid else Glass, "monthFill"))
                            .border(if (isSelected) 2.dp else 1.dp, animatedColor(if (isSelected) Color.White else GlassEdge, "monthEdge"), shape)
                            .botPress(enabled = enabled) { onPick(month) }
                            .semantics {
                                contentDescription = monthLabel(month)
                                selected = isSelected
                            }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(monthShort(monthNumber), style = body(17f, FontWeight.Medium, lineHeight = 1.3f), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassCircle(
    icon: ImageVector,
    label: String,
    size: Int,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(Glass)
            .border(1.dp, GlassEdge, CircleShape)
            .botPress(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        BotIcon(icon, size = 22.dp, tint = Color.White)
    }
}

@Composable
private fun GlassPill(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(100.dp)
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(Glass)
            .border(1.dp, GlassEdge, shape)
            .botPress(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
