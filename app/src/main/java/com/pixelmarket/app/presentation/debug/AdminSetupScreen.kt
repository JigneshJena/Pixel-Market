package com.pixelmarket.app.presentation.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelmarket.app.util.AdminSetup
import kotlinx.coroutines.launch

/**
 * Debug/Setup Screen for Admin Account Creation
 * 
 * USAGE:
 * 1. Add this screen to your navigation temporarily
 * 2. Open it once to create the admin account
 * 3. Remove from navigation before production release
 * 
 * ⚠️ This screen should NOT be included in production builds!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSetupScreen(
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("Ready to create admin account") }
    var isLoading by remember { mutableStateOf(false) }
    var accountInfo by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Setup (Debug)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warning Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Debug Tool - Remove Before Production",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "This screen is for development only. Delete before releasing the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Admin Credentials Card
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Default Admin Credentials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Divider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Email:", fontWeight = FontWeight.Bold)
                        Text(
                            "pixelMadmin@gmail.com",
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Password:", fontWeight = FontWeight.Bold)
                        Text(
                            "admin1234",
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Role:", fontWeight = FontWeight.Bold)
                        Text("admin", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            // Create Admin Button
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        statusMessage = "Creating admin account..."
                        
                        val result = AdminSetup.createDefaultAdminAccount()
                        
                        statusMessage = result.fold(
                            onSuccess = { it },
                            onFailure = { "Error: ${it.message}" }
                        )
                        
                        isLoading = false
                        
                        // Load account info
                        accountInfo = AdminSetup.getAdminAccountInfo()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Admin Account")
            }
            
            // Verify Button
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val exists = AdminSetup.verifyAdminAccount()
                        statusMessage = if (exists) {
                            "✅ Admin account exists and is properly configured"
                        } else {
                            "❌ Admin account not found. Please create it first."
                        }
                        accountInfo = AdminSetup.getAdminAccountInfo()
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verify Admin Account")
            }
            
            // Status Message
            if (statusMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (statusMessage.contains("✅") || statusMessage.contains("success")) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else if (statusMessage.contains("❌") || statusMessage.contains("Error")) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Text(
                        statusMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Account Info
            if (accountInfo.isNotEmpty()) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Account Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            accountInfo,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Instructions
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Divider()
                    
                    Text("1. Click 'Create Admin Account'")
                    Text("2. Wait for success message")
                    Text("3. Click 'Verify Admin Account' to confirm")
                    Text("4. Login with the credentials above")
                    Text("5. Access Admin Panel from Profile")
                    Text("6. Remove this screen before production!")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "⚠️ Security Note",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Change the default password after first login!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
