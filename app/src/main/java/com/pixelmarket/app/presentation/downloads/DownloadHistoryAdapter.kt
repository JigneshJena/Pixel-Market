package com.pixelmarket.app.presentation.downloads

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.pixelmarket.app.databinding.ItemDownloadHistoryBinding
import com.pixelmarket.app.domain.model.Download
import java.text.SimpleDateFormat
import java.util.*

class DownloadHistoryAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<Download, DownloadHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemDownloadHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(download: Download) {
            binding.tvTitle.text = download.assetTitle
            binding.tvSeller.text = "by ${download.sellerName}"
            binding.tvPrice.text = "$${String.format("%.2f", download.price)}"
            binding.tvFileSize.text = download.fileSize
            binding.tvDownloadDate.text = formatDate(download.downloadDate.toDate())
            binding.ivThumbnail.load(download.thumbnailUrl)
            
            binding.root.setOnClickListener {
                onItemClick(download.assetId)
            }
        }

        private fun formatDate(date: Date): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            return sdf.format(date)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Download>() {
        override fun areItemsTheSame(oldItem: Download, newItem: Download): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Download, newItem: Download): Boolean {
            return oldItem == newItem
        }
    }
}
