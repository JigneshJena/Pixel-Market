package com.pixelmarket.app.presentation.downloads

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import android.widget.Toast
import com.google.android.material.tabs.TabLayout
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
    private lateinit var uploadsAdapter: AssetGridAdapter
    private lateinit var downloadHistoryAdapter: DownloadHistoryAdapter
    private lateinit var cartAdapter: CartAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDownloadsBinding.bind(view)

        setupRecyclerViews()
        setupTabs()
        observeViewModel()

        arguments?.getString("screenTitle")?.let { title ->
            // Keep the custom title if passed
            binding.tvTopHeader.text = title
        }

        val isDeveloperAssetsMode = arguments?.getBoolean("showDeveloperAssets", false) == true
        if (isDeveloperAssetsMode) {
            // Select "My Uploads" tab automatically
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1))
            showUploadsTab()
        } else {
            showPurchasedTab()
        }

        // Always trigger loading developers assets so they are ready
        viewModel.loadDeveloperAssets()

        arguments?.getBoolean("scrollToCart", false)?.let { shouldScroll ->
            if (shouldScroll) {
                binding.scrollView.post {
                    binding.scrollView.smoothScrollTo(0, binding.sectionCart.top)
                }
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showPurchasedTab()
                    1 -> showUploadsTab()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showPurchasedTab() {
        binding.scrollView.isVisible = true
        binding.scrollViewUploads.isVisible = false
        updateCheckoutBarVisibility()
    }

    private fun showUploadsTab() {
        binding.scrollView.isVisible = false
        binding.scrollViewUploads.isVisible = true
        updateCheckoutBarVisibility()
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

        // Purchased Library Grid
        libraryAdapter = AssetGridAdapter { assetId ->
            val action = DownloadsFragmentDirections.actionDownloadsFragmentToAssetDetailsFragment(assetId)
            findNavController().navigate(action)
        }
        binding.rvLibrary.adapter = libraryAdapter

        // My Uploads Grid
        uploadsAdapter = AssetGridAdapter { assetId ->
            val action = DownloadsFragmentDirections.actionDownloadsFragmentToAssetDetailsFragment(assetId)
            findNavController().navigate(action)
        }
        binding.rvUploads.adapter = uploadsAdapter

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
                updateCheckoutBarVisibility()
                
                if (list.isNotEmpty()) {
                    binding.tvCartCount.text = "${list.size} ${if (list.size == 1) "Item" else "Items"} in Cart"
                    val total = list.sumOf { it.price }
                    binding.tvCartTotal.text = "₹${String.format("%.2f", total)}"
                }
                
                updatePurchasedEmptyState()
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
                    is Resource.Loading -> { }
                    else -> {}
                }
            }
        }

        // Purchased Assets
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.purchasedAssets.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val list = resource.data ?: emptyList()
                        libraryAdapter.submitList(list)
                        binding.tvLibraryHeader.isVisible = list.isNotEmpty()
                        binding.rvLibrary.isVisible = list.isNotEmpty()
                        updatePurchasedEmptyState()
                    }
                    is Resource.Error -> {
                        libraryAdapter.submitList(emptyList())
                        binding.tvLibraryHeader.isVisible = false
                        binding.rvLibrary.isVisible = false
                        updatePurchasedEmptyState()
                    }
                    is Resource.Loading -> {}
                }
            }
        }

        // Uploaded (Developer) Assets
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.developerAssets.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val list = resource.data ?: emptyList()
                        uploadsAdapter.submitList(list)
                        val isEmpty = list.isEmpty()
                        binding.tvEmptyUploads.isVisible = isEmpty
                        binding.rvUploads.isVisible = !isEmpty
                    }
                    is Resource.Error -> {
                        uploadsAdapter.submitList(emptyList())
                        binding.tvEmptyUploads.isVisible = true
                        binding.rvUploads.isVisible = false
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    private fun updatePurchasedEmptyState() {
        val cartEmpty = viewModel.cartItems.value.isEmpty()
        val purchasedEmpty = (viewModel.purchasedAssets.value as? Resource.Success)?.data.isNullOrEmpty()
        val showEmpty = cartEmpty && purchasedEmpty
        binding.tvEmpty.isVisible = showEmpty
    }

    private fun updateCheckoutBarVisibility() {
        val isPurchasedTabActive = binding.tabLayout.selectedTabPosition == 0
        val hasCartItems = viewModel.cartItems.value.isNotEmpty()
        binding.layoutCheckoutBar.isVisible = isPurchasedTabActive && hasCartItems
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
