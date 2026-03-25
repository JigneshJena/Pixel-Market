package com.pixelmarket.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.repository.AssetRepository
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class AssetViewModel @Inject constructor(
    private val repository: AssetRepository
) : ViewModel() {

    private val _featuredAssets = MutableStateFlow<Resource<List<Asset>>>(Resource.Loading())
    val featuredAssets: StateFlow<Resource<List<Asset>>> = _featuredAssets

    private val _trendingAssets = MutableStateFlow<Resource<List<Asset>>>(Resource.Loading())
    val trendingAssets: StateFlow<Resource<List<Asset>>> = _trendingAssets

    private val _newReleases = MutableStateFlow<Resource<List<Asset>>>(Resource.Loading())
    val newReleases: StateFlow<Resource<List<Asset>>> = _newReleases

    private val _searchResults = MutableStateFlow<Resource<List<Asset>>>(Resource.Success(emptyList()))
    val searchResults: StateFlow<Resource<List<Asset>>> = _searchResults

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    init {
        fetchFeatured()
        fetchTrending()
        fetchNewReleases()
    }

    fun fetchFeatured() {
        repository.getFeaturedAssets().onEach { _featuredAssets.value = it }.launchIn(viewModelScope)
    }

    fun fetchTrending() {
        repository.getTrendingAssets().onEach { _trendingAssets.value = it }.launchIn(viewModelScope)
    }

    fun fetchNewReleases() {
        repository.getNewReleases().onEach { _newReleases.value = it }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchAssets()
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        searchAssets()
    }

    private fun searchAssets() {
        repository.searchAssets(_searchQuery.value, if (_selectedCategory.value == "All") null else _selectedCategory.value)
            .onEach { _searchResults.value = it }
            .launchIn(viewModelScope)
    }
}
