package com.spendwise.app.export

import com.spendwise.app.domain.Account
import com.spendwise.app.domain.AccountType
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.Transfer
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodCsvExporterTest {
    private val zone = ZoneId.of("Asia/Kuala_Lumpur")
    private fun millis(date: String) = LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()

    private val food = Category(1, "Food", 0, "restaurant")
    private val salary = Category(6, "Salary", 0, "wallet", isIncomeAdjustment = true)
    private val accounts = listOf(
        Account(1, "Maybank", AccountType.Bank, 0, 0, 0, "", 0, false),
        Account(2, "Touch 'n Go", AccountType.EWallet, 0, 0, 0, "", 1, false)
    )

    @Test
    fun writesEveryKindOldestFirstWithSignedAmounts() {
        val csv = PeriodCsvExporter.buildCsv(
            expenses = listOf(
                Expense(1, 3_850, food.id, "Food", 1, "Kenny Hills \"Bakers\"", "Coffee, cake", millis("2026-09-07"), 1),
                Expense(2, 420_000, salary.id, "Salary", 1, "", "", millis("2026-09-01"), 2)
            ),
            transfers = listOf(Transfer(3, 1, "Maybank", 2, "Touch 'n Go", 10_000, "", millis("2026-09-06"), 3)),
            categories = listOf(food, salary),
            accounts = accounts,
            zone = zone
        )
        assertTrue(csv.startsWith("﻿Date,Type,Name,Amount (MYR),Category,Account,To account,Notes\r\n"))
        val rows = csv.removePrefix("﻿").split("\r\n")
        assertEquals(4, rows.size)
        assertEquals("\"2026-09-01\",\"Income\",\"Salary\",4200.00,\"Salary\",\"Maybank\",\"\",\"\"", rows[1])
        assertEquals("\"2026-09-06\",\"Transfer\",\"Account transfer\",100.00,\"\",\"Maybank\",\"Touch 'n Go\",\"\"", rows[2])
        assertEquals(
            "\"2026-09-07\",\"Expense\",\"Kenny Hills \"\"Bakers\"\"\",-38.50,\"Food\",\"Maybank\",\"\",\"Coffee, cake\"",
            rows[3]
        )
    }

    @Test
    fun neutralisesSpreadsheetFormulas() {
        assertEquals("\"'=SUM(A1)\"", PeriodCsvExporter.text("=SUM(A1)"))
        assertEquals("\"' -1\"", PeriodCsvExporter.text(" -1"))
        assertEquals("\"'@cmd\"", PeriodCsvExporter.text("@cmd"))
        assertEquals("\"Mamak 24/7\"", PeriodCsvExporter.text("Mamak 24/7"))
    }

    @Test
    fun emptyPeriodIsJustTheHeader() {
        val csv = PeriodCsvExporter.buildCsv(emptyList(), emptyList(), emptyList(), emptyList(), zone)
        assertEquals("﻿Date,Type,Name,Amount (MYR),Category,Account,To account,Notes", csv)
    }
}
