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

        setupUI()
        setupRecyclerView()
        loadExistingBudget()
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
    }

    private fun setupRecyclerView() {
        setBudgetCategoryAdapter = SetBudgetCategoryAdapter()
        
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

                // Get all default categories
                val allCategories = TransactionCategory.getDefaultCategories()
                    .filter { it.id != "salary" } // Exclude income categories from budget setting

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

            } catch (e: Exception) {
                android.util.Log.e("SetMonthlyBudgetFragment", "Failed to load categories: ${e.message}")
                Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadExistingBudget() {
        lifecycleScope.launch {
            try {
                val existingBudget = transactionRepository.getSingleBudget()
                existingBudget?.let { budget ->
                    binding.etTotalBudget.setText(budget.totalBudget.toString())
                    
                    // Load existing category budgets
                    val categoryBudgets = transactionRepository.getCategoriesForBudget(budget.id)
                    val budgetMap = categoryBudgets.associateBy { it.name }
                    
                    // Update adapter with existing budget amounts
                    setBudgetCategoryAdapter.updateBudgetAmounts(budgetMap)
                }
            } catch (e: Exception) {
                android.util.Log.e("SetMonthlyBudgetFragment", "Failed to load existing budget: ${e.message}")
            }
        }
    }

    private fun saveBudget() {
        lifecycleScope.launch {
            try {
                val totalBudgetText = binding.etTotalBudget.text.toString()
                if (totalBudgetText.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter total budget", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val totalBudget = totalBudgetText.toDoubleOrNull()
                if (totalBudget == null || totalBudget <= 0) {
                    Toast.makeText(requireContext(), "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Get category budget amounts from adapter
                val categoryBudgets = setBudgetCategoryAdapter.getCategoryBudgets()
                val totalCategoryBudgets = categoryBudgets.sumOf { it.budgetAmount }

                if (totalCategoryBudgets > totalBudget) {
                    Toast.makeText(
                        requireContext(), 
                        "Category budgets (₹${String.format("%.0f", totalCategoryBudgets)}) exceed total budget", 
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Clear existing budget
                transactionRepository.clearBudgets()

                // Create new budget
                val budget = Budget(
                    totalBudget = totalBudget,
                    savings = totalBudget - totalCategoryBudgets
                )
                val budgetId = transactionRepository.insertBudget(budget)

                // Create budget categories
                val budgetCategories = categoryBudgets
                    .filter { it.budgetAmount > 0 }
                    .map { categoryBudget ->
                        BudgetCategory(
                            budgetId = budgetId.toInt(),
                            name = categoryBudget.categoryName,
                            allocatedAmount = categoryBudget.budgetAmount,
                            spentAmount = categoryBudget.currentSpending
                        )
                    }

                if (budgetCategories.isNotEmpty()) {
                    transactionRepository.insertAllBudgetCategories(budgetCategories)
                }

                Toast.makeText(requireContext(), "Budget saved successfully!", Toast.LENGTH_SHORT).show()
                
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
