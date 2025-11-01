package com.koshpal_android.koshpalapp.ui.insights

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.databinding.DialogCategoryDrilldownBinding
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.ui.transactions.TransactionListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class CategoryDrilldownDialog : DialogFragment() {

    private var _binding: DialogCategoryDrilldownBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryId: String
    private lateinit var categoryName: String
    private var categoryIcon: Int = 0

    companion object {
        fun newInstance(
            categoryId: String,
            categoryName: String,
            categoryIcon: Int
        ): CategoryDrilldownDialog {
            return CategoryDrilldownDialog().apply {
                arguments = Bundle().apply {
                    putString("categoryId", categoryId)
                    putString("categoryName", categoryName)
                    putInt("categoryIcon", categoryIcon)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getString("categoryId") ?: ""
            categoryName = it.getString("categoryName") ?: ""
            categoryIcon = it.getInt("categoryIcon", 0)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCategoryDrilldownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupViewPager()
    }

    private fun setupUI() {
        binding.tvCategoryName.text = categoryName
        if (categoryIcon != 0) {
            binding.ivCategoryIcon.setImageResource(categoryIcon)
        }

        binding.ivClose.setOnClickListener {
            dismiss()
        }
    }

    private fun setupViewPager() {
        val adapter = MonthPagerAdapter(this, categoryId)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Current Month"
                1 -> "Previous Month"
                else -> ""
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    class MonthPagerAdapter(
        fragment: DialogFragment,
        private val categoryId: String
    ) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            return when (position) {
                0 -> TransactionListFragment.newInstance(categoryId, true)
                1 -> TransactionListFragment.newInstance(categoryId, false)
                else -> throw IllegalStateException("Invalid position: $position")
            }
        }
    }
}

class TransactionListFragment : androidx.fragment.app.Fragment() {

    private lateinit var categoryId: String
    private var isCurrentMonth: Boolean = true

    companion object {
        fun newInstance(categoryId: String, isCurrentMonth: Boolean): TransactionListFragment {
            return TransactionListFragment().apply {
                arguments = Bundle().apply {
                    putString("categoryId", categoryId)
                    putBoolean("isCurrentMonth", isCurrentMonth)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getString("categoryId") ?: ""
            isCurrentMonth = it.getBoolean("isCurrentMonth", true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            com.koshpal_android.koshpalapp.R.layout.fragment_transaction_list,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvTransactions = view.findViewById<androidx.recyclerview.widget.RecyclerView>(
            com.koshpal_android.koshpalapp.R.id.rvTransactions
        )
        val tvTotalAmount = view.findViewById<android.widget.TextView>(
            com.koshpal_android.koshpalapp.R.id.tvTotalAmount
        )
        val tvTransactionCount = view.findViewById<android.widget.TextView>(
            com.koshpal_android.koshpalapp.R.id.tvTransactionCount
        )
        val layoutEmptyState = view.findViewById<android.view.ViewGroup>(
            com.koshpal_android.koshpalapp.R.id.layoutEmptyState
        )
        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(
            com.koshpal_android.koshpalapp.R.id.swipeRefresh
        )

        rvTransactions.layoutManager = LinearLayoutManager(requireContext())

        swipeRefresh?.setOnRefreshListener {
            loadTransactions(
                rvTransactions,
                tvTotalAmount,
                tvTransactionCount,
                layoutEmptyState
            )
            swipeRefresh.isRefreshing = false
        }

        loadTransactions(
            rvTransactions,
            tvTotalAmount,
            tvTransactionCount,
            layoutEmptyState
        )
    }

    private fun loadTransactions(
        rvTransactions: androidx.recyclerview.widget.RecyclerView,
        tvTotalAmount: android.widget.TextView,
        tvTransactionCount: android.widget.TextView,
        layoutEmptyState: android.view.ViewGroup
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val db = KoshpalDatabase.getDatabase(requireContext().applicationContext)
                val monthRange = if (isCurrentMonth) {
                    getCurrentMonthRange()
                } else {
                    getPreviousMonthRange()
                }

                val transactions = withContext(Dispatchers.IO) {
                    db.transactionDao().getTransactionsByCategoryAndMonth(
                        categoryId,
                        monthRange.first,
                        monthRange.second
                    )
                }

                if (transactions.isEmpty()) {
                    rvTransactions.visibility = View.GONE
                    layoutEmptyState.visibility = View.VISIBLE
                    tvTotalAmount.text = "₹0"
                    tvTransactionCount.text = "0 transactions"
                } else {
                    rvTransactions.visibility = View.VISIBLE
                    layoutEmptyState.visibility = View.GONE

                    val total = transactions.sumOf { it.amount }
                    tvTotalAmount.text = "₹${String.format("%.0f", total)}"
                    tvTransactionCount.text = "${transactions.size} transaction${if (transactions.size > 1) "s" else ""}"

                    val adapter = com.koshpal_android.koshpalapp.ui.transactions.TransactionAdapter(
                        onTransactionClick = { /* Handle click if needed */ },
                        onTransactionDelete = { _, _ -> /* Handle delete if needed */ }
                    )
                    rvTransactions.adapter = adapter
                    // Convert Transaction list to TransactionListItem.Data list
                    val transactionItems = transactions.map { TransactionListItem.Data(it) }
                    adapter.submitList(transactionItems)
                }
            } catch (e: Exception) {
                android.util.Log.e("TransactionListFragment", "Error loading transactions: ${e.message}")
            }
        }
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    private fun getPreviousMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis

        return Pair(start, end)
    }
}
