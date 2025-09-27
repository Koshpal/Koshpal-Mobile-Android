package com.koshpal_android.koshpalapp.ui.transactions.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.koshpal_android.koshpalapp.databinding.DialogTransactionCategorizationBinding
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.ui.transactions.TransactionsViewModel
import com.koshpal_android.koshpalapp.ui.transactions.adapter.CategorySelectionAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransactionCategorizationDialog : BottomSheetDialogFragment() {

    private var _binding: DialogTransactionCategorizationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by viewModels()
    private lateinit var categoryAdapter: CategorySelectionAdapter
    
    private var transaction: Transaction? = null
    private var onCategorySelected: ((Transaction, TransactionCategory) -> Unit)? = null

    companion object {
        fun newInstance(
            transaction: Transaction,
            onCategorySelected: (Transaction, TransactionCategory) -> Unit
        ): TransactionCategorizationDialog {
            return TransactionCategorizationDialog().apply {
                this.transaction = transaction
                this.onCategorySelected = onCategorySelected
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTransactionCategorizationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        loadCategories()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategorySelectionAdapter { category ->
            transaction?.let { txn ->
                onCategorySelected?.invoke(txn, category)
                dismiss()
            }
        }

        binding.rvCategories.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = categoryAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                // Get default categories for now
                val categories = TransactionCategory.getDefaultCategories()
                categoryAdapter.submitList(categories)
            } catch (e: Exception) {
                android.util.Log.e("TransactionCategorization", "Failed to load categories: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
