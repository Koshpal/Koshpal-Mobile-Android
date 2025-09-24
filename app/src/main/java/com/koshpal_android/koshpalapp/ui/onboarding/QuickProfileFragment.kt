package com.koshpal_android.koshpalapp.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.koshpal_android.koshpalapp.databinding.FragmentQuickProfileBinding

class QuickProfileFragment : Fragment() {
    private var _binding: FragmentQuickProfileBinding? = null
    private val binding get() = _binding!!

    private var selectedAgeGroup: String = ""
    private var selectedSalary: String = ""
    private var selectedFinancialSituation: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuickProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupChipGroups()
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
        // Age Group chips
        val ageGroups = listOf("22-25", "26-30", "31-35", "36-40", "41-45", "45+")
        ageGroups.forEach { age ->
            val chip = Chip(requireContext())
            chip.text = age
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedAgeGroup = age
                    // Uncheck other chips in this group
                    for (i in 0 until binding.chipGroupAge.childCount) {
                        val otherChip = binding.chipGroupAge.getChildAt(i) as Chip
                        if (otherChip != chip) {
                            otherChip.isChecked = false
                        }
                    }
                }
                updateNextButtonState()
            }
            binding.chipGroupAge.addView(chip)
        }

        // Salary chips
        val salaryRanges = listOf("Under ₹30k", "₹30k-50k", "₹50k-75k", "₹75k-1L", "₹1L-1.5L", "Above ₹1.5L")
        salaryRanges.forEach { salary ->
            val chip = Chip(requireContext())
            chip.text = salary
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedSalary = salary
                    // Uncheck other chips in this group
                    for (i in 0 until binding.chipGroupSalary.childCount) {
                        val otherChip = binding.chipGroupSalary.getChildAt(i) as Chip
                        if (otherChip != chip) {
                            otherChip.isChecked = false
                        }
                    }
                }
                updateNextButtonState()
            }
            binding.chipGroupSalary.addView(chip)
        }

        // Financial Situation chips
        val financialSituations = listOf(
            "Living paycheck to paycheck",
            "Getting by but no savings",
            "Comfortable with some savings",
            "Financially secure",
            "Building wealth"
        )
        financialSituations.forEach { situation ->
            val chip = Chip(requireContext())
            chip.text = situation
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedFinancialSituation = situation
                    // Uncheck other chips in this group
                    for (i in 0 until binding.chipGroupFinancialSituation.childCount) {
                        val otherChip = binding.chipGroupFinancialSituation.getChildAt(i) as Chip
                        if (otherChip != chip) {
                            otherChip.isChecked = false
                        }
                    }
                }
                updateNextButtonState()
            }
            binding.chipGroupFinancialSituation.addView(chip)
        }
    }

    private fun validateAnswers(): Boolean {
        return selectedAgeGroup.isNotEmpty() && 
               selectedSalary.isNotEmpty() && 
               selectedFinancialSituation.isNotEmpty()
    }

    private fun saveAnswers() {
        val activity = activity as? OnboardingActivity
        activity?.updateAnswer("age_group", selectedAgeGroup, 1)
        activity?.updateAnswer("monthly_salary", selectedSalary, 1)
        activity?.updateAnswer("financial_situation", selectedFinancialSituation, 1)
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
