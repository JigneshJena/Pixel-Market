package com.pixelmarket.app.presentation.upload

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.FragmentUploadBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import coil.load
import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class UploadFragment : Fragment(R.layout.fragment_upload) {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UploadViewModel by viewModels()

    private val imageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadThumbnail(it) }
    }

    private val fileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadAssetFile(it) }
    }

    // Preview video picker — accepts only video files
    private val videoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadPreviewVideo(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUploadBinding.bind(view)

        binding.cvThumbnail.setOnClickListener { imageLauncher.launch("image/*") }
        binding.cvAssetFile.setOnClickListener { fileLauncher.launch("*/*") }
        binding.cvPreviewVideo.setOnClickListener { videoLauncher.launch("video/*") }
        
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnPublish.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val category = binding.etCategory.text.toString().trim()
            val priceStr = binding.etPrice.text.toString().trim()
            val price = priceStr.toDoubleOrNull() ?: 0.0

            if (title.isNotEmpty() && category.isNotEmpty()) {
                viewModel.publishAsset(title, description, category, price)
            } else {
                Toast.makeText(requireContext(), "Fill required fields", Toast.LENGTH_SHORT).show()
            }
        }

        observeViewModel()
        startEntranceAnimations()

        // Check for edit mode
        arguments?.getString("editAssetId")?.takeIf { it.isNotBlank() }?.let { editAssetId ->
            binding.tvScreenTitle.text = "EDIT ASSET"
            binding.btnPublish.text = "Update Asset"
            viewModel.initEditMode(editAssetId)
        }
    }

    private fun startEntranceAnimations() {
        binding.topBar.alpha = 0f
        binding.topBar.translationY = -20f
        binding.topBar.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .start()

        // Animate the main content container slowly
        binding.bottomBar.translationY = 100f
        binding.bottomBar.animate()
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(200)
            .start()
    }

    private fun observeViewModel() {
        // Prefill existing data in Edit Mode
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.initialEditData.collectLatest { asset ->
                val b = _binding ?: return@collectLatest
                if (asset != null) {
                    if (b.etTitle.text.isNullOrBlank()) {
                        b.etTitle.setText(asset.title)
                        b.etDescription.setText(asset.description)
                        b.etCategory.setText(asset.category)
                        b.etPrice.setText(asset.price.toString())
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.thumbnailUrl.collectLatest { url ->
                val b = _binding ?: return@collectLatest
                if (url != null) {
                    b.tvThumbnailStatus.text = "Cover Image Ready"
                    b.llThumbnailEmpty.visibility = View.GONE
                    b.ivThumbnailPreview.visibility = View.VISIBLE
                    b.ivThumbnailPreview.load(url) {
                        crossfade(true)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.assetFileUrl.collectLatest { url ->
                val b = _binding ?: return@collectLatest
                if (url != null) {
                    b.tvAssetFileStatus.text = "Package Uploaded"
                    b.ivAssetFileStatus.setImageResource(R.drawable.ic_verified)
                    b.ivAssetFileStatus.setColorFilter(android.graphics.Color.parseColor("#10B981"))
                }
            }
        }

        // Observe preview video status
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.previewVideoUrl.collectLatest { url ->
                val b = _binding ?: return@collectLatest
                if (url != null) {
                    b.tvVideoStatus.text = "Preview Video Uploaded ✅"
                    b.ivVideoStatus.setImageResource(R.drawable.ic_play)
                    b.ivVideoStatus.setColorFilter(
                        android.graphics.Color.parseColor("#22C55E") // green check
                    )
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val b = _binding ?: return@collectLatest
                when (state) {
                    is UploadUiState.UploadingFiles -> {
                        b.btnPublish.isEnabled = false
                        b.btnPublish.text = "Uploading..."
                    }
                    is UploadUiState.SavingAsset -> {
                        b.btnPublish.isEnabled = false
                        b.btnPublish.text = "Publishing..."
                    }
                    is UploadUiState.Success -> {
                        b.btnPublish.isEnabled = true
                        b.btnPublish.text = "Confirm & Publish"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        if (state.message.contains("published")) resetForm()
                    }
                    is UploadUiState.Error -> {
                        b.btnPublish.isEnabled = true
                        b.btnPublish.text = "Confirm & Publish"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        b.btnPublish.isEnabled = true
                        b.btnPublish.text = "Confirm & Publish"
                    }
                }
            }
        }
    }

    private fun resetForm() {
        binding.etTitle.text?.clear()
        binding.etDescription.text?.clear()
        binding.etCategory.text?.clear()
        binding.etPrice.text?.clear()
        binding.tvThumbnailStatus.text = "Choose Cover Image"
        binding.ivThumbnailPreview.visibility = View.GONE
        binding.llThumbnailEmpty.visibility = View.VISIBLE
        binding.tvAssetFileStatus.text = "Upload Asset Source"
        binding.ivAssetFileStatus.setImageResource(R.drawable.ic_layers)
        binding.ivAssetFileStatus.clearColorFilter()
        binding.tvVideoStatus.text = "Preview Clip (Optional)"
        binding.ivVideoStatus.clearColorFilter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
