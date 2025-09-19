package com.koshpal_android.koshpalapp.ui.savings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshpal_android.koshpalapp.databinding.FragmentSavingsGoalsBinding
import com.koshpal_android.koshpalapp.model.SavingsGoal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SavingsGoalsFragment : Fragment() {
    
    private var _binding: FragmentSavingsGoalsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SavingsGoalsViewModel by viewModels()
    
    private lateinit var activeGoalsAdapter: SavingsGoalsAdapter
    private lateinit var completedGoalsAdapter: CompletedGoalsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavingsGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
        
        viewModel.loadSavingsGoals()
    }
    
    private fun setupRecyclerViews() {
        activeGoalsAdapter = SavingsGoalsAdapter(
            onAddMoneyClick = { goal ->
                showAddMoneyDialog(goal)
            },
            onDetailsClick = { goal ->
                showGoalDetailsDialog(goal)
            },
            onMoreClick = { goal ->
                showGoalOptionsMenu(goal)
            }
        )
        
        binding.rvSavingsGoals.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = activeGoalsAdapter
        }
        
        completedGoalsAdapter = CompletedGoalsAdapter { goal ->
            showGoalDetailsDialog(goal)
        }
        
        binding.rvCompletedGoals.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = completedGoalsAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.btnCreateGoal.setOnClickListener {
            showCreateGoalDialog()
        }
        
        binding.btnCreateFirstGoal.setOnClickListener {
            showCreateGoalDialog()
        }
        
        binding.fabAddGoal.setOnClickListener {
            showCreateGoalDialog()
        }
        
        binding.btnAddMoney.setOnClickListener {
            showQuickAddMoneyDialog()
        }
        
        binding.chipFilter.setOnClickListener {
            showFilterDialog()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeGoals.collect { goals ->
                activeGoalsAdapter.submitList(goals)
                updateEmptyState(goals.isEmpty())
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.completedGoals.collect { goals ->
                completedGoalsAdapter.submitList(goals)
                binding.cardCompletedGoals.visibility = if (goals.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.savingsOverview.collect { overview ->
                updateSavingsOverview(overview)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun updateSavingsOverview(overview: SavingsOverview) {
        binding.apply {
            tvTotalSaved.text = "₹${String.format("%.0f", overview.totalSaved)}"
            tvTotalTarget.text = "of ₹${String.format("%.0f", overview.totalTarget)} target"
            
            val progressPercentage = if (overview.totalTarget > 0) {
                (overview.totalSaved / overview.totalTarget * 100).toInt()
            } else 0
            
            progressOverall.progress = progressPercentage
            tvOverallProgress.text = "$progressPercentage% completed"
            tvGoalsCompleted.text = "${overview.completedGoals} of ${overview.totalGoals} goals achieved"
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvSavingsGoals.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun showCreateGoalDialog() {
        val dialog = CreateSavingsGoalDialog { goal ->
            viewModel.createGoal(goal)
        }
        dialog.show(parentFragmentManager, "CreateSavingsGoalDialog")
    }
    
    private fun showAddMoneyDialog(goal: SavingsGoal) {
        val dialog = AddMoneyDialog.newInstance(goal.id) { amount ->
            viewModel.addMoneyToGoal(goal.id, amount)
        }
        dialog.show(parentFragmentManager, "AddMoneyDialog")
    }
    
    private fun showGoalDetailsDialog(goal: SavingsGoal) {
        val dialog = SavingsGoalDetailsDialog.newInstance(goal.id)
        dialog.show(parentFragmentManager, "SavingsGoalDetailsDialog")
    }
    
    private fun showGoalOptionsMenu(goal: SavingsGoal) {
        val dialog = SavingsGoalOptionsBottomSheet.newInstance(goal.id) { action ->
            when (action) {
                GoalAction.EDIT -> showEditGoalDialog(goal)
                GoalAction.DELETE -> viewModel.deleteGoal(goal)
                GoalAction.PAUSE -> viewModel.pauseGoal(goal)
                GoalAction.SHARE -> shareGoalProgress(goal)
            }
        }
        dialog.show(parentFragmentManager, "SavingsGoalOptionsBottomSheet")
    }
    
    private fun showEditGoalDialog(goal: SavingsGoal) {
        val dialog = EditSavingsGoalDialog.newInstance(goal.id) { updatedGoal ->
            viewModel.updateGoal(updatedGoal)
        }
        dialog.show(parentFragmentManager, "EditSavingsGoalDialog")
    }
    
    private fun showQuickAddMoneyDialog() {
        val dialog = QuickAddMoneyDialog { goalId, amount ->
            viewModel.addMoneyToGoal(goalId, amount)
        }
        dialog.show(parentFragmentManager, "QuickAddMoneyDialog")
    }
    
    private fun showFilterDialog() {
        val dialog = GoalFilterDialog { filter ->
            viewModel.applyFilter(filter)
        }
        dialog.show(parentFragmentManager, "GoalFilterDialog")
    }
    
    private fun shareGoalProgress(goal: SavingsGoal) {
        // Implement sharing functionality
        val shareText = "I'm ${goal.progressPercentage.toInt()}% towards my ${goal.name} goal! " +
                "Saved ₹${String.format("%.0f", goal.currentAmount)} out of ₹${String.format("%.0f", goal.targetAmount)} 💪"
        
        // Create share intent
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Goal Progress"))
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

enum class GoalAction {
    EDIT, DELETE, PAUSE, SHARE
}
