package com.koshpal_android.koshpalapp.ui.budget

import androidx.fragment.app.DialogFragment
import com.koshpal_android.koshpalapp.model.Budget

class CreateBudgetDialog(
    private val onBudgetCreated: (Budget) -> Unit
) : DialogFragment() {
    // Stub implementation - can be expanded later
}

class BudgetDetailsDialog : DialogFragment() {
    companion object {
        fun newInstance(budgetId: String): BudgetDetailsDialog {
            return BudgetDetailsDialog()
        }
    }
}

class BudgetOptionsBottomSheet : DialogFragment() {
    companion object {
        fun newInstance(budgetId: String, onAction: (String) -> Unit): BudgetOptionsBottomSheet {
            return BudgetOptionsBottomSheet()
        }
    }
}

class EditBudgetDialog : DialogFragment() {
    companion object {
        fun newInstance(budgetId: String, onBudgetUpdated: (Budget) -> Unit): EditBudgetDialog {
            return EditBudgetDialog()
        }
    }
}

class BudgetSuggestionsDialog(
    private val onSuggestionsAccepted: (Map<String, Double>) -> Unit
) : DialogFragment() {
    // Stub implementation - can be expanded later
}
