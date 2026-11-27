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

enum class DebtDirection {
    OWE_ME,
    I_OWE
}

enum class AssetType {
    EMAS,
    SAHAM,
    REKSADANA,
    CRYPTO,
    PROPERTI,
    KENDARAAN,
    LAINNYA
}
