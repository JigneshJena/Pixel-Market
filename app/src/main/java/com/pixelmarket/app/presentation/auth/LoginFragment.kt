package com.pixelmarket.app.presentation.auth

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.FragmentLoginBinding
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        // ── Email/password login ──────────────────────────────────
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // ── Google Sign-In ────────────────────────────────────────
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(requireActivity(), gso)

        val googleLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    viewModel.googleLogin(account.idToken!!)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Google Sign-In failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.btnGoogle.setOnClickListener {
            googleLauncher.launch(googleClient.signInIntent)
        }

        // ── Observe auth state ────────────────────────────────────
        lifecycleScope.launch {
            viewModel.authState.collectLatest { state ->
                binding.progressBar.isVisible = state is Resource.Loading
                binding.btnLogin.isEnabled = state !is Resource.Loading
                binding.btnGoogle.isEnabled = state !is Resource.Loading

                when (state) {
                    is Resource.Success -> {
                        val user = state.data
                        if (user != null && user.isAdmin) {
                            // Admin → go straight to Admin Dashboard
                            findNavController().navigate(
                                R.id.action_loginFragment_to_adminDashboardFragment
                            )
                        } else {
                            // Regular user → go to Home
                            findNavController().navigate(
                                R.id.action_loginFragment_to_homeFragment
                            )
                        }
                        viewModel.resetAuthState()
                    }
                    is Resource.Error -> {
                        binding.tvError.text = state.message
                        binding.tvError.isVisible = true
                    }
                    else -> {
                        binding.tvError.isVisible = false
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
