package com.pixelmarket.app.presentation.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.pixelmarket.app.presentation.auth.AuthViewModel
import com.pixelmarket.app.presentation.common.components.AnimatedGradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToAdmin: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val isDarkMode = isSystemInDarkTheme()
    
    // Get current user from Firebase
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: "guest@pixelmarket.com"
    val userName = currentUser?.displayName ?: userEmail.substringBefore("@").capitalize()
    val userInitials = userName.take(2).uppercase()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }
    
    // Check if user is admin
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    isAdmin = document.getBoolean("isAdmin") ?: false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        AnimatedGradientBackground(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Animated Profile Picture
                AnimatedProfilePicture(initials = userInitials)

                Spacer(modifier = Modifier.height(20.dp))
                
                // User Info with fade-in animation
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(600)) + 
                            expandVertically(animationSpec = tween(600))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Comprehensive Stats Section with Firebase data
                UserStatsSection(currentUser?.uid ?: "")

                Spacer(modifier = Modifier.height(24.dp))

                // Account Settings
                SettingsSectionTitle("Account Settings")
                SettingsOptionCard(
                    icon = Icons.Default.Person,
                    title = "Edit Profile",
                    subtitle = "Update your personal information",
                    onClick = { /* TODO */ }
                )
                SettingsOptionCard(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Manage notification preferences",
                    onClick = { /* TODO */ }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Admin Panel (only show for admin users)
                if (isAdmin) {
                    SettingsSectionTitle("Administration")
                    SettingsOptionCard(
                        icon = Icons.Default.AdminPanelSettings,
                        title = "Admin Panel",
                        subtitle = "Manage users, themes, and assets",
                        onClick = onNavigateToAdmin
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                SettingsSectionTitle("App Settings")
                SettingsOptionCard(
                    icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    title = "Theme",
                    subtitle = "Currently: ${if (isDarkMode) "Dark" else "Light"} Mode",
                    onClick = { /* Dark mode follows system settings */ }
                )
                SettingsOptionCard(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = "English (US)",
                    onClick = { /* TODO */ }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsSectionTitle("Support")
                SettingsOptionCard(
                    icon = Icons.Default.Help,
                    title = "Help & Support",
                    subtitle = "Get help and contact support",
                    onClick = { /* TODO */ }
                )
                SettingsOptionCard(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Version 1.0.0",
                    onClick = { /* TODO */ }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(Icons.Default.Logout, contentDescription = null)
            },
            title = {
                Text("Logout")
            },
            text = {
                Text("Are you sure you want to logout?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AnimatedProfilePicture(initials: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "profile_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(contentAlignment = Alignment.Center) {
        // Glowing ring
        Surface(
            modifier = Modifier
                .size(120.dp)
                .scale(1f + glowAlpha * 0.1f),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
        ) {}

        // Profile circle
        Surface(
            modifier = Modifier
                .size(110.dp)
                .shadow(8.dp, CircleShape),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun UserStatsSection(userId: String) {
    var stats by remember { mutableStateOf<com.pixelmarket.app.domain.model.UserStats?>(null) }
    
    // Fetch user statistics from Firebase
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("statistics")
                .document("summary")
                .get()
                .addOnSuccessListener { document ->
                    stats = com.pixelmarket.app.domain.model.UserStats(
                        totalSales = document.getLong("totalSales")?.toInt() ?: 0,
                        totalPurchases = document.getLong("totalPurchases")?.toInt() ?: 0,
                        totalDownloads = document.getLong("totalDownloads")?.toInt() ?: 0,
                        totalIncome = document.getDouble("totalIncome") ?: 0.0,
                        totalSpent = document.getDouble("totalSpent") ?: 0.0,
                        assetsUploaded = document.getLong("assetsUploaded")?.toInt() ?: 0,
                        favoriteCount = document.getLong("favoriteCount")?.toInt() ?: 0,
                        accountBalance = document.getDouble("accountBalance") ?: 0.0
                    )
                }
                .addOnFailureListener {
                    // Use default values on error
                    stats = com.pixelmarket.app.domain.model.UserStats()
                }
        }
    }
    
    val currentStats = stats ?: com.pixelmarket.app.domain.model.UserStats()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Title
        Text(
            text = "My Statistics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Top Row - Income and Balance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Income Card
            StatsCard(
                modifier = Modifier.weight(1f),
                title = "Total Income",
                value = "$${String.format("%.2f", currentStats.totalIncome)}",
                icon = Icons.Default.AccountBalance,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Account Balance Card
            StatsCard(
                modifier = Modifier.weight(1f),
                title = "Balance",
                value = "$${String.format("%.2f", currentStats.accountBalance)}",
                icon = Icons.Default.Wallet,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Middle Row - Sales and Purchases
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Sales
            StatsCard(
                modifier = Modifier.weight(1f),
                title = "Sales",
                value = currentStats.totalSales.toString(),
                subtitle = "Items sold",
                icon = Icons.Default.Sell,
                color = MaterialTheme.colorScheme.secondary
            )
            
            // Total Purchases
            StatsCard(
                modifier = Modifier.weight(1f),
                title = "Purchases",
                value = currentStats.totalPurchases.toString(),
                subtitle = "Items bought",
                icon = Icons.Default.ShoppingCart,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Bottom Row - Downloads and Uploads
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Downloads
            StatsCard(
                modifier = Modifier.weight(1f),
                title = "Downloads",
                value = currentStats.totalDownloads.toString(),
                icon = Icons.Default.Download,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Uploads
            StatsCard(
                modifier = Modifier.weight(1f),
                title = "Uploads",
                value = currentStats.assetsUploaded.toString(),
                icon = Icons.Default.CloudUpload,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun StatsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    
    Card(
        onClick = {
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .scale(scale.value),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
