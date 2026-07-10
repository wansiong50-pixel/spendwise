package com.spendwise.app.backup

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Naming and retention rules for automatic backups. Pure string/time logic —
 * no Android types — so it runs on the JVM test source set.
 *
 * Auto backups use the `spendwise-auto-` prefix; the manual export flow names
 * its files `spendwise-backup-*`, so rotation here can never delete a backup
 * the user created by hand.
 */
object BackupRotation {

    const val AUTO_BACKUP_PREFIX = "spendwise-auto-"
    const val DEFAULT_KEEP = 7

    // KL time, matching every other timestamp the app renders.
    private val ZONE = ZoneId.of("Asia/Kuala_Lumpur")
    private val NAME_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")

    /** e.g. `spendwise-auto-20260710-0930.json` for the given instant. */
    fun autoBackupFileName(nowMillis: Long): String {
        val stamp = Instant.ofEpochMilli(nowMillis).atZone(ZONE).format(NAME_STAMP)
        return "$AUTO_BACKUP_PREFIX$stamp.json"
    }

    /**
     * Given the file names present in the backup folder, return the auto-backup
     * files that should be deleted so only the newest [keep] remain. Non-auto
     * files (manual backups, unrelated documents) are never returned.
     *
     * The timestamp is embedded in the name, so lexicographic order equals
     * chronological order — including SAF collision suffixes like
     * `spendwise-auto-20260710-0930 (1).json`, which sort next to their
     * original and are rotated out together.
     */
    fun filesToPrune(existingNames: List<String>, keep: Int = DEFAULT_KEEP): List<String> {
        return existingNames
            .filter { it.startsWith(AUTO_BACKUP_PREFIX) && it.endsWith(".json") }
            .sortedDescending()
            .drop(keep)
    }
}
