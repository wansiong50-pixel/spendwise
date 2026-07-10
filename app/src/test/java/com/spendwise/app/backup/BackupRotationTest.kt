package com.spendwise.app.backup

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRotationTest {

    @Test
    fun `file name embeds KL-time stamp`() {
        // 2026-07-10 09:30 in Asia/Kuala_Lumpur
        val millis = ZonedDateTime.of(2026, 7, 10, 9, 30, 0, 0, ZoneId.of("Asia/Kuala_Lumpur"))
            .toInstant()
            .toEpochMilli()
        assertEquals("spendwise-auto-20260710-0930.json", BackupRotation.autoBackupFileName(millis))
    }

    @Test
    fun `file name crosses the day boundary in KL time not UTC`() {
        // 2026-07-09 17:00 UTC = 2026-07-10 01:00 KL — the name must carry
        // the KL date, matching every other date the app renders.
        val millis = ZonedDateTime.of(2026, 7, 9, 17, 0, 0, 0, ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        assertEquals("spendwise-auto-20260710-0100.json", BackupRotation.autoBackupFileName(millis))
    }

    @Test
    fun `prunes oldest beyond keep limit`() {
        val names = (1..10).map { day ->
            "spendwise-auto-202607%02d-0900.json".format(day)
        }
        val pruned = BackupRotation.filesToPrune(names.shuffled(), keep = 7)
        assertEquals(
            listOf(
                "spendwise-auto-20260703-0900.json",
                "spendwise-auto-20260702-0900.json",
                "spendwise-auto-20260701-0900.json"
            ),
            pruned
        )
    }

    @Test
    fun `nothing pruned when at or below keep limit`() {
        val names = (1..7).map { "spendwise-auto-202607%02d-0900.json".format(it) }
        assertTrue(BackupRotation.filesToPrune(names).isEmpty())
        assertTrue(BackupRotation.filesToPrune(emptyList()).isEmpty())
    }

    @Test
    fun `manual backups and unrelated files are never pruned`() {
        val names = (1..9).map { "spendwise-auto-202607%02d-0900.json".format(it) } +
            listOf(
                "spendwise-backup-20260101-0000.json", // manual export
                "holiday-photo.png",
                "spendwise-auto-notes.txt" // right prefix, wrong extension
            )
        val pruned = BackupRotation.filesToPrune(names)
        assertEquals(2, pruned.size)
        assertTrue(pruned.all { it.startsWith(BackupRotation.AUTO_BACKUP_PREFIX) && it.endsWith(".json") })
        assertTrue("spendwise-backup-20260101-0000.json" !in pruned)
    }

    @Test
    fun `SAF collision suffixes sort beside their original and rotate out together`() {
        val names = (1..8).map { "spendwise-auto-202607%02d-0900.json".format(it) } +
            "spendwise-auto-20260701-0900 (1).json"
        val pruned = BackupRotation.filesToPrune(names, keep = 7)
        // The two oldest entries are the day-1 pair; both go.
        assertEquals(
            setOf(
                "spendwise-auto-20260701-0900.json",
                "spendwise-auto-20260701-0900 (1).json"
            ),
            pruned.toSet()
        )
    }
}
