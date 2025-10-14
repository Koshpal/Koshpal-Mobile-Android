package com.koshpal_android.koshpalapp.ui.home.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.databinding.DialogAddTransactionBinding
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID

@AndroidEntryPoint
class AddTransactionDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var database: KoshpalDatabase
    private var onTransactionAddedListener: (() -> Unit)? = null
    
    private val categories = listOf(
        "Food & Dining",
        "Shopping",
        "Transportation",
        "Bills & Utilities",
        "Entertainment",
        "Healthcare",
        "Education",
        "Travel",
        "Groceries",
        "Salary",
        "Business",
        "Investment",
        "Others"
    )
    
    private val banks = listOf(
        "SBI",
        "HDFC Bank",
        "ICICI Bank",
        "Axis Bank",
        "Kotak Mahindra Bank",
        "Yes Bank",
        "IndusInd Bank",
        "Punjab National Bank",
        "Bank of Baroda",
        "Canara Bank",
        "Union Bank of India",
        "Others"
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.peekHeight = 800
            behavior.isDraggable = true
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        database = KoshpalDatabase.getDatabase(requireContext())
        
        setupCategoryDropdown()
        setupBankDropdown()
        setupPaymentMethodToggle()
        setupButtons()
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
        binding.actvCategory.setText(categories[0], false)
    }

    private fun setupBankDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, banks)
        binding.actvBank.setAdapter(adapter)
    }

    private fun setupPaymentMethodToggle() {
        binding.chipGroupPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipCash -> {
                    binding.tilBank.visibility = View.GONE
                }
                R.id.chipOnline -> {
                    binding.tilBank.visibility = View.VISIBLE
                    binding.actvBank.setText(banks[0], false)
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                saveTransaction()
            }
        }
    }

    private fun validateInput(): Boolean {
        var isValid = true

        // Validate amount
        val amountText = binding.etAmount.text.toString()
        if (amountText.isEmpty()) {
            binding.tilAmount.error = "Amount is required"
            isValid = false
        } else {
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                binding.tilAmount.error = "Please enter a valid amount"
                isValid = false
            } else {
                binding.tilAmount.error = null
            }
        }

        // Validate description
        if (binding.etDescription.text.toString().isEmpty()) {
            binding.tilDescription.error = "Description is required"
            isValid = false
        } else {
            binding.tilDescription.error = null
        }

        // Validate bank for online payment
        if (binding.chipOnline.isChecked && binding.actvBank.text.toString().isEmpty()) {
            binding.tilBank.error = "Please select a bank"
            isValid = false
        } else {
            binding.tilBank.error = null
        }

        return isValid
    }

    private fun saveTransaction() {
        val amount = binding.etAmount.text.toString().toDouble()
        val description = binding.etDescription.text.toString()
        val merchant = binding.etMerchant.text.toString()
        val category = binding.actvCategory.text.toString()
        
        val isCash = binding.chipCash.isChecked
        val bankName = if (isCash) "Cash" else binding.actvBank.text.toString()
        
        val transactionType = if (binding.chipExpense.isChecked) {
            TransactionType.DEBIT
        } else {
            TransactionType.CREDIT
        }

        // Get category ID (you may want to map category names to IDs)
        val categoryId = getCategoryId(category)

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            description = description,
            merchant = merchant.ifEmpty { "Manual Entry" },
            categoryId = categoryId,
            type = transactionType,
            date = System.currentTimeMillis(),
            isProcessed = true,
            isManuallySet = true,
            bankName = bankName,
            isCashFlow = isCash,
            confidence = 1.0f,
            isBankEnabled = true
        )

        // Save to database
        lifecycleScope.launch {
            try {
                // First, ensure default categories exist
                ensureDefaultCategoriesExist()
                
                // Then insert the transaction
                database.transactionDao().insertTransaction(transaction)
                
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Transaction added successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    onTransactionAddedListener?.invoke()
                    dismiss()
                }
            } catch (e: Exception) {
                android.util.Log.e("AddTransactionDialog", "Error saving transaction", e)
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Error adding transaction: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun ensureDefaultCategoriesExist() {
        try {
            // Check if categories exist
            val existingCategories = database.categoryDao().getAllCategoriesOnce()
            
            if (existingCategories.isEmpty()) {
                // Insert default categories
                val defaultCategories = com.koshpal_android.koshpalapp.model.TransactionCategory.getDefaultCategories()
                defaultCategories.forEach { category ->
                    database.categoryDao().insertCategory(category)
                }
                android.util.Log.d("AddTransactionDialog", "✅ Inserted ${defaultCategories.size} default categories")
            }
        } catch (e: Exception) {
            android.util.Log.e("AddTransactionDialog", "Error ensuring categories exist", e)
        }
    }

    private fun getCategoryId(categoryName: String): String {
        // Map category names to IDs
        return when (categoryName) {
            "Food & Dining" -> "food"
            "Shopping" -> "shopping"
            "Transportation" -> "transport"
            "Bills & Utilities" -> "bills"
            "Entertainment" -> "entertainment"
            "Healthcare" -> "healthcare"
            "Education" -> "education"
            "Travel" -> "travel"
            "Groceries" -> "groceries"
            "Salary" -> "salary"
            "Business" -> "business"
            "Investment" -> "investment"
            else -> "others"
        }
    }

    fun setOnTransactionAddedListener(listener: () -> Unit) {
        onTransactionAddedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddTransactionDialog"
    }
}
