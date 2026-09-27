package com.spendwise.app.ui.botanical

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.AccountType
import com.spendwise.app.domain.MoneyFormatter
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    archivedAccounts: List<Account>,
    totalBalance: Long,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Account) -> Unit,
    onRestore: (Account) -> Unit
) {
    DarkPage {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pageTopPadding(), bottom = bottomPadding)
        ) {
            item {
                TopBar("Your accounts", "Back", onBack) {
                    CircleIconButton(BotIcons.Plus, "Add account", onClick = onAdd, size = 46.dp)
                }
            }
            item {
                Kicker("Across ${accounts.size} ${if (accounts.size == 1) "account" else "accounts"}", Modifier.padding(bottom = 8.dp))
                Money(
                    cents = totalBalance,
                    style = moneyStyle(58f, letterSpacingPx = -2f),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)
                )
            }
            items(accounts, key = { it.id }) { account ->
                AccountCard(account, onEdit = { onEdit(account) }, modifier = Modifier.padding(bottom = 18.dp))
            }
            if (accounts.isEmpty()) {
                item {
                    EmptyState(
                        title = "Your first account",
                        body = "Add a bank, wallet, or cash account to get started.",
                        action = "Add account",
                        onAction = onAdd
                    )
                }
            }
            item {
                BotButton(
                    "Add another account",
                    onClick = onAdd,
                    type = ButtonType.Secondary,
                    icon = BotIcons.Plus,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (archivedAccounts.isNotEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 26.dp).height(1.dp).background(Color(0x25FFFFFF)))
                    Kicker("Archived · kept for your records", Modifier.padding(top = 22.dp, bottom = 8.dp))
                }
                items(archivedAccounts, key = { "archived-${it.id}" }) { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 17.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        Box(
                            Modifier
                                .size(43.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0x0EFFFFFF))
                                .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(15.dp)),
                            contentAlignment = Alignment.Center
                        ) { BotIcon(BotIcons.Wallet, tint = Color.White) }
                        Column(Modifier.weight(1f)) {
                            Text(account.name, style = display(23f), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                formatRm(account.currentBalanceCents),
                                style = body(13f, lineHeight = 1.5f),
                                color = Color(0xFFA3ACC0),
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                        TextLinkButton("Restore", onClick = { onRestore(account) }, color = Bot.PageMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCard(account: Account, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 224.dp)
            .clip(shape)
    ) {
        Image(
            painterResource(accountSceneFor(account.iconName, account.type).drawable),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.1f to Color.Transparent, 1f to Color(0xBB000000)))
        )
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArtImage(accountIconFor(account.type), size = 44.dp)
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xE0101C3A))
                        .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(99.dp))
                        .botPress(onClick = onEdit)
                        .semantics { contentDescription = "Edit ${account.name}" }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BotIcon(BotIcons.Edit, size = 15.dp, tint = Color.White)
                    Text("Edit", style = display(18f), color = Color.White)
                }
            }
            Text(
                account.name,
                style = display(32f, FontWeight.Light, lineHeight = 1.15f),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 34.dp, bottom = 6.dp)
            )
            Money(
                cents = account.currentBalanceCents,
                style = moneyStyle(46f, letterSpacingPx = -2f),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${account.type.displayLabel} account",
                style = body(10f, letterSpacing = 0.5f),
                color = Color(0xFFE5E6EC),
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

private enum class BalanceSign(val label: String) { Positive("Positive"), Negative("Negative") }

/**
 * New / edit account. Editing sets the account's *current* balance; the
 * difference is folded into its opening balance so every existing entry
 * stays put.
 */
@Composable
fun AccountFormSheet(
    visible: Boolean,
    existing: Account?,
    onSave: (name: String, type: AccountType, startingBalance: String, iconName: String, color: Long) -> String?,
    onArchive: suspend () -> String?,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    val shown = rememberRetained(if (visible) AccountKey(existing) else null)
    BotSheet(visible = visible, onDismiss = onDismiss, title = if (shown?.account != null) "Edit account" else "New account") {
        if (shown != null) AccountFormBody(shown.account, onSave, onArchive, onRestore)
    }
}

private data class AccountKey(val account: Account?)

@Composable
private fun AccountFormBody(
    existing: Account?,
    onSave: (name: String, type: AccountType, startingBalance: String, iconName: String, color: Long) -> String?,
    onArchive: suspend () -> String?,
    onRestore: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var type by remember(existing) { mutableStateOf(existing?.type ?: AccountType.Bank) }
    val current = existing?.currentBalanceCents ?: 0L
    var amount by remember(existing) { mutableStateOf(kotlin.math.abs(current)) }
    var sign by remember(existing) { mutableStateOf(if (current < 0) BalanceSign.Negative else BalanceSign.Positive) }
    var scene by remember(existing) {
        mutableStateOf(existing?.let { accountSceneFor(it.iconName, it.type) } ?: AccountScene.defaultFor(AccountType.Bank))
    }
    var sceneTouched by remember(existing) { mutableStateOf(existing != null) }
    // Save errors are about the name (blank or taken); archive errors are
    // about the account's history, so only the former outline the field.
    var error by remember(existing) { mutableStateOf<String?>(null) }
    var nameError by remember(existing) { mutableStateOf(false) }

    FieldLabel("Account name")
    BotTextField(
        value = name,
        onValueChange = {
            name = it
            error = null
            nameError = false
        },
        placeholder = "e.g. Maybank",
        maxLength = 60,
        isError = nameError,
        contentLabel = "Account name"
    )
    FieldLabel("Account type", modifier = Modifier.padding(top = 16.dp))
    SelectField(
        options = AccountTypeOrder,
        selected = type,
        onSelect = {
            type = it
            // A new account's landscape follows its type until the user picks one.
            if (!sceneTouched) scene = AccountScene.defaultFor(it)
        },
        label = { it.displayLabel },
        contentLabel = "Account type"
    )
    FormCaption(if (existing != null) "Current balance" else "Opening balance", Modifier.padding(top = 20.dp))
    AmountInput(cents = amount, onChange = { amount = it }, label = "Account balance")
    PillSegment(
        options = BalanceSign.entries,
        selected = sign,
        onSelect = { sign = it },
        label = { it.label },
        tone = SegmentTone.OnLight
    )
    if (existing != null) {
        InlineNote(
            "Adjusting this balance keeps your existing transactions and updates the account’s opening balance.",
            Modifier.padding(top = 16.dp)
        )
    }
    FormCaption("Choose your artwork", Modifier.padding(top = 20.dp, bottom = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AccountScene.entries.forEachIndexed { index, option ->
            val selected = option == scene
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 64.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .then(if (selected) Modifier.border(2.dp, Bot.ActionSolid, RoundedCornerShape(18.dp)) else Modifier)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF0F2F6))
                        .botPress {
                            scene = option
                            sceneTouched = true
                        }
                        .semantics {
                            contentDescription = "Artwork ${index + 1}"
                            this.selected = selected
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painterResource(option.drawable),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize(0.92f)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
    error?.let { ErrorBox(it, Modifier.padding(top = 16.dp)) }
    BotButton(
        "Save account",
        onClick = {
            val target = if (sign == BalanceSign.Negative) -amount else amount
            // Keep every recorded movement: shift the opening balance by the
            // difference between the balance typed and the balance now.
            val starting = if (existing != null) {
                target - (existing.currentBalanceCents - existing.startingBalanceCents)
            } else target
            error = onSave(name, type, MoneyFormatter.centsToInput(starting), scene.key, existing?.color ?: DEFAULT_ACCOUNT_COLOR)
            nameError = error != null
        },
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
    )
    if (existing != null) {
        TextLinkButton(
            if (existing.isArchived) "Restore account" else "Archive account",
            onClick = {
                if (existing.isArchived) {
                    onRestore()
                } else {
                    scope.launch {
                        nameError = false
                        error = onArchive()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
    }
}

/** Slate, as the data layer seeds; the redesign draws accounts from their artwork. */
private const val DEFAULT_ACCOUNT_COLOR = 0xFF64748BL
