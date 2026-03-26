package com.pixelmarket.app.presentation.profile

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.FragmentProfileBinding
import com.pixelmarket.app.domain.repository.WalletRepository

import com.pixelmarket.app.util.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import coil.load
import com.pixelmarket.app.domain.model.User

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var auth: FirebaseAuth
    
    @Inject
    lateinit var walletRepository: WalletRepository
    
    @Inject
    lateinit var firestore: FirebaseFirestore
    
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Initial data from Auth
            val email = user.email ?: ""
            val name = user.displayName ?: email.substringBefore("@")
            binding.tvName.text = name
            binding.tvEmail.text = email
            
            // Load full profile data including image
            loadUserProfile(user.uid)
            loadWalletBalance(user.uid)
        }

        val themeManager = ThemeManager(requireContext())
        updateThemeIcon(themeManager.isDarkMode())

        // ── Menu item titles ────────────────────────────────────────────────
        binding.menuAccount.title.text = "Profile Settings"
        binding.menuCart.title.text = "Purchase History"
        binding.menuPurchases.title.text = "My Assets Library"
        binding.menuWallet.title.text = "Wallet & Payments"
        binding.menuDeveloperAssets.title.text = "Manage My Uploads"
        binding.menuDeveloperDashboard.title.text = "Earnings & Stats"

        // ── Set distinct icons per menu item programmatically ───────────────
        // (app:icon in <include> tags doesn't propagate without data binding)
        binding.menuAccount.icon.setImageResource(R.drawable.ic_settings)
        binding.menuCart.icon.setImageResource(R.drawable.ic_analytics)
        binding.menuPurchases.icon.setImageResource(R.drawable.ic_layers)
        binding.menuWallet.icon.setImageResource(R.drawable.ic_wallet)
        binding.menuDeveloperAssets.icon.setImageResource(R.drawable.ic_upload)
        binding.menuDeveloperDashboard.icon.setImageResource(R.drawable.ic_trending_up)

        // ── Tint icon container backgrounds for visual flair ───────────────
        val indigo  = android.graphics.Color.parseColor("#1A6366F1")
        val purple  = android.graphics.Color.parseColor("#1A8B5CF6")
        val emerald = android.graphics.Color.parseColor("#1A10B981")
        val amber   = android.graphics.Color.parseColor("#1AF59E0B")
        val rose    = android.graphics.Color.parseColor("#1AEF4444")
        val sky     = android.graphics.Color.parseColor("#1A0EA5E8")

        fun tintIconCard(menu: android.view.View, color: Int) {
            val imageView = menu.findViewById<android.widget.ImageView>(
                com.pixelmarket.app.R.id.icon
            )
            val card = imageView?.parent as? com.google.android.material.card.MaterialCardView
            card?.setCardBackgroundColor(color)
        }
        tintIconCard(binding.menuAccount.root, indigo)
        tintIconCard(binding.menuCart.root, purple)
        tintIconCard(binding.menuPurchases.root, emerald)
        tintIconCard(binding.menuWallet.root, amber)
        tintIconCard(binding.menuDeveloperAssets.root, rose)
        tintIconCard(binding.menuDeveloperDashboard.root, sky)


        binding.btnThemeToggle.setOnClickListener {
            val isDark = !themeManager.isDarkMode()
            themeManager.setDarkMode(isDark)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.btnAdmin.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_adminDashboardFragment)
        }
        
        binding.btnUpgradeToDeveloper.setOnClickListener {
            findNavController().navigate(R.id.developerRegistrationFragment)
        }
        
        binding.root.findViewById<android.view.View>(R.id.menuWallet)?.setOnClickListener {
            findNavController().navigate(R.id.walletFragment)
        }

        binding.root.findViewById<android.view.View>(R.id.menuPurchases)?.setOnClickListener {
            findNavController().navigate(R.id.downloadsFragment)
        }

        binding.root.findViewById<android.view.View>(R.id.menuCart)?.setOnClickListener {
            val bundle = Bundle().apply { 
                putBoolean("scrollToCart", true) 
                putString("screenTitle", "MY ORDERS")
            }
            findNavController().navigate(R.id.downloadsFragment, bundle)
        }

        binding.menuDeveloperAssets.root.setOnClickListener {
            val bundle = Bundle().apply { 
                putBoolean("showDeveloperAssets", true)
                putString("screenTitle", "MY UPLOADS")
            }
            findNavController().navigate(R.id.downloadsFragment, bundle)
        }

        binding.menuDeveloperDashboard.root.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_developerStatsFragment)
        }

        binding.menuAccount.root.setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun showEditProfileDialog() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        
        val editText = android.widget.EditText(requireContext()).apply {
            setText(binding.tvName.text)
            hint = "Enter your name"
            setPadding(40, 40, 40, 40)
            setTextColor(android.graphics.Color.BLACK)
        }
        
        val container = android.widget.FrameLayout(requireContext()).apply {
            setPadding(40, 20, 40, 0)
            addView(editText)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Base_Theme_PixelMarket) // Use project style for better UI
            .setTitle("Edit Profile")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    firestore.collection("users").document(user.uid)
                        .update("username", newName)
                        .addOnSuccessListener {
                            android.widget.Toast.makeText(requireContext(), "Profile updated", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            android.widget.Toast.makeText(requireContext(), "Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadUserProfile(uid: String) {
        firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                val b = _binding ?: return@addSnapshotListener
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                
                val user = snapshot.toObject(User::class.java) ?: return@addSnapshotListener
                
                b.tvName.text = user.username.ifEmpty { user.email.substringBefore("@") }
                b.tvEmail.text = user.email
                
                // Load profile image
                user.profileImageUrl?.let { url ->
                    b.ivProfile.load(url) {
                        crossfade(true)
                        placeholder(R.drawable.ic_profile)
                        error(R.drawable.ic_profile)
                    }
                }
                
                // Load Stats
                b.tvTotalPurchases.text = user.totalPurchases.toString()
                b.tvTotalDownloads.text = user.totalDownloads.toString()
                
                // Update Developer Status (and Admin status)
                val role = snapshot.getString("role")?.lowercase() ?: "buyer"
                
                // Robust flag checking (handles Boolean, String "true", or Number 1)
                fun isFlagEnabled(field: String) = try {
                    val v = snapshot.get(field)
                    v is Boolean && v || v?.toString()?.lowercase() == "true" || v?.toString() == "1"
                } catch (ex: Exception) { false }

                val isDeveloper = isFlagEnabled("isDeveloper")
                val isAdmin = isFlagEnabled("isAdmin")
                val legacyDeveloper = isFlagEnabled("developer")
                val legacyAdmin = isFlagEnabled("admin")

                val hasDevAccess = isDeveloper || isAdmin || legacyDeveloper || legacyAdmin || 
                                   role == "seller" || role == "admin" || role == "developer"

                if (hasDevAccess) {
                    b.btnUpgradeToDeveloper.visibility = View.GONE
                    b.developerBadge.visibility = View.VISIBLE
                    b.tvDeveloperBadge.text = if (isAdmin || role == "admin" || legacyAdmin) "ADMIN • CORE" else "DEVELOPER • PRO"
                    b.tvDeveloperBadge.setTextColor(if (isAdmin || role == "admin" || legacyAdmin) ContextCompat.getColor(requireContext(), R.color.rose_500) else ContextCompat.getColor(requireContext(), R.color.white))

                    // ── Show Developer Earnings card ──────────────────────────
                    b.developerEarningsCard.visibility = View.VISIBLE
                    val totalEarnings    = snapshot.getDouble("totalEarnings")    ?: 0.0
                    val availableBalance = snapshot.getDouble("availableBalance") ?: 0.0
                    val totalSales       = snapshot.getLong("totalSales")?.toInt() ?: 0
                    b.tvTotalEarnings.text    = "₹${String.format("%.2f", totalEarnings)}"
                    b.tvAvailableBalance.text = "₹${String.format("%.2f", availableBalance)}"
                    b.tvTotalSales.text       = totalSales.toString()

                    // Show Developer Menus
                    b.menuDeveloperAssets.root.visibility = View.VISIBLE
                    b.menuDeveloperDashboard.root.visibility = View.VISIBLE

                } else {
                    b.btnUpgradeToDeveloper.visibility = View.VISIBLE
                    b.developerBadge.visibility = View.GONE
                    b.developerEarningsCard.visibility = View.GONE
                }

                // Update Admin Button visibility
                val isActualAdmin = isAdmin || role == "admin" || legacyAdmin
                b.btnAdmin.visibility = if (isActualAdmin) View.VISIBLE else View.GONE
            }
    }

    private fun updateThemeIcon(isDark: Boolean) {
        val iconView = binding.btnThemeToggle.findViewById<android.widget.ImageView>(R.id.icon)
        if (isDark) {
            iconView?.setImageResource(R.drawable.ic_light_mode)
        } else {
            iconView?.setImageResource(R.drawable.ic_dark_mode)
        }
    }
    
    private fun loadWalletBalance(uid: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val balance = walletRepository.getWalletBalance(uid)
                val b = _binding ?: return@launch
                b.tvWalletBalance.text = "$${String.format("%.2f", balance)}"
            } catch (e: Exception) {
                _binding?.tvWalletBalance?.text = "$0.00"
            }
        }
    }


    
    private fun showComingSoonDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Developer Registration")
            .setMessage("The developer registration feature is currently under development. You'll be able to register as a developer and start selling your assets soon!")
            .setPositiveButton("Got it", null)
            .show()
    }


    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
