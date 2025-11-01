package com.koshpal_android.koshpalapp.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshpal_android.koshpalapp.databinding.FragmentSetMonthlyBudgetBinding
import com.koshpal_android.koshpalapp.model.Budget
import com.koshpal_android.koshpalapp.model.BudgetCategory
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.ui.categories.adapter.SetBudgetCategoryAdapter
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SetMonthlyBudgetFragment : Fragment() {

    private var _binding: FragmentSetMonthlyBudgetBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private lateinit var setBudgetCategoryAdapter: SetBudgetCategoryAdapter
    
    // Month selection properties
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetMonthlyBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide bottom app bar, bottom navigation and FAB
        (activity as? HomeActivity)?.let { homeActivity ->
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.bottomAppBar)?.visibility = View.GONE
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.bottomNavigation)?.visibility = View.GONE
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.fabCenter)?.visibility = View.GONE
        }

        setupUI()
        setupRecyclerView()
    }

    private fun setupUI() {
        // Update month display
        updateMonthDisplay()

        // Back button
        binding.btnBack.setOnClickListener {
            (activity as? HomeActivity)?.onBackPressed()
        }

        // Save button
        binding.btnSave.setOnClickListener {
            saveBudget()
        }

        // Add Category button
        binding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun setupMonthPicker() {
        // Add month picker functionality if needed in future
    }

    private fun setupRecyclerView() {
        setBudgetCategoryAdapter = SetBudgetCategoryAdapter { 
            // Callback when budget amounts change
            updateTotalBudget()
        }
        
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = setBudgetCategoryAdapter
        }

        // Load categories with current spending
        loadCategoriesWithSpending()
    }

    private fun updateMonthDisplay() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, selectedYear)
        calendar.set(Calendar.MONTH, selectedMonth)
        
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        binding.tvMonth.text = monthFormat.format(calendar.time)
    }

    private fun updateTotalBudget() {
        val categoryBudgets = setBudgetCategoryAdapter.getCategoryBudgets()
        val totalBudget = categoryBudgets.sumOf { it.budgetAmount }
        binding.tvTotalBudget.text = "₹${String.format("%.0f", totalBudget)}"
    }

    private fun loadCategoriesWithSpending() {
        lifecycleScope.launch {
            try {
                // Calculate selected month date range
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, selectedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis

                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endOfMonth = calendar.timeInMillis

                // Get current month spending by category
                val categorySpending = transactionRepository.getCurrentMonthCategorySpending(startOfMonth, endOfMonth)
                val spendingMap = categorySpending.associateBy { it.categoryId }

                // Get categories from DB (includes defaults + any custom active categories)
                val allCategories = transactionRepository.getAllActiveCategoriesList()
                    .filter { it.id != "salary" }

                // Create category budget items
                val categoryBudgetItems = allCategories.map { category ->
                    val currentSpending = spendingMap[category.id]?.totalAmount ?: 0.0
                    CategoryBudgetItem(
                        categoryId = category.id,
                        categoryName = category.name,
                        categoryIcon = category.icon,
                        categoryColor = category.color,
                        currentSpending = currentSpending,
                        budgetAmount = 0.0
                    )
                }

                setBudgetCategoryAdapter.submitList(categoryBudgetItems)
                
                // Load existing budget after categories are loaded
                loadExistingBudget()

            } catch (e: Exception) {
                android.util.Log.e("SetMonthlyBudgetFragment", "Failed to load categories: ${e.message}")
                Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddCategoryDialog() {
        val context = requireContext()
        val input = android.widget.EditText(context)
        input.hint = "Category name"
        input.setSingleLine()

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle("Add Category")
            .setView(input)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val addBtn = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            addBtn.setOnClickListener {
                val name = input.text?.toString()?.trim() ?: ""
                if (name.isEmpty()) {
                    input.error = "Enter a category name"
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    try {
                        // Create custom category with default icon/color
                        val created = transactionRepository.insertCustomCategory(name)

                        // Append to the list with zero budget by default
                        val current = setBudgetCategoryAdapter.getCategoryBudgets().toMutableList()
                        val exists = current.any { it.categoryId == created.id || it.categoryName.equals(created.name, true) }
                        if (!exists) {
                            current.add(
                                CategoryBudgetItem(
                                    categoryId = created.id,
                                    categoryName = created.name,
                                    categoryIcon = created.icon,
                                    categoryColor = created.color,
                                    currentSpending = 0.0,
                                    budgetAmount = 0.0
                                )
                            )
                            setBudgetCategoryAdapter.submitList(current)
                        }
                        updateTotalBudget()
                        Toast.makeText(context, "Category added", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        android.util.Log.e("SetMonthlyBudget", "Failed to add category: ${'$'}{e.message}")
                        Toast.makeText(context, "Failed to add category", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun loadExistingBudget() {
        lifecycleScope.launch {
            try {
                val existingBudget = transactionRepository.getSingleBudget()
                existingBudget?.let { budget ->
                    // Load existing category budgets
                    val categoryBudgets = transactionRepository.getCategoriesForBudget(budget.id)
                    val budgetMap = categoryBudgets.associateBy { it.name }
                    
                    // Update adapter with existing budget amounts
                    setBudgetCategoryAdapter.updateBudgetAmounts(budgetMap)
                    
                    // Update total budget display
                    updateTotalBudget()
                }
            } catch (e: Exception) {
                android.util.Log.e("SetMonthlyBudgetFragment", "Failed to load existing budget: ${e.message}")
            }
        }
    }

    private fun saveBudget() {
        lifecycleScope.launch {
            try {
                // Get category budget amounts from adapter
                val categoryBudgets = setBudgetCategoryAdapter.getCategoryBudgets()
                val totalBudget = categoryBudgets.sumOf { it.budgetAmount }

                if (totalBudget <= 0) {
                    Toast.makeText(requireContext(), "Please set budget for at least one category", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Check if budget already exists
                val existingBudget = transactionRepository.getSingleBudget()
                val budgetId = if (existingBudget != null) {
                    // Update existing budget
                    android.util.Log.d("SetMonthlyBudget", "🔄 Updating existing budget: Total ₹$totalBudget")
                    val updatedBudget = existingBudget.copy(
                        totalBudget = totalBudget,
                        savings = 0.0
                    )
                    transactionRepository.updateBudget(updatedBudget)
                    android.util.Log.d("SetMonthlyBudget", "✅ Budget updated with ID: ${existingBudget.id}")
                    existingBudget.id
                } else {
                    // Create new budget
                    android.util.Log.d("SetMonthlyBudget", "💰 Creating new budget: Total ₹$totalBudget")
                    val budget = Budget(
                        totalBudget = totalBudget,
                        savings = 0.0
                    )
                    val newBudgetId = transactionRepository.insertBudget(budget)
                    android.util.Log.d("SetMonthlyBudget", "✅ Budget created with ID: $newBudgetId")
                    newBudgetId.toInt()
                }

                // Clear existing budget categories for this budget
                android.util.Log.d("SetMonthlyBudget", "🗑️ Clearing existing budget categories for budget ID: $budgetId")
                transactionRepository.clearBudgetCategoriesForBudget(budgetId)

                // Create budget categories
                val budgetCategories = categoryBudgets
                    .filter { it.budgetAmount > 0 }
                    .map { categoryBudget ->
                        BudgetCategory(
                            budgetId = budgetId,
                            name = categoryBudget.categoryName,
                            allocatedAmount = categoryBudget.budgetAmount,
                            spentAmount = categoryBudget.currentSpending
                        )
                    }

                android.util.Log.d("SetMonthlyBudget", "📊 Creating ${budgetCategories.size} budget categories:")
                budgetCategories.forEach { category ->
                    android.util.Log.d("SetMonthlyBudget", "   - ${category.name}: ₹${category.allocatedAmount}")
                }

                if (budgetCategories.isNotEmpty()) {
                    transactionRepository.insertAllBudgetCategories(budgetCategories)
                    android.util.Log.d("SetMonthlyBudget", "✅ Budget categories saved successfully")
                } else {
                    android.util.Log.w("SetMonthlyBudget", "⚠️ No budget categories to save")
                }

                Toast.makeText(requireContext(), "Budget saved successfully!", Toast.LENGTH_SHORT).show()
                
                // Reset budget notification flags when budget is updated
                try {
                    val budgetMonitor = com.koshpal_android.koshpalapp.utils.BudgetMonitor.getInstance(requireContext())
                    budgetMonitor.resetNotificationFlags()
                    android.util.Log.d("SetMonthlyBudget", "🔄 Budget notification flags reset")
                } catch (e: Exception) {
                    android.util.Log.e("SetMonthlyBudget", "❌ Failed to reset budget notification flags", e)
                }
                
                // Navigate back
                (activity as? HomeActivity)?.onBackPressed()

            } catch (e: Exception) {
                android.util.Log.e("SetMonthlyBudgetFragment", "Failed to save budget: ${e.message}")
                Toast.makeText(requireContext(), "Failed to save budget", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // Show bottom app bar, bottom navigation and FAB again when leaving this fragment
        (activity as? HomeActivity)?.let { homeActivity ->
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.bottomAppBar)?.visibility = View.VISIBLE
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.bottomNavigation)?.visibility = View.VISIBLE
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.fabCenter)?.visibility = View.VISIBLE
        }
        
        _binding = null
    }

    data class CategoryBudgetItem(
        val categoryId: String,
        val categoryName: String,
        val categoryIcon: Int,
        val categoryColor: String,
        val currentSpending: Double,
        var budgetAmount: Double
    )
}
