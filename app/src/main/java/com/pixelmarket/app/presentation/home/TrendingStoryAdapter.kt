package com.pixelmarket.app.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.ItemTrendingStoryBinding
import com.pixelmarket.app.domain.model.Asset

class TrendingStoryAdapter(
    private val onAssetClick: (Asset) -> Unit
) : ListAdapter<Asset, TrendingStoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrendingStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTrendingStoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(asset: Asset) {
            binding.ivThumbnail.load(asset.thumbnailUrl) {
                placeholder(R.drawable.ic_assets)
                error(R.drawable.ic_assets)
                crossfade(true)
            }
            
            binding.tvTitle.text = asset.title
            
            binding.root.setOnClickListener {
                onAssetClick(asset)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Asset>() {
        override fun areItemsTheSame(oldItem: Asset, newItem: Asset) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Asset, newItem: Asset) = oldItem == newItem
    }
}
