package com.koshpal_android.koshpalapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.koshpal_android.koshpalapp.databinding.FragmentHomeBinding
import com.koshpal_android.koshpalapp.ui.auth.LoginActivity
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.network.RetrofitClient
import com.koshpal_android.koshpalapp.repository.UserRepository
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Create dependencies - these will be initialized when first accessed
    private val userPreferences by lazy {
        UserPreferences(requireContext())
    }

    private val userRepository by lazy {
        UserRepository(
            apiService = RetrofitClient.instance,
            userPreferences = userPreferences
        )
    }

    // Option 1: Using ViewModels with factory
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            userRepository = userRepository,
            userPreferences = userPreferences
        )
    }

    // Alternative Option 2: Manual ViewModel creation (simpler)
    // private val viewModel: HomeViewModel by lazy {
    //     HomeViewModel(userRepository, userPreferences)
    // }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun observeViewModel() {
        // Proper lifecycle-aware collection
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvWelcome.text = state.welcomeMessage

                    if (state.isLoggedOut) {
                        navigateToLogin()
                    }
                }
            }
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finishAffinity()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}