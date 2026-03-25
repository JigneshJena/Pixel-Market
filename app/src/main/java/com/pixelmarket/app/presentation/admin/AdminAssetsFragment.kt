package com.pixelmarket.app.presentation.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.FragmentAdminAssetsBinding
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminAssetsFragment : Fragment(R.layout.fragment_admin_assets) {

    private var _binding: FragmentAdminAssetsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by activityViewModels()
    private lateinit var adapter: AdminAssetAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminAssetsBinding.bind(view)

        setupRecyclerView()
        setupToolbar()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = AdminAssetAdapter(
            onApproveToggle = { id, approved -> viewModel.approveAsset(id, approved) },
            onFeaturedToggle = { id, featured -> viewModel.toggleAssetFeatured(id, featured) },
            onDeleteClick = { id -> viewModel.deleteAsset(id) }
        )
        binding.rvAssets.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = this@AdminAssetsFragment.adapter
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                val assets = viewModel.assets.value
                val filtered = if (newText.isNullOrBlank()) assets 
                else assets.filter { it.title.contains(newText, ignoreCase = true) }
                adapter.submitList(filtered)
                return true
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.assets.collectLatest { list ->
                val b = _binding ?: return@collectLatest
                adapter.submitList(list)
                b.tvEmptyState.isVisible = list.isEmpty()
                b.rvAssets.isVisible = list.isNotEmpty()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { resource ->
                val b = _binding ?: return@collectLatest
                b.progressBar.isVisible = resource is Resource.Loading
                if (resource is Resource.Error) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        resource.message ?: "An error occurred",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
