package com.pixelmarket.app.presentation.marketplace

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.FragmentMarketplaceBinding
import com.pixelmarket.app.presentation.home.AssetAdapter
import com.pixelmarket.app.presentation.home.AssetGridAdapter
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MarketplaceFragment : Fragment(R.layout.fragment_marketplace) {

    private var _binding: FragmentMarketplaceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MarketplaceViewModel by viewModels()
    private lateinit var assetAdapter: AssetGridAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private val categories = listOf("All", "3D Models", "Textures", "UI Kits", "Scripts", "Animations", "Music", "Others")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMarketplaceBinding.bind(view)

        setupRecyclerViews()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Assets RecyclerView
        assetAdapter = AssetGridAdapter { id ->
            val action = MarketplaceFragmentDirections.actionMarketplaceFragmentToAssetDetailsFragment(id)
            findNavController().navigate(action)
        }
        binding.rvMarketplace.adapter = assetAdapter

        // Categories RecyclerView
        categoryAdapter = CategoryAdapter(categories) { category ->
            viewModel.filterByCategory(category)
        }
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupSearch() {
        // Remove the default background from the search plate to kill internal shadows
        val searchPlate = binding.searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchPlate?.background = null
        val submitArea = binding.searchView.findViewById<View>(androidx.appcompat.R.id.submit_area)
        submitArea?.background = null

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchAssets(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchAssets(newText ?: "")
                return true
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.assets.collectLatest { resource ->
                val b = _binding ?: return@collectLatest
                b.progressBar.isVisible = resource is Resource.Loading
                when (resource) {
                    is Resource.Success -> {
                        assetAdapter.submitList(resource.data)
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Loading -> {
                        // Handled by visibility
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedCategory.collectLatest { category ->
                if (_binding == null) return@collectLatest
                categoryAdapter.updateSelected(category)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
