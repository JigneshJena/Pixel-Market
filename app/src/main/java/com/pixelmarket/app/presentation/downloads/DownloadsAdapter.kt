package com.pixelmarket.app.presentation.downloads

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pixelmarket.app.databinding.ItemDownloadBinding

class DownloadsAdapter(
    private val onCancel: (Long) -> Unit
) : ListAdapter<DownloadItem, DownloadsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onCancel)
    }

    class ViewHolder(private val binding: ItemDownloadBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DownloadItem, onCancel: (Long) -> Unit) {
            binding.tvAssetName.text = item.assetTitle
            binding.downloadProgress.progress = item.progress
            binding.downloadProgress.isVisible = !item.isComplete && !item.isFailed
            
            binding.tvDownloadStatus.text = when {
                item.isComplete -> "Completed"
                item.isFailed -> "Failed"
                else -> "${item.progress}%"
            }

            binding.btnCancelDownload.setOnClickListener {
                onCancel(item.downloadId)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem) = oldItem.downloadId == newItem.downloadId
        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem) = oldItem == newItem
    }
}
