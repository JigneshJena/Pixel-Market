package com.pixelmarket.app.presentation.downloads

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import android.widget.Toast
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.FragmentDownloadsBinding
import com.pixelmarket.app.presentation.home.AssetGridAdapter
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadsFragment : Fragment(R.layout.fragment_downloads) {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DownloadsViewModel by viewModels()
    private lateinit var downloadsAdapter: DownloadsAdapter
    private lateinit var libraryAdapter: AssetGridAdapter
    private lateinit var downloadHistoryAdapter: DownloadHistoryAdapter
    private lateinit var cartAdapter: CartAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDownloadsBinding.bind(view)

        setupRecyclerViews()
        observeViewModel()

        arguments?.getString("screenTitle")?.let { title ->
            binding.tvTopHeader.text = title
        }

        val isDeveloperAssetsMode = arguments?.getBoolean("showDeveloperAssets", false) == true
        if (isDeveloperAssetsMode) {
            viewModel.loadDeveloperAssets()
            binding.tvLibraryHeader.text = "MY UPLOADED ASSETS"
        }

        arguments?.getBoolean("scrollToCart", false)?.let { shouldScroll ->
            if (shouldScroll) {
                binding.scrollView.post {
                    binding.scrollView.smoothScrollTo(0, binding.sectionCart.top)
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        // Shopping Cart
        cartAdapter = CartAdapter(
            onItemClick = { assetId ->
                val action = DownloadsFragmentDirections.actionDownloadsFragmentToAssetDetailsFragment(assetId)
                findNavController().navigate(action)
            },
            onRemoveClick = { assetId ->
                viewModel.removeFromCart(assetId)
            }
        )
        binding.rvCart.adapter = cartAdapter

        // Active Downloads
        downloadsAdapter = DownloadsAdapter { downloadId ->
            viewModel.cancelDownload(downloadId)
        }
        binding.rvDownloads.adapter = downloadsAdapter

        // Purchased Library
        libraryAdapter = AssetGridAdapter { assetId ->
            val action = DownloadsFragmentDirections.actionDownloadsFragmentToAssetDetailsFragment(assetId)
            findNavController().navigate(action)
        }
        binding.rvLibrary.adapter = libraryAdapter

        // Download History
        downloadHistoryAdapter = DownloadHistoryAdapter { assetId ->
            val action = DownloadsFragmentDirections.actionDownloadsFragmentToAssetDetailsFragment(assetId)
            findNavController().navigate(action)
        }
        binding.rvDownloadHistory.adapter = downloadHistoryAdapter
        
        binding.btnCheckout.setOnClickListener {
            viewModel.checkoutAll()
        }

        binding.btnBrowseMarket.setOnClickListener {
            findNavController().navigate(R.id.marketplaceFragment)
        }
    }

    private fun observeViewModel() {
        val isDeveloperAssetsMode = arguments?.getBoolean("showDeveloperAssets", false) == true

        // Active Downloads
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.downloads.collectLatest { list ->
                downloadsAdapter.submitList(list)
                binding.tvActiveDownloadsHeader.isVisible = list.isNotEmpty()
                binding.rvDownloads.isVisible = list.isNotEmpty()
                binding.sectionActive.isVisible = list.isNotEmpty()
            }
        }

        // Shopping Cart Items
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cartItems.collectLatest { list ->
                cartAdapter.submitList(list)
                binding.sectionCart.isVisible = list.isNotEmpty()
                
                // Update Sticky Checkout Bar
                binding.layoutCheckoutBar.isVisible = list.isNotEmpty()
                if (list.isNotEmpty()) {
                    binding.tvCartCount.text = "${list.size} ${if (list.size == 1) "Item" else "Items"} in Cart"
                    val total = list.sumOf { it.price }
                    binding.tvCartTotal.text = "₹${String.format("%.2f", total)}"
                }
                
                updateEmptyState()
            }
        }

        // Checkout Status
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.checkoutStatus.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        Toast.makeText(requireContext(), "Purchase successful! Assets added to library.", Toast.LENGTH_LONG).show()
                        viewModel.resetCheckoutStatus()
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                        viewModel.resetCheckoutStatus()
                    }
                    is Resource.Loading -> {
                        // could show a progress dialog here
                    }
                    else -> {}
                }
            }
        }

        // Purchased Assets Library (or Developer Assets)
        viewLifecycleOwner.lifecycleScope.launch {
            if (isDeveloperAssetsMode) {
                viewModel.developerAssets.collectLatest { resource ->
                    handleAssetListUpdate(resource)
                }
            } else {
                viewModel.purchasedAssets.collectLatest { resource ->
                    handleAssetListUpdate(resource)
                }
            }
        }
    }

    private fun handleAssetListUpdate(resource: Resource<List<Asset>>) {
        when (resource) {
            is Resource.Success -> {
                val list = resource.data ?: emptyList()
                libraryAdapter.submitList(list)
                binding.tvLibraryHeader.isVisible = list.isNotEmpty()
                binding.rvLibrary.isVisible = list.isNotEmpty()
                updateEmptyState()
            }
            is Resource.Error -> {
                libraryAdapter.submitList(emptyList())
                binding.tvLibraryHeader.isVisible = false
                binding.rvLibrary.isVisible = false
                updateEmptyState()
            }
            is Resource.Loading -> {}
        }
    }

    private fun updateEmptyState() {
        val cartEmpty = viewModel.cartItems.value.isEmpty()
        val purchasedEmpty = (viewModel.purchasedAssets.value as? Resource.Success)?.data.isNullOrEmpty()
        val developerEmpty = (viewModel.developerAssets.value as? Resource.Success)?.data.isNullOrEmpty()
        
        val isDevMode = arguments?.getBoolean("showDeveloperAssets", false) == true
        val showEmpty = if (isDevMode) developerEmpty else (cartEmpty && purchasedEmpty)
        
        binding.tvEmpty.isVisible = showEmpty
        binding.btnBrowseMarket.isVisible = showEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

