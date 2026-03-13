package com.pixelmarket.app.domain.repository

import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    /** Add an asset to the user's cart. Idempotent — if already in cart, no-op. */
    suspend fun addToCart(userId: String, asset: Asset): Result<Unit>

    /** Remove a specific asset from the cart. */
    suspend fun removeFromCart(userId: String, assetId: String): Result<Unit>

    /** Real-time stream of cart items for the given user. */
    fun getCartItems(userId: String): Flow<List<CartItem>>

    /** Check if an asset is already in the cart. */
    suspend fun isInCart(userId: String, assetId: String): Boolean

    /** Clear all items from the cart (e.g. after checkout). */
    suspend fun clearCart(userId: String): Result<Unit>
}
