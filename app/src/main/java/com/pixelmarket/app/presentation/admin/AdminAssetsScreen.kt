package com.pixelmarket.app.presentation.admin

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pixelmarket.app.domain.model.Asset
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAssetsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val assets by viewModel.assets.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf<Asset?>(null) }
    var filterApproved by remember { mutableStateOf<Boolean?>(null) } // null = all, true = approved, false = pending
    
    val filteredAssets = remember(assets, filterApproved) {
        when (filterApproved) {
            null -> assets
            true -> assets.filter { it.approved }
            false -> assets.filter { !it.approved }
        }
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
                title = { Text("Asset Management") },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status Message
            AnimatedVisibility(
                visible = uiState is com.pixelmarket.app.util.Resource.Success,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
            
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterApproved == null,
                    onClick = { filterApproved = null },
                    label = { Text("All (${assets.size})") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                FilterChip(
                    selected = filterApproved == true,
                    onClick = { filterApproved = true },
                    label = { Text("Approved (${assets.count { it.approved }})") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                FilterChip(
                    selected = filterApproved == false,
                    onClick = { filterApproved = false },
                    label = { Text("Pending (${assets.count { !it.approved }})") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Pending,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            
            // Assets list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredAssets) { asset ->
                    AssetManagementCard(
                        asset = asset,
                        onApprove = { viewModel.approveAsset(asset.id, true) },
                        onReject = { viewModel.approveAsset(asset.id, false) },
                        onToggleFeatured = { viewModel.toggleAssetFeatured(asset.id, !asset.featured) },
                        onDelete = { showDeleteDialog = asset }
                    )
                }
                
                if (filteredAssets.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No assets found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { asset ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Delete Asset?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to delete \"${asset.title}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAsset(asset.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetManagementCard(
    asset: Asset,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onToggleFeatured: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                // Thumbnail
                AsyncImage(
                    model = asset.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            asset.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!asset.approved) {
                            AssistChip(
                                onClick = { },
                                label = { Text("Pending", fontSize = 10.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Pending,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    labelColor = MaterialTheme.colorScheme.error,
                                    leadingIconContentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        
                        if (asset.featured) {
                            AssistChip(
                                onClick = { },
                                label = { Text("Featured", fontSize = 10.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.secondary,
                                    leadingIconContentColor = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        
                        if (asset.thumbnailType == "gif") {
                            AssistChip(
                                onClick = { },
                                label = { Text("GIF", fontSize = 10.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Animation,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "By ${asset.sellerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Category: ${asset.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!asset.approved) {
                            DropdownMenuItem(
                                text = { Text("Approve") },
                                onClick = {
                                    onApprove()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reject") },
                                onClick = {
                                    onReject()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Cancel, contentDescription = null)
                                }
                            )
                        }
                        
                        DropdownMenuItem(
                            text = { Text(if (asset.featured) "Unfeature" else "Feature") },
                            onClick = {
                                onToggleFeatured()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (asset.featured) Icons.Default.StarBorder else Icons.Default.Star,
                                    contentDescription = null
                                )
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            
            // Asset Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AssetStat(
                    label = "Price",
                    value = "$${asset.price}",
                    icon = Icons.Default.AttachMoney
                )
                AssetStat(
                    label = "Size",
                    value = asset.fileSize,
                    icon = Icons.Default.Storage
                )
                AssetStat(
                    label = "Downloads",
                    value = asset.downloadCount.toString(),
                    icon = Icons.Default.Download
                )
                AssetStat(
                    label = "Rating",
                    value = String.format("%.1f", asset.rating),
                    icon = Icons.Default.Star
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            Text(
                "Uploaded: ${dateFormat.format(asset.createdAt.toDate())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AssetStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
