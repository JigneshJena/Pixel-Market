package com.pixelmarket.app.presentation.admin

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.FragmentAdminDashboardBinding
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.model.User
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminDashboardBinding.bind(view)

        loadAdminInfo()
        setupClickListeners()
        observeStats()
        observeRecentAssets()
        observeRecentUsers()
    }

    // ── Load admin name + avatar ─────────────────────────────────
    private fun loadAdminInfo() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                val name = doc.getString("username")
                    ?: doc.getString("email")?.substringBefore("@")
                    ?: "Admin"
                binding.tvAdminName.text = name
                val avatarUrl = doc.getString("profileImageUrl")
                if (!avatarUrl.isNullOrBlank()) {
                    binding.ivAdminAvatar.load(avatarUrl) { crossfade(true) }
                }
            }
    }

    // ── Wiring ───────────────────────────────────────────────────
    private fun setupClickListeners() {
        binding.cardManageUsers.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_adminUsersFragment)
        }
        binding.cardManageAssets.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_adminAssetsFragment)
        }
        binding.cardDevApps.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_developerApplicationsFragment)
        }
        binding.tvViewAllAssets.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_adminAssetsFragment)
        }
        binding.tvViewAllUsers.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_adminUsersFragment)
        }
        binding.btnUploadAsset.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Upload...", Toast.LENGTH_SHORT).show()
        }
        binding.btnNotifications.setOnClickListener {
            // Tapping bell → go to Dev Applications if there are pending ones
            val pending = viewModel.adminStats.value.pendingDeveloperApplications
            if (pending > 0) {
                findNavController().navigate(R.id.action_adminDashboardFragment_to_developerApplicationsFragment)
            } else {
                Toast.makeText(requireContext(), "No new notifications", Toast.LENGTH_SHORT).show()
            }
        }
        binding.tvBannerReview.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_developerApplicationsFragment)
        }
        binding.bannerDevApps.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_developerApplicationsFragment)
        }

        // Logout
        binding.btnAdminLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    findNavController().navigate(R.id.action_adminDashboardFragment_to_loginFragment)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // ── Live stats ───────────────────────────────────────────────
    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.adminStats.collectLatest { stats ->
                val b = _binding ?: return@collectLatest
                b.tvTotalRevenue.text = "₹${formatCount(stats.totalCombinedRevenue.toLong())}"
                b.tvTotalSalesCount.text = formatCount(stats.totalSales.toLong())
                b.tvTotalDownloads.text = formatCount(stats.totalDownloads.toLong())
                b.tvActiveAssetsCount.text = stats.totalAssets.toString()
                b.tvPendingCount.text = stats.pendingApprovals.toString()
                b.tvFollowersCount.text = formatCount(stats.totalUsers.toLong())

                val devPending = stats.pendingDeveloperApplications

                // ① Notification bell red dot
                b.notifBadge.isVisible = devPending > 0

                // ② Amber alert banner
                if (devPending > 0) {
                    b.bannerDevApps.visibility = View.VISIBLE
                    b.tvBannerText.text =
                        "$devPending developer ${if (devPending == 1) "application" else "applications"} waiting for review"
                    // Badge on row item
                    b.tvDevAppsBadge.text = devPending.toString()
                    b.tvDevAppsBadge.isVisible = true
                } else {
                    b.bannerDevApps.visibility = View.GONE
                    b.tvDevAppsBadge.isVisible = false
                }
            }
        }
    }

    // ── Recent assets (last 5) ───────────────────────────────────
    private fun observeRecentAssets() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.assets.collectLatest { assets ->
                val b = _binding ?: return@collectLatest
                // Sort on background thread
                val recent = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    assets.sortedByDescending { it.createdAt }.take(5)
                }
                
                val container = b.containerRecentAssets
                container.removeAllViews()
                if (recent.isEmpty()) {
                    container.addView(makeEmptyText("No assets yet"))
                } else {
                    recent.forEach { container.addView(makeAssetRow(it)) }
                }
            }
        }
    }

    // ── Recent users (last 5) ────────────────────────────────────
    private fun observeRecentUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.users.collectLatest { users ->
                val b = _binding ?: return@collectLatest
                // Sort on background thread
                val recent = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    users.sortedByDescending { it.createdAt }.take(5)
                }

                val container = b.containerRecentUsers
                container.removeAllViews()
                if (recent.isEmpty()) {
                    container.addView(makeEmptyText("No users yet"))
                } else {
                    recent.forEach { container.addView(makeUserRow(it)) }
                }
            }
        }
    }

    // ── Asset row ────────────────────────────────────────────────
    private fun makeAssetRow(asset: Asset): View {
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density

        val row = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (8 * dp).toInt() }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * dp).toInt(), (12 * dp).toInt(), (14 * dp).toInt(), (12 * dp).toInt())
            background = ContextCompat.getDrawable(ctx, R.drawable.glass_panel_rounded)
        }

        val thumb = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((40 * dp).toInt(), (40 * dp).toInt())
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.ic_image_placeholder)
        }
        if (asset.thumbnailUrl.isNotBlank()) {
            thumb.load(asset.thumbnailUrl) { crossfade(true) }
        }
        row.addView(thumb)

        val info = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .also { it.marginStart = (12 * dp).toInt() }
            orientation = LinearLayout.VERTICAL
        }
        info.addView(TextView(ctx).apply {
            text = asset.title.ifBlank { "Untitled" }
            textSize = 14f
            setTextColor(ContextCompat.getColor(ctx, R.color.slate_900))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        info.addView(TextView(ctx).apply {
            text = "${asset.category}  •  ${asset.downloadCount} downloads"
            textSize = 12f
            setTextColor(ContextCompat.getColor(ctx, R.color.slate_500))
        })
        row.addView(info)

        row.addView(TextView(ctx).apply {
            text = if (asset.approved) "Live" else "Pending"
            textSize = 11f
            setTextColor(if (asset.approved) Color.parseColor("#16A34A") else Color.parseColor("#D97706"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        return row
    }

    // ── User row ─────────────────────────────────────────────────
    private fun makeUserRow(user: User): View {
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density

        val row = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (8 * dp).toInt() }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * dp).toInt(), (12 * dp).toInt(), (14 * dp).toInt(), (12 * dp).toInt())
            background = ContextCompat.getDrawable(ctx, R.drawable.glass_panel_rounded)
        }

        // Initial circle
        val initial = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((38 * dp).toInt(), (38 * dp).toInt())
            gravity = Gravity.CENTER
            textSize = 15f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            text = (user.username.ifBlank { user.email }).firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            background = ContextCompat.getDrawable(ctx, R.drawable.glass_panel_rounded)
            background?.setTint(Color.parseColor("#6366F1"))
        }
        row.addView(initial)

        val info = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .also { it.marginStart = (12 * dp).toInt() }
            orientation = LinearLayout.VERTICAL
        }
        info.addView(TextView(ctx).apply {
            text = user.username.ifBlank { user.email.substringBefore("@") }
            textSize = 14f
            setTextColor(ContextCompat.getColor(ctx, R.color.slate_900))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        info.addView(TextView(ctx).apply {
            text = when {
                user.isAdmin -> "Admin"
                user.isDeveloper -> "Developer"
                else -> user.email
            }
            textSize = 12f
            setTextColor(ContextCompat.getColor(ctx, R.color.slate_500))
        })
        row.addView(info)

        // Role chip
        val roleColor = when {
            user.isAdmin -> Color.parseColor("#6366F1")
            user.isDeveloper -> Color.parseColor("#0EA5E9")
            else -> Color.parseColor("#94A3B8")
        }
        row.addView(TextView(ctx).apply {
            text = when {
                user.isAdmin -> "Admin"
                user.isDeveloper -> "Dev"
                else -> "User"
            }
            textSize = 11f
            setTextColor(roleColor)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        return row
    }

    private fun makeEmptyText(msg: String) = TextView(requireContext()).apply {
        text = msg
        textSize = 14f
        setTextColor(ContextCompat.getColor(requireContext(), R.color.slate_500))
        gravity = Gravity.CENTER
        setPadding(0, 24, 0, 24)
    }

    private fun formatCount(n: Long): String = when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
        n >= 1_000 -> "%.1fk".format(n / 1_000.0)
        else -> n.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
