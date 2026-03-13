package com.pixelmarket.app.presentation.wallet

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.PaymentResultListener
import com.pixelmarket.app.R
import com.pixelmarket.app.data.payment.RazorpayManager
import com.pixelmarket.app.databinding.FragmentWalletBinding
import com.pixelmarket.app.domain.repository.WalletRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class WalletFragment : Fragment(R.layout.fragment_wallet), PaymentResultListener {

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var walletRepository: WalletRepository
    @Inject lateinit var razorpayManager: RazorpayManager
    @Inject lateinit var firestore: FirebaseFirestore

    private lateinit var transactionAdapter: TransactionAdapter
    private var currentTopUpAmount: Double = 500.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWalletBinding.bind(view)

        setupRecyclerView()
        setupButtons()
        observeWalletData()
    }

    // ──────────────────────────────────────────────────────────────────────
    // Setup
    // ──────────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAddMoney.setOnClickListener { initiateAddMoney() }

        binding.btnWithdraw.setOnClickListener { initiateWithdraw() }

        // Analytics button → show a quick summary bottom sheet / dialog
        binding.btnAnalytics.setOnClickListener { showAnalyticsDialog() }

        // "View All" — currently stays on same screen (all are shown)
        binding.btnViewAll.setOnClickListener {
            Toast.makeText(requireContext(), "Showing all transactions", Toast.LENGTH_SHORT).show()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Live data
    // ──────────────────────────────────────────────────────────────────────

    private fun observeWalletData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // ── 1. Real-time balance + available for withdrawal from users doc ──
        firestore.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                val b = _binding ?: return@addSnapshotListener

                val balance  = snap.getDouble("walletBalance") ?: 0.0
                val earnings = snap.getDouble("availableBalance") ?: 0.0   // developer earnings available

                b.tvBalance.text = "₹${String.format("%.2f", balance)}"

                // "Available for withdrawal" = developer's available balance (if developer)
                // For regular buyers it shows their wallet balance (same thing)
                val isDev = snap.getBoolean("isDeveloper") == true ||
                            snap.getString("role") in listOf("seller","both","admin")
                b.tvAvailableWithdraw.text = if (isDev)
                    "₹${String.format("%.2f", earnings)}"
                else
                    "₹${String.format("%.2f", balance)}"
            }

        // ── 2. Real-time transactions list ──────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            walletRepository.getTransactions(uid).collectLatest { transactions ->
                val b = _binding ?: return@collectLatest

                if (transactions.isEmpty()) {
                    b.rvTransactions.isVisible = false
                    b.tvEmptyTransactions.isVisible = true
                } else {
                    b.rvTransactions.isVisible = true
                    b.tvEmptyTransactions.isVisible = false
                    transactionAdapter.submitList(transactions)
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Add Money (Razorpay)
    // ──────────────────────────────────────────────────────────────────────

    private fun initiateAddMoney() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val inputLayout = com.google.android.material.textfield.TextInputLayout(requireContext()).apply {
            hint = "Amount (Min ₹500)"
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxStrokeWidth = 3
        }
        val input = com.google.android.material.textfield.TextInputEditText(inputLayout.context).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("500")
            background = null
            setPadding(40, 40, 40, 40)
        }
        inputLayout.addView(input)

        val container = android.widget.FrameLayout(requireContext()).apply {
            val params = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = 72; rightMargin = 72; topMargin = 24; bottomMargin = 8 }
            addView(inputLayout, params)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Money to Wallet")
            .setMessage("Enter the amount you want to add.")
            .setView(container)
            .setPositiveButton("Pay via Razorpay") { _, _ ->
                val amount = input.text.toString().toDoubleOrNull() ?: 0.0
                if (amount < 500.0) {
                    Toast.makeText(requireContext(), "Minimum amount is ₹500", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                currentTopUpAmount = amount
                razorpayManager.startWalletPayment(
                    activity  = requireActivity(),
                    amount    = amount,
                    userEmail = user.email ?: "",
                    userPhone = user.phoneNumber ?: "",
                    userName  = user.displayName ?: "User",
                    listener  = this
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onPaymentSuccess(paymentId: String?) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = walletRepository.addMoney(
                user.uid,
                currentTopUpAmount,
                paymentId ?: "WALLET_${System.currentTimeMillis()}"
            )
            if (result.isSuccess) {
                Toast.makeText(
                    requireContext(),
                    "✅ ₹${String.format("%.2f", currentTopUpAmount)} added successfully!",
                    Toast.LENGTH_LONG
                ).show()
                // Balance auto-updates via the snapshot listener above
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to update wallet: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onPaymentError(code: Int, description: String?) {
        Toast.makeText(requireContext(), "Payment failed: $description", Toast.LENGTH_LONG).show()
    }

    // ──────────────────────────────────────────────────────────────────────
    // Withdraw (Developer only — deducts from availableBalance)
    // ──────────────────────────────────────────────────────────────────────

    private fun initiateWithdraw() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        // First fetch current available balance
        viewLifecycleOwner.lifecycleScope.launch {
            val snap = try {
                firestore.collection("users").document(user.uid).get().await()
            } catch (e: Exception) { null }

            val availableBalance = snap?.getDouble("availableBalance") ?: 0.0
            val isDev = snap?.getBoolean("isDeveloper") == true ||
                        snap?.getString("role") in listOf("seller","both","admin")

            if (!isDev) {
                Toast.makeText(requireContext(),
                    "Withdrawal is available for developers only",
                    Toast.LENGTH_LONG).show()
                return@launch
            }

            if (availableBalance < 100.0) {
                Toast.makeText(requireContext(),
                    "Minimum withdrawal is ₹100. Available: ₹${String.format("%.2f", availableBalance)}",
                    Toast.LENGTH_LONG).show()
                return@launch
            }

            // Show withdraw dialog
            val inputLayout = com.google.android.material.textfield.TextInputLayout(requireContext()).apply {
                hint = "Amount (Min ₹100)"
                boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                boxStrokeWidth = 3
            }
            val input = com.google.android.material.textfield.TextInputEditText(inputLayout.context).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(String.format("%.2f", availableBalance))
                background = null
                setPadding(40, 40, 40, 40)
            }
            inputLayout.addView(input)

            val container = android.widget.FrameLayout(requireContext()).apply {
                val params = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { leftMargin = 72; rightMargin = 72; topMargin = 24; bottomMargin = 8 }
                addView(inputLayout, params)
            }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Withdraw Earnings")
                .setMessage("Available: ₹${String.format("%.2f", availableBalance)}\n\nFunds will be transferred to your registered bank account within 2–3 business days.")
                .setView(container)
                .setPositiveButton("Request Withdrawal") { _, _ ->
                    val amount = input.text.toString().toDoubleOrNull() ?: 0.0
                    when {
                        amount < 100.0 -> Toast.makeText(requireContext(), "Minimum withdrawal is ₹100", Toast.LENGTH_SHORT).show()
                        amount > availableBalance -> Toast.makeText(requireContext(), "Amount exceeds available balance", Toast.LENGTH_SHORT).show()
                        else -> processWithdrawal(user.uid, amount)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun processWithdrawal(userId: String, amount: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Deduct from availableBalance + record transaction
                firestore.runTransaction { txn ->
                    val ref  = firestore.collection("users").document(userId)
                    val snap = txn.get(ref)
                    val avail = snap.getDouble("availableBalance") ?: 0.0
                    if (avail < amount) throw Exception("Insufficient available balance")
                    txn.update(ref, mapOf(
                        "availableBalance" to com.google.firebase.firestore.FieldValue.increment(-amount),
                        "totalWithdrawn"   to com.google.firebase.firestore.FieldValue.increment(amount)
                    ))
                }.await()

                // Record withdrawal transaction
                val txnId = firestore.collection("transactions").document().id
                firestore.collection("transactions").document(txnId).set(
                    mapOf(
                        "id"          to txnId,
                        "userId"      to userId,
                        "type"        to "withdrawal",
                        "amount"      to amount,
                        "description" to "Withdrawal request — ₹${String.format("%.2f", amount)}",
                        "status"      to "pending",
                        "timestamp"   to com.google.firebase.Timestamp.now()
                    )
                ).await()

                Toast.makeText(requireContext(),
                    "✅ Withdrawal of ₹${String.format("%.2f", amount)} requested!\nTransfer in 2–3 business days.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Withdrawal failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Analytics Dialog (live summary from transactions)
    // ──────────────────────────────────────────────────────────────────────

    private fun showAnalyticsDialog() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val snap = firestore.collection("users").document(uid).get().await()
                val walletBalance    = snap.getDouble("walletBalance")    ?: 0.0
                val totalEarnings    = snap.getDouble("totalEarnings")    ?: 0.0
                val availableBalance = snap.getDouble("availableBalance") ?: 0.0
                val totalWithdrawn   = snap.getDouble("totalWithdrawn")   ?: 0.0
                val totalSpending    = snap.getDouble("totalSpending")    ?: 0.0
                val totalSales       = snap.getLong("totalSales")?.toInt() ?: 0

                val message = buildString {
                    appendLine("💰  Wallet Balance:  ₹${String.format("%.2f", walletBalance)}")
                    appendLine()
                    appendLine("📈  Total Earned:     ₹${String.format("%.2f", totalEarnings)}")
                    appendLine("✅  Available:        ₹${String.format("%.2f", availableBalance)}")
                    appendLine("🏦  Withdrawn:        ₹${String.format("%.2f", totalWithdrawn)}")
                    appendLine()
                    appendLine("🛒  Total Spent:      ₹${String.format("%.2f", totalSpending)}")
                    appendLine("📦  Total Sales:      $totalSales assets sold")
                }

                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("💹 Wallet Analytics")
                    .setMessage(message.trim())
                    .setPositiveButton("Close", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not load analytics", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
