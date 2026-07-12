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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Transfer
import com.spendwise.app.ui.theme.AppIsDark
import com.spendwise.app.ui.theme.AppOnSurfaceVariant
import com.spendwise.app.ui.theme.AppSurface
import com.spendwise.app.ui.theme.AppSurfaceContainer
import com.spendwise.app.ui.theme.AppSurfaceLow
import com.spendwise.app.ui.theme.SpendWiseMotion
import com.spendwise.app.ui.theme.SwInk
import com.spendwise.app.ui.theme.SwNeg
import com.spendwise.app.ui.theme.SwSky
import com.spendwise.app.ui.theme.pressableNoIndication
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ZONE_KL = ZoneId.of("Asia/Kuala_Lumpur")
private val DETAIL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

private fun transferRm(cents: Long): String =
    "RM ${formatRinggit(cents / 100L)}.${"%02d".format(cents % 100L)}"

/**
 * One transfer in the Activity timeline. Deliberately visually neutral —
 * no +/− sign, no income/expense color — because a transfer is net-zero
 * for the user: the same money, different pocket.
 */
@Composable
internal fun V2TransferRow(
    transfer: Transfer,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        V2Tile(color = SwSky, icon = Icons.Filled.SwapHoriz, size = 42.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${transfer.fromAccountName} → ${transfer.toAccountName}",
                color = SwInk,
                style = v2T(14.5f, FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (transfer.notes.isBlank()) "Transfer" else "Transfer · ${transfer.notes}",
                color = AppOnSurfaceVariant,
                style = v2T(12f, FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        Text(
            text = transferRm(transfer.amountCents),
            color = AppOnSurfaceVariant,
            style = v2N(13.5f, FontWeight.Bold)
        )
    }
}

/**
 * Detail sheet for a transfer — read + delete. Transfers aren't editable in
 * place (delete and recreate; they're two taps to make), which keeps the
 * add-sheet's edit path expense-only.
 */
@Composable
internal fun V2TransferDetailSheet(
    transfer: Transfer?,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val visible = transfer != null
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(SpendWiseMotion.Sheet, easing = SpendWiseMotion.EaseStandard)),
        exit = fadeOut(tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseStandard))
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (AppIsDark) 0.62f else 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = tween(SpendWiseMotion.Sheet, easing = SpendWiseMotion.EaseDrawer),
                    initialOffsetY = { it }
                ),
                exit = slideOutVertically(
                    animationSpec = tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseDrawer),
                    targetOffsetY = { it }
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = AppSurface,
                    shadowElevation = 24.dp,
                    tonalElevation = 0.dp
                ) {
                    val t = transfer ?: return@Surface
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            V2Tile(color = SwSky, icon = Icons.Filled.SwapHoriz, size = 48.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${t.fromAccountName} → ${t.toAccountName}",
                                    color = SwInk,
                                    style = v2T(16f, FontWeight.Bold)
                                )
                                Text(
                                    text = Instant.ofEpochMilli(t.occurredAtMillis)
                                        .atZone(ZONE_KL).toLocalDate().format(DETAIL_DATE),
                                    color = AppOnSurfaceVariant,
                                    style = v2T(12.5f, FontWeight.Medium)
                                )
                            }
                            Text(
                                text = transferRm(t.amountCents),
                                color = SwInk,
                                style = v2N(16f, FontWeight.Bold)
                            )
                        }
                        if (t.notes.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(AppSurfaceLow)
                                    .padding(14.dp)
                            ) {
                                Text(t.notes, color = SwInk, style = v2T(13.5f, FontWeight.Medium))
                            }
                        }
                        Text(
                            text = "Transfers move money between your accounts — they never count as spending or income.",
                            color = AppOnSurfaceVariant,
                            style = v2T(12f, FontWeight.Medium)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(AppSurfaceLow)
                                .pressableNoIndication { onDelete(t.id) }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete transfer",
                                color = SwNeg,
                                style = v2T(13.5f, FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
