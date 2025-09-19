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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by viewModels()
    private lateinit var transactionsAdapter: TransactionAdapter
    private lateinit var backPressedCallback: OnBackPressedCallback

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
        observeViewModel()

        // Load initial data
        viewModel.loadTransactions()
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
            // Handle transaction click - navigate to details or categorization
            viewModel.onTransactionClick(transaction)
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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.transactions.collect { transactions ->
                transactionsAdapter.submitList(transactions)
                updateEmptyState(transactions.isEmpty())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.summaryData.collect { summary ->
                binding.tvTotalIncome.text = "₹${String.format("%.2f", summary.totalIncome)}"
                binding.tvTotalExpense.text = "₹${String.format("%.2f", summary.totalExpense)}"
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvTransactions.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun navigateBackToHome() {
        // Navigate back to HomeFragment by replacing the current fragment
        val homeActivity = requireActivity() as HomeActivity
        homeActivity.supportFragmentManager.beginTransaction()
            .replace(com.koshpal_android.koshpalapp.R.id.fragmentContainer, HomeFragment())
            .commit()
        
        // Also update the bottom navigation to show Home tab as selected
        // We need to find the bottom navigation view and update it
        val bottomNavigation = homeActivity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            com.koshpal_android.koshpalapp.R.id.bottomNavigation
        )
        bottomNavigation?.selectedItemId = com.koshpal_android.koshpalapp.R.id.nav_home
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
