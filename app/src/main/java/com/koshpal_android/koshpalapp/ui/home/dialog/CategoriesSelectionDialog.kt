package com.koshpal_android.koshpalapp.ui.home.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.databinding.DialogCategoriesSelectionBinding
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.ui.home.dialog.adapter.CategoryGridAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class CategoriesSelectionDialog : BottomSheetDialogFragment() {

    private var _binding: DialogCategoriesSelectionBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    private lateinit var database: KoshpalDatabase
    private lateinit var categoryAdapter: CategoryGridAdapter
    
    private var onCategorySelected: ((TransactionCategory) -> Unit)? = null

    companion object {
        const val TAG = "CategoriesSelectionDialog"
        
        fun newInstance(
            onCategorySelected: (TransactionCategory) -> Unit
        ): CategoriesSelectionDialog {
            return CategoriesSelectionDialog().apply {
                this.onCategorySelected = onCategorySelected
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.peekHeight = resources.displayMetrics.heightPixels
            behavior.isDraggable = true
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCategoriesSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        database = KoshpalDatabase.getDatabase(requireContext())
        
        setupRecyclerView()
        setupClickListeners()
        setupNewCategoryInput()
        loadCategories()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryGridAdapter { category ->
            onCategorySelected?.invoke(category)
            dismiss()
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

    private fun setupNewCategoryInput() {
        binding.etNewCategory.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                createNewCategory()
                true
            } else {
                false
            }
        }
    }

    private fun createNewCategory() {
        val categoryName = binding.etNewCategory.text.toString().trim()
        
        if (categoryName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Check if category already exists
                val existingCategories = database.categoryDao().getAllCategoriesOnce()
                val categoryExists = existingCategories.any { 
                    it.name.equals(categoryName, ignoreCase = true) 
                }
                
                if (categoryExists) {
                    Toast.makeText(requireContext(), "Category already exists", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create new category
                val newCategory = TransactionCategory(
                    id = UUID.randomUUID().toString(),
                    name = categoryName,
                    icon = R.drawable.ic_category_default,
                    color = "#607D8B", // Default grey color
                    keywords = emptyList(),
                    isDefault = false,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )

                // Save to database
                database.categoryDao().insertCategory(newCategory)
                
                // Refresh the list
                loadCategories()
                
                // Clear input
                binding.etNewCategory.text?.clear()
                
                Toast.makeText(requireContext(), "Category created successfully", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                android.util.Log.e("CategoriesSelectionDialog", "Error creating category: ${e.message}", e)
                Toast.makeText(requireContext(), "Error creating category: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                // Ensure defaults exist
                transactionRepository.ensureDefaultCategoriesExist()
                
                // Load all active categories (includes custom ones)
                val categories = transactionRepository.getAllActiveCategoriesList()
                categoryAdapter.submitList(categories)
                
            } catch (e: Exception) {
                android.util.Log.e("CategoriesSelectionDialog", "Failed to load categories: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

