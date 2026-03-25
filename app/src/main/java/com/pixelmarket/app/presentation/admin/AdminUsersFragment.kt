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
import com.pixelmarket.app.databinding.FragmentAdminUsersBinding
import com.pixelmarket.app.domain.model.User
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminUsersFragment : Fragment(R.layout.fragment_admin_users) {

    private var _binding: FragmentAdminUsersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by activityViewModels()
    private lateinit var adapter: AdminUserAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminUsersBinding.bind(view)

        setupRecyclerView()
        setupToolbar()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = AdminUserAdapter(
            onEditClick = { user -> showEditUserDialog(user) },
            onDeleteClick = { userId -> 
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete User?")
                    .setMessage("Are you sure you want to delete this user? This action cannot be undone.")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteUser(userId) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.rvUsers.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = this@AdminUsersFragment.adapter
        }
    }

    private fun showEditUserDialog(user: User) {
        val roles = arrayOf("buyer", "seller", "both", "admin")
        val currentRoleIndex = roles.indexOf(user.role)
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit User: ${user.username}")
            .setSingleChoiceItems(roles.map { it.replaceFirstChar { c -> c.uppercase() } }.toTypedArray(), currentRoleIndex) { dialog, which ->
                val newRole = roles[which]
                val isAdmin = newRole == "admin"
                viewModel.updateUserRole(user.uid, newRole, isAdmin)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                // Simplified filtering
                val users = viewModel.users.value
                val filtered = if (newText.isNullOrBlank()) users 
                else users.filter { it.username.contains(newText, ignoreCase = true) || it.email.contains(newText, ignoreCase = true) }
                adapter.submitList(filtered)
                return true
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.users.collectLatest { list ->
                val b = _binding ?: return@collectLatest
                adapter.submitList(list)
                b.tvEmptyState.isVisible = list.isEmpty()
                b.rvUsers.isVisible = list.isNotEmpty()
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
