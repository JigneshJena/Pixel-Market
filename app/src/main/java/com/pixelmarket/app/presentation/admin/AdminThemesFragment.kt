package com.pixelmarket.app.presentation.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.FragmentAdminThemesBinding
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminThemesFragment : Fragment(R.layout.fragment_admin_themes) {

    private var _binding: FragmentAdminThemesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminThemesBinding.bind(view)

        setupToolbar()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupListeners() {
        binding.btnSaveTheme.setOnClickListener {
            val primary = binding.etPrimaryColor.text.toString()
            val secondary = binding.etSecondaryColor.text.toString()
            
            if (primary.isNotBlank() && secondary.isNotBlank()) {
                val currentTheme = viewModel.themeSettings.value
                val newTheme = currentTheme.copy(
                    primaryColor = primary,
                    secondaryColor = secondary
                )
                viewModel.updateThemeSettings(newTheme, "Admin")
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.themeSettings.collectLatest { settings ->
                val b = _binding ?: return@collectLatest
                b.etPrimaryColor.setText(settings.primaryColor)
                b.etSecondaryColor.setText(settings.secondaryColor)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { resource ->
                val b = _binding ?: return@collectLatest
                b.progressBar.isVisible = resource is Resource.Loading
                if (resource is Resource.Success) {
                    Toast.makeText(requireContext(), resource.data, Toast.LENGTH_SHORT).show()
                    viewModel.clearUiState()
                } else if (resource is Resource.Error) {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    viewModel.clearUiState()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
