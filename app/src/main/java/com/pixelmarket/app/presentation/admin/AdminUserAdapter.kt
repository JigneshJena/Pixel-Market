package com.pixelmarket.app.presentation.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.pixelmarket.app.databinding.ItemAdminUserBinding
import com.pixelmarket.app.domain.model.User
import java.text.SimpleDateFormat
import java.util.Locale

class AdminUserAdapter(
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (String) -> Unit,
    private val onToggleStatusClick: (User) -> Unit
) : ListAdapter<User, AdminUserAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    inner class ViewHolder(private val binding: ItemAdminUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvUsername.text = user.username
            binding.tvEmail.text = user.email
            binding.tvRoleDetail.text = user.role.replaceFirstChar { it.uppercase() }
            binding.tvPurchasesCount.text = user.totalPurchases.toString()
            binding.tvJoinedDate.text = dateFormat.format(user.createdAt.toDate())
            binding.tvLastLoginDate.text = dateFormat.format(user.lastLoginAt.toDate())
            binding.tvUid.text = "UID: ${user.uid}"
            
            binding.ivUserImage.load(user.profileImageUrl) {
                placeholder(com.pixelmarket.app.R.drawable.ic_profile)
                error(com.pixelmarket.app.R.drawable.ic_profile)
            }
            
            // Handle active/inactive status visuals
            if (user.isActive) {
                binding.btnToggleStatus.setImageResource(com.pixelmarket.app.R.drawable.ic_lock) // Or perhaps an unlock icon, up to you. lock normally means "lock this user"
                binding.btnToggleStatus.setColorFilter(android.graphics.Color.parseColor("#FFA000")) // Warning color
                binding.root.alpha = 1.0f
            } else {
                binding.btnToggleStatus.setImageResource(com.pixelmarket.app.R.drawable.ic_verified) // Some generic indicator for enabling
                binding.btnToggleStatus.setColorFilter(android.graphics.Color.parseColor("#388E3C")) // Green
                binding.root.alpha = 0.6f // Disable visually
            }

            binding.btnEdit.setOnClickListener { onEditClick(user) }
            binding.btnDelete.setOnClickListener { onDeleteClick(user.uid) }
            binding.btnToggleStatus.setOnClickListener { onToggleStatusClick(user) }
            
            // Hide delete/edit/status if it's the admin itself
            val isEditingSelf = user.role == "admin"
            binding.btnDelete.visibility = if (isEditingSelf) View.GONE else View.VISIBLE
            binding.btnEdit.visibility = if (isEditingSelf) View.GONE else View.VISIBLE
            binding.btnToggleStatus.visibility = if (isEditingSelf) View.GONE else View.VISIBLE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}
