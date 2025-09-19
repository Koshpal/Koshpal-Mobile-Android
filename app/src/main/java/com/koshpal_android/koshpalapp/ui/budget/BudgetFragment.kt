package com.koshpal_android.koshpalapp.ui.budget

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.FragmentBudgetBinding
import com.koshpal_android.koshpalapp.databinding.DialogBudgetSuccessBinding
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.home.HomeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class BudgetFragment : Fragment() {
    
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BudgetViewModel by viewModels()
    private var isMonthlySelected = true
    private var isCustomizedSelected = true
    private lateinit var categoryAdapter: BudgetCategoryAdapter
    private var selectedDate = Calendar.getInstance()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupCategoryRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Set defaults
        selectCustomizedTab()
        selectMonthlyTab()
        updateDateDisplay()
    }
    
    private fun setupCategoryRecyclerView() {
        categoryAdapter = BudgetCategoryAdapter { category ->
            // Handle category selection
        }
        
        binding.rvCategories.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = categoryAdapter
        }
        
        // Load sample categories
        loadSampleCategories()
    }
    
    private fun setupClickListeners() {
        binding.apply {
            // Back button
            btnBack.setOnClickListener {
                navigateBackToHome()
            }
            
            // Simple/Customized tabs
            tabSimple.setOnClickListener {
                selectSimpleTab()
            }
            
            tabCustomized.setOnClickListener {
                selectCustomizedTab()
            }
            
            // Date selector
            cardDateSelector.setOnClickListener {
                showDatePicker()
            }
            
            // Monthly/Yearly tabs
            tabMonthly.setOnClickListener {
                selectMonthlyTab()
            }
            
            tabYearly.setOnClickListener {
                selectYearlyTab()
            }
            
            // Next button
            btnNext.setOnClickListener {
                val amount = etBudgetAmount.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) {
                    if (isCustomizedSelected) {
                        navigateToCategorySelection(amount)
                    } else {
                        createSimpleBudget(amount)
                    }
                } else {
                    showError("Please enter a valid budget amount")
                }
            }
        }
    }
    
    private fun selectSimpleTab() {
        isCustomizedSelected = false
        binding.apply {
            tabSimple.setCardBackgroundColor(resources.getColor(R.color.text_secondary, null))
            tabCustomized.setCardBackgroundColor(resources.getColor(R.color.primary, null))
        }
    }
    
    private fun selectCustomizedTab() {
        isCustomizedSelected = true
        binding.apply {
            tabCustomized.setCardBackgroundColor(resources.getColor(R.color.primary, null))
            tabSimple.setCardBackgroundColor(resources.getColor(R.color.text_secondary, null))
        }
    }
    
    private fun selectMonthlyTab() {
        isMonthlySelected = true
        binding.apply {
            tabMonthly.setCardBackgroundColor(resources.getColor(android.R.color.white, null))
            tabMonthly.strokeColor = resources.getColor(R.color.primary, null)
            tabYearly.setCardBackgroundColor(resources.getColor(R.color.background_light, null))
            tabYearly.strokeColor = resources.getColor(android.R.color.transparent, null)
        }
    }
    
    private fun selectYearlyTab() {
        isMonthlySelected = false
        binding.apply {
            tabYearly.setCardBackgroundColor(resources.getColor(android.R.color.white, null))
            tabYearly.strokeColor = resources.getColor(R.color.primary, null)
            tabMonthly.setCardBackgroundColor(resources.getColor(R.color.background_light, null))
            tabMonthly.strokeColor = resources.getColor(android.R.color.transparent, null)
        }
    }
    
    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                updateDateDisplay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun updateDateDisplay() {
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val monthName = monthNames[selectedDate.get(Calendar.MONTH)]
        val year = selectedDate.get(Calendar.YEAR)
        binding.tvSelectedDate.text = "$monthName $year"
    }
    
    private fun loadSampleCategories() {
        // Load sample categories for demonstration
        val categories = listOf(
            BudgetCategory("1", "Fun & Holiday Exp...", R.drawable.ic_entertainment, true),
            BudgetCategory("2", "Food & Beverages", R.drawable.ic_menu_eat, true),
            BudgetCategory("3", "Shopping", R.drawable.ic_store, false),
            BudgetCategory("4", "General Home Exp...", R.drawable.ic_home, false)
        )
        categoryAdapter.submitList(categories)
    }
    
    private fun createSimpleBudget(amount: Double) {
        viewModel.createBudget(amount, isMonthlySelected)
        showSuccessDialog()
    }
    
    private fun navigateToCategorySelection(amount: Double) {
        // Store the budget amount for later use
        // Navigate to category selection screen
        val homeActivity = requireActivity() as HomeActivity
        val categoryFragment = BudgetCategoriesFragment().apply {
            arguments = Bundle().apply {
                putDouble("budget_amount", amount)
                putBoolean("is_monthly", isMonthlySelected)
            }
        }
        
        homeActivity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, categoryFragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun showSuccessDialog() {
        val dialogBinding = DialogBudgetSuccessBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
            
        dialogBinding.btnSeeMyBudget.setOnClickListener {
            dialog.dismiss()
            navigateBackToHome()
        }
        
        dialog.show()
    }
    
    private fun navigateBackToHome() {
        val homeActivity = requireActivity() as HomeActivity
        homeActivity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()
        
        // Update bottom navigation to show Home tab as selected
        val bottomNavigation = homeActivity.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation?.selectedItemId = R.id.nav_home
    }
    
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Handle UI state changes
                if (state.isLoading) {
                    // Show loading state
                } else {
                    // Hide loading state
                }
                
                state.errorMessage?.let { error ->
                    showError(error)
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
