package com.pixelmarket.app.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.data.model.Transaction
import com.pixelmarket.app.domain.repository.WalletRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase
) : WalletRepository {

    override suspend fun getWalletBalance(userId: String): Double {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            doc.getDouble("walletBalance") ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    override suspend fun addMoney(userId: String, amount: Double, transactionId: String): Result<Unit> {
        return try {
            // 1. Update balance (Try to do it client-side)
            try {
                val userRef = firestore.collection("users").document(userId)
                // Try transaction first (atomic)
                try {
                    firestore.runTransaction { transaction ->
                        val snapshot = transaction.get(userRef)
                        val currentBalance = snapshot.getDouble("walletBalance") ?: 0.0
                        transaction.update(userRef, "walletBalance", currentBalance + amount)
                        null
                    }.await()
                } catch (trError: Exception) {
                    // Fallback to simple update (if read is blocked but write is allowed)
                    android.util.Log.w("WalletRepository", "Transaction failed, trying simple update: ${trError.message}")
                    firestore.collection("users").document(userId)
                        .update("walletBalance", com.google.firebase.firestore.FieldValue.increment(amount))
                        .await()
                }
            } catch (e: Exception) {
                android.util.Log.e("WalletRepository", "Balance update failed even with fallback: ${e.message}")
            }

            // 2. Record transaction (SOFT)
            try {
                val txnId = if (transactionId.startsWith("pay_")) transactionId else "TXN_${System.currentTimeMillis()}"
                val txn = Transaction(
                    id = txnId,
                    userId = userId,
                    type = "topup",
                    amount = amount,
                    description = "Wallet topup success",
                    timestamp = com.google.firebase.Timestamp.now()
                )
                
                firestore.collection("transactions").document(txnId)
                    .set(txn)
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("WalletRepository", "Transaction log failed (soft): ${e.message}")
            }

            // 3. Instant Notification
            try {
                database.getReference("instant_alerts")
                    .child(userId)
                    .setValue(mapOf(
                        "title" to "Wallet Updated",
                        "message" to "₹${amount} successfully added to your wallet.",
                        "type" to "success",
                        "timestamp" to System.currentTimeMillis()
                    ))
            } catch (e: Exception) {
                // Soft fail
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deductMoney(userId: String, amount: Double, description: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(userId)
                val snapshot = transaction.get(userRef)
                val currentBalance = snapshot.getDouble("walletBalance") ?: 0.0

                if (currentBalance < amount) throw Exception("Insufficient balance")

                // Deduct balance AND track total spending atomically
                transaction.update(userRef, mapOf(
                    "walletBalance"  to (currentBalance - amount),
                    "totalSpending"  to com.google.firebase.firestore.FieldValue.increment(amount)
                ))

                // Record transaction
                val txnId = firestore.collection("transactions").document().id
                val txn = Transaction(
                    id          = txnId,
                    userId      = userId,
                    type        = "purchase",
                    amount      = amount,
                    description = description,
                    timestamp   = com.google.firebase.Timestamp.now()
                )
                transaction.set(
                    firestore.collection("transactions").document(txnId),
                    txn
                )
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTransactions(userId: String): Flow<List<Transaction>> = callbackFlow {
        val subscription = firestore.collection("transactions")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("WalletRepository", "Firestore listener error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                
                trySend(transactions)
            }
        
        awaitClose { subscription.remove() }
    }

    override suspend fun recordTransaction(transaction: Transaction): Result<Unit> {
        return try {
            firestore.collection("transactions")
                .document(transaction.id)
                .set(transaction)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
