package com.spendwise.app.data

import com.spendwise.app.domain.Account
import com.spendwise.app.domain.AccountType
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        color = color,
        iconName = iconName,
        isIncomeAdjustment = isIncomeAdjustment,
        isCustom = isCustom
    )
}

fun ExpenseEntity.toDomain(category: CategoryEntity?): Expense {
    return Expense(
        id = id,
        amountCents = amountCents,
        categoryId = categoryId,
        categoryName = category?.name ?: "Uncategorized",
        accountId = accountId,
        merchant = merchant,
        notes = notes,
        occurredAtMillis = occurredAtMillis,
        createdAtMillis = createdAtMillis,
        categoryIconName = category?.iconName.orEmpty(),
        categoryColor = category?.color
    )
}

/**
 * Maps an account row to the domain model with its computed current balance
 * already filled in. [currentBalanceCents] is provided by the repository
 * (which has access to the expense ledger); this function is a pure rename so
 * the data layer's storage shape doesn't leak into the UI.
 */
fun AccountEntity.toDomain(currentBalanceCents: Long): Account {
    return Account(
        id = id,
        name = name,
        type = AccountType.fromStorageKey(type),
        startingBalanceCents = startingBalanceCents,
        currentBalanceCents = currentBalanceCents,
        color = color,
        iconName = iconName,
        sortOrder = sortOrder,
        isArchived = isArchived
    )
}
