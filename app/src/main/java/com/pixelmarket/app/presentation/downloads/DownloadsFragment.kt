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

        // Purchased Assets Library
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.purchasedAssets.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val list = resource.data ?: emptyList()
                        libraryAdapter.submitList(list)
                        binding.tvLibraryHeader.isVisible = list.isNotEmpty()
                        binding.rvLibrary.isVisible = list.isNotEmpty()
                        updateEmptyState()
                    }
                    is Resource.Error -> {
                        // Silently ignore errors (e.g. Firestore permission denied).
                        // Show an empty list — never expose raw error messages.
                        libraryAdapter.submitList(emptyList())
                        binding.tvLibraryHeader.isVisible = false
                        binding.rvLibrary.isVisible = false
                        updateEmptyState()
                    }
                    is Resource.Loading -> {
                        // loading
                    }
                }
            }
        }

        // Download History
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.downloadHistory.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val list = resource.data ?: emptyList()
                        downloadHistoryAdapter.submitList(list)
                        binding.tvDownloadHistoryHeader.isVisible = list.isNotEmpty()
                        binding.rvDownloadHistory.isVisible = list.isNotEmpty()

                        // Show empty state only when library, history and cart are empty
                        val hasNoData = list.isEmpty() &&
                            (viewModel.purchasedAssets.value as? Resource.Success)?.data.isNullOrEmpty() &&
                            viewModel.cartItems.value.isEmpty()
                        binding.tvEmpty.isVisible = hasNoData
                    }
                    is Resource.Error -> {
                        // Silently ignore (e.g. Firestore permission denied).
                        // Still check if there is any library content visible.
                        downloadHistoryAdapter.submitList(emptyList())
                        binding.tvDownloadHistoryHeader.isVisible = false
                        binding.rvDownloadHistory.isVisible = false
                        val hasLibrary = (viewModel.purchasedAssets.value as? Resource.Success)?.data.isNullOrEmpty() == false
                        binding.tvEmpty.isVisible = !hasLibrary && viewModel.cartItems.value.isEmpty()
                    }
                    is Resource.Loading -> {
                        // loading
                    }
                }
            }
        }
    }

    private fun updateEmptyState() {
        val cartEmpty = viewModel.cartItems.value.isEmpty()
        val libraryEmpty = (viewModel.purchasedAssets.value as? Resource.Success)?.data.isNullOrEmpty()
        val showEmpty = cartEmpty && libraryEmpty
        binding.tvEmpty.isVisible = showEmpty
        binding.btnBrowseMarket.isVisible = showEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

