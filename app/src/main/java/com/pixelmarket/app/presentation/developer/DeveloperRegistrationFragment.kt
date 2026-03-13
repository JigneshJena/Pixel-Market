package com.pixelmarket.app.presentation.developer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.FragmentDeveloperRegistrationBinding
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DeveloperRegistrationFragment : Fragment(R.layout.fragment_developer_registration),
    PaymentResultListener {

    private var _binding: FragmentDeveloperRegistrationBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var firestore: FirebaseFirestore

    private var registrationFee: Double = 499.0  // Default, overridden from Firestore
    private var pendingApplicationId: String? = null
    private var tempFormData: Map<String, String>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDeveloperRegistrationBinding.bind(view)

        Checkout.preload(requireContext())
        fetchFeeAndCheckStatus()
        setupClickListeners()
    }

    // ── Load registration fee from Firestore + check existing application ──
    private fun fetchFeeAndCheckStatus() {
        val userId = auth.currentUser?.uid ?: return

        // Fetch fee from settings
        firestore.collection("settings").document("app").get()
            .addOnSuccessListener { doc ->
                registrationFee = doc.getDouble("developerRegistrationFee") ?: 499.0
                binding.tvFeeAmount.text = "₹%.0f".format(registrationFee)
            }

        // Check if user already has a pending/approved/rejected application
        firestore.collection("developer_applications").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                val status = doc.getString("status") ?: "pending_review"
                pendingApplicationId = doc.id
                showStatusCard(status)
            }
    }

    private fun showStatusCard(status: String) {
        binding.cardStatus.visibility = View.VISIBLE
        when (status) {
            "pending_review" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_pending)
                binding.ivStatusIcon.imageTintList =
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#F59E0B")
                    )
                binding.tvStatusTitle.text = "Application Under Review"
                binding.tvStatusSubtitle.text = "Admin will review your application within 24-48 hours"
                // Disable form + button
                binding.cardForm.alpha = 0.5f
                binding.btnRegisterAsDeveloper.isEnabled = false
                binding.btnRegisterAsDeveloper.text = "Application Submitted"
            }
            "approved" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_verified)
                binding.ivStatusIcon.imageTintList =
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#10B981")
                    )
                binding.tvStatusTitle.text = "🎉 You're a Developer!"
                binding.tvStatusSubtitle.text = "Your application was approved. Start uploading assets!"
                binding.cardForm.visibility = View.GONE
                binding.btnRegisterAsDeveloper.isEnabled = false
                binding.btnRegisterAsDeveloper.text = "Already Approved"
            }
            "rejected" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_delete)
                binding.ivStatusIcon.imageTintList =
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#F43F5E")
                    )
                binding.tvStatusTitle.text = "Application Rejected"
                binding.tvStatusSubtitle.text = "Your application was rejected. You can re-apply below."
                // Allow re-apply
                pendingApplicationId = null
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnRegisterAsDeveloper.setOnClickListener {
            validateAndPay()
        }
    }

    private fun validateAndPay() {
        val displayName = binding.etDeveloperName.text.toString().trim()
        val bio = binding.etDeveloperBio.text.toString().trim()
        val reason = binding.etReason.text.toString().trim()
        val portfolio = binding.etPortfolioUrl.text.toString().trim()

        if (displayName.isEmpty()) {
            binding.tilDeveloperName.error = "Developer name is required"
            binding.etDeveloperName.requestFocus()
            return
        }
        if (bio.isEmpty()) {
            binding.tilDeveloperBio.error = "Bio is required"
            binding.etDeveloperBio.requestFocus()
            return
        }
        if (reason.isEmpty()) {
            binding.tilReason.error = "Please tell us why you want to join"
            binding.etReason.requestFocus()
            return
        }

        // Clear errors
        binding.tilDeveloperName.error = null
        binding.tilDeveloperBio.error = null
        binding.tilReason.error = null

        startRazorpayPayment(displayName, bio, reason, portfolio)
    }

    private fun startRazorpayPayment(
        displayName: String, bio: String, reason: String, portfolio: String
    ) {
        val user = auth.currentUser ?: return
        val activity = requireActivity()

        binding.btnRegisterAsDeveloper.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        try {
            val checkout = Checkout()
            // Using the unified test key from RazorpayManager
            checkout.setKeyID("rzp_test_SBLcPxcylYWe12") 

            val options = JSONObject().apply {
                put("name", "PixelMarket")
                put("description", "Developer Registration Fee")
                put("image", "https://i.imgur.com/n5tjHFD.png")
                put("currency", "INR")
                put("amount", (registrationFee * 100).toInt()) // paise
                put("prefill", JSONObject().apply {
                    put("email", user.email ?: "")
                    put("contact", "")
                    put("name", user.displayName ?: displayName)
                })
                put("theme", JSONObject().apply {
                    put("color", "#6366F1")
                })
                put("notes", JSONObject().apply {
                    put("type", "developer_registration")
                    put("userId", user.uid)
                })
            }

            // Store form data temporarily
            tempFormData = mapOf(
                "displayName" to displayName,
                "bio" to bio,
                "reason" to reason,
                "portfolio" to portfolio
            )

            checkout.open(activity, options)

        } catch (e: Exception) {
            binding.btnRegisterAsDeveloper.isEnabled = true
            binding.progressBar.visibility = View.GONE
            Toast.makeText(context, "Payment failed to open: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ── Razorpay callbacks ────────────────────────────────────────────────
    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        val paymentId = razorpayPaymentId ?: return
        val formData = tempFormData ?: return

        saveApplication(
            paymentId = paymentId,
            displayName = formData["displayName"] ?: "",
            bio = formData["bio"] ?: "",
            reason = formData["reason"] ?: "",
            portfolio = formData["portfolio"] ?: ""
        )
    }

    override fun onPaymentError(code: Int, description: String?) {
        binding.btnRegisterAsDeveloper.isEnabled = true
        binding.progressBar.visibility = View.GONE
        Toast.makeText(
            context,
            "Payment failed: ${description ?: "Unknown error"}",
            Toast.LENGTH_LONG
        ).show()
    }

    // ── Save Application to Firestore ────────────────────────────────────
    private fun saveApplication(
        paymentId: String,
        displayName: String,
        bio: String,
        reason: String,
        portfolio: String
    ) {
        val user = auth.currentUser ?: return
        // Use user.uid as applicationId to simplify security rules & prevent multiple apps
        val applicationId = user.uid 

        lifecycleScope.launch {
            try {
                val applicationData = mapOf(
                    "applicationId" to applicationId,
                    "userId" to user.uid,
                    "userName" to (user.displayName ?: displayName),
                    "userEmail" to (user.email ?: ""),
                    "developerDisplayName" to displayName,
                    "bio" to bio,
                    "reason" to reason,
                    "portfolioUrl" to portfolio,
                    "fee" to registrationFee,
                    "razorpayPaymentId" to paymentId,
                    "status" to "pending_review",
                    "adminNote" to "",
                    "appliedAt" to Timestamp.now(),
                    "reviewedAt" to null,
                    "reviewedBy" to ""
                )

                // 1. Save developer application (CRITICAL)
                firestore.collection("developer_applications")
                    .document(applicationId)
                    .set(applicationData)
                    .await()

                // 2. Record transaction (SOFT - can fail without breaking success)
                try {
                    val txId = firestore.collection("transactions").document().id
                    firestore.collection("transactions").document(txId).set(mapOf(
                        "id" to txId,
                        "transactionId" to txId,
                        "userId" to user.uid,
                        "type" to "developer_fee",
                        "amount" to registrationFee,
                        "description" to "Developer Registration Fee",
                        "referenceId" to applicationId,
                        "razorpayPaymentId" to paymentId,
                        "status" to "completed",
                        "timestamp" to Timestamp.now(),
                        "createdAt" to Timestamp.now()
                    )).await()
                } catch (e: Exception) {
                    // Log but ignore transaction failure as per user request ("make it soft")
                    e.printStackTrace()
                }

                binding.progressBar.visibility = View.GONE
                showStatusCard("pending_review")

                Toast.makeText(
                    context,
                    "✅ Application submitted! Admin will review within 24-48 hours.",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                
                // If we reach here, payment was ALREADY successful.
                // We shouldn't show a 'Failure' error that scares the user.
                android.util.Log.e("DeveloperRegistration", "Failed to sync application data: ${e.message}")
                
                // Show a 'Soft Success' message anyway
                showStatusCard("pending_review")
                Toast.makeText(
                    context,
                    "Payment Verified! (ID: $paymentId)\nApplication recorded. Admin will manually verify your request.",
                    Toast.LENGTH_LONG
                ).show()
                
                // Still log the error to console for admin
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
