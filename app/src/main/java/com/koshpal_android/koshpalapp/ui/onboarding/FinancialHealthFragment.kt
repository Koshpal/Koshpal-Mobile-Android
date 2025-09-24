package com.koshpal_android.koshpalapp.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.koshpal_android.koshpalapp.databinding.FragmentFinancialHealthBinding

class FinancialHealthFragment : Fragment() {
    private var _binding: FragmentFinancialHealthBinding? = null
    private val binding get() = _binding!!

    private var selectedExpenseTracking: String = ""
    private var selectedMoneyWorryLevel: Int = 0
    private var selectedHighestExpenseCategory: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinancialHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupChipGroups()
        setupRatingBar()
    }

    private fun setupUI() {
        binding.btnNext.setOnClickListener {
            if (validateAnswers()) {
                saveAnswers()
                (activity as? OnboardingActivity)?.nextSection()
            }
        }
        
        updateNextButtonState()
    }

    private fun setupChipGroups() {
        // Expense Tracking chips
        val expenseTrackingOptions = listOf(
            "Never", 
            "Rarely (few times/month)", 
            "Sometimes (weekly)", 
            "Regularly (daily)", 
            "I use an app"
        )
        expenseTrackingOptions.forEach { option ->
            val chip = Chip(requireContext())
            chip.text = option
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedExpenseTracking = option
                    // Uncheck other chips in this group
                    for (i in 0 until binding.chipGroupExpenseTracking.childCount) {
                        val otherChip = binding.chipGroupExpenseTracking.getChildAt(i) as Chip
                        if (otherChip != chip) {
                            otherChip.isChecked = false
                        }
                    }
                }
                updateNextButtonState()
            }
            binding.chipGroupExpenseTracking.addView(chip)
        }

        // Highest Expense Category chips
        val expenseCategories = listOf(
            "Food & dining", 
            "Shopping", 
            "Transportation", 
            "EMIs/loans", 
            "Entertainment", 
            "Bills & utilities", 
            "Other"
        )
        expenseCategories.forEach { category ->
            val chip = Chip(requireContext())
            chip.text = category
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedHighestExpenseCategory = category
                    // Uncheck other chips in this group
                    for (i in 0 until binding.chipGroupExpenseCategory.childCount) {
                        val otherChip = binding.chipGroupExpenseCategory.getChildAt(i) as Chip
                        if (otherChip != chip) {
                            otherChip.isChecked = false
                        }
                    }
                }
                updateNextButtonState()
            }
            binding.chipGroupExpenseCategory.addView(chip)
        }
    }

    private fun setupRatingBar() {
        // Setup rating buttons for money worry level
        val ratingButtons = listOf(
            binding.rating1,
            binding.rating2,
            binding.rating3,
            binding.rating4,
            binding.rating5
        )

        ratingButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectedMoneyWorryLevel = index + 1
                updateRatingButtons()
                updateNextButtonState()
            }
        }
    }

    private fun updateRatingButtons() {
        val ratingButtons = listOf(
            binding.rating1,
            binding.rating2,
            binding.rating3,
            binding.rating4,
            binding.rating5
        )

        ratingButtons.forEachIndexed { index, button ->
            if (index < selectedMoneyWorryLevel) {
                button.setBackgroundColor(resources.getColor(android.R.color.holo_purple, null))
                button.setTextColor(resources.getColor(android.R.color.white, null))
            } else {
                button.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
                button.setTextColor(resources.getColor(android.R.color.black, null))
            }
        }
    }

    private fun validateAnswers(): Boolean {
        return selectedExpenseTracking.isNotEmpty() && 
               selectedMoneyWorryLevel > 0 && 
               selectedHighestExpenseCategory.isNotEmpty()
    }

    private fun saveAnswers() {
        val activity = activity as? OnboardingActivity
        activity?.updateAnswer("expense_tracking", selectedExpenseTracking, 2)
        activity?.updateRatingAnswer("money_worry_level", selectedMoneyWorryLevel, 2)
        activity?.updateAnswer("highest_expense_category", selectedHighestExpenseCategory, 2)
    }

    private fun updateNextButtonState() {
        binding.btnNext.isEnabled = validateAnswers()
        binding.btnNext.alpha = if (validateAnswers()) 1.0f else 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
