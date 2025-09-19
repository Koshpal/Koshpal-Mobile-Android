package com.koshpal_android.koshpalapp.ui.savings

import androidx.fragment.app.DialogFragment
import com.koshpal_android.koshpalapp.model.SavingsGoal

class CreateSavingsGoalDialog(
    private val onGoalCreated: (SavingsGoal) -> Unit
) : DialogFragment() {
    // Stub implementation - can be expanded later
}

class AddMoneyDialog : DialogFragment() {
    companion object {
        fun newInstance(goalId: String, onMoneyAdded: (Double) -> Unit): AddMoneyDialog {
            return AddMoneyDialog()
        }
    }
}

class SavingsGoalDetailsDialog : DialogFragment() {
    companion object {
        fun newInstance(goalId: String): SavingsGoalDetailsDialog {
            return SavingsGoalDetailsDialog()
        }
    }
}

class SavingsGoalOptionsBottomSheet : DialogFragment() {
    companion object {
        fun newInstance(goalId: String, onAction: (GoalAction) -> Unit): SavingsGoalOptionsBottomSheet {
            return SavingsGoalOptionsBottomSheet()
        }
    }
}

class EditSavingsGoalDialog : DialogFragment() {
    companion object {
        fun newInstance(goalId: String, onGoalUpdated: (SavingsGoal) -> Unit): EditSavingsGoalDialog {
            return EditSavingsGoalDialog()
        }
    }
}

class QuickAddMoneyDialog(
    private val onMoneyAdded: (String, Double) -> Unit
) : DialogFragment() {
    // Stub implementation - can be expanded later
}

class GoalFilterDialog(
    private val onFilterSelected: (GoalFilter) -> Unit
) : DialogFragment() {
    // Stub implementation - can be expanded later
}
