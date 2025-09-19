package com.koshpal_android.koshpalapp.ui.budget

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.FragmentBudgetAnalysisBinding
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class BudgetAnalysisFragment : Fragment() {
    
    private var _binding: FragmentBudgetAnalysisBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BudgetViewModel by viewModels()
    private lateinit var budgetCategoriesAdapter: BudgetAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        setupAnimations()
        
        // Load initial data
        viewModel.loadBudgets()
    }
    
    private fun setupRecyclerView() {
        budgetCategoriesAdapter = BudgetAdapter(
            onBudgetClick = { budget ->
                // Handle budget category click - navigate to budget details or edit
            },
            onMoreClick = { budget ->
                // Handle more options click - show context menu
            }
        )
        
        binding.rvBudgetCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = budgetCategoriesAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.apply {
            btnAddBudget.setOnClickListener {
                navigateToAddBudget()
            }
            
            btnInfo.setOnClickListener {
                showBudgetInfo()
            }
            
            tvMonthSelector.setOnClickListener {
                showMonthSelector()
            }
            
            cardBudgetHistory.setOnClickListener {
                showBudgetHistory()
            }
        }
    }
    
    private fun setupAnimations() {
        // Animate progress circle
        val progressAnimation = ObjectAnimator.ofInt(binding.progressBudget, "progress", 0, 100)
        progressAnimation.duration = 1000
        progressAnimation.start()
        
        // Animate cards with stagger
        val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        val fadeInAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        
        binding.layoutEmptyCategories.startAnimation(fadeInAnimation)
        binding.cardBudgetHistory.startAnimation(slideUpAnimation)
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateBudgetAnalysis(state)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.budgets.collect { budgets ->
                budgetCategoriesAdapter.submitList(budgets)
                updateEmptyState(budgets.isEmpty())
            }
        }
    }
    
    private fun updateBudgetAnalysis(state: BudgetUiState) {
        binding.apply {
            tvTotalBudget.text = "₹${String.format("%.0f", state.totalBudget)}"
            tvTotalSpent.text = "₹${String.format("%.0f", state.totalSpent)} (From Budget)"
            tvAvailableBudget.text = "₹${String.format("%.0f", state.remainingBudget)}"
            
            // Mock data for income/expenses/savings - you can replace with real data
            tvIncome.text = "₹0"
            tvExpenses.text = "₹${String.format("%.0f", state.totalSpent)} (All)"
            tvLeftForSavings.text = "₹${String.format("%.0f", state.remainingBudget)}"
            
            // Update progress
            val progressPercentage = if (state.totalBudget > 0) {
                ((state.totalSpent / state.totalBudget) * 100).toInt()
            } else 0
            
            progressBudget.progress = 100 - progressPercentage
            tvProgressPercentage.text = "${100 - progressPercentage}%"
            
            // Update category count - you can get this from budgets list
            tvCategoryCount.text = "2 categories"
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            layoutEmptyCategories.visibility = if (isEmpty) View.VISIBLE else View.GONE
            rvBudgetCategories.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }
    
    private fun navigateToAddBudget() {
        val homeActivity = requireActivity() as HomeActivity
        homeActivity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, BudgetFragment())
            .addToBackStack(null)
            .commit()
    }
    
    private fun showBudgetInfo() {
        // Show budget information dialog
    }
    
    private fun showMonthSelector() {
        // Show month/year picker
    }
    
    private fun showBudgetHistory() {
        // Navigate to budget history screen
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
