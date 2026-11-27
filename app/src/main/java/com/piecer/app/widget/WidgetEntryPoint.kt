package com.piecer.app.widget

import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.data.repository.BudgetRepository
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.data.repository.WalletRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun settingsRepository(): SettingsRepository
    fun transactionRepository(): TransactionRepository
    fun walletRepository(): WalletRepository
    fun budgetRepository(): BudgetRepository
    fun categoryRepository(): CategoryRepository
}
