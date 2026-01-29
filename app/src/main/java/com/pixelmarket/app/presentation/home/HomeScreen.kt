package com.pixelmarket.app.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.presentation.common.components.GlassmorphicCard
import com.pixelmarket.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetails: (String) -> Unit,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val featuredAssets by viewModel.featuredAssets.collectAsState()
    val trendingAssets by viewModel.trendingAssets.collectAsState()
    val newReleases by viewModel.newReleases.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PixelMarket", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Featured Carousel
            item {
                Text(
                    text = "Featured Assets",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                FeaturedCarousel(featuredAssets, onNavigateToDetails)
            }

            // Categories
            item {
                CategoryPills()
            }

            // Trending
            item {
                SectionTitle("Trending Now")
                AssetRow(trendingAssets, onNavigateToDetails)
            }

            // New Releases
            item {
                SectionTitle("New Releases")
                AssetRow(newReleases, onNavigateToDetails)
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FeaturedCarousel(
    state: Resource<List<Asset>>,
    onAssetClick: (String) -> Unit
) {
    when (state) {
        is Resource.Loading -> {
            // Shimmer effect
        }
        is Resource.Success -> {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.data ?: emptyList()) { asset ->
                    FeaturedAssetCard(asset, onAssetClick)
                }
            }
        }
        is Resource.Error -> {
            Text(state.message ?: "Error", modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun FeaturedAssetCard(asset: Asset, onClick: (String) -> Unit) {
    GlassmorphicCard(
        modifier = Modifier
            .width(280.dp)
            .height(180.dp)
    ) {
        Box {
            AsyncImage(
                model = asset.thumbnailUrl,
                contentDescription = asset.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = asset.title,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "$${asset.price}",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPills() {
    val categories = listOf("3D Models", "Textures", "Audio", "Blender", "Scripts")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        items(categories) { category ->
            AssistChip(
                onClick = { /* TODO */ },
                label = { Text(category) },
                shape = MaterialTheme.shapes.extraLarge
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun AssetRow(
    state: Resource<List<Asset>>,
    onAssetClick: (String) -> Unit
) {
    when (state) {
        is Resource.Loading -> { /* Shimmer */ }
        is Resource.Success -> {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.data ?: emptyList()) { asset ->
                    AssetCardSmall(asset, onAssetClick)
                }
            }
        }
        is Resource.Error -> { /* Error */ }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetCardSmall(asset: Asset, onClick: (String) -> Unit) {
    Card(
        onClick = { onClick(asset.id) },
        modifier = Modifier.width(140.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            AsyncImage(
                model = asset.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = asset.title,
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "$${asset.price}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
