package com.pixelmarket.app.presentation.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.ActivityMainBinding
import com.pixelmarket.app.util.ThemeManager
import com.pixelmarket.app.data.repository.ThemeRepository
import com.razorpay.PaymentResultListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), PaymentResultListener {

    @Inject
    lateinit var themeRepository: ThemeRepository

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var database: FirebaseDatabase

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var roleListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme first
        val themeManager = ThemeManager(this)
        themeManager.applyThemeToActivity(this)

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observe Maintenance Mode (Global)
        observeMaintenanceMode()
        
        // Observe Global Broadcasts
        observeBroadcasts()

        // Observe Remote Theme
        lifecycleScope.launch {
            themeRepository.observeThemeSettings().collectLatest { settings ->
                try {
                    val remotePrimary = settings.primaryColor
                    if (remotePrimary.isNullOrEmpty() || !remotePrimary.startsWith("#")) return@collectLatest

                    val currentPrimary = themeManager.getPrimaryColor()
                    val newPrimary = android.graphics.Color.parseColor(remotePrimary)

                    if (currentPrimary != newPrimary) {
                        themeManager.saveRemoteColors(settings.primaryColor, settings.secondaryColor)
                        if (savedInstanceState != null) {
                            recreate()
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup tap listeners
        binding.navHome.setOnClickListener { navController.navigate(R.id.homeFragment) }
        binding.navMarket.setOnClickListener { navController.navigate(R.id.marketplaceFragment) }
        binding.navUploadContainer.setOnClickListener { navController.navigate(R.id.uploadFragment) }
        binding.navDownloads.setOnClickListener { navController.navigate(R.id.downloadsFragment) }
        binding.navProfile.setOnClickListener { navController.navigate(R.id.profileFragment) }


        // Theme colors
        val primaryColor = themeManager.getPrimaryColor()
        val inactiveColor = if (themeManager.isDarkMode())
            android.graphics.Color.parseColor("#475569")
        else
            android.graphics.Color.parseColor("#94A3B8")

        // Role-based nav with auth listener
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                setupRoleBasedNavigation()
                observeInstantNotifications()
                setupUserPresence()
            } else {
                roleListener?.remove()
                roleListener = null
                binding.navUploadContainer.visibility = View.GONE
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateNavSelection(destination.id, primaryColor, inactiveColor)

            // Show/Hide nav bar
            when (destination.id) {
                R.id.splashFragment, R.id.loginFragment, R.id.registerFragment,
                R.id.assetDetailsFragment, R.id.developerRegistrationFragment,
                R.id.adminDashboardFragment, R.id.adminUsersFragment,
                R.id.adminAssetsFragment, R.id.adminThemesFragment,
                R.id.adminDeveloperApplicationsFragment -> hideBottomNav()
                else -> showBottomNav()
            }
        }
    }

    private fun updateNavSelection(destId: Int, primaryColor: Int, inactiveColor: Int) {
        // Each entry: icon, label, dot indicator, fragment destination id
        data class NavEntry(
            val icon: android.widget.ImageView,
            val label: android.widget.TextView,
            val dot: View,
            val fragId: Int
        )

        val navEntries = listOf(
            NavEntry(binding.ivNavHome,      binding.tvNavHome,      binding.dotNavHome,      R.id.homeFragment),
            NavEntry(binding.ivNavMarket,    binding.tvNavMarket,    binding.dotNavMarket,    R.id.marketplaceFragment),
            NavEntry(binding.ivNavDownloads, binding.tvNavDownloads, binding.dotNavDownloads, R.id.downloadsFragment),
            NavEntry(binding.ivNavProfile,   binding.tvNavProfile,   binding.dotNavProfile,   R.id.profileFragment)
        )

        navEntries.forEach { entry ->
            val isActive = destId == entry.fragId
            val color = if (isActive) primaryColor else inactiveColor

            // Color
            entry.icon.imageTintList = android.content.res.ColorStateList.valueOf(color)
            entry.label.setTextColor(color)

            // Dot indicator
            entry.dot.visibility = if (isActive) View.VISIBLE else View.INVISIBLE

            // Subtle scale on active icon
            entry.icon.animate()
                .scaleX(if (isActive) 1.1f else 1.0f)
                .scaleY(if (isActive) 1.1f else 1.0f)
                .setDuration(150)
                .start()
        }

        // Upload tab (developer only)
        if (binding.navUploadContainer.visibility == View.VISIBLE) {
            val isUploadActive = destId == R.id.uploadFragment
            val uploadColor = if (isUploadActive) primaryColor else inactiveColor
            binding.ivNavUpload.imageTintList = android.content.res.ColorStateList.valueOf(uploadColor)
            binding.tvNavUpload.setTextColor(uploadColor)
            binding.dotNavUpload.visibility = if (isUploadActive) View.VISIBLE else View.INVISIBLE
            binding.ivNavUpload.animate()
                .scaleX(if (isUploadActive) 1.1f else 1.0f)
                .scaleY(if (isUploadActive) 1.1f else 1.0f)
                .setDuration(150)
                .start()
        }
    }

    // Razorpay Callbacks delegation
    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

        if (currentFragment is PaymentResultListener) {
            currentFragment.onPaymentSuccess(razorpayPaymentId)
        }
    }

    override fun onPaymentError(code: Int, description: String?) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

        if (currentFragment is PaymentResultListener) {
            currentFragment.onPaymentError(code, description)
        }
    }

    private fun setupRoleBasedNavigation() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        roleListener?.remove()

        roleListener = firestore.collection("users").document(currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

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

                android.util.Log.d("PIXEL_DEBUG", "NAV_CHECK: role=$role, isDev=$isDeveloper, isAdmin=$isAdmin")

                // Show upload tab if user has ANY developer/admin indicator
                val canUpload = isDeveloper || isAdmin || legacyDeveloper || legacyAdmin || 
                                role == "seller" || role == "admin" || role == "developer"
                
                binding.navUploadContainer.visibility = if (canUpload) View.VISIBLE else View.GONE
                
                if (canUpload) {
                    binding.navUploadContainer.setOnClickListener {
                        navController.navigate(R.id.uploadFragment)
                    }
                    // Refresh colors so icons aren't gray when they first appear
                    updateNavSelection(
                        navController.currentDestination?.id ?: 0,
                        ThemeManager(this).getPrimaryColor(),
                        if (ThemeManager(this).isDarkMode()) android.graphics.Color.parseColor("#475569") else android.graphics.Color.parseColor("#94A3B8")
                    )
                }
            }
    }

    private fun showBottomNav() {
        if (binding.bottomNavBar.visibility == View.VISIBLE && binding.bottomNavBar.translationY == 0f) return
        binding.bottomNavBar.clearAnimation()
        binding.navDivider.visibility = View.VISIBLE
        binding.bottomNavBar.visibility = View.VISIBLE
        binding.bottomNavBar.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun hideBottomNav() {
        if (binding.bottomNavBar.visibility == View.GONE) return
        binding.bottomNavBar.clearAnimation()
        binding.bottomNavBar.animate()
            .translationY(binding.bottomNavBar.height.toFloat() + 2f)
            .alpha(0f)
            .setDuration(180)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                binding.bottomNavBar.visibility = View.GONE
                binding.navDivider.visibility = View.GONE
            }
            .start()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun observeInstantNotifications() {
        val user = auth.currentUser ?: return
        val alertsRef = database.getReference("instant_alerts").child(user.uid)

        alertsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val title = snapshot.child("title").getValue(String::class.java) ?: "Notification"
                val message = snapshot.child("message").getValue(String::class.java) ?: ""
                val type = snapshot.child("type").getValue(String::class.java) ?: "info"

                // Show Premium Material Dialog
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Awesome") { _, _ ->
                        // Clear notification after reading
                        alertsRef.removeValue()
                    }
                    .setOnDismissListener {
                        alertsRef.removeValue()
                    }
                    .show()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupUserPresence() {
        val user = auth.currentUser ?: return
        val presenceRef = database.getReference("presence").child(user.uid)
        val connectionRef = database.getReference(".info/connected")

        connectionRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    // Set online and auto-offline on disconnect
                    presenceRef.onDisconnect().setValue("offline")
                    presenceRef.setValue("online")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun observeMaintenanceMode() {
        database.getReference("settings/maintenanceMode")
            .addValueEventListener(object : ValueEventListener {
                private var maintenanceDialog: androidx.appcompat.app.AlertDialog? = null

                override fun onDataChange(snapshot: DataSnapshot) {
                    val isMaintenance = snapshot.getValue(Boolean::class.java) ?: false
                    
                    if (isMaintenance) {
                        if (maintenanceDialog == null || !maintenanceDialog!!.isShowing) {
                            maintenanceDialog = MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle("🚧 Under Maintenance")
                                .setMessage("PixelMarket is currently undergoing scheduled maintenance to improve your experience. Please check back later.")
                                .setCancelable(false)
                                .setIcon(R.drawable.ic_launcher_foreground)
                                .show()
                        }
                    } else {
                        maintenanceDialog?.dismiss()
                        maintenanceDialog = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun observeBroadcasts() {
        val startupTime = System.currentTimeMillis()
        database.getReference("broadcast")
            .addValueEventListener(object : ValueEventListener {
                private var lastTimestamp: Long = startupTime

                override fun onDataChange(snapshot: DataSnapshot) {
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0
                    if (timestamp <= lastTimestamp) return
                    lastTimestamp = timestamp

                    val title = snapshot.child("title").getValue(String::class.java) ?: "Announcement"
                    val message = snapshot.child("message").getValue(String::class.java) ?: ""

                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("Awesome", null)
                        .show()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
