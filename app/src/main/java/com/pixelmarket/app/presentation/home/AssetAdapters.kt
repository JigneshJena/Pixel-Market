package com.pixelmarket.app.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.pixelmarket.app.databinding.ItemAssetFeaturedBinding
import com.pixelmarket.app.databinding.ItemAssetSmallBinding
import com.pixelmarket.app.databinding.ItemAssetMarketplaceBinding
import com.pixelmarket.app.databinding.ItemAssetBinding
import com.pixelmarket.app.domain.model.Asset

class FeaturedAssetAdapter(private val onItemClick: (String) -> Unit) :
    ListAdapter<Asset, FeaturedAssetAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAssetFeaturedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAssetFeaturedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(asset: Asset) {
            binding.tvTitle.text = asset.title
            binding.tvPrice.text = "₹${asset.price}"
            binding.ivThumbnail.load(asset.thumbnailUrl) { crossfade(true) }
            binding.root.setOnClickListener { onItemClick(asset.id) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Asset>() {
        override fun areItemsTheSame(oldItem: Asset, newItem: Asset) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Asset, newItem: Asset) = oldItem == newItem
    }
}

/**
 * Adapter for small horizontal asset cards (item_asset_small.xml)
 * Used in the Home screen "New Drops" row.
 * The "Add to Cart" button navigates to asset details — same as card click.
 */
class AssetAdapter(private val onItemClick: (String) -> Unit) :
    ListAdapter<Asset, AssetAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAssetSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAssetSmallBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(asset: Asset) {
            binding.tvTitle.text = asset.title
            binding.tvPrice.text = if (asset.price == 0.0) "Free" else "₹${asset.price.toInt()}"
            binding.ivThumbnail.load(asset.thumbnailUrl) {
                crossfade(true)
                placeholder(com.pixelmarket.app.R.drawable.ic_image_placeholder)
                error(com.pixelmarket.app.R.drawable.ic_image_placeholder)
            }
            // Full card click → details
            binding.root.setOnClickListener { onItemClick(asset.id) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Asset>() {
        override fun areItemsTheSame(oldItem: Asset, newItem: Asset) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Asset, newItem: Asset) = oldItem == newItem
    }
}

/**
 * Adapter for marketplace grid cards (item_asset_marketplace.xml)
 * Used in Home trending row and Marketplace grid.
 * The "Add to Cart" button navigates to asset details for purchase.
 */
class AssetGridAdapter(private val onItemClick: (String) -> Unit) :
    ListAdapter<Asset, AssetGridAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAssetMarketplaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAssetMarketplaceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(asset: Asset) {
            binding.tvTitle.text = asset.title
            binding.tvCategory.text = asset.category
            binding.tvPrice.text = if (asset.price == 0.0) "Free" else "₹${asset.price.toInt()}"
            binding.ivThumbnail.load(asset.thumbnailUrl) {
                crossfade(true)
                placeholder(com.pixelmarket.app.R.drawable.ic_image_placeholder)
                error(com.pixelmarket.app.R.drawable.ic_image_placeholder)
            }
            // Full card click → details
            binding.root.setOnClickListener { onItemClick(asset.id) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Asset>() {
        override fun areItemsTheSame(oldItem: Asset, newItem: Asset) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Asset, newItem: Asset) = oldItem == newItem
    }
}

/**
 * Adapter for full-size asset cards (item_asset.xml)
 * The "Add to Cart" button navigates to asset details for purchase.
 */
class FullAssetAdapter(private val onItemClick: (String) -> Unit) :
    ListAdapter<Asset, FullAssetAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAssetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAssetBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(asset: Asset) {
            binding.tvTitle.text = asset.title
            binding.tvPrice.text = if (asset.price == 0.0) "Free" else "₹${asset.price.toInt()}"
            binding.tvRating.text = String.format("%.1f", asset.rating)
            binding.tvSellerName.text = asset.sellerName
            binding.ivThumbnail.load(asset.thumbnailUrl) {
                crossfade(true)
                placeholder(com.pixelmarket.app.R.drawable.ic_image_placeholder)
                error(com.pixelmarket.app.R.drawable.ic_image_placeholder)
            }
            binding.root.setOnClickListener { onItemClick(asset.id) }
            binding.btnAddToCart.setOnClickListener { onItemClick(asset.id) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Asset>() {
        override fun areItemsTheSame(oldItem: Asset, newItem: Asset) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Asset, newItem: Asset) = oldItem == newItem
    }
}
