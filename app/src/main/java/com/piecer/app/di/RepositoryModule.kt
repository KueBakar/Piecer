package com.piecer.app.di

import com.piecer.app.data.repository.BudgetRepository
import com.piecer.app.data.repository.BudgetRepositoryImpl
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.CategoryRepositoryImpl
import com.piecer.app.data.repository.GoalRepository
import com.piecer.app.data.repository.GoalRepositoryImpl
import com.piecer.app.data.repository.PieceRepository
import com.piecer.app.data.repository.PieceRepositoryImpl
import com.piecer.app.data.repository.RecurringRepository
import com.piecer.app.data.repository.RecurringRepositoryImpl
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.data.repository.TransactionRepositoryImpl
import com.piecer.app.data.repository.WalletRepository
import com.piecer.app.data.repository.WalletRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWalletRepository(impl: WalletRepositoryImpl): WalletRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    @Singleton
    abstract fun bindPieceRepository(impl: PieceRepositoryImpl): PieceRepository

    @Binds
    @Singleton
    abstract fun bindRecurringRepository(impl: RecurringRepositoryImpl): RecurringRepository
}
