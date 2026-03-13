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
    private val onDeleteClick: (String) -> Unit
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
            
            binding.btnEdit.setOnClickListener { onEditClick(user) }
            binding.btnDelete.setOnClickListener { onDeleteClick(user.uid) }
            
            // Hide delete/edit if it's the admin itself
            binding.btnDelete.visibility = if (user.role == "admin") View.GONE else View.VISIBLE
            binding.btnEdit.visibility = if (user.role == "admin") View.GONE else View.VISIBLE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}
