package com.koshpal_android.koshpalapp.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.koshpal_android.koshpalapp.databinding.FragmentGoalsPrioritiesBinding

class GoalsPrioritiesFragment : Fragment() {
    private var _binding: FragmentGoalsPrioritiesBinding? = null
    private val binding get() = _binding!!

    private var selectedFinancialGoal: String = ""
    private var selectedGoalTimeframe: String = ""
    private var selectedFinancialStressArea: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsPrioritiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupChipGroups()
    }

    private fun setupUI() {
        binding.btnComplete.setOnClickListener {
            if (validateAnswers()) {
                saveAnswers()
                (activity as? OnboardingActivity)?.completeOnboarding()
            }
        }
        
        updateCompleteButtonState()
    }

    private fun setupChipGroups() {
        // Financial Goal chips
        val financialGoals = listOf(
            "Emergency fund",
            "Pay off debt",
            "Save for big purchase",
            "Invest for future",
            "Tax planning",
            "Retirement planning"
        )
        financialGoals.forEach { goal ->
            val chip = Chip(requireContext())
            chip.text = goal
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedFinancialGoal = goal
                    // Uncheck other chips in this group
                    for (i in 0 until binding.chipGroupFinancialGoal.childCount) {
                        val otherChip = binding.chipGroupFinancialGoal.getChildAt(i) as Chip
                        if (otherChip != chip) {
                            otherChip.isChecked = false
                        }
                    }
                }
                updateCompleteButtonState()
            }
            binding.chipGroupFinancialGoal.addView(chip)
        }

        // Goal Timeframe chips
        val goalTimeframes = listOf(
            "Within 6 months",
            "6 months-1 year",
            "1-2 years",
            "2-5 years",
            "More than 5 years"
        )
        goalTimeframes.forEach { timeframe ->
            val chip = Chip(requireContext())
            chip.text = timeframe
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedGoalTimeframe = timeframe
                    // Uncheck other chips in this group
                    for (i in 0 until binding.chipGroupGoalTimeframe.childCount) {
                        val otherChip = binding.chipGroupGoalTimeframe.getChildAt(i) as Chip
                        if (otherChip != chip) {
                            otherChip.isChecked = false
                        }
                    }
                }
                updateCompleteButtonState()
            }
            binding.chipGroupGoalTimeframe.addView(chip)
        }

        // Financial Stress Area chips
        val stressAreas = listOf(
            "Daily expenses",
            "Debt payments",
            "Saving money",
            "Investment decisions",
            "Tax planning",
            "Emergency fund",
            "Future planning"
        )
        stressAreas.forEach { area ->
            val chip = Chip(requireContext())
            chip.text = area
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedFinancialStressArea = area
                    // Uncheck other chips in this group
                    for (i in 0 until binding.chipGroupFinancialStress.childCount) {
                        val otherChip = binding.chipGroupFinancialStress.getChildAt(i) as Chip
                        if (otherChip != chip) {
                            otherChip.isChecked = false
                        }
                    }
                }
                updateCompleteButtonState()
            }
            binding.chipGroupFinancialStress.addView(chip)
        }
    }

    private fun validateAnswers(): Boolean {
        return selectedFinancialGoal.isNotEmpty() && 
               selectedGoalTimeframe.isNotEmpty() && 
               selectedFinancialStressArea.isNotEmpty()
    }

    private fun saveAnswers() {
        val activity = activity as? OnboardingActivity
        activity?.updateAnswer("primary_financial_goal", selectedFinancialGoal, 3)
        activity?.updateAnswer("goal_timeframe", selectedGoalTimeframe, 3)
        activity?.updateAnswer("financial_stress_area", selectedFinancialStressArea, 3)
    }

    private fun updateCompleteButtonState() {
        binding.btnComplete.isEnabled = validateAnswers()
        binding.btnComplete.alpha = if (validateAnswers()) 1.0f else 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
