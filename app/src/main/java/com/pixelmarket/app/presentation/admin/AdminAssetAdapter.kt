package com.pixelmarket.app.presentation.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.pixelmarket.app.databinding.ItemAdminAssetBinding
import com.pixelmarket.app.domain.model.Asset

class AdminAssetAdapter(
    private val onApproveToggle: (String, Boolean) -> Unit,
    private val onFeaturedToggle: (String, Boolean) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<Asset, AdminAssetAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminAssetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val asset = getItem(position)
        holder.bind(asset)
    }

    inner class ViewHolder(private val binding: ItemAdminAssetBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(asset: Asset) {
            binding.tvAssetTitle.text = asset.title
            binding.tvSellerName.text = "by ${asset.sellerName}"
            binding.tvPrice.text = if (asset.price == 0.0) "Free" else "₹${asset.price}"
            binding.tvCategoryStat.text = asset.category
            binding.tvDownloadCount.text = asset.downloadCount.toString()
            binding.tvFileType.text = asset.fileType.uppercase()
            binding.tvAssetId.text = "ID: ${asset.id}"
            
            binding.ivAssetThumbnail.load(asset.thumbnailUrl) {
                crossfade(true)
                placeholder(com.pixelmarket.app.R.drawable.ic_image_placeholder)
                error(com.pixelmarket.app.R.drawable.ic_image_placeholder)
            }
            
            binding.switchApproval.isChecked = asset.approved
            binding.btnFeatured.text = if (asset.featured) "Unfeature" else "Feature"
            
            binding.switchApproval.setOnCheckedChangeListener { _, isChecked ->
                onApproveToggle(asset.id, isChecked)
            }
            
            binding.btnFeatured.setOnClickListener {
                onFeaturedToggle(asset.id, !asset.featured)
            }
            
            binding.btnDeleteAsset.setOnClickListener {
                onDeleteClick(asset.id)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Asset>() {
        override fun areItemsTheSame(oldItem: Asset, newItem: Asset) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Asset, newItem: Asset) = oldItem == newItem
    }
}
