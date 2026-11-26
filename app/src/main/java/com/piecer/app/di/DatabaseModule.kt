package com.piecer.app.di

import android.content.Context
import androidx.room.Room
import com.piecer.app.data.local.PiecerDatabase
import com.piecer.app.data.local.dao.CategoryDao
import com.piecer.app.data.local.dao.TransactionDao
import com.piecer.app.data.local.dao.WalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePiecerDatabase(
        @ApplicationContext context: Context
    ): PiecerDatabase {
        return Room.databaseBuilder(
            context,
            PiecerDatabase::class.java,
            PiecerDatabase.DATABASE_NAME
        )
            .addCallback(PiecerDatabase.createSeedCallback())
            .build()
    }

    @Provides
    fun provideWalletDao(database: PiecerDatabase): WalletDao = database.walletDao()

    @Provides
    fun provideCategoryDao(database: PiecerDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: PiecerDatabase): TransactionDao = database.transactionDao()
}
