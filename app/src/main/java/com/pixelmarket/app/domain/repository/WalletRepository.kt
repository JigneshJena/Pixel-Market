package com.pixelmarket.app.domain.repository

import com.pixelmarket.app.data.model.Transaction
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    suspend fun getWalletBalance(userId: String): Double
    suspend fun addMoney(userId: String, amount: Double, transactionId: String): Result<Unit>
    suspend fun deductMoney(userId: String, amount: Double, description: String): Result<Unit>
    suspend fun getTransactions(userId: String): Flow<List<Transaction>>
    suspend fun recordTransaction(transaction: Transaction): Result<Unit>
}
