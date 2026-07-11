@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.spendwise.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.MoneyFormatter
import com.spendwise.app.domain.RecurrenceCadence
import com.spendwise.app.domain.RecurringRule
import com.spendwise.app.ui.theme.AppIsDark
import com.spendwise.app.ui.theme.AppOnSurfaceVariant
import com.spendwise.app.ui.theme.AppSurface
import com.spendwise.app.ui.theme.AppSurfaceContainer
import com.spendwise.app.ui.theme.AppSurfaceLow
import com.spendwise.app.ui.theme.SpendWiseMotion
import com.spendwise.app.ui.theme.SwInk
import com.spendwise.app.ui.theme.SwNeg
import com.spendwise.app.ui.theme.SwPos
import com.spendwise.app.ui.theme.SwViolet
import com.spendwise.app.ui.theme.SwVioletSoft
import com.spendwise.app.ui.theme.pressableNoIndication
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ZONE_KL = ZoneId.of("Asia/Kuala_Lumpur")
private val DUE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
private val FIRST_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

private fun formatRm(cents: Long): String =
    "RM ${formatRinggit(cents / 100L)}.${"%02d".format(cents % 100L)}"

@Composable
internal fun V2RecurringScreen(
    rules: List<RecurringRule>,
    categories: List<Category>,
    accounts: List<Account>,
    onBack: () -> Unit,
    // (id, amountInput, categoryId, accountId, merchant, notes, cadence,
    //  firstOccurrenceInput) -> error or null
    onSave: (Long?, String, Long?, Long?, String, String, RecurrenceCadence, String) -> String?,
    onDelete: (Long) -> Unit,
    onSetPaused: (Long, Boolean) -> Unit
) {
    val isDark = AppIsDark

    var formOpen by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<RecurringRule?>(null) }

    val activeCount = rules.count { !it.isPaused }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 48.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                V2CircleButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Back",
                    bg = if (isDark) AppSurfaceLow else null
                )
                Text("Recurring", color = SwInk, style = v2T(15f, FontWeight.Bold))
                V2CircleButton(
                    icon = Icons.Filled.Add,
                    onClick = {
                        editingRule = null
                        formOpen = true
                    },
                    contentDescription = "Add recurring transaction",
                    bg = if (isDark) AppSurfaceLow else null
                )
            }
        }

        // Hero summary
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                V2Eyebrow(
                    text = if (rules.isEmpty()) "No rules yet"
                    else "${rules.size} ${if (rules.size == 1) "rule" else "rules"} · $activeCount active"
                )
                Text(
                    text = "Bills and income that log themselves.",
                    color = SwInk,
                    style = v2T(22f, FontWeight.Bold, letter = -0.22f)
                )
            }
        }

        // Rule list
        if (rules.isNotEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    V2Card(padding = 6.dp) {
                        Column {
                            rules.forEachIndexed { index, rule ->
                                if (index > 0) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(v2Hairline())
                                    )
                                }

                                val vis = visualForCategory(
                                    rule.categoryName, rule.categoryIconName, rule.categoryColor
                                )
                                val nextDue = LocalDate.ofEpochDay(rule.nextDueEpochDay)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            editingRule = rule
                                            formOpen = true
                                        }
                                        .alpha(if (rule.isPaused) 0.55f else 1f)
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    V2Tile(color = vis.color, icon = vis.icon, size = 42.dp)

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = rule.merchant.ifBlank { rule.categoryName },
                                                color = SwInk,
                                                style = v2T(14.5f, FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (rule.isPaused) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(percent = 50))
                                                        .background(AppSurfaceLow)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        "PAUSED",
                                                        color = AppOnSurfaceVariant,
                                                        style = v2T(9.5f, FontWeight.Bold, letter = 0.06f)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${rule.cadence.displayLabel} · next ${nextDue.format(DUE_FORMAT)}",
                                            color = AppOnSurfaceVariant,
                                            style = v2T(12f, FontWeight.Medium),
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = (if (rule.isIncome) "+" else "−") + formatRm(rule.amountCents),
                                            color = if (rule.isIncome) SwPos else SwInk,
                                            style = v2N(13.5f, FontWeight.Bold)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Edit rule",
                                            tint = AppOnSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CTA
        item {
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .pressableNoIndication(scale = 0.97f) {
                            editingRule = null
                            formOpen = true
                        },
                    color = if (isDark) SwViolet else SwInk,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = if (isDark) 12.dp else 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "New recurring transaction",
                            color = Color.White,
                            style = v2T(14f, FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Footer note
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(SwVioletSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text("?", color = SwInk, style = v2T(12f, FontWeight.ExtraBold))
                }
                Text(
                    text = "Due transactions are logged automatically when you open the app — " +
                        "if you were away, every missed occurrence is backfilled with its " +
                        "correct date. Pause a rule to stop it without losing its setup.",
                    color = AppOnSurfaceVariant,
                    style = v2T(12.5f, FontWeight.Medium),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    V2RecurringFormSheet(
        visible = formOpen,
        existing = editingRule,
        categories = categories,
        accounts = accounts,
        onSave = { id, amount, categoryId, accountId, merchant, notes, cadence, firstDate ->
            val err = onSave(id, amount, categoryId, accountId, merchant, notes, cadence, firstDate)
            if (err == null) formOpen = false
            err
        },
        onDelete = { id ->
            onDelete(id)
            formOpen = false
        },
        onSetPaused = { id, paused ->
            onSetPaused(id, paused)
            formOpen = false
        },
        onDismiss = { formOpen = false }
    )
}

@Composable
private fun V2RecurringFormSheet(
    visible: Boolean,
    existing: RecurringRule?,
    categories: List<Category>,
    accounts: List<Account>,
    onSave: (Long?, String, Long?, Long?, String, String, RecurrenceCadence, String) -> String?,
    onDelete: (Long) -> Unit,
    onSetPaused: (Long, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val isEdit = existing != null

    var amountInput by rememberSaveable(visible, existing) {
        mutableStateOf(existing?.let { MoneyFormatter.centsToInput(it.amountCents) } ?: "")
    }
    var merchant by rememberSaveable(visible, existing) { mutableStateOf(existing?.merchant ?: "") }
    var notes by rememberSaveable(visible, existing) { mutableStateOf(existing?.notes ?: "") }
    var categoryId by rememberSaveable(visible, existing) { mutableStateOf(existing?.categoryId) }
    var accountId by rememberSaveable(visible, existing) {
        mutableStateOf(existing?.accountId ?: accounts.firstOrNull()?.id)
    }
    var cadence by rememberSaveable(visible, existing) {
        mutableStateOf(existing?.cadence ?: RecurrenceCadence.Monthly)
    }
    var firstDateInput by rememberSaveable(visible, existing) {
        mutableStateOf(
            existing?.let { LocalDate.ofEpochDay(it.anchorEpochDay).toString() }
                ?: LocalDate.now(ZONE_KL).toString()
        )
    }
    var datePickerOpen by remember(visible) { mutableStateOf(false) }
    var error by rememberSaveable(visible) { mutableStateOf<String?>(null) }

    // firstDateInput is always written by the calendar picker, so this parse
    // can only fail for a pre-picker draft — fall back to today.
    val firstDate = remember(firstDateInput) {
        runCatching { LocalDate.parse(firstDateInput) }.getOrElse { LocalDate.now(ZONE_KL) }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(SpendWiseMotion.Sheet, easing = SpendWiseMotion.EaseOut)
        ) + fadeIn(tween(SpendWiseMotion.Sheet)),
        exit = slideOutVertically(
            targetOffsetY = { it / 3 },
            animationSpec = tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseInOut)
        ) + fadeOut(tween(SpendWiseMotion.ModalExit))
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (AppIsDark) 0.62f else 0.42f))
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(AppSurface)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(top = 12.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(AppSurfaceContainer)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    V2CircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onDismiss,
                        contentDescription = "Back",
                        size = 38.dp,
                        bg = if (AppIsDark) AppSurfaceLow else null
                    )
                    Text(
                        if (isEdit) "Edit recurring" else "New recurring",
                        color = SwInk,
                        style = v2T(15f, FontWeight.Bold)
                    )
                    Text(
                        text = "Save",
                        color = SwViolet,
                        modifier = Modifier.pressableNoIndication {
                            error = onSave(
                                existing?.id, amountInput, categoryId, accountId,
                                merchant, notes, cadence, firstDateInput
                            )
                        },
                        style = v2T(13.5f, FontWeight.Bold)
                    )
                }

                // Amount + merchant hero
                Box(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 24.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppSurfaceLow,
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            V2Eyebrow(text = "Amount · RM")
                            BasicTextField(
                                value = amountInput,
                                onValueChange = {
                                    amountInput = it
                                    error = null
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                textStyle = v2N(26f, FontWeight.ExtraBold).copy(color = SwInk),
                                cursorBrush = SolidColor(SwInk),
                                decorationBox = { inner ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (amountInput.isBlank()) {
                                            Text(
                                                "0.00",
                                                color = AppOnSurfaceVariant,
                                                style = v2N(26f, FontWeight.ExtraBold)
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                            V2Eyebrow(text = "Merchant / description")
                            BasicTextField(
                                value = merchant,
                                onValueChange = {
                                    merchant = it
                                    error = null
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                textStyle = v2T(15f, FontWeight.SemiBold).copy(color = SwInk),
                                cursorBrush = SolidColor(SwInk),
                                decorationBox = { inner ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (merchant.isBlank()) {
                                            Text(
                                                "e.g. Rent, Netflix, Salary",
                                                color = AppOnSurfaceVariant,
                                                style = v2T(15f, FontWeight.SemiBold)
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                        }
                    }
                }

                RecurringSectionLabel("Category")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val selected = categoryId == cat.id
                        val vis = visualForCategory(cat.name, cat.iconName, cat.color)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(if (selected) SwInk else AppSurfaceLow)
                                .pressableNoIndication {
                                    categoryId = cat.id
                                    error = null
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(vis.color)
                            )
                            Text(
                                text = cat.name,
                                color = if (selected) Color.White else SwInk,
                                style = v2T(12.5f, FontWeight.Bold)
                            )
                        }
                    }
                }

                if (accounts.size > 1) {
                    RecurringSectionLabel("Account")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { acct ->
                            val selected = accountId == acct.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(if (selected) SwInk else AppSurfaceLow)
                                    .pressableNoIndication {
                                        accountId = acct.id
                                        error = null
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = acct.name,
                                    color = if (selected) Color.White else SwInk,
                                    style = v2T(12.5f, FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                RecurringSectionLabel("Repeats")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(AppSurfaceLow)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    RecurrenceCadence.entries.forEach { option ->
                        RecurringSeg(
                            label = option.displayLabel,
                            selected = cadence == option,
                            modifier = Modifier.weight(1f)
                        ) { cadence = option }
                    }
                }

                RecurringSectionLabel("First occurrence")
                Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppSurfaceLow,
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressableNoIndication { datePickerOpen = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = AppOnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = firstDate.format(FIRST_FORMAT),
                                color = SwInk,
                                style = v2N(14.5f, FontWeight.SemiBold),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Pick a date",
                                tint = AppOnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Past dates backfill each missed occurrence when you save.",
                    color = AppOnSurfaceVariant,
                    style = v2T(11.5f, FontWeight.Medium),
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp)
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        color = SwNeg,
                        style = v2T(12.5f, FontWeight.SemiBold),
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp)
                    )
                }

                if (isEdit) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(AppSurfaceLow)
                                .pressableNoIndication {
                                    onSetPaused(existing!!.id, !existing.isPaused)
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (existing!!.isPaused) "Resume" else "Pause",
                                color = SwInk,
                                style = v2T(13.5f, FontWeight.Bold)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(AppSurfaceLow)
                                .pressableNoIndication { onDelete(existing!!.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete rule",
                                color = SwNeg,
                                style = v2T(13.5f, FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 14.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Box(Modifier.navigationBarsPadding())
            }
        }
    }

    // Calendar picker — rendered after (so on top of) the form sheet. Same
    // component the add-expense form uses; no entry dots here because a
    // recurrence anchor doesn't care which days already have transactions.
    V2DatePickerSheet(
        visible = visible && datePickerOpen,
        selectedDate = firstDate,
        dayEntryCounts = emptyMap(),
        onSelect = { picked ->
            firstDateInput = picked.toString()
            error = null
            datePickerOpen = false
        },
        onDismiss = { datePickerOpen = false }
    )
}

@Composable
private fun RecurringSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = AppOnSurfaceVariant,
        style = v2T(11f, FontWeight.Bold, letter = 0.08f),
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 10.dp)
    )
}

@Composable
private fun RecurringSeg(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val selectedFg = if (AppIsDark) Color(0xFF0F0D1A) else Color.White
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) SwInk else Color.Transparent)
            .pressableNoIndication(scale = 0.96f, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) selectedFg else AppOnSurfaceVariant,
            style = v2T(13f, FontWeight.Bold)
        )
    }
}
