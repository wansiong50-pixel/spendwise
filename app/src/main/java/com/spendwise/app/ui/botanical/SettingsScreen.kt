package com.spendwise.app.ui.botanical

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.app.data.BackupSettings
import java.time.Instant
import java.time.format.DateTimeFormatter

private val SavedAt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "26 Sept, 14:05" */
fun backupTimeLabel(millis: Long): String {
    val at = Instant.ofEpochMilli(millis).atZone(KL_ZONE)
    return "${shortDateLabel(at.toLocalDate())}, ${at.format(SavedAt)}"
}

/** Human name for a SAF tree URI ("Download/SpendWise" → "SpendWise"). */
fun backupFolderName(treeUri: String): String = runCatching {
    val decoded = Uri.decode(Uri.parse(treeUri).lastPathSegment.orEmpty())
    decoded.substringAfter(':').substringAfterLast('/').ifBlank { decoded.substringBefore(':') }
}.getOrNull().orEmpty().ifBlank { "your backup folder" }

@Composable
fun SettingsScreen(
    backup: BackupSettings,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onAccounts: () -> Unit,
    onCategories: () -> Unit,
    onRecurring: () -> Unit,
    onBackupNow: () -> Unit,
    onRestore: () -> Unit,
    onToggleDaily: (Boolean) -> Unit,
    onChooseFolder: () -> Unit,
    onDownloadCopy: () -> Unit
) {
    val folder = backup.treeUri?.let(::backupFolderName)
    DarkPage {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = pageTopPadding(), bottom = bottomPadding)
        ) {
            TopBar("Settings", "Back to Home", onBack, modifier = Modifier.padding(bottom = 0.dp))
            SettingsGroup("Make it yours") {
                SettingsRow(BotIcons.Wallet, "Accounts", "Bank, cash, and everyday wallets", chevron = true, onClick = onAccounts)
                SettingsRow(BotIcons.Grid, "Categories", "Icons and monthly budgets", chevron = true, onClick = onCategories)
                SettingsRow(BotIcons.Repeat, "Recurring", "Automatic entries for regular bills and income", chevron = true, last = true, onClick = onRecurring)
            }
            SettingsGroup("Keep a copy") {
                SettingsRow(
                    BotIcons.Download,
                    "Back up now",
                    when {
                        backup.lastBackupMillis != null -> "Last saved · ${backupTimeLabel(backup.lastBackupMillis)}"
                        folder != null -> "Save a file to $folder"
                        else -> "Save your entries, accounts and rules to a file"
                    },
                    onClick = onBackupNow
                )
                SettingsRow(BotIcons.Upload, "Restore a backup", "SpendWise JSON · review before restoring", chevron = true, onClick = onRestore)
                SettingsRow(
                    BotIcons.Repeat,
                    "Daily backups",
                    "One file per day in your chosen folder",
                    onClick = { onToggleDaily(!backup.enabled) },
                    trailing = {
                        BotToggle(checked = backup.enabled, onToggle = { onToggleDaily(!backup.enabled) }, label = "Daily backups")
                    }
                )
                SettingsRow(
                    BotIcons.Wallet,
                    if (folder != null) "Change backup folder" else "Choose backup folder",
                    folder ?: "Select a folder on this device",
                    chevron = true,
                    last = true,
                    onClick = onChooseFolder
                )
            }
            Note(
                "A backup is saved to your folder once a day, even while SpendWise is closed. The seven most recent daily files are kept; files you save yourself are never removed.",
                Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp)
            )
            backup.lastError?.let { message ->
                Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp)) {
                    Box(Modifier.size(width = 3.dp, height = 20.dp).background(Color(0xFFDB81A9)))
                    Text(
                        message,
                        style = body(13f, lineHeight = 1.6f),
                        color = Color.White,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
            if (folder != null) {
                TextLinkButton(
                    "Save a copy somewhere else",
                    onClick = onDownloadCopy,
                    color = Bot.PageMuted,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(
            title,
            style = body(14f, FontWeight.Medium, lineHeight = 1.4f),
            color = Bot.PageMuted,
            modifier = Modifier
                .padding(start = 4.dp, end = 4.dp, bottom = 10.dp)
                .semantics { heading() }
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x0DFFFFFF))
                .border(1.dp, Color(0x1CFFFFFF), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp)
        ) { content() }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    chevron: Boolean = false,
    last: Boolean = false,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth().botRowPress(pressedColor = Color(0x14FFFFFF), onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                Modifier
                    .size(43.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0x12FFFFFF))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) { BotIcon(icon, tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text(title, style = display(23f), color = Color.White)
                Text(
                    subtitle,
                    style = body(13f, lineHeight = 1.5f),
                    color = Color(0xFFA3ACC0),
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            when {
                trailing != null -> trailing()
                chevron -> BotIcon(BotIcons.Next, size = 16.dp, tint = Color.White)
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
    }
}
