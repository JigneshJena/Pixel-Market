package com.pixelmarket.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.model.CartItem
import com.pixelmarket.app.domain.repository.CartRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CartRepository {

    /** Firestore path: users/{userId}/cart/{assetId} */
    private fun cartRef(userId: String) =
        firestore.collection("users").document(userId).collection("cart")

    override suspend fun addToCart(userId: String, asset: Asset): Result<Unit> = runCatching {
        if (userId.isEmpty()) throw Exception("User is not logged in")

        val itemMap = hashMapOf(
            "cartItemId" to asset.id,
            "assetId" to asset.id,
            "title" to asset.title,
            "thumbnailUrl" to asset.thumbnailUrl,
            "price" to asset.price,
            "sellerId" to asset.sellerId,
            "sellerName" to asset.sellerName,
            "category" to asset.category,
            "addedAt" to com.google.firebase.Timestamp.now()
        )

        cartRef(userId).document(asset.id).set(itemMap, SetOptions.merge()).await()
    }

    override suspend fun removeFromCart(userId: String, assetId: String): Result<Unit> = runCatching {
        cartRef(userId).document(assetId).delete().await()
    }

    override fun getCartItems(userId: String): Flow<List<CartItem>> = callbackFlow {
        val listener = cartRef(userId)
            .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CartItem::class.java)?.copy(cartItemId = doc.id)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun isInCart(userId: String, assetId: String): Boolean {
        return try {
            cartRef(userId).document(assetId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun clearCart(userId: String): Result<Unit> = runCatching {
        val batch = firestore.batch()
        val docs = cartRef(userId).get().await().documents
        docs.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }
}
