package com.pixelmarket.app.presentation.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.FragmentHomeBinding
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AssetViewModel by viewModels()

    private lateinit var trendingStoryAdapter: TrendingStoryAdapter
    private lateinit var newReleasesAdapter: AssetAdapter
    private lateinit var searchResultsAdapter: FullAssetAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupRecyclerViews()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Trending story cards
        trendingStoryAdapter = TrendingStoryAdapter { asset ->
            navigateToDetails(asset.id)
        }
        binding.rvTrending.adapter = trendingStoryAdapter

        // New releases horizontal cards
        newReleasesAdapter = AssetAdapter { id -> navigateToDetails(id) }
        binding.rvNewReleases.adapter = newReleasesAdapter

        // Inline search results grid
        searchResultsAdapter = FullAssetAdapter { id -> navigateToDetails(id) }
        binding.rvSearchResults.adapter = searchResultsAdapter
    }

    private fun setupSearch() {
        // Text change → trigger search
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                viewModel.onSearchQueryChanged(query)

                // Toggle clear button
                binding.btnClearSearch.isVisible = query.isNotEmpty()

                // Toggle between browse & search views
                if (query.isEmpty()) {
                    showBrowseMode()
                } else {
                    showSearchMode()
                }
            }
        })

        // IME search action
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else false
        }

        // Clear button
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            binding.etSearch.requestFocus()
            showBrowseMode()
        }
    }

    private fun showSearchMode() {
        binding.scrollContent.isVisible = false
        binding.layoutSearchResults.isVisible = true
    }

    private fun showBrowseMode() {
        binding.scrollContent.isVisible = true
        binding.layoutSearchResults.isVisible = false
        binding.layoutSearchEmpty.isVisible = false
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    private fun navigateToDetails(assetId: String) {
        val action = HomeFragmentDirections.actionHomeFragmentToAssetDetailsFragment(assetId)
        findNavController().navigate(action)
    }

    private fun observeViewModel() {
        // Trending
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.trendingAssets.collectLatest { resource ->
                if (_binding == null) return@collectLatest
                if (resource is Resource.Success) {
                    trendingStoryAdapter.submitList(resource.data)
                }
            }
        }

        // New releases
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.newReleases.collectLatest { resource ->
                if (_binding == null) return@collectLatest
                if (resource is Resource.Success) {
                    newReleasesAdapter.submitList(resource.data)
                }
            }
        }

        // Search results — only update the grid when in search mode
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchResults.collectLatest { resource ->
                val b = _binding ?: return@collectLatest
                val query = viewModel.searchQuery.value
                if (query.isEmpty()) return@collectLatest

                when (resource) {
                    is Resource.Success -> {
                        val results = resource.data ?: emptyList()
                        searchResultsAdapter.submitList(results)

                        val hasResults = results.isNotEmpty()
                        b.rvSearchResults.isVisible = hasResults
                        b.layoutSearchEmpty.isVisible = !hasResults
                        b.tvSearchResultsLabel.text =
                            if (hasResults) "${results.size} result${if (results.size == 1) "" else "s"} for \"$query\""
                            else "No results"
                    }
                    is Resource.Loading -> {
                        // Keep showing previous results while loading
                    }
                    is Resource.Error -> {
                        b.layoutSearchEmpty.isVisible = true
                        b.rvSearchResults.isVisible = false
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
