package com.koshpal_android.koshpalapp.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import com.koshpal_android.koshpalapp.ui.categories.compose.SetMonthlyBudgetScreen
import com.koshpal_android.koshpalapp.ui.categories.compose.viewmodel.SetMonthlyBudgetViewModel
import com.koshpal_android.koshpalapp.ui.categories.compose.MonthPickerDialog
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.theme.KoshpalTheme
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class SetMonthlyBudgetFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Hide bottom app bar, bottom navigation and FAB
        (activity as? HomeActivity)?.let { homeActivity ->
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.bottomAppBar)?.visibility = View.GONE
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.bottomNavigation)?.visibility = View.GONE
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.fabCenter)?.visibility = View.GONE
        }
        
        return ComposeView(requireContext()).apply {
            setContent {
                KoshpalTheme {
                    SetMonthlyBudgetScreenContent()
                }
            }
        }
    }

    @Composable
    private fun SetMonthlyBudgetScreenContent() {
        val viewModel: SetMonthlyBudgetViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        
        // Format month display
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, uiState.selectedYear)
            set(Calendar.MONTH, uiState.selectedMonth)
        }
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthDisplay = monthFormat.format(calendar.time)
        
        // Month Picker Dialog
        if (uiState.showMonthPicker) {
            MonthPickerDialog(
                selectedMonth = uiState.selectedMonth,
                selectedYear = uiState.selectedYear,
                onMonthSelected = { month, year ->
                    viewModel.setSelectedMonth(month, year)
                },
                onDismiss = { viewModel.hideMonthPicker() }
            )
        }
        
        // Show error message if any
        uiState.errorMessage?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
        
        SetMonthlyBudgetScreen(
            totalBudget = uiState.totalBudget,
            categoryBudgets = uiState.categoryBudgets,
            monthDisplay = monthDisplay,
            onBackClicked = {
                (activity as? HomeActivity)?.onBackPressed()
            },
            onMonthClick = {
                viewModel.showMonthPicker()
            },
            onTotalBudgetChanged = { amount ->
                viewModel.updateTotalBudget(amount)
            },
            onCategoryBudgetChanged = { categoryId, amount ->
                viewModel.updateCategoryBudget(categoryId, amount)
            },
            onAddCategoryClicked = {
                showAddCategoryDialog(viewModel)
            },
            onSaveClicked = {
                viewModel.saveBudget(
                    onSuccess = {
                        // Reset budget notification flags
                        try {
                            val budgetMonitor = com.koshpal_android.koshpalapp.utils.BudgetMonitor.getInstance(requireContext())
                            budgetMonitor.resetNotificationFlags()
                            android.util.Log.d("SetMonthlyBudgetFragment", "🔄 Budget notification flags reset")
                        } catch (e: Exception) {
                            android.util.Log.e("SetMonthlyBudgetFragment", "❌ Failed to reset budget notification flags", e)
                        }
                        
                        Toast.makeText(requireContext(), "Budget saved successfully!", Toast.LENGTH_SHORT).show()
                        (activity as? HomeActivity)?.onBackPressed()
                    },
                    onError = { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
    
    private fun showAddCategoryDialog(viewModel: SetMonthlyBudgetViewModel) {
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

                viewModel.addCategory(name) {
                    Toast.makeText(context, "Category added", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // Show bottom app bar, bottom navigation and FAB again when leaving this fragment
        (activity as? HomeActivity)?.let { homeActivity ->
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.bottomAppBar)?.visibility = View.VISIBLE
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.bottomNavigation)?.visibility = View.VISIBLE
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.fabCenter)?.visibility = View.VISIBLE
        }
    }
}
