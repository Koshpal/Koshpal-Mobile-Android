package com.koshpal_android.koshpalapp.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.FragmentTransactionsBinding
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.home.HomeFragment
import com.koshpal_android.koshpalapp.ui.transactions.TransactionAdapter
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionCategorizationDialog
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionDetailsDialog
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by viewModels()
    private lateinit var transactionsAdapter: TransactionAdapter
    private lateinit var backPressedCallback: OnBackPressedCallback
    private lateinit var itemTouchHelper: ItemTouchHelper
    
    @Inject
    lateinit var transactionRepository: TransactionRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        setupSearchFilter()
        setupBackPressedCallback()
        setupFilterChips()
        loadTransactionsDirectly()
    }

    private fun setupBackPressedCallback() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToHome()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    private fun setupRecyclerView() {
        transactionsAdapter = TransactionAdapter(
            onTransactionClick = { transaction ->
                // Show enhanced transaction details dialog when transaction is clicked
                showTransactionDetailsDialog(transaction)
            },
            onTransactionDelete = { transaction, position ->
                handleTransactionDelete(transaction, position)
            }
        )

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionsAdapter
        }

        // Setup swipe-to-delete
        val swipeToDeleteCallback = SwipeToDeleteCallback(requireContext()) { position ->
            transactionsAdapter.deleteItem(position)
        }
        itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvTransactions)
    }

    private fun setupClickListeners() {
        binding.apply {
            btnFilter.setOnClickListener {
                // Show filter options
            }

            btnBack.setOnClickListener {
                navigateBackToHome()
            }

            btnSearch.setOnClickListener {
                toggleSearchVisibility()
            }

            // Filter chips with single selection
            chipGroupFilters.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.chipAll -> filterAllTransactions()
                    R.id.chipIncome -> filterIncomeTransactions()
                    R.id.chipExpense -> filterExpenseTransactions()
                    R.id.chipThisMonth -> filterThisMonthTransactions()
                    R.id.chipLastMonth -> filterLastMonthTransactions()
                    R.id.chipStarred -> filterStarredTransactions()
                    R.id.chipCashFlow -> filterCashFlowTransactions()
                }
            }
        }
    }

    private fun setupSearchFilter() {
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            viewModel.searchTransactions(text.toString())
        }
    }

    private fun setupFilterChips() {
        // Ensure "All" chip is selected by default
        binding.chipGroupFilters.check(R.id.chipAll)
    }

    private fun toggleSearchVisibility() {
        binding.layoutSearch.visibility = if (binding.layoutSearch.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun filterStarredTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "🌟 Filtering starred transactions...")
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Get all transactions and filter starred ones
                val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                val allTransactions = database.transactionDao().getAllTransactionsOnce()
                val starredTransactions = allTransactions.filter { it.isStarred }
                
                android.util.Log.d("TransactionsFragment", "⭐ Found ${starredTransactions.size} starred transactions")
                
                // Update UI
                transactionsAdapter.submitList(starredTransactions)
                updateEmptyState(starredTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ Starred transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load starred transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                updateEmptyState(true)
            }
        }
    }

    private fun filterCashFlowTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "💰 Filtering cash flow transactions...")
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Get cash flow transactions from separate table
                val cashFlowTransactions = transactionRepository.getCashFlowTransactions()
                
                android.util.Log.d("TransactionsFragment", "💰 Found ${cashFlowTransactions.size} cash flow transactions")
                
                // Update UI
                transactionsAdapter.submitList(cashFlowTransactions)
                updateEmptyState(cashFlowTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ Cash flow transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load cash flow transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                updateEmptyState(true)
            }
        }
    }

    private fun filterAllTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "📋 Loading all transactions...")
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Get all transactions
                val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                val allTransactions = database.transactionDao().getAllTransactionsOnce()
                
                android.util.Log.d("TransactionsFragment", "📋 Found ${allTransactions.size} total transactions")
                
                // Update UI
                transactionsAdapter.submitList(allTransactions)
                updateEmptyState(allTransactions.isEmpty())
                updateTransactionCount(allTransactions.size)
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ All transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load all transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                updateEmptyState(true)
            }
        }
    }

    private fun filterIncomeTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "💚 Filtering income transactions...")
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Get all transactions and filter income ones
                val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                val allTransactions = database.transactionDao().getAllTransactionsOnce()
                val incomeTransactions = allTransactions.filter { it.type == com.koshpal_android.koshpalapp.model.TransactionType.CREDIT }
                
                android.util.Log.d("TransactionsFragment", "💚 Found ${incomeTransactions.size} income transactions")
                
                // Update UI
                transactionsAdapter.submitList(incomeTransactions)
                updateEmptyState(incomeTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ Income transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load income transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                updateEmptyState(true)
            }
        }
    }

    private fun filterExpenseTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "🔴 Filtering expense transactions...")
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Get all transactions and filter expense ones
                val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                val allTransactions = database.transactionDao().getAllTransactionsOnce()
                val expenseTransactions = allTransactions.filter { it.type == com.koshpal_android.koshpalapp.model.TransactionType.DEBIT }
                
                android.util.Log.d("TransactionsFragment", "🔴 Found ${expenseTransactions.size} expense transactions")
                
                // Update UI
                transactionsAdapter.submitList(expenseTransactions)
                updateEmptyState(expenseTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ Expense transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load expense transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                updateEmptyState(true)
            }
        }
    }

    private fun filterThisMonthTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "📅 Filtering this month transactions...")
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Calculate this month's date range
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis
                
                calendar.add(java.util.Calendar.MONTH, 1)
                calendar.add(java.util.Calendar.MILLISECOND, -1)
                val endOfMonth = calendar.timeInMillis
                
                // Get all transactions and filter this month ones
                val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                val allTransactions = database.transactionDao().getAllTransactionsOnce()
                val thisMonthTransactions = allTransactions.filter { it.date in startOfMonth..endOfMonth }
                
                android.util.Log.d("TransactionsFragment", "📅 Found ${thisMonthTransactions.size} transactions for this month")
                
                // Update UI
                transactionsAdapter.submitList(thisMonthTransactions)
                updateEmptyState(thisMonthTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ This month transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load this month transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                updateEmptyState(true)
            }
        }
    }

    private fun filterLastMonthTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "📆 Filtering last month transactions...")
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Calculate last month's date range
                val calendar = java.util.Calendar.getInstance()
                calendar.add(java.util.Calendar.MONTH, -1) // Go to last month
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                val startOfLastMonth = calendar.timeInMillis
                
                calendar.add(java.util.Calendar.MONTH, 1)
                calendar.add(java.util.Calendar.MILLISECOND, -1)
                val endOfLastMonth = calendar.timeInMillis
                
                // Get all transactions and filter last month ones
                val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                val allTransactions = database.transactionDao().getAllTransactionsOnce()
                val lastMonthTransactions = allTransactions.filter { it.date in startOfLastMonth..endOfLastMonth }
                
                android.util.Log.d("TransactionsFragment", "📆 Found ${lastMonthTransactions.size} transactions for last month")
                
                // Update UI
                transactionsAdapter.submitList(lastMonthTransactions)
                updateEmptyState(lastMonthTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ Last month transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load last month transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                updateEmptyState(true)
            }
        }
    }

    private fun loadTransactionsDirectly() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "🚀 Loading transactions directly...")
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Get data directly from database
                val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                val transactions = database.transactionDao().getAllTransactionsOnce()
                
                android.util.Log.d("TransactionsFragment", "📊 Found ${transactions.size} transactions")
                
                // Get current month boundaries
                val calendar = java.util.Calendar.getInstance()
                val currentMonth = calendar.get(java.util.Calendar.MONTH)
                val currentYear = calendar.get(java.util.Calendar.YEAR)
                
                // Calculate THIS MONTH summary only
                var currentMonthIncome = 0.0
                var currentMonthExpense = 0.0
                
                transactions.forEach { transaction ->
                    // Check if transaction is from current month
                    calendar.timeInMillis = transaction.timestamp
                    val transactionMonth = calendar.get(java.util.Calendar.MONTH)
                    val transactionYear = calendar.get(java.util.Calendar.YEAR)
                    
                    if (transactionMonth == currentMonth && transactionYear == currentYear) {
                        when (transaction.type) {
                            com.koshpal_android.koshpalapp.model.TransactionType.CREDIT -> {
                                currentMonthIncome += transaction.amount
                            }
                            com.koshpal_android.koshpalapp.model.TransactionType.DEBIT,
                            com.koshpal_android.koshpalapp.model.TransactionType.TRANSFER -> {
                                currentMonthExpense += transaction.amount
                            }
                        }
                    }
                }
                
                android.util.Log.d("TransactionsFragment", "📊 Current Month - Income: ₹$currentMonthIncome, Expense: ₹$currentMonthExpense")
                
                // Update UI
                transactionsAdapter.submitList(transactions)
                updateEmptyState(transactions.isEmpty())
                
                // Update summary with CURRENT MONTH data
                binding.tvTotalIncome.text = "₹${String.format("%.2f", currentMonthIncome)}"
                binding.tvTotalExpense.text = "₹${String.format("%.2f", currentMonthExpense)}"
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ Transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                updateEmptyState(true)
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.rvTransactions.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvTransactions.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun updateTransactionCount(count: Int) {
        val countText = when (count) {
            0 -> "No transactions this month"
            1 -> "1 transaction this month"
            else -> "$count transactions this month"
        }
        binding.tvTransactionCount.text = countText
    }

    private fun navigateBackToHome() {
        // Since Transactions is now in bottom nav, back button goes to Home
        val homeActivity = requireActivity() as HomeActivity
        
        // Update bottom navigation to Home (will trigger fragment change)
        homeActivity.showHomeFragment()
        
        android.util.Log.d("TransactionsFragment", "✅ Navigating back to Home via bottom nav")
    }

    private var isUpdatingTransaction = false
    
    private fun handleTransactionDelete(transaction: com.koshpal_android.koshpalapp.model.Transaction, position: Int) {
        // Show undo snackbar for 2 seconds
        val snackbar = Snackbar.make(
            binding.root,
            "Transaction deleted",
            Snackbar.LENGTH_SHORT
        ).apply {
            setAction("UNDO") {
                // Restore the transaction in the adapter
                transactionsAdapter.restoreItem(transaction, position)
                android.util.Log.d("TransactionsFragment", "✅ Transaction restored: ${transaction.merchant}")
            }
            
            // Set custom duration (2 seconds)
            duration = 2000
            
            // Style the snackbar
            setBackgroundTint(androidx.core.content.ContextCompat.getColor(requireContext(), com.koshpal_android.koshpalapp.R.color.error_dark))
            setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.koshpal_android.koshpalapp.R.color.white))
            setActionTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.koshpal_android.koshpalapp.R.color.warning))
            
            addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    
                    // If dismissed without undo action, permanently delete from database
                    if (event != DISMISS_EVENT_ACTION) {
                        lifecycleScope.launch {
                            try {
                                transactionRepository.deleteTransaction(transaction)
                                android.util.Log.d("TransactionsFragment", "🗑️ Transaction permanently deleted: ${transaction.merchant}")
                                
                                // Reload data to update summaries
                                loadTransactionsDirectly()
                            } catch (e: Exception) {
                                android.util.Log.e("TransactionsFragment", "❌ Failed to delete transaction: ${e.message}")
                                
                                // If deletion failed, restore the item
                                transactionsAdapter.restoreItem(transaction, position)
                                
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    "Failed to delete transaction",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            })
        }
        
        snackbar.show()
    }
    
    private fun showTransactionDetailsDialog(transaction: com.koshpal_android.koshpalapp.model.Transaction) {
        val dialog = TransactionDetailsDialog.newInstance(transaction) { updatedTransaction ->
            // Reload transactions to show updated data (the dialog already saves to DB)
            android.util.Log.d("TransactionsFragment", "🔄 Refreshing transactions after update...")
            loadTransactionsDirectly()
        }
        
        dialog.show(parentFragmentManager, "TransactionDetailsDialog")
    }

    private fun showTransactionCategorizationDialog(transaction: com.koshpal_android.koshpalapp.model.Transaction) {
        val dialog = TransactionCategorizationDialog.newInstance(transaction) { txn, category ->
            // Prevent multiple simultaneous updates
            if (isUpdatingTransaction) {
                android.util.Log.w("TransactionsFragment", "⚠️ Transaction update already in progress, ignoring...")
                return@newInstance
            }
            
            // Update transaction with selected category (simplified like HomeFragment)
            lifecycleScope.launch {
                isUpdatingTransaction = true
                try {
                    transactionRepository.updateTransactionCategory(txn.id, category.id)
                    android.util.Log.d("TransactionsFragment", "✅ Transaction ${txn.id} categorized as ${category.name}")
                    
                    // Refresh data to show updated transaction
                    loadTransactionsDirectly()
                    
                    // Show success message
                    Toast.makeText(
                        requireContext(),
                        "Transaction categorized as ${category.name}",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (e: Exception) {
                    android.util.Log.e("TransactionsFragment", "❌ Failed to categorize transaction: ${e.message}")
                    Toast.makeText(
                        requireContext(),
                        "Failed to categorize transaction",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    // Always reset the flag
                    isUpdatingTransaction = false
                }
            }
        }
        
        dialog.show(parentFragmentManager, "TransactionCategorizationDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
