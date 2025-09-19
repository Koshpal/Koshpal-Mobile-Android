package com.koshpal_android.koshpalapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ProfileViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeViewModel()
    }
    
    private fun setupClickListeners() {
        binding.apply {
            btnEditProfile.setOnClickListener {
                // Navigate to edit profile screen
                showMessage("Edit Profile feature coming soon!")
            }
            
            cardPersonalInfo.setOnClickListener {
                showMessage("Personal Information settings coming soon!")
            }
            
            cardNotifications.setOnClickListener {
                showMessage("Notification settings coming soon!")
            }
            
            cardPrivacy.setOnClickListener {
                showMessage("Privacy settings coming soon!")
            }
            
            cardHelp.setOnClickListener {
                showMessage("Help & Support coming soon!")
            }
            
            btnLogout.setOnClickListener {
                showLogoutConfirmation()
            }
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
                
                state.errorMessage?.let { error ->
                    showMessage(error)
                }
            }
        }
    }
    
    private fun updateUI(state: ProfileUiState) {
        binding.apply {
            tvUserName.text = state.userName
            tvUserEmail.text = state.userEmail
            tvTotalTransactions.text = state.totalTransactions.toString()
            tvActiveBudgets.text = state.activeBudgets.toString()
            tvSavingsGoals.text = state.savingsGoals.toString()
        }
    }
    
    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showLogoutConfirmation() {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                viewModel.logout()
                // Navigate to login screen or close app
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
