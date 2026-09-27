package com.spendwise.app.ui.botanical

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.YearMonth

/** Midnight-navy page background (`.b-page.dark`). */
@Composable
fun DarkPage(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Bot.PageGradient),
        content = content
    )
}

/** Top padding every page shares: below the status bar, plus breathing room. */
@Composable
fun pageTopPadding(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp

/**
 * Activity / Insights header: title with a trailing action, then the month
 * control. Both screens share the geometry so switching tabs doesn't jump.
 */
@Composable
fun ScreenHeader(
    title: String,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onOpenPicker: () -> Unit,
    action: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = display(26f),
                color = Color.White,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { action() }
        }
        PeriodSelector(month = month, onChange = onMonthChange, onOpenPicker = onOpenPicker)
    }
}

/** Secondary pages (`.b-top`): back, centred title, optional action. */
@Composable
fun TopBar(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircleIconButton(BotIcons.Back, backLabel, onClick = onBack, size = 46.dp)
        Text(
            title,
            style = display(28f),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (action != null) {
            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { action() }
        } else {
            Spacer(Modifier.size(46.dp))
        }
    }
}
