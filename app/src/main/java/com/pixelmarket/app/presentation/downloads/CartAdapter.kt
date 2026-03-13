package com.pixelmarket.app.presentation.downloads

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.ItemCartAssetBinding
import com.pixelmarket.app.domain.model.CartItem

class CartAdapter(
    private val onItemClick: (String) -> Unit,
    private val onRemoveClick: (String) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartAssetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(private val binding: ItemCartAssetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            binding.tvTitle.text = item.title
            binding.tvPrice.text = if (item.price > 0) "₹${item.price}" else "FREE"
            
            binding.ivThumbnail.load(item.thumbnailUrl) {
                crossfade(true)
                placeholder(R.color.glass_surface)
            }

            binding.root.setOnClickListener {
                onItemClick(item.assetId)
            }

            binding.btnRemove.setOnClickListener {
                onRemoveClick(item.assetId)
            }
        }
    }

    class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.cartItemId == newItem.cartItemId
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}
