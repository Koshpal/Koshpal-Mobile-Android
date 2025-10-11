package com.koshpal_android.koshpalapp.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.koshpal_android.koshpalapp.databinding.FragmentTransactionsBinding
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.home.HomeFragment
import com.koshpal_android.koshpalapp.ui.transactions.TransactionAdapter
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionCategorizationDialog
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

        setupBackPressHandling()
        setupRecyclerView()
        setupClickListeners()
        setupSearchFilter()

        // Load data directly without ViewModel Flow issues
        loadTransactionsDirectly()
    }

    private fun setupBackPressHandling() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToHome()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    private fun setupRecyclerView() {
        transactionsAdapter = TransactionAdapter { transaction ->
            // Show categorization dialog when transaction is clicked
            showTransactionCategorizationDialog(transaction)
        }

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionsAdapter
        }
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

            // Filter chips
            chipAll.setOnClickListener { viewModel.filterTransactions("all") }
            chipIncome.setOnClickListener { viewModel.filterTransactions("income") }
            chipExpense.setOnClickListener { viewModel.filterTransactions("expense") }
            chipThisMonth.setOnClickListener { viewModel.filterTransactions("this_month") }
            chipLastMonth.setOnClickListener { viewModel.filterTransactions("last_month") }
        }
    }

    private fun setupSearchFilter() {
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            viewModel.searchTransactions(text.toString())
        }
    }

    private fun toggleSearchVisibility() {
        binding.layoutSearch.visibility = if (binding.layoutSearch.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
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
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvTransactions.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun navigateBackToHome() {
        // Since Transactions is now in bottom nav, back button goes to Home
        val homeActivity = requireActivity() as HomeActivity
        
        // Update bottom navigation to Home (will trigger fragment change)
        val bottomNavigation = homeActivity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            com.koshpal_android.koshpalapp.R.id.bottomNavigation
        )
        bottomNavigation?.selectedItemId = com.koshpal_android.koshpalapp.R.id.homeFragment
        
        android.util.Log.d("TransactionsFragment", "✅ Navigating back to Home via bottom nav")
    }

    private var isUpdatingTransaction = false
    
    private fun showTransactionCategorizationDialog(transaction: com.koshpal_android.koshpalapp.model.Transaction) {
        val dialog = TransactionCategorizationDialog.newInstance(transaction) { txn, category ->
            // Prevent multiple simultaneous updates
            if (isUpdatingTransaction) {
                android.util.Log.w("TransactionsFragment", "⚠️ Transaction update already in progress, ignoring...")
                return@newInstance
            }
            
            // Update transaction with selected category
            lifecycleScope.launch {
                isUpdatingTransaction = true
                try {
                    android.util.Log.d("TransactionsFragment", "🔄 BEFORE UPDATE: Transaction ${txn.id}, Current categoryId: '${txn.categoryId}'")
                    android.util.Log.d("TransactionsFragment", "🔄 UPDATING: Setting categoryId to '${category.id}' for transaction ${txn.id}")
                    
                    val rowsAffected = transactionRepository.updateTransactionCategory(txn.id, category.id)
                    android.util.Log.d("TransactionsFragment", "✅ Database UPDATE result: $rowsAffected rows affected")
                    
                    // Small delay to ensure database transaction is committed
                    kotlinx.coroutines.delay(50)
                    
                    if (rowsAffected > 0) {
                        android.util.Log.d("TransactionsFragment", "✅ Transaction ${txn.id} categorized as ${category.name} (categoryId: ${category.id})")
                        
                        // Verify the update worked
                        val updatedTransaction = transactionRepository.getTransactionById(txn.id)
                        android.util.Log.d("TransactionsFragment", "✅ AFTER UPDATE: Transaction categoryId is now: '${updatedTransaction?.categoryId}'")
                        
                        if (updatedTransaction?.categoryId == category.id) {
                            android.util.Log.d("TransactionsFragment", "✅ VERIFICATION SUCCESS: Category update confirmed in database")
                        } else {
                            android.util.Log.e("TransactionsFragment", "❌ VERIFICATION FAILED: Expected '${category.id}', got '${updatedTransaction?.categoryId}'")
                        }
                        
                        // Reload transactions to show updated data
                        loadTransactionsDirectly()
                        
                        // Show success message
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Transaction categorized as ${category.name}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        android.util.Log.e("TransactionsFragment", "❌ DATABASE UPDATE FAILED: No rows affected for transaction ${txn.id}")
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Failed to categorize transaction",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("TransactionsFragment", "Failed to categorize transaction: ${e.message}")
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Failed to categorize transaction",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    // Always reset the flag, even if there was an error
                    isUpdatingTransaction = false
                    android.util.Log.d("TransactionsFragment", "🔓 Transaction update completed, ready for next update")
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
