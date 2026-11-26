package com.piecer.app.data.local.entity

enum class WalletType {
    CASH,
    BANK,
    EWALLET,
    OTHER
}

enum class CategoryType {
    INCOME,
    EXPENSE
}

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

enum class RecurringInterval {
    WEEKLY,
    MONTHLY
}
