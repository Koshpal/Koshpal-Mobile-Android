package com.koshpal_android.koshpalapp.ui.bank

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.FragmentBankTransactionsBinding
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.ui.transactions.TransactionAdapter
import com.koshpal_android.koshpalapp.utils.BankThemeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BankTransactionsFragment : Fragment() {

    private var _binding: FragmentBankTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BankTransactionsViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    private var bankName: String? = null
    private var currentTypeFilter: TransactionType? = null
    private var currentMonthFilter: BankTransactionsViewModel.MonthFilter = BankTransactionsViewModel.MonthFilter.THIS_MONTH

    companion object {
        private const val ARG_BANK_NAME = "bank_name"

        fun newInstance(bankName: String): BankTransactionsFragment {
            return BankTransactionsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_BANK_NAME, bankName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bankName = arguments?.getString(ARG_BANK_NAME)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBankTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupFilters()
        setupBankInfo()
        observeTransactions()
        
        // Load transactions for the bank (default to This Month)
        bankName?.let { 
            viewModel.loadBankTransactions(it, BankTransactionsViewModel.MonthFilter.THIS_MONTH, null) 
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onTransactionClick = { transaction ->
                // Handle transaction click if needed
            },
            onTransactionDelete = { transaction, position ->
                // Handle transaction delete if needed
            }
        )

        binding.rvTransactions.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupFilters() {
        // Set initial state - This Month and All buttons selected
        updateMonthFilterButton(binding.btnFilterThisMonth, true)
        updateMonthFilterButton(binding.btnFilterAllTime, false)
        updateTypeFilterButton(binding.btnFilterAll, true)
        updateTypeFilterButton(binding.btnFilterCredit, false)
        updateTypeFilterButton(binding.btnFilterDebit, false)

        // Month filter buttons
        binding.btnFilterThisMonth.setOnClickListener {
            currentMonthFilter = BankTransactionsViewModel.MonthFilter.THIS_MONTH
            updateMonthFilterButtons()
            viewModel.updateMonthFilter(BankTransactionsViewModel.MonthFilter.THIS_MONTH)
        }

        binding.btnFilterAllTime.setOnClickListener {
            currentMonthFilter = BankTransactionsViewModel.MonthFilter.ALL_TIME
            updateMonthFilterButtons()
            viewModel.updateMonthFilter(BankTransactionsViewModel.MonthFilter.ALL_TIME)
        }

        // Type filter buttons
        binding.btnFilterAll.setOnClickListener {
            currentTypeFilter = null
            updateTypeFilterButtons()
            viewModel.updateTypeFilter(null)
        }

        binding.btnFilterCredit.setOnClickListener {
            currentTypeFilter = TransactionType.CREDIT
            updateTypeFilterButtons()
            viewModel.updateTypeFilter(TransactionType.CREDIT)
        }

        binding.btnFilterDebit.setOnClickListener {
            currentTypeFilter = TransactionType.DEBIT
            updateTypeFilterButtons()
            viewModel.updateTypeFilter(TransactionType.DEBIT)
        }
    }

    private fun updateMonthFilterButtons() {
        updateMonthFilterButton(binding.btnFilterThisMonth, currentMonthFilter == BankTransactionsViewModel.MonthFilter.THIS_MONTH)
        updateMonthFilterButton(binding.btnFilterAllTime, currentMonthFilter == BankTransactionsViewModel.MonthFilter.ALL_TIME)
    }

    private fun updateTypeFilterButtons() {
        updateTypeFilterButton(binding.btnFilterAll, currentTypeFilter == null)
        updateTypeFilterButton(binding.btnFilterCredit, currentTypeFilter == TransactionType.CREDIT)
        updateTypeFilterButton(binding.btnFilterDebit, currentTypeFilter == TransactionType.DEBIT)
    }

    private fun updateMonthFilterButton(button: com.google.android.material.button.MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        }
    }

    private fun updateTypeFilterButton(button: com.google.android.material.button.MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            when (button.id) {
                R.id.btnFilterCredit -> button.setTextColor(ContextCompat.getColor(requireContext(), R.color.income))
                R.id.btnFilterDebit -> button.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense))
                else -> button.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            }
        }
    }

    private fun setupBankInfo() {
        bankName?.let { name ->
            val theme = BankThemeProvider.getThemeForBankConsistent(name)
            
            // Set bank name and icon
            binding.tvBankName.text = theme.displayName
            
            // Set bank icon
            if (theme.iconDrawable != null) {
                binding.ivBankIcon.setImageResource(theme.iconDrawable)
                binding.ivBankIcon.visibility = View.VISIBLE
                binding.tvBankInitials.visibility = View.GONE
            } else {
                binding.ivBankIcon.visibility = View.GONE
                binding.tvBankInitials.visibility = View.VISIBLE
                binding.tvBankInitials.text = theme.iconInitials
            }

            // Apply theme to bank icon card
            binding.cardBankIcon.setCardBackgroundColor(theme.primaryColor)
        }
    }

    private fun observeTransactions() {
        // Observe loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    binding.layoutLoading.visibility = View.VISIBLE
                    binding.rvTransactions.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.GONE
                } else {
                    binding.layoutLoading.visibility = View.GONE
                }
            }
        }

        // Observe transactions
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.transactions.collect { transactions ->
                transactionAdapter.submitList(transactions)
                
                // Update UI based on transaction count
                if (transactions.isEmpty()) {
                    binding.rvTransactions.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvTransactions.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }
            }
        }

        // Observe bank summary
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bankSummary.collect { summary ->
                summary?.let {
                    binding.tvTransactionCount.text = "${it.transactionCount} transactions"
                    binding.tvTotalSpent.text = "₹${String.format("%,.0f", it.totalSpent)}"
                    
                    // Update toolbar title
                    binding.toolbar.title = "${it.bankName} Transactions"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
