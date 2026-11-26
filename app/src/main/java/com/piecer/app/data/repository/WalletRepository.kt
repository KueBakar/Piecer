package com.piecer.app.data.repository

import com.piecer.app.data.local.dao.TransactionDao
import com.piecer.app.data.local.dao.WalletDao
import com.piecer.app.data.local.dao.WalletWithBalance
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.util.calculateWalletBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface WalletRepository {
    fun observeAll(): Flow<List<WalletEntity>>
    fun observeById(id: Long): Flow<WalletEntity?>
    fun observeWalletsWithBalance(): Flow<List<WalletWithBalance>>
    fun observeWalletWithBalance(walletId: Long): Flow<WalletWithBalance?>
    suspend fun insert(wallet: WalletEntity): Long
    suspend fun update(wallet: WalletEntity)
    suspend fun delete(wallet: WalletEntity)
}

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao
) : WalletRepository {
    override fun observeAll(): Flow<List<WalletEntity>> = walletDao.observeAll()

    override fun observeById(id: Long): Flow<WalletEntity?> = walletDao.observeById(id)

    override fun observeWalletsWithBalance(): Flow<List<WalletWithBalance>> =
        combine(walletDao.observeAll(), transactionDao.observeAll()) { wallets, transactions ->
            val now = System.currentTimeMillis()
            wallets.map { w ->
                WalletWithBalance(
                    walletId = w.id,
                    name = w.name,
                    type = w.type,
                    initialBalance = w.initialBalance,
                    iconKey = w.iconKey,
                    colorKey = w.colorKey,
                    createdAt = w.createdAt,
                    currentBalance = calculateWalletBalance(w.id, w.initialBalance, transactions, now)
                )
            }
        }

    override fun observeWalletWithBalance(walletId: Long): Flow<WalletWithBalance?> =
        observeWalletsWithBalance().map { list -> list.firstOrNull { it.walletId == walletId } }

    override suspend fun insert(wallet: WalletEntity): Long = walletDao.insert(wallet)

    override suspend fun update(wallet: WalletEntity) = walletDao.update(wallet)

    override suspend fun delete(wallet: WalletEntity) {
        transactionDao.deleteByWalletId(wallet.id)
        walletDao.delete(wallet)
    }
}
