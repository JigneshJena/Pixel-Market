package com.pixelmarket.app.presentation.wallet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pixelmarket.app.R
import com.pixelmarket.app.data.model.Transaction
import com.pixelmarket.app.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Transaction>() {
            override fun areItemsTheSame(a: Transaction, b: Transaction) = a.id == b.id
            override fun areContentsTheSame(a: Transaction, b: Transaction) = a == b
        }

        // Types that mean money came IN to the user
        private val CREDIT_TYPES = setOf("topup", "earning", "CREDIT", "developer_fee_received", "refund")
    }

    inner class VH(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(txn: Transaction) {
            val isCredit = txn.type.lowercase() in CREDIT_TYPES.map { it.lowercase() } ||
                           txn.type.uppercase() == "CREDIT"

            // Amount text + color
            val sign = if (isCredit) "+" else "-"
            binding.tvAmount.text = "$sign₹${String.format("%.2f", txn.amount)}"
            binding.tvAmount.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (isCredit) R.color.emerald_500 else R.color.rose_500
                )
            )

            // Description
            binding.tvDescription.text = txn.description.ifBlank {
                when (txn.type.lowercase()) {
                    "topup"   -> "Wallet Top-up"
                    "purchase" -> "Asset Purchase"
                    "earning", "developer_fee_received" -> "Sale Earning"
                    "withdrawal" -> "Withdrawal"
                    "debit", "debit".uppercase() -> "Debit"
                    else -> txn.type.replaceFirstChar { it.uppercase() }
                }
            }

            // Date
            try {
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                binding.tvDate.text = sdf.format(txn.timestamp.toDate())
            } catch (e: Exception) {
                binding.tvDate.text = ""
            }

            // Icon
            val iconRes = when {
                isCredit -> R.drawable.ic_add
                txn.type.lowercase() == "purchase" -> R.drawable.ic_shopping_cart
                txn.type.lowercase() == "withdrawal" -> R.drawable.ic_payouts
                else -> R.drawable.ic_wallet
            }
            binding.ivType.setImageResource(iconRes)
            binding.ivType.setColorFilter(
                ContextCompat.getColor(
                    binding.root.context,
                    if (isCredit) R.color.emerald_500 else R.color.primary
                )
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
