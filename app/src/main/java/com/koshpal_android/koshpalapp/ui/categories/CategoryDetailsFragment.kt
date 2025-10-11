package com.koshpal_android.koshpalapp.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshpal_android.koshpalapp.databinding.FragmentCategoryDetailsBinding
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.transactions.TransactionAdapter
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionCategorizationDialog
import com.koshpal_android.koshpalapp.model.TransactionCategory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class CategoryDetailsFragment : Fragment() {

    private var _binding: FragmentCategoryDetailsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private lateinit var transactionsAdapter: TransactionAdapter
    private lateinit var backPressedCallback: OnBackPressedCallback

    // Parameters passed from CategoriesFragment
    private var categoryId: String = ""
    private var categoryName: String = ""
    private var categoryIcon: Int = 0
    private var month: Int = 0
    private var year: Int = 0

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_CATEGORY_NAME = "category_name"
        private const val ARG_CATEGORY_ICON = "category_icon"
        private const val ARG_MONTH = "month"
        private const val ARG_YEAR = "year"

        fun newInstance(
            categoryId: String,
            categoryName: String,
            categoryIcon: Int,
            month: Int,
            year: Int
        ): CategoryDetailsFragment {
            return CategoryDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_ID, categoryId)
                    putString(ARG_CATEGORY_NAME, categoryName)
                    putInt(ARG_CATEGORY_ICON, categoryIcon)
                    putInt(ARG_MONTH, month)
                    putInt(ARG_YEAR, year)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getString(ARG_CATEGORY_ID, "")
            categoryName = it.getString(ARG_CATEGORY_NAME, "")
            categoryIcon = it.getInt(ARG_CATEGORY_ICON, 0)
            month = it.getInt(ARG_MONTH, Calendar.getInstance().get(Calendar.MONTH))
            year = it.getInt(ARG_YEAR, Calendar.getInstance().get(Calendar.YEAR))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackPressHandling()
        setupToolbar()
        setupRecyclerView()
        setupCategoryHeader()
        loadTransactions()
    }

    private fun setupBackPressHandling() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            navigateBack()
        }
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

    private fun setupCategoryHeader() {
        binding.apply {
            // Set category name and icon
            tvCategoryName.text = categoryName
            if (categoryIcon != 0) {
                ivCategoryIcon.setImageResource(categoryIcon)
            }

            // Set month/year display
            val calendar = Calendar.getInstance()
            calendar.set(year, month, 1)
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            tvMonthYear.text = monthFormat.format(calendar.time)
        }
    }

    private fun loadTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d(
                    "CategoryDetails",
                    "📊 Loading transactions for category: $categoryId, month: $month, year: $year"
                )

                // Get transactions for this category and month
                val transactions = transactionRepository.getTransactionsByCategory(
                    categoryId,
                    month,
                    year
                )

                android.util.Log.d(
                    "CategoryDetails",
                    "✅ Loaded ${transactions.size} transactions"
                )

                if (transactions.isEmpty()) {
                    showEmptyState()
                } else {
                    showTransactionsList(transactions)

                    // Calculate totals
                    val totalAmount = transactions
                        .filter { it.isExpense }
                        .sumOf { it.amount }

                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                    binding.tvTotalAmount.text = currencyFormat.format(totalAmount)
                    binding.tvTransactionCount.text = transactions.size.toString()
                }

            } catch (e: Exception) {
                android.util.Log.e(
                    "CategoryDetails",
                    "❌ Error loading transactions: ${e.message}",
                    e
                )
                Toast.makeText(
                    requireContext(),
                    "Failed to load transactions: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showEmptyState() {
        binding.rvTransactions.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.tvTotalAmount.text = "₹0"
        binding.tvTransactionCount.text = "0"
    }

    private fun showTransactionsList(transactions: List<com.koshpal_android.koshpalapp.model.Transaction>) {
        binding.rvTransactions.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
        transactionsAdapter.submitList(transactions)
    }

    private fun showTransactionCategorizationDialog(transaction: com.koshpal_android.koshpalapp.model.Transaction) {
        val dialog = TransactionCategorizationDialog.newInstance(transaction) { txn, category ->
            // Update transaction with selected category
            lifecycleScope.launch {
                try {
                    transactionRepository.updateTransactionCategory(txn.id, category.id)
                    android.util.Log.d(
                        "CategoryDetails",
                        "✅ Transaction ${txn.id} recategorized from $categoryId to ${category.id}"
                    )

                    Toast.makeText(
                        requireContext(),
                        "Transaction moved to ${category.name}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Refresh the list (transaction will be removed if category changed)
                    loadTransactions()

                    // Refresh the categories fragment data
                    (activity as? HomeActivity)?.refreshCategoriesData()

                } catch (e: Exception) {
                    android.util.Log.e(
                        "CategoryDetails",
                        "❌ Failed to recategorize transaction: ${e.message}"
                    )
                    Toast.makeText(
                        requireContext(),
                        "Failed to update category",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show(parentFragmentManager, "TransactionCategorizationDialog")
    }

    private fun navigateBack() {
        // Navigate back to Categories fragment
        (activity as? HomeActivity)?.navigateBackFromCategoryDetails()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

