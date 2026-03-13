package com.pixelmarket.app.presentation.admin

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pixelmarket.app.databinding.ItemDeveloperApplicationBinding
import com.pixelmarket.app.domain.model.DeveloperApplication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeveloperApplicationAdapter(
    private val onApprove: (DeveloperApplication) -> Unit,
    private val onReject: (DeveloperApplication) -> Unit
) : ListAdapter<DeveloperApplication, DeveloperApplicationAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<DeveloperApplication>() {
            override fun areItemsTheSame(a: DeveloperApplication, b: DeveloperApplication) =
                a.applicationId == b.applicationId
            override fun areContentsTheSame(a: DeveloperApplication, b: DeveloperApplication) =
                a == b
        }
    }

    inner class ViewHolder(private val binding: ItemDeveloperApplicationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: DeveloperApplication) {
            binding.tvApplicantName.text = app.userName
            binding.tvApplicantEmail.text = app.userEmail
            binding.tvDevDisplayName.text = "Studio: ${app.developerDisplayName}"
            binding.tvReasonSnippet.text = if (app.reason.isNotEmpty()) app.reason else app.bio
            binding.tvFeeAmount.text = "Fee Paid: ₹%.0f".format(app.fee)

            // Date
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.tvAppliedDate.text = sdf.format(Date(app.appliedAt.seconds * 1000))

            // Status
            when (app.status) {
                "pending_review" -> {
                    binding.tvStatus.text = "Pending"
                    binding.tvStatus.setTextColor(Color.parseColor("#F59E0B"))
                    binding.tvStatus.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#1FF59E0B"))
                    binding.llActions.visibility = View.VISIBLE
                }
                "approved" -> {
                    binding.tvStatus.text = "Approved"
                    binding.tvStatus.setTextColor(Color.parseColor("#10B981"))
                    binding.tvStatus.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#1010B981"))
                    binding.llActions.visibility = View.GONE
                }
                "rejected" -> {
                    binding.tvStatus.text = "Rejected"
                    binding.tvStatus.setTextColor(Color.parseColor("#F43F5E"))
                    binding.tvStatus.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#1FF43F5E"))
                    binding.llActions.visibility = View.GONE
                }
                else -> {
                    binding.tvStatus.text = app.status
                    binding.llActions.visibility = View.GONE
                }
            }

            binding.btnApprove.setOnClickListener { onApprove(app) }
            binding.btnReject.setOnClickListener { onReject(app) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeveloperApplicationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
