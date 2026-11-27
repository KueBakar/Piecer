package com.piecer.app.data.local

import androidx.room.TypeConverter
import com.piecer.app.data.local.entity.AssetType
import com.piecer.app.data.local.entity.BudgetPeriodType
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.DebtDirection
import com.piecer.app.data.local.entity.RecurringInterval
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletType

class Converters {
    @TypeConverter
    fun fromWalletType(value: WalletType): String = value.name

    @TypeConverter
    fun toWalletType(value: String): WalletType = WalletType.valueOf(value)

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromBudgetPeriodType(value: BudgetPeriodType): String = value.name

    @TypeConverter
    fun toBudgetPeriodType(value: String): BudgetPeriodType = BudgetPeriodType.valueOf(value)

    @TypeConverter
    fun fromRecurringInterval(value: RecurringInterval): String = value.name

    @TypeConverter
    fun toRecurringInterval(value: String): RecurringInterval = RecurringInterval.valueOf(value)

    @TypeConverter
    fun fromDebtDirection(value: DebtDirection): String = value.name

    @TypeConverter
    fun toDebtDirection(value: String): DebtDirection = DebtDirection.valueOf(value)

    @TypeConverter
    fun fromAssetType(value: AssetType): String = value.name

    @TypeConverter
    fun toAssetType(value: String): AssetType = AssetType.valueOf(value)
}
