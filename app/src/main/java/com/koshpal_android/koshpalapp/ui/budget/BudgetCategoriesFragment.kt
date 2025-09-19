package com.koshpal_android.koshpalapp.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.FragmentBudgetCategoriesBinding
import com.koshpal_android.koshpalapp.databinding.DialogBudgetSuccessBinding
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.home.HomeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BudgetCategoriesFragment : Fragment() {
    
    private var _binding: FragmentBudgetCategoriesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BudgetViewModel by viewModels()
    private lateinit var categoriesAdapter: CategoryLimitAdapter
    
    private var budgetAmount: Double = 0.0
    private var isMonthly: Boolean = true
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get arguments
        arguments?.let {
            budgetAmount = it.getDouble("budget_amount", 0.0)
            isMonthly = it.getBoolean("is_monthly", true)
        }
        
        setupRecyclerView()
        setupClickListeners()
        updateBudgetSummary()
        loadSampleCategories()
    }
    
    private fun setupRecyclerView() {
        categoriesAdapter = CategoryLimitAdapter { category ->
            // Handle category removal
            removeCategory(category)
        }
        
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoriesAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
            
            btnAddCategory.setOnClickListener {
                // Show category selection dialog
                showAddCategoryDialog()
            }
            
            btnSave.setOnClickListener {
                saveBudgetWithCategories()
            }
        }
    }
    
    private fun updateBudgetSummary() {
        binding.apply {
            tvTotalBudget.text = "₹${String.format("%.0f", budgetAmount)}"
            tvRemainingBudget.text = "₹${String.format("%.0f", budgetAmount)}" // Will be updated as categories are added
        }
    }
    
    private fun loadSampleCategories() {
        // Load sample categories with limits
        val categories = listOf(
            CategoryLimit("1", "Fun & Holiday Expenses", R.drawable.ic_entertainment, 2700.0, 0.0),
            CategoryLimit("2", "Food & Beverages", R.drawable.ic_menu_eat, 300.0, 0.0)
        )
        categoriesAdapter.submitList(categories)
        updateSelectedCount(categories.size)
    }
    
    private fun updateSelectedCount(count: Int) {
        binding.tvSelectedCount.text = "$count categories selected"
    }
    
    private fun removeCategory(category: CategoryLimit) {
        val currentList = categoriesAdapter.currentList.toMutableList()
        currentList.remove(category)
        categoriesAdapter.submitList(currentList)
        updateSelectedCount(currentList.size)
    }
    
    private fun showAddCategoryDialog() {
        val categories = arrayOf("Shopping", "Transport", "Healthcare", "Education", "Bills")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Category")
            .setItems(categories) { _, which ->
                val categoryName = categories[which]
                addCategory(categoryName)
            }
            .show()
    }
    
    private fun addCategory(categoryName: String) {
        val currentList = categoriesAdapter.currentList.toMutableList()
        val newCategory = CategoryLimit(
            id = System.currentTimeMillis().toString(),
            name = categoryName,
            iconRes = R.drawable.ic_category_default,
            budgetLimit = 0.0,
            spent = 0.0
        )
        currentList.add(newCategory)
        categoriesAdapter.submitList(currentList)
        updateSelectedCount(currentList.size)
    }
    
    private fun saveBudgetWithCategories() {
        lifecycleScope.launch {
            try {
                // Create budget with the entered amount
                viewModel.createBudget(budgetAmount, isMonthly)
                
                // Show success dialog
                showSuccessDialog()
            } catch (e: Exception) {
                // Handle error
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Error")
                    .setMessage("Failed to create budget: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class CategoryLimit(
    val id: String,
    val name: String,
    val iconRes: Int,
    val budgetLimit: Double,
    val spent: Double
)
