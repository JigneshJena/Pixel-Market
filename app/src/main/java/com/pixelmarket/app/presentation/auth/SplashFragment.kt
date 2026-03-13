package com.pixelmarket.app.presentation.auth

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    /** Keep a reference so we can cancel it in onDestroyView */
    private var fillAnimator: ValueAnimator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSplashBinding.bind(view)

        // Fade in the logo block
        binding.logoBlock.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(100)
            .setDuration(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Animate the loading fill bar width from 0 → full
        binding.loadingFill.post {
            val b = _binding ?: return@post          // guard: view may already be gone
            val parent = b.loadingBar
            fillAnimator = ValueAnimator.ofInt(0, parent.width).apply {
                duration = 2200
                startDelay = 300
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { anim ->
                    val safeBinding = _binding ?: return@addUpdateListener   // ← NPE fix
                    val lp = safeBinding.loadingFill.layoutParams
                    lp.width = anim.animatedValue as Int
                    safeBinding.loadingFill.layoutParams = lp
                }
                start()
            }
        }

        // Navigate after 2.6s — check admin status for already-logged-in users
        lifecycleScope.launch {
            delay(2600)
            if (!isAdded) return@launch

            val isLoggedIn = viewModel.isUserLoggedIn.value
            if (!isLoggedIn) {
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                return@launch
            }

            // User is logged in — check if they are admin
            val isAdmin = try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val doc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .get()
                        .await()
                    doc.getBoolean("isAdmin") == true
                } else false
            } catch (e: Exception) {
                false // on any error, fall back to regular home
            }

            if (isAdmin) {
                findNavController().navigate(R.id.action_splashFragment_to_adminDashboardFragment)
            } else {
                findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fillAnimator?.cancel()
        fillAnimator = null
        _binding = null
    }
}
