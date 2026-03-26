package com.pixelmarket.app.presentation.details

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.pixelmarket.app.R
import com.pixelmarket.app.data.payment.RazorpayManager
import com.pixelmarket.app.databinding.FragmentAssetDetailsBinding
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.util.Resource
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AssetDetailsFragment : Fragment(R.layout.fragment_asset_details), PaymentResultListener {

    private var _binding: FragmentAssetDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AssetDetailsViewModel by viewModels()
    private val args: AssetDetailsFragmentArgs by navArgs()

    @Inject
    lateinit var razorpayManager: RazorpayManager

    private val currentUser get() = FirebaseAuth.getInstance().currentUser
    private var isPlayingVideo = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.asset.value.data?.let { initiatePayment(it) }
        } else {
            Toast.makeText(
                requireContext(),
                "Storage permission is required to download assets",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAssetDetailsBinding.bind(view)

        razorpayManager.preloadCheckout(requireActivity())

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        viewModel.loadAssetDetails(args.assetId)

        setupClickListeners()
        observeViewModel()
    }

    // ──────────────────────────────────────────────────────────────
    // Click Listeners
    // ──────────────────────────────────────────────────────────────
    private fun setupClickListeners() {
        binding.btnBuy.setOnClickListener {
            viewModel.asset.value.data?.let { asset ->
                if (currentUser == null) {
                    Toast.makeText(requireContext(), "Please login to purchase", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (viewModel.isPurchased.value) {
                    viewModel.downloadAssetDirectly(asset)
                } else {
                    showModernPaymentSheet(asset)
                }
            }
        }

        binding.btnFavorite.setOnClickListener {
            if (currentUser == null) {
                Toast.makeText(requireContext(), "Please login to like assets", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Haptic/Visual feedback
            binding.btnFavorite.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100)
                .withEndAction {
                    binding.btnFavorite.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }.start()
                
            viewModel.toggleLike()
        }

        binding.btnShare.setOnClickListener {
            val asset = viewModel.asset.value.data ?: return@setOnClickListener
            shareAsset(asset.title, asset.id)
        }

        // Play / pause video preview
        binding.btnPlayVideo.setOnClickListener { toggleVideoPreview() }

        binding.btnSubmitRating.setOnClickListener {
            val rating = binding.ratingBar.rating
            if (rating < 1.0f) {
                Toast.makeText(requireContext(), "Please select at least 1 star", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.submitRating(rating)
            // Success Toast will be implied by the section disappearing
        }

        binding.btnAddToCart.setOnClickListener {
            viewModel.toggleCart()
        }

        binding.btnCreator.setOnClickListener {
            viewModel.asset.value.data?.let { asset ->
                showCreatorBottomSheet(asset)
            }
        }

        // Edit button — navigate to UploadFragment in edit mode (owner only)
        binding.btnEditAsset.setOnClickListener {
            val bundle = android.os.Bundle().apply {
                putString("editAssetId", args.assetId)
            }
            findNavController().navigate(R.id.action_assetDetailsFragment_to_uploadFragment, bundle)
        }
    }

    private fun showCreatorBottomSheet(asset: Asset) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_creator, null)
        dialog.setContentView(view)

        val ivCreator = view.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.ivCreatorSheet)
        val tvName = view.findViewById<android.widget.TextView>(R.id.tvCreatorSheetName)
        val tvAssets = view.findViewById<android.widget.TextView>(R.id.tvCreatorSheetAssets)
        val tvRating = view.findViewById<android.widget.TextView>(R.id.tvCreatorSheetRating)
        val btnClose = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseCreatorSheet)

        val seller = viewModel.sellerProfile.value
        val sellerName = seller?.username?.ifBlank { seller.email?.substringBefore("@") } ?: asset.sellerName.ifBlank { "Anonymous Creator" }
        tvName.text = sellerName
        tvAssets.text = "${seller?.assetsUploaded ?: asset.sellerAssetCount} Assets"
        val rating = seller?.rating ?: asset.sellerRating
        tvRating.text = if (rating > 0) String.format("%.1f ★", rating) else "Unrated"

        val avatar = seller?.profileImageUrl ?: asset.sellerAvatarUrl
        if (!avatar.isNullOrBlank()) {
            ivCreator.load(avatar) { crossfade(true) }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }


    // ──────────────────────────────────────────────────────────────
    // ViewModel Observers
    // ──────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.hasUserRated.collectLatest {
                updateRatingVisibility()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isPurchased.collectLatest {
                val b = _binding ?: return@collectLatest
                b.btnBuy.text = if (it) "Download" else "Buy Now"
                // Hide cart icon when user already owns the asset
                b.btnAddToCart.isVisible = !it
                updateRatingVisibility()
            }
        }

        // Owner check — swap Buy for Edit button
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isOwnAsset.collectLatest { isOwner ->
                val b = _binding ?: return@collectLatest
                b.btnBuy.isVisible = !isOwner
                b.btnAddToCart.isVisible = !isOwner && !viewModel.isPurchased.value
                b.btnEditAsset.isVisible = isOwner
                // Owners don't rate their own asset
                if (isOwner) {
                    b.containerRating.isVisible = false
                    b.tvRatingLocked.isVisible = false
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLiked.collectLatest { isLiked ->
                val b = _binding ?: return@collectLatest
                if (isLiked) {
                    b.ivFavorite.setImageResource(R.drawable.ic_favorite)
                    b.ivFavorite.setColorFilter(android.graphics.Color.parseColor("#EF4444"))
                    b.btnFavorite.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EF4444")))
                } else {
                    b.ivFavorite.setImageResource(R.drawable.ic_favorite_outline)
                    b.ivFavorite.clearColorFilter()
                    b.ivFavorite.setColorFilter(requireContext().getColor(R.color.slate_900))
                    b.btnFavorite.setStrokeColor(android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.glass_border)))
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.likeCount.collectLatest { count ->
                val b = _binding ?: return@collectLatest
                b.tvLikeCount.text = formatCount(count)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.asset.collectLatest { resource ->
                if (_binding == null) return@collectLatest
                when (resource) {
                    is Resource.Success -> bindAsset(resource.data ?: return@collectLatest)
                    is Resource.Loading -> { /* optionally show shimmer */ }
                    is Resource.Error -> { /* silently handle */ }
                }
            }
        }

        // Live seller info overrides
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sellerProfile.collectLatest { seller ->
                val b = _binding ?: return@collectLatest
                if (seller != null) {
                    val fallbackName = viewModel.asset.value.data?.sellerName ?: "Anonymous"
                    b.tvSeller.text = seller.username.ifBlank { seller.email.substringBefore("@") }.ifBlank { fallbackName }

                    val roleLabel = if (seller.isAdmin || seller.legacyAdmin || seller.role == "admin") "Admin on PixelMarket"
                                    else if (seller.isDeveloper || seller.legacyDeveloper || seller.role == "seller" || seller.role == "developer") "Developer on PixelMarket"
                                    else "Creator on PixelMarket"
                    
                    val stats = buildString {
                        if (seller.assetsUploaded > 0) append("${seller.assetsUploaded} Assets")
                        if (seller.rating > 0) {
                            if (isNotEmpty()) append("  •  ")
                            append(String.format("%.1f", seller.rating)).append(" ★")
                        }
                        if (isEmpty()) append(roleLabel)
                    }
                    b.tvCreatorStats.text = stats

                    if (!seller.profileImageUrl.isNullOrBlank()) {
                        b.ivCreator.load(seller.profileImageUrl) { 
                            crossfade(true)
                            placeholder(R.drawable.ic_profile)
                            error(R.drawable.ic_profile)
                        }
                    } else {
                        b.ivCreator.setImageResource(R.drawable.ic_profile)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.purchaseState.collectLatest { state ->
                val b = _binding ?: return@collectLatest
                when (state) {
                    is PurchaseState.Processing -> {
                        b.btnBuy.isEnabled = false
                        b.btnBuy.text = "Processing..."
                    }
                    is PurchaseState.Success -> {
                        b.btnBuy.isEnabled = true
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("🎉 Purchase Successful!")
                            .setMessage("Your asset is being downloaded.\nCheck the Downloads section to view your purchased assets.")
                            .setPositiveButton("OK") { d, _ -> d.dismiss() }
                            .setNeutralButton("View Downloads") { d, _ ->
                                d.dismiss()
                                findNavController().navigate(R.id.downloadsFragment)
                            }
                            .show()
                        viewModel.resetPurchaseState()
                    }
                    is PurchaseState.Error -> {
                        b.btnBuy.isEnabled = true
                        b.btnBuy.text = "Buy Now"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetPurchaseState()
                    }
                    is PurchaseState.Idle -> {
                        b.btnBuy.isEnabled = true
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ratingSuccessEvent.collectLatest {
                val b = _binding ?: return@collectLatest
                Toast.makeText(requireContext(), "⭐ Thanks for rating!", Toast.LENGTH_SHORT).show()
                b.ratingBar.rating = 0f
                updateRatingVisibility()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorEvent.collectLatest { message ->
                if (_binding == null) return@collectLatest
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteEvent.collectLatest { event ->
                if (_binding == null) return@collectLatest
                if (event == "success") {
                    Toast.makeText(requireContext(), "Asset deleted successfully", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else if (event.startsWith("error:")) {
                    Toast.makeText(requireContext(), event.substringAfter("error:"), Toast.LENGTH_LONG).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isInCart.collectLatest { inCart ->
                val b = _binding ?: return@collectLatest
                // Tint icon green when in cart, primary color otherwise
                // And change button text
                val tintColor = if (inCart)
                    requireContext().getColor(android.R.color.holo_green_dark)
                else
                    requireContext().getColor(R.color.primary)
                
                b.ivCartIcon.setColorFilter(tintColor)
                b.btnAddToCart.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(tintColor)
                )
                // If btnAddToCart was a MaterialButton with text, we could set it
                // Currently it's a CardView in the layout (top right), but let's 
                // check if the user wants it in the bottom bar or just a tooltip.
                // Re-reading fragment_asset_details.xml... 
                // btnAddToCart is a MaterialCardView at line 276.
                // Wait, it doesn't have a TextView inside it for text.
                // I will add a tooltip or change the icon.
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cartEvent.collectLatest { event ->
                if (_binding == null) return@collectLatest
                val msg = when {
                    event == "added" -> "🛒 Added to cart!"
                    event == "removed" -> "🗑️ Removed from cart"
                    event == "already_in_cart" -> "Already in your cart"
                    event == "already_purchased" -> "You already own this asset"
                    event == "login_required" -> "Please login to use the cart"
                    event.startsWith("error:") -> event.substringAfter("error:")
                    else -> "Action failed. Try again."
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Bind asset data to UI — ALL dynamic, nothing hardcoded
    // ──────────────────────────────────────────────────────────────
    private fun bindAsset(asset: Asset) {
        // Thumbnail
        binding.ivThumbnail.load(asset.thumbnailUrl) { crossfade(true) }

        // Title & category
        binding.tvTitle.text = asset.title
        binding.tvAssetCategory.text = asset.category.uppercase()
        binding.tvBadge.text = asset.category.uppercase()

        // Description
        binding.tvDescription.text = asset.description.ifBlank { "No description provided." }



        // Metrics (Real-time synced via Snapshot Listener)
        binding.tvRating.text = if (asset.rating > 0) String.format("%.1f", asset.rating) else "—"
        binding.tvDownloads.text = formatCount(asset.downloadCount)
        // tvLikeCount is handled by its own observer for extra snappiness

        // Price
        binding.tvPrice.text = if (asset.price == 0.0) "Free" else "₹${String.format("%.2f", asset.price)}"

        // File size chips
        val sizeLabel = asset.fileSize.ifBlank { "—" }
        binding.tvFileSize.text = sizeLabel
        binding.tvSpecFileSize.text = sizeLabel

        // Tech specs
        binding.tvFormat.text = asset.fileType.uppercase().ifBlank { "—" }
        binding.tvSpecCategory.text = asset.category.ifBlank { "—" }
        binding.tvReviewCount.text = asset.reviewCount.toString()

        // Tags as chips
        binding.chipGroupTags.removeAllViews()
        asset.tags.take(8).forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = "#$tag"
                isCheckable = false
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#1A6366F1")
                )
                setTextColor(android.graphics.Color.parseColor("#6366F1"))
                textSize = 11f
            }
            binding.chipGroupTags.addView(chip)
        }
        binding.chipGroupTags.isVisible = asset.tags.isNotEmpty()

        // ── Creator (dynamic) ──────────────────────────────────────
        binding.tvSeller.text = asset.sellerName.ifBlank { "Anonymous" }

        val creatorStats = buildString {
            if (asset.sellerAssetCount > 0) append("${asset.sellerAssetCount} Assets")
            if (asset.sellerRating > 0) {
                if (isNotEmpty()) append("  •  ")
                append(String.format("%.1f", asset.sellerRating)).append(" ★")
            }
            if (isEmpty()) append("Creator on PixelMarket")
        }
        binding.tvCreatorStats.text = creatorStats

        // Creator avatar
        if (!asset.sellerAvatarUrl.isNullOrBlank()) {
            binding.ivCreator.load(asset.sellerAvatarUrl) { crossfade(true) }
        } else {
            binding.ivCreator.setImageResource(R.drawable.ic_profile)
        }

        // ── Preview video ──────────────────────────────────────────
        // Primary: use previewVideoUrl field; Fallback: check if main asset file is a video
        val videoUrl = asset.previewVideoUrl?.takeIf { it.isNotBlank() }
            ?: asset.fileUrls.firstOrNull { url ->
                val lower = url.lowercase()
                lower.contains(".mp4") || lower.contains(".mov") ||
                lower.contains(".avi") || lower.contains(".webm") ||
                lower.contains("/video/") || lower.contains("preview_videos")
            }

        if (!videoUrl.isNullOrBlank()) {
            binding.btnPlayVideo.isVisible = true
            setupVideoPlayer(videoUrl)
        } else {
            binding.btnPlayVideo.isVisible = false
            binding.videoPreview.isVisible = false
            binding.tvPreviewLabel.isVisible = false
        }

    }

    // ──────────────────────────────────────────────────────────────
    // Preview Video
    // ──────────────────────────────────────────────────────────────
    private fun setupVideoPlayer(url: String) {
        binding.videoPreview.setVideoURI(Uri.parse(url))
        binding.videoPreview.setOnPreparedListener { mp ->
            mp.isLooping = true  // loop short preview clip
            mp.setVolume(0f, 0f) // muted by default
        }
        binding.videoPreview.setOnErrorListener { _, _, _ ->
            // Silently fall back to thumbnail on error
            binding.videoPreview.isVisible = false
            binding.btnPlayVideo.isVisible = false
            binding.tvPreviewLabel.isVisible = false
            true
        }
    }

    private fun toggleVideoPreview() {
        if (!isPlayingVideo) {
            binding.ivThumbnail.isVisible = false
            binding.videoPreview.isVisible = true
            binding.tvPreviewLabel.isVisible = true
            binding.btnPlayVideo.isVisible = false
            binding.videoPreview.start()
            isPlayingVideo = true
        } else {
            binding.videoPreview.pause()
            binding.videoPreview.isVisible = false
            binding.ivThumbnail.isVisible = true
            binding.tvPreviewLabel.isVisible = false
            binding.btnPlayVideo.isVisible = true
            isPlayingVideo = false
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Payment
    // ──────────────────────────────────────────────────────────────
    private fun showModernPaymentSheet(asset: Asset) {
        lifecycleScope.launch {
            val balance = viewModel.getWalletBalance()
            val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
            val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_payment, null)
            dialog.setContentView(sheetView)

            sheetView.findViewById<android.widget.TextView>(R.id.bsAssetTitle)?.text = asset.title
            sheetView.findViewById<android.widget.TextView>(R.id.bsAssetPrice)?.text =
                "₹${String.format("%.2f", asset.price)}"
            sheetView.findViewById<android.widget.ImageView>(R.id.bsThumbnail)?.load(asset.thumbnailUrl) { crossfade(true) }

            val walletCard = sheetView.findViewById<android.view.View>(R.id.cardWalletPay)
            val razorpayCard = sheetView.findViewById<android.view.View>(R.id.cardRazorpayPay)
            val walletBalanceTv = sheetView.findViewById<android.widget.TextView>(R.id.tvWalletBalance)
            val cancelBtn = sheetView.findViewById<android.widget.TextView>(R.id.btnCancel)

            walletBalanceTv?.text = "Balance: ₹${String.format("%.2f", balance)}"
            if (balance >= asset.price) {
                walletCard?.alpha = 1f
                walletCard?.isEnabled = true
            } else {
                walletCard?.alpha = 0.5f
                walletCard?.isEnabled = false
                walletBalanceTv?.text = "Balance: ₹${String.format("%.2f", balance)} (Insufficient)"
            }

            walletCard?.setOnClickListener { dialog.dismiss(); viewModel.buyWithWallet(asset) }
            razorpayCard?.setOnClickListener { dialog.dismiss(); checkPermissionsAndPay(asset) }
            cancelBtn?.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
    }

    private fun checkPermissionsAndPay(asset: Asset) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }
        }
        initiatePayment(asset)
    }

    private fun initiatePayment(asset: Asset) {
        val user = currentUser ?: return
        razorpayManager.startPayment(
            activity = requireActivity(),
            asset = asset,
            userEmail = user.email ?: "user@pixelmarket.com",
            userPhone = user.phoneNumber ?: "9999999999",
            userName = user.displayName ?: "PixelMarket User",
            listener = this
        )
    }

    private fun shareAsset(title: String, assetId: String) {
        val text = "Check out \"$title\" on PixelMarket! 🎨\n\nhttps://pixelmarket.app/asset/$assetId"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "PixelMarket – $title")
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Share Asset via"))
    }

    // ──────────────────────────────────────────────────────────────
    // Razorpay callbacks
    // ──────────────────────────────────────────────────────────────
    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Log.d("PaymentSuccess", "Payment ID: $razorpayPaymentId")
        viewModel.asset.value.data?.let { viewModel.buyNow(it) }
        Toast.makeText(requireContext(), "Payment successful! Downloading asset...", Toast.LENGTH_LONG).show()
    }

    override fun onPaymentError(errorCode: Int, errorDescription: String?) {
        Log.e("PaymentError", "Error Code: $errorCode, Description: $errorDescription")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Payment Failed")
            .setMessage(
                when (errorCode) {
                    Checkout.NETWORK_ERROR -> "Network error. Please check your internet connection."
                    Checkout.INVALID_OPTIONS -> "Invalid payment configuration. Please try again."
                    Checkout.PAYMENT_CANCELED -> "Payment was cancelled."
                    Checkout.TLS_ERROR -> "Security error. Please update your device."
                    else -> errorDescription ?: "Payment failed. Please try again."
                }
            )
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .setNeutralButton("Retry") { d, _ ->
                d.dismiss()
                viewModel.asset.value.data?.let { initiatePayment(it) }
            }
            .show()
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────
    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> "${String.format("%.1f", count / 1_000_000.0)}M"
        count >= 1_000 -> "${String.format("%.1f", count / 1_000.0)}k"
        else -> count.toString()
    }

    override fun onPause() {
        super.onPause()
        if (isPlayingVideo) {
            binding.videoPreview.pause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.videoPreview.stopPlayback()
        _binding = null
    }

    private fun updateRatingVisibility() {
        val b = _binding ?: return
        val purchased = viewModel.isPurchased.value
        val alreadyRated = viewModel.hasUserRated.value
        val loggedIn = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null

        when {
            // Not logged in — hide everything
            !loggedIn -> {
                b.containerRating.isVisible = false
                b.tvRatingLocked.isVisible = false
            }
            // Purchased + already rated — show thank-you
            purchased && alreadyRated -> {
                b.containerRating.isVisible = false
                b.tvRatingLocked.text = "⭐ Thanks for your rating!"
                b.tvRatingLocked.isVisible = true
            }
            // Purchased + not yet rated — show rate form
            purchased && !alreadyRated -> {
                b.containerRating.isVisible = true
                b.tvRatingLocked.isVisible = false
            }
            // Not purchased — show lock hint
            else -> {
                b.containerRating.isVisible = false
                b.tvRatingLocked.text = "🔒 Purchase this asset to rate it"
                b.tvRatingLocked.isVisible = true
            }
        }
    }
}
