package com.pixelmarket.app.presentation.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.repository.AssetRepository
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val assetRepository: AssetRepository
) : ViewModel() {

    private val _assets = MutableStateFlow<Resource<List<Asset>>>(Resource.Loading())
    val assets: StateFlow<Resource<List<Asset>>> = _assets

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    init {
        loadAllAssets()
    }

    fun loadAllAssets() {
        viewModelScope.launch {
            assetRepository.getAllAssets().collect { result ->
                _assets.value = result
            }
        }
    }

    fun searchAssets(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            loadAllAssets()
        } else {
            viewModelScope.launch {
                assetRepository.searchAssets(query, if (_selectedCategory.value == "All") null else _selectedCategory.value)
                    .collect { result ->
                        _assets.value = result
                    }
            }
        }
    }

    fun filterByCategory(category: String) {
        _selectedCategory.value = category
        if (_searchQuery.value.isBlank()) {
            if (category == "All") {
                loadAllAssets()
            } else {
                viewModelScope.launch {
                    assetRepository.searchAssets("", category).collect { result ->
                        _assets.value = result
                    }
                }
            }
        } else {
            searchAssets(_searchQuery.value)
        }
    }

    fun refresh() {
        loadAllAssets()
    }
}
