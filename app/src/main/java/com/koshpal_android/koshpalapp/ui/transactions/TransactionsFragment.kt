package com.koshpal_android.koshpalapp.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.theme.KoshpalTheme
import com.koshpal_android.koshpalapp.ui.transactions.compose.AllTransactionsScreen
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionDetailsDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransactionsFragment : Fragment() {

    private val viewModel: TransactionsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Set status bar color
        setStatusBarColor()
        
        return ComposeView(requireContext()).apply {
            setContent {
                KoshpalTheme {
                    AllTransactionsScreenContent()
                }
            }
        }
    }
    
    @Composable
    private fun AllTransactionsScreenContent() {
        // Observe ViewModel state
        val transactions by viewModel.displayedTransactions.collectAsState()
        val summaryData by viewModel.summaryData.collectAsState()
        val currentFilter by viewModel.currentFilter.collectAsState()
        val loadingState by viewModel.loadingState.collectAsState()
        val selectedMonth by viewModel.selectedMonth.collectAsState()
        
        // Memoize callbacks to prevent unnecessary recompositions
        val context = requireContext()
        val onBackClicked: () -> Unit = remember { { (activity as? HomeActivity)?.onBackPressed() } }
        val onSearchClicked: () -> Unit = remember(context) { 
            { showSearchDialog() }
        }
        val onFilterSelected: (String) -> Unit = remember { { filter -> viewModel.filterTransactions(filter) } }
        val onTransactionClick: (Transaction) -> Unit = remember { { transaction -> showTransactionDetailsDialog(transaction) } }
        val onLoadMore: () -> Unit = remember { { viewModel.loadMoreTransactions() } }
        val onMonthSelected: (Int?, Int?) -> Unit = remember { { month, year -> viewModel.setSelectedMonth(month, year) } }
        val onProfileClick: () -> Unit = remember {
            {
                val intent = android.content.Intent(context, com.koshpal_android.koshpalapp.ui.profile.ProfileActivity::class.java)
                startActivity(intent)
            }
        }
        
        AllTransactionsScreen(
            transactions = transactions,
            incomeTotal = summaryData.totalIncome,
            expenseTotal = summaryData.totalExpense,
            selectedFilter = currentFilter,
            loadingState = loadingState,
            selectedMonth = selectedMonth,
            onBackClicked = onBackClicked,
            onSearchClicked = onSearchClicked,
            onFilterSelected = onFilterSelected,
            onTransactionClick = onTransactionClick,
            onLoadMore = onLoadMore,
            onMonthSelected = onMonthSelected,
            onProfileClick = onProfileClick
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Load transactions
        viewModel.loadTransactions()
    }
    
    private fun setStatusBarColor() {
        activity?.window?.let { window ->
            // Set status bar and navigation bar to dark/black for dark theme
            window.statusBarColor = ContextCompat.getColor(requireContext(), android.R.color.black)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                window.navigationBarColor = ContextCompat.getColor(requireContext(), android.R.color.black)
            }
            
            // Make status bar and navigation bar icons light (white) for dark background
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                var flags = window.decorView.systemUiVisibility
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() // Clear light status bar flag
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv() // Clear light nav bar flag
                }
                window.decorView.systemUiVisibility = flags
            }
        }
    }
    
    private fun showTransactionDetailsDialog(transaction: Transaction) {
        val dialog = TransactionDetailsDialog.newInstance(transaction) { updatedTransaction ->
            // Reload transactions to show updated data (the dialog already saves to DB)
            viewModel.loadTransactions()
        }
        
        dialog.show(parentFragmentManager, "TransactionDetailsDialog")
    }
    
    private fun showSearchDialog() {
        val context = requireContext()
        val editText = android.widget.EditText(context).apply {
            hint = "Search by merchant, description, or amount"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.GRAY)
            setBackgroundColor(android.graphics.Color.parseColor("#1A1F2E"))
            setPadding(32, 24, 32, 24)
        }
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle("Search Transactions")
            .setView(editText)
            .setPositiveButton("Search") { _, _ ->
                val query = editText.text.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.searchTransactions(query)
                    Toast.makeText(context, "Searching for: $query", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Clear") { _, _ ->
                viewModel.searchTransactions("")
                Toast.makeText(context, "Search cleared", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Restore status bar and navigation bar to dark when leaving TransactionsFragment
        activity?.window?.let { window ->
            window.statusBarColor = ContextCompat.getColor(requireContext(), android.R.color.black)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                window.navigationBarColor = ContextCompat.getColor(requireContext(), android.R.color.black)
            }
            
            // Make status bar and navigation bar icons light (white) for dark background
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                var flags = window.decorView.systemUiVisibility
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() // Clear light status bar flag
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv() // Clear light nav bar flag
                }
                window.decorView.systemUiVisibility = flags
            }
        }
    }
}
