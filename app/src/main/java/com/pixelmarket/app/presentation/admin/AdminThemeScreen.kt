package com.pixelmarket.app.presentation.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.pixelmarket.app.domain.model.ThemeSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminThemeScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val themeSettings by viewModel.themeSettings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var primaryColor by remember { mutableStateOf(themeSettings.primaryColor) }
    var secondaryColor by remember { mutableStateOf(themeSettings.secondaryColor) }
    var accentColor by remember { mutableStateOf(themeSettings.accentColor) }
    var backgroundColor by remember { mutableStateOf(themeSettings.backgroundColor) }
    var surfaceColor by remember { mutableStateOf(themeSettings.surfaceColor) }
    
    var editingColor by remember { mutableStateOf<String?>(null) }
    var colorInputText by remember { mutableStateOf("") }
    
    LaunchedEffect(themeSettings) {
        primaryColor = themeSettings.primaryColor
        secondaryColor = themeSettings.secondaryColor
        accentColor = themeSettings.accentColor
        backgroundColor = themeSettings.backgroundColor
        surfaceColor = themeSettings.surfaceColor
    }
    
    LaunchedEffect(uiState) {
        if (uiState is com.pixelmarket.app.util.Resource.Success) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearUiState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Customization") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            primaryColor = themeSettings.primaryColor
                            secondaryColor = themeSettings.secondaryColor
                            accentColor = themeSettings.accentColor
                            backgroundColor = themeSettings.backgroundColor
                            surfaceColor = themeSettings.surfaceColor
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset")
                    }
                    
                    Button(
                        onClick = {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            viewModel.updateThemeSettings(
                                ThemeSettings(
                                    primaryColor = primaryColor,
                                    secondaryColor = secondaryColor,
                                    accentColor = accentColor,
                                    backgroundColor = backgroundColor,
                                    surfaceColor = surfaceColor
                                ),
                                updatedBy = currentUser?.uid ?: ""
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = uiState !is com.pixelmarket.app.util.Resource.Loading
                    ) {
                        if (uiState is com.pixelmarket.app.util.Resource.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Theme")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Message
            item {
                AnimatedVisibility(
                    visible = uiState is com.pixelmarket.app.util.Resource.Success,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                (uiState as? com.pixelmarket.app.util.Resource.Success)?.data ?: "",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "App Color Scheme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Customize the app's color palette. Changes will be visible to all users after saving.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item {
                Text(
                    "Color Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Primary Color
            item {
                ColorSettingCard(
                    title = "Primary Color",
                    description = "Main brand color used throughout the app",
                    colorHex = primaryColor,
                    onEdit = {
                        editingColor = "primary"
                        colorInputText = primaryColor
                    }
                )
            }
            
            // Secondary Color
            item {
                ColorSettingCard(
                    title = "Secondary Color",
                    description = "Accent color for secondary elements",
                    colorHex = secondaryColor,
                    onEdit = {
                        editingColor = "secondary"
                        colorInputText = secondaryColor
                    }
                )
            }
            
            // Accent Color
            item {
                ColorSettingCard(
                    title = "Accent Color",
                    description = "Highlights and call-to-action elements",
                    colorHex = accentColor,
                    onEdit = {
                        editingColor = "accent"
                        colorInputText = accentColor
                    }
                )
            }
            
            // Background Color
            item {
                ColorSettingCard(
                    title = "Background Color",
                    description = "Main background color",
                    colorHex = backgroundColor,
                    onEdit = {
                        editingColor = "background"
                        colorInputText = backgroundColor
                    }
                )
            }
            
            // Surface Color
            item {
                ColorSettingCard(
                    title = "Surface Color",
                    description = "Card and surface backgrounds",
                    colorHex = surfaceColor,
                    onEdit = {
                        editingColor = "surface"
                        colorInputText = surfaceColor
                    }
                )
            }
            
            // Color Presets
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Quick Presets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemePresetCard(
                        name = "Teal Ocean",
                        colors = listOf("#088395", "#7AB2B2", "#09637E", "#EBF4F6", "#FFFFFF"),
                        onApply = {
                            primaryColor = "#088395"
                            secondaryColor = "#7AB2B2"
                            accentColor = "#09637E"
                            backgroundColor = "#EBF4F6"
                            surfaceColor = "#FFFFFF"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ThemePresetCard(
                        name = "Purple Dream",
                        colors = listOf("#6200EA", "#BA68C8", "#7C4DFF", "#F3E5F5", "#FFFFFF"),
                        onApply = {
                            primaryColor = "#6200EA"
                            secondaryColor = "#BA68C8"
                            accentColor = "#7C4DFF"
                            backgroundColor = "#F3E5F5"
                            surfaceColor = "#FFFFFF"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemePresetCard(
                        name = "Sunset Orange",
                        colors = listOf("#FF6F00", "#FFB74D", "#FF8F00", "#FFF3E0", "#FFFFFF"),
                        onApply = {
                            primaryColor = "#FF6F00"
                            secondaryColor = "#FFB74D"
                            accentColor = "#FF8F00"
                            backgroundColor = "#FFF3E0"
                            surfaceColor = "#FFFFFF"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ThemePresetCard(
                        name = "Forest Green",
                        colors = listOf("#2E7D32", "#81C784", "#43A047", "#E8F5E9", "#FFFFFF"),
                        onApply = {
                            primaryColor = "#2E7D32"
                            secondaryColor = "#81C784"
                            accentColor = "#43A047"
                            backgroundColor = "#E8F5E9"
                            surfaceColor = "#FFFFFF"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    
    // Color Edit Dialog
    if (editingColor != null) {
        AlertDialog(
            onDismissRequest = { editingColor = null },
            icon = {
                Icon(Icons.Default.Palette, contentDescription = null)
            },
            title = {
                Text("Edit ${editingColor?.capitalize()} Color")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = colorInputText,
                        onValueChange = { colorInputText = it },
                        label = { Text("Hex Color Code") },
                        placeholder = { Text("#088395") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(colorInputText))
                                        } catch (e: Exception) {
                                            Color.Gray
                                        }
                                    )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Enter a hex color code (e.g., #088395)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            // Validate hex color
                            android.graphics.Color.parseColor(colorInputText)
                            when (editingColor) {
                                "primary" -> primaryColor = colorInputText
                                "secondary" -> secondaryColor = colorInputText
                                "accent" -> accentColor = colorInputText
                                "background" -> backgroundColor = colorInputText
                                "surface" -> surfaceColor = colorInputText
                            }
                            editingColor = null
                        } catch (e: Exception) {
                            // Invalid color, keep dialog open
                        }
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingColor = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSettingCard(
    title: String,
    description: String,
    colorHex: String,
    onEdit: () -> Unit
) {
    Card(
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(colorHex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                    )
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    colorHex,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePresetCard(
    name: String,
    colors: List<String>,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onApply,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                colors.forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(colorHex))
                                } catch (e: Exception) {
                                    Color.Gray
                                }
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
