package com.koshpal_android.koshpalapp.ui.categorization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.koshpal_android.koshpalapp.databinding.BottomSheetCategorySelectionBinding
import com.koshpal_android.koshpalapp.model.TransactionCategory
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategorySelectionBottomSheet : BottomSheetDialogFragment() {
    
    private var _binding: BottomSheetCategorySelectionBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CategorySelectionViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryGridAdapter
    
    private var onCategorySelected: ((TransactionCategory) -> Unit)? = null
    private var selectedTransactionId: String? = null
    
    companion object {
        private const val ARG_TRANSACTION_ID = "transaction_id"
        
        fun newInstance(
            transactionId: String,
            onCategorySelected: (TransactionCategory) -> Unit
        ): CategorySelectionBottomSheet {
            return CategorySelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TRANSACTION_ID, transactionId)
                }
                this.onCategorySelected = onCategorySelected
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCategorySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        selectedTransactionId = arguments?.getString(ARG_TRANSACTION_ID)
        
        setupRecyclerView()
        setupSearchBar()
        setupClickListeners()
        observeViewModel()
        
        viewModel.loadCategories()
    }
    
    private fun setupRecyclerView() {
        categoryAdapter = CategoryGridAdapter { category ->
            onCategorySelected?.invoke(category)
            dismiss()
        }
        
        binding.rvCategories.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = categoryAdapter
        }
    }
    
    private fun setupSearchBar() {
        binding.etSearchCategory.addTextChangedListener { text ->
            viewModel.searchCategories(text.toString())
        }
    }
    
    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        
        binding.btnCreateCustomCategory.setOnClickListener {
            // TODO: Implement custom category creation dialog
            showCreateCustomCategoryDialog()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collect { categories ->
                categoryAdapter.submitList(categories)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredCategories.collect { categories ->
                categoryAdapter.submitList(categories)
            }
        }
    }
    
    private fun showCreateCustomCategoryDialog() {
        val dialog = CreateCustomCategoryDialog { category ->
            viewModel.createCustomCategory(category)
        }
        dialog.show(parentFragmentManager, "CreateCustomCategoryDialog")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
