package com.pixelmarket.app.presentation.details

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.presentation.common.components.AnimatedGradientBackground
import com.pixelmarket.app.presentation.ui.theme.Dimens
import com.pixelmarket.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailsScreen(
    assetId: String,
    onBack: () -> Unit,
    viewModel: AssetDetailsViewModel = hiltViewModel()
) {
    val asset by viewModel.asset.collectAsState()
    val purchaseState by viewModel.purchaseState.collectAsState()
    val context = LocalContext.current
    
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var downloadUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(assetId) {
        viewModel.loadAssetDetails(assetId)
    }

    LaunchedEffect(purchaseState) {
        when (val state = purchaseState) {
            is PurchaseState.Success -> {
                downloadUrl = state.downloadUrl
                showSuccessDialog = true
                viewModel.resetPurchaseState()
            }
            is PurchaseState.Error -> {
                showPurchaseDialog = false
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asset Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
        when (asset) {
            is Resource.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Success -> {
                val assetData = (asset as Resource.Success<Asset>).data ?: return@Scaffold
                DetailsContent(
                    asset = assetData,
                    padding = padding,
                    purchaseState = purchaseState,
                    onBuyNow = {
                        showPurchaseDialog = true
                    }
                )
            }
            is Resource.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text((asset as Resource.Error).message ?: "Error loading asset")
                }
            }
        }
    }

    // Purchase Confirmation Dialog
    if (showPurchaseDialog) {
        val assetData = (asset as? Resource.Success<Asset>)?.data
        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false },
            icon = { Icon(Icons.Default.ShoppingCart, null) },
            title = { Text("Confirm Purchase") },
            text = {
                Column {
                    Text("Buy: ${assetData?.title}")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Price: $${assetData?.price}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        assetData?.let { viewModel.buyNow(it) }
                        showPurchaseDialog = false
                    },
                    enabled = purchaseState !is PurchaseState.Processing
                ) {
                    if (purchaseState is PurchaseState.Processing) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Buy Now")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Dialog with Download Button
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Purchase Successful! 🎉") },
            text = { Text("Your asset is ready to download!") },
            confirmButton = {
                Button(
                    onClick = {
                        downloadUrl?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                        showSuccessDialog = false
                    }
                ) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("Later")
                }
            }
        )
    }
}

@Composable
fun DetailsContent(
    asset: Asset,
    padding: PaddingValues,
    purchaseState: PurchaseState,
    onBuyNow: () -> Unit
) {
    AnimatedGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Thumbnail
            AsyncImage(
                model = asset.thumbnailUrl,
                contentDescription = asset.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .shadow(8.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(Dimens.Space24))

            // Content
            Column(Modifier.padding(horizontal = Dimens.ContentPadding)) {
                // Title
                Text(
                    text = asset.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                // Seller & Stats
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(asset.sellerName, fontSize = 14.sp)
                    Spacer(Modifier.width(16.dp))
                    Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${asset.downloadCount}", fontSize = 14.sp)
                }

                Spacer(Modifier.height(Dimens.Space24))

                // Price Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(Dimens.Space16),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Price", fontSize = 14.sp)
                            Text(
                                "$${asset.price}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = onBuyNow,
                            modifier = Modifier.height(56.dp),
                            enabled = purchaseState !is PurchaseState.Processing
                        ) {
                            if (purchaseState is PurchaseState.Processing) {
                                CircularProgressIndicator(
                                    Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.ShoppingCart, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Buy Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Dimens.Space24))

                // Description
                Text(
                    "Description",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    asset.description.ifBlank { "No description available" },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 24.sp
                )

                Spacer(Modifier.height(Dimens.Space24))

                // Info Cards
                InfoRow("Category", asset.category)
                InfoRow("Rating", "${asset.rating}/5.0")
                InfoRow("Downloads", "${asset.downloadCount}")

                Spacer(Modifier.height(Dimens.Space48))
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}
