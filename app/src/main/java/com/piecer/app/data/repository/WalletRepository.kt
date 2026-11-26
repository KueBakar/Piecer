package com.piecer.app.data.repository

import com.piecer.app.data.local.dao.WalletDao
import com.piecer.app.data.local.dao.WalletWithBalance
import com.piecer.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow
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
    private val walletDao: WalletDao
) : WalletRepository {
    override fun observeAll(): Flow<List<WalletEntity>> = walletDao.observeAll()

    override fun observeById(id: Long): Flow<WalletEntity?> = walletDao.observeById(id)

    override fun observeWalletsWithBalance(): Flow<List<WalletWithBalance>> =
        walletDao.observeWalletsWithBalance()

    override fun observeWalletWithBalance(walletId: Long): Flow<WalletWithBalance?> =
        walletDao.observeWalletWithBalance(walletId)

    override suspend fun insert(wallet: WalletEntity): Long = walletDao.insert(wallet)

    override suspend fun update(wallet: WalletEntity) = walletDao.update(wallet)

    override suspend fun delete(wallet: WalletEntity) = walletDao.delete(wallet)
}
