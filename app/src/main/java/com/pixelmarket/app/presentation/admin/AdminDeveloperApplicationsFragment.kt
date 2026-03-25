package com.pixelmarket.app.presentation.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.FragmentAdminDeveloperApplicationsBinding
import com.pixelmarket.app.domain.model.DeveloperApplication
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminDeveloperApplicationsFragment :
    Fragment(R.layout.fragment_admin_developer_applications) {

    private var _binding: FragmentAdminDeveloperApplicationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by activityViewModels()

    private lateinit var adapter: DeveloperApplicationAdapter
    private var currentFilter = "pending_review"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminDeveloperApplicationsBinding.bind(view)

        setupRecyclerView()
        setupTabs()
        observeApplications()
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupRecyclerView() {
        adapter = DeveloperApplicationAdapter(
            onApprove = { app -> showApproveDialog(app) },
            onReject = { app -> showRejectDialog(app) }
        )
        binding.rvApplications.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    0 -> "pending_review"
                    1 -> "approved"
                    2 -> "rejected"
                    else -> "pending_review"
                }
                filterAndDisplay()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeApplications() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.developerApplications.collectLatest { apps ->
                val b = _binding ?: return@collectLatest
                val pending = apps.count { it.status == "pending_review" }
                b.tvPendingCount.text = "$pending pending review"
                b.tvBadgeCount.text = pending.toString()
                filterAndDisplay()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val b = _binding ?: return@collectLatest
                state ?: return@collectLatest
                b.progressBar.visibility = View.GONE
                when (state) {
                    is com.pixelmarket.app.util.Resource.Loading -> {
                        b.progressBar.visibility = View.VISIBLE
                    }
                    is com.pixelmarket.app.util.Resource.Success -> {
                        Toast.makeText(context, state.data, Toast.LENGTH_SHORT).show()
                        viewModel.clearUiState()
                    }
                    is com.pixelmarket.app.util.Resource.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.clearUiState()
                    }
                }
            }
        }
    }

    private fun filterAndDisplay() {
        val all = viewModel.developerApplications.value
        val filtered = all.filter { it.status == currentFilter }
        adapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvApplications.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvApplications.visibility = View.VISIBLE
        }
    }

    private fun showApproveDialog(app: DeveloperApplication) {
        AlertDialog.Builder(requireContext())
            .setTitle("Approve Developer Application")
            .setMessage("Approve ${app.userName} as a developer on PixelMarket?\n\nThey will immediately get upload access.")
            .setPositiveButton("Approve") { _, _ ->
                viewModel.approveDeveloperApplication(app)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRejectDialog(app: DeveloperApplication) {
        val noteInput = EditText(requireContext()).apply {
            hint = "Reason for rejection (optional)"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Reject Application")
            .setMessage("Reject ${app.userName}'s developer application?")
            .setView(noteInput)
            .setPositiveButton("Reject") { _, _ ->
                val note = noteInput.text.toString().trim()
                viewModel.rejectDeveloperApplication(app, note)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
