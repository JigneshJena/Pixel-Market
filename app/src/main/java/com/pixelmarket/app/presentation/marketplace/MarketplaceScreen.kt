package com.pixelmarket.app.presentation.marketplace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelmarket.app.presentation.home.AssetCardSmall
import com.pixelmarket.app.presentation.home.AssetViewModel
import com.pixelmarket.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    onNavigateToDetails: (String) -> Unit,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    var isGridView by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Marketplace") },
                    actions = {
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                if (isGridView) Icons.Default.List else Icons.Default.GridView,
                                contentDescription = "Toggle View"
                            )
                        }
                        IconButton(onClick = { /* TODO: Filters */ }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters")
                        }
                    }
                )
                @OptIn(ExperimentalMaterial3Api::class)
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onSearch = { viewModel.onSearchQueryChanged(it) },
                    active = false,
                    onActiveChange = {},
                    placeholder = { Text("Search assets...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            MarketplaceContent(
                state = searchResults,
                isGridView = isGridView,
                onAssetClick = onNavigateToDetails
            )
        }
    }
}

@Composable
fun MarketplaceContent(
    state: Resource<List<com.pixelmarket.app.domain.model.Asset>>,
    isGridView: Boolean,
    onAssetClick: (String) -> Unit
) {
    when (state) {
        is Resource.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is Resource.Success -> {
            val assets = state.data ?: emptyList()
            if (assets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No assets found")
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(assets) { asset ->
                            AssetCardSmall(asset = asset, onClick = onAssetClick)
                        }
                    }
                } else {
                    // List View
                    androidx.compose.foundation.lazy.LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(assets.size) { index ->
                            val asset = assets[index]
                            MarketplaceListItem(asset = asset, onClick = onAssetClick)
                        }
                    }
                }
            }
        }
        is Resource.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(state.message ?: "Unknown Error")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceListItem(
    asset: com.pixelmarket.app.domain.model.Asset,
    onClick: (String) -> Unit
) {
    Card(
        onClick = { onClick(asset.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            coil.compose.AsyncImage(
                model = asset.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = asset.title, fontWeight = FontWeight.Bold)
                Text(text = asset.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$${asset.price}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
