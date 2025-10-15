package com.koshpal_android.koshpalapp.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.FragmentTransactionsBinding
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.transactions.TransactionAdapter
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionCategorizationDialog
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionDetailsDialog
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionsAdapter: TransactionAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private val shimmerItemCount = 8 // Number of shimmer placeholders to show while loading
    
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
        
        // Set status bar color to primary blue
        setStatusBarColor()
        
        setupRecyclerView()
        setupClickListeners()
        setupSearchFilter()
        setupFilterChips()
        setupBackPressHandler()
        
        // Show shimmer placeholders initially
        showShimmerItems()
        
        // Defer data loading to allow view to render first
        view.post {
            loadTransactionsDirectly()
        }
    }
    
    private fun setStatusBarColor() {
        activity?.window?.let { window ->
            // Set status bar color to primary blue
            window.statusBarColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary)
            
            // Make status bar icons white (for dark background)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = 0 // Clear light status bar flag for white icons
            }
            
            android.util.Log.d("TransactionsFragment", "🎨 Status bar color set to primary blue")
        }
    }
    
    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.layoutSearch.visibility == View.VISIBLE) {
                // If search is open, close it instead of going back
                closeSearch()
            } else {
                // If search is not open, allow normal back behavior
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
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
            btnSearch.setOnClickListener {
                if (binding.layoutSearch.visibility == View.VISIBLE) {
                    // Close search using the unified close function
                    closeSearch()
                } else {
                    // Open search
                    binding.layoutSearch.visibility = View.VISIBLE
                    
                    // Hide summary cards and filter chips when searching
                    binding.layoutSummary.visibility = View.GONE
                    binding.scrollViewFilters.visibility = View.GONE
                    
                    // Hide bottom navigation
                    hideBottomNavigation()
                    
                    binding.etSearch.requestFocus()
                    // Show keyboard
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(binding.etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
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
            val searchQuery = text.toString().trim()
            
            // Show/hide clear button
            binding.btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
            
            if (searchQuery.isEmpty()) {
                // If search is empty, reload based on current filter
                when (binding.chipGroupFilters.checkedChipId) {
                    R.id.chipAll -> filterAllTransactions()
                    R.id.chipIncome -> filterIncomeTransactions()
                    R.id.chipExpense -> filterExpenseTransactions()
                    R.id.chipThisMonth -> filterThisMonthTransactions()
                    R.id.chipLastMonth -> filterLastMonthTransactions()
                    R.id.chipStarred -> filterStarredTransactions()
                    R.id.chipCashFlow -> filterCashFlowTransactions()
                }
            } else {
                // Search in current transactions
                searchTransactions(searchQuery)
            }
        }
        
        // Clear search button
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }
        
        // Handle back press on keyboard - restore views when search loses focus
        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.layoutSearch.visibility == View.VISIBLE) {
                // Search field lost focus, close search and restore views
                closeSearch()
            }
        }
        
        // Add IME action listener for "Done" button on keyboard
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || 
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                // User pressed done/search on keyboard, hide keyboard but keep search open
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                binding.etSearch.clearFocus()
                true
            } else {
                false
            }
        }
    }
    
    private fun closeSearch() {
        // Close search bar
        binding.layoutSearch.visibility = View.GONE
        
        // Show summary cards and filter chips
        binding.layoutSummary.visibility = View.VISIBLE
        binding.scrollViewFilters.visibility = View.VISIBLE
        
        // Show bottom navigation
        showBottomNavigation()
        
        // Clear search
        binding.etSearch.text?.clear()
        
        // Hide keyboard
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }
    
    private fun searchTransactions(query: String) {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "🔍 Searching transactions for: '$query'")
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Get all transactions and search on IO dispatcher
                val filteredTransactions = withContext(Dispatchers.IO) {
                    val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                    val allTransactions = database.transactionDao().getAllTransactionsOnce()
                    
                    allTransactions.filter { transaction ->
                        transaction.merchant.contains(query, ignoreCase = true) ||
                        transaction.description.contains(query, ignoreCase = true) ||
                        transaction.amount.toString().contains(query) ||
                        transaction.notes?.contains(query, ignoreCase = true) == true ||
                        transaction.tags?.contains(query, ignoreCase = true) == true
                    }
                }
                
                android.util.Log.d("TransactionsFragment", "🔍 Found ${filteredTransactions.size} transactions matching '$query'")
                
                // Replace shimmer with search results
                showTransactionData(filteredTransactions)
                updateEmptyState(filteredTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to search transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                showTransactionData(emptyList())
                updateEmptyState(true)
            }
        }
    }

    private fun setupFilterChips() {
        // Ensure "All" chip is selected by default
        binding.chipGroupFilters.check(R.id.chipAll)
    }

    private fun filterStarredTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "🌟 Filtering starred transactions...")
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Get all transactions and filter starred ones on IO dispatcher
                val starredTransactions = withContext(Dispatchers.IO) {
                    val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                    val allTransactions = database.transactionDao().getAllTransactionsOnce()
                    allTransactions.filter { it.isStarred }
                }
                
                android.util.Log.d("TransactionsFragment", "⭐ Found ${starredTransactions.size} starred transactions")
                
                // Replace shimmer with starred transactions
                showTransactionData(starredTransactions)
                updateEmptyState(starredTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ Starred transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load starred transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                showTransactionData(emptyList())
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
                
                // Get cash flow transactions from separate table on IO dispatcher
                val cashFlowTransactions = withContext(Dispatchers.IO) {
                    transactionRepository.getCashFlowTransactions()
                }
                
                android.util.Log.d("TransactionsFragment", "💰 Found ${cashFlowTransactions.size} cash flow transactions")
                
                // Replace shimmer with cash flow transactions
                showTransactionData(cashFlowTransactions)
                updateEmptyState(cashFlowTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ Cash flow transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load cash flow transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                showTransactionData(emptyList())
                updateEmptyState(true)
            }
        }
    }

    private fun filterAllTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "📋 Loading all transactions...")
                
                // Show shimmer placeholders
                showShimmerItems()
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Get all transactions on IO dispatcher
                val allTransactions = withContext(Dispatchers.IO) {
                    val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                    database.transactionDao().getAllTransactionsOnce()
                }
                
                android.util.Log.d("TransactionsFragment", "📋 Found ${allTransactions.size} total transactions")
                
                // Replace shimmer with actual data
                showTransactionData(allTransactions)
                updateEmptyState(allTransactions.isEmpty())
                updateTransactionCount(allTransactions.size)
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ All transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load all transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                showTransactionData(emptyList())
                updateEmptyState(true)
            }
        }
    }

    private fun filterIncomeTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "💚 Filtering income transactions...")
                
                // Show shimmer placeholders
                showShimmerItems()
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Get all transactions and filter income ones on IO dispatcher
                val incomeTransactions = withContext(Dispatchers.IO) {
                    val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                    val allTransactions = database.transactionDao().getAllTransactionsOnce()
                    allTransactions.filter { it.type == com.koshpal_android.koshpalapp.model.TransactionType.CREDIT }
                }
                
                android.util.Log.d("TransactionsFragment", "💚 Found ${incomeTransactions.size} income transactions")
                
                // Replace shimmer with actual data
                showTransactionData(incomeTransactions)
                updateEmptyState(incomeTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ Income transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load income transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                showTransactionData(emptyList())
                updateEmptyState(true)
            }
        }
    }

    private fun filterExpenseTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "🔴 Filtering expense transactions...")
                
                // Show shimmer placeholders
                showShimmerItems()
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Get all transactions and filter expense ones on IO dispatcher
                val expenseTransactions = withContext(Dispatchers.IO) {
                    val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                    val allTransactions = database.transactionDao().getAllTransactionsOnce()
                    allTransactions.filter { it.type == com.koshpal_android.koshpalapp.model.TransactionType.DEBIT }
                }
                
                android.util.Log.d("TransactionsFragment", "🔴 Found ${expenseTransactions.size} expense transactions")
                
                // Replace shimmer with actual data
                showTransactionData(expenseTransactions)
                updateEmptyState(expenseTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ Expense transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load expense transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                showTransactionData(emptyList())
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
                
                // Get this month's transactions on IO dispatcher
                val thisMonthTransactions = withContext(Dispatchers.IO) {
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
                    allTransactions.filter { it.date in startOfMonth..endOfMonth }
                }
                
                android.util.Log.d("TransactionsFragment", "📅 Found ${thisMonthTransactions.size} transactions for this month")
                
                // Replace shimmer with this month transactions
                showTransactionData(thisMonthTransactions)
                updateEmptyState(thisMonthTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ This month transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load this month transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                showTransactionData(emptyList())
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
                
                // Get last month's transactions on IO dispatcher
                val lastMonthTransactions = withContext(Dispatchers.IO) {
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
                    allTransactions.filter { it.date in startOfLastMonth..endOfLastMonth }
                }
                
                android.util.Log.d("TransactionsFragment", "📆 Found ${lastMonthTransactions.size} transactions for last month")
                
                // Replace shimmer with last month transactions
                showTransactionData(lastMonthTransactions)
                updateEmptyState(lastMonthTransactions.isEmpty())
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.d("TransactionsFragment", "✅ Last month transactions loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("TransactionsFragment", "❌ Failed to load last month transactions: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                showTransactionData(emptyList())
                updateEmptyState(true)
            }
        }
    }

    private fun loadTransactionsDirectly() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TransactionsFragment", "🚀 Loading transactions directly...")
                
                // Show shimmer placeholders in RecyclerView
                showShimmerItems()
                
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                
                // Perform database operations on IO dispatcher
                val result = withContext(Dispatchers.IO) {
                    // Get data directly from database
                    val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                    val transactions = database.transactionDao().getAllTransactionsOnce()
                    
                    android.util.Log.d("TransactionsFragment", "📊 Found ${transactions.size} transactions")
                    
                    // Calculate current month start time once
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    val startOfMonth = calendar.timeInMillis
                    
                    // Calculate THIS MONTH summary - optimized
                    var currentMonthIncome = 0.0
                    var currentMonthExpense = 0.0
                    
                    transactions.forEach { transaction ->
                        // Check if transaction is from current month using simple comparison
                        if (transaction.date >= startOfMonth) {
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
                    
                    Triple(transactions, currentMonthIncome, currentMonthExpense)
                }
                
                val (transactions, currentMonthIncome, currentMonthExpense) = result
                
                android.util.Log.d("TransactionsFragment", "📊 Current Month - Income: ₹$currentMonthIncome, Expense: ₹$currentMonthExpense")
                
                // Replace shimmer with actual transaction data
                showTransactionData(transactions)
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
                // Show empty list on error (removes shimmer)
                showTransactionData(emptyList())
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
       // binding.tvTransactionCount.text = countText
    }
    
    private fun hideBottomNavigation() {
        try {
            val homeActivity = activity as? HomeActivity
            homeActivity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)?.visibility = View.GONE
            homeActivity?.findViewById<com.google.android.material.bottomappbar.BottomAppBar>(R.id.bottomAppBar)?.visibility = View.GONE
            homeActivity?.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabCenter)?.visibility = View.GONE
        } catch (e: Exception) {
            android.util.Log.e("TransactionsFragment", "Failed to hide bottom nav: ${e.message}")
        }
    }
    
    private fun showBottomNavigation() {
        try {
            val homeActivity = activity as? HomeActivity
            homeActivity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)?.visibility = View.VISIBLE
            homeActivity?.findViewById<com.google.android.material.bottomappbar.BottomAppBar>(R.id.bottomAppBar)?.visibility = View.VISIBLE
            homeActivity?.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabCenter)?.visibility = View.VISIBLE
        } catch (e: Exception) {
            android.util.Log.e("TransactionsFragment", "Failed to show bottom nav: ${e.message}")
        }
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
                    android.util.Log.d("TransactionsFragment", "🔄 ===== STARTING CATEGORIZATION =====")
                    android.util.Log.d("TransactionsFragment", "📝 Transaction ID: ${txn.id}")
                    android.util.Log.d("TransactionsFragment", "📝 Merchant: ${txn.merchant}")
                    android.util.Log.d("TransactionsFragment", "📝 Old Category: ${txn.categoryId}")
                    android.util.Log.d("TransactionsFragment", "📝 New Category: ${category.id} (${category.name})")
                    android.util.Log.d("TransactionsFragment", "📝 Amount: ₹${txn.amount}")
                    android.util.Log.d("TransactionsFragment", "📝 Date: ${java.util.Date(txn.date)}")
                    
                    val rowsUpdated = transactionRepository.updateTransactionCategory(txn.id, category.id)
                    android.util.Log.d("TransactionsFragment", "✅ Update completed - Rows affected: $rowsUpdated")
                    
                    if (rowsUpdated > 0) {
                        android.util.Log.d("TransactionsFragment", "✅ Transaction ${txn.id} categorized as ${category.name}")
                        
                        // Verify the update by reading it back
                        delay(100) // Small delay to ensure DB write completes
                        val updatedTxn = transactionRepository.getTransactionById(txn.id)
                        android.util.Log.d("TransactionsFragment", "🔍 Verification - categoryId after update: ${updatedTxn?.categoryId}")
                    } else {
                        android.util.Log.e("TransactionsFragment", "❌ NO ROWS UPDATED! Something went wrong!")
                    }
                    
                    // Refresh data to show updated transaction
                    loadTransactionsDirectly()
                    
                    // ✅ FIX: Refresh Categories fragment so categorized transactions appear there
                    (activity as? HomeActivity)?.refreshCategoriesData()
                    android.util.Log.d("TransactionsFragment", "🔄 Categories fragment refresh triggered")
                    
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

    /**
     * Show shimmer loading placeholders in the RecyclerView
     * Creates a list of Loading items that display shimmer animation
     */
    private fun showShimmerItems() {
        val shimmerList = List(shimmerItemCount) { TransactionListItem.Loading() }
        transactionsAdapter.submitList(shimmerList)
        android.util.Log.d("TransactionsFragment", "✨ Showing $shimmerItemCount shimmer placeholders")
    }

    /**
     * Replace shimmer items with actual transaction data
     * Automatically triggers fade-in animation via adapter's bind() method
     */
    private fun showTransactionData(transactions: List<Transaction>) {
        val dataList = transactions.map { TransactionListItem.Data(it) }
        transactionsAdapter.submitList(dataList)
        android.util.Log.d("TransactionsFragment", "✅ Replaced shimmer with ${transactions.size} actual transactions")
    }

    override fun onResume() {
        super.onResume()
        
        // Ensure all views are visible when fragment resumes
        if (binding.layoutSearch.visibility == View.GONE) {
            binding.layoutSummary.visibility = View.VISIBLE
            binding.scrollViewFilters.visibility = View.VISIBLE
            showBottomNavigation()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Restore status bar color to white when leaving TransactionsFragment
        activity?.window?.let { window ->
            window.statusBarColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
            
            // Make status bar icons dark (for light background)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            
            android.util.Log.d("TransactionsFragment", "🎨 Status bar color restored to white")
        }
        // Restore bottom navigation when leaving fragment
        showBottomNavigation()
        _binding = null
    }
    
    override fun onPause() {
        super.onPause()
        // Restore bottom navigation when fragment is paused
        showBottomNavigation()
    }
}
