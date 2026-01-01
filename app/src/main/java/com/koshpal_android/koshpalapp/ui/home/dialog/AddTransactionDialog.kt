package com.koshpal_android.koshpalapp.ui.home.dialog

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.databinding.DialogAddTransactionBinding
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.ui.transactions.adapter.TagsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddTransactionDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var database: KoshpalDatabase
    private var onTransactionAddedListener: (() -> Unit)? = null
    
    private var selectedDate = Calendar.getInstance()
    private var selectedCategory: String? = null
    private var selectedCategoryId: String? = null
    private var selectedAccount = "CASH"
    private var isExpense = true
    private var isSpendSelected = true
    private var shouldAddAnother = false
    private var selectedTags: String? = null
    private val currentTags = mutableListOf<String>()
    private var selectedAttachmentUri: Uri? = null
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedAttachmentUri = uri
                binding.tvAttachmentHint.text = "Photo attached"
                binding.tvAttachmentHint.setTextColor(requireContext().getColor(R.color.primary))
            }
        }
    }
    
    private val categories = listOf(
        "Food & Dining",
        "Grocery",
        "Transportation",
        "Bills & Utilities",
        "Education",
        "Entertainment",
        "Healthcare",
        "Shopping",
        "Salary & Income",
        "Others"
    )
    
    private val accounts = listOf(
        "CASH",
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
            behavior.peekHeight = resources.displayMetrics.heightPixels
            behavior.isDraggable = true
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
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
        
        setupUI()
        setupClickListeners()
        updateDateTimeDisplay()
    }

    private fun setupUI() {
        // Set initial state - Spend selected, Income unselected
        updateTransactionTypeButtons()
        
        // Set initial category
        selectedCategory = categories[0]
        selectedCategoryId = getCategoryId(categories[0])
        binding.tvCategory.text = selectedCategory
        
        // Set initial account
        binding.tvAccount.text = selectedAccount
        
        // Set expense toggle based on Spend/Income selection
        binding.switchExpense.isChecked = isExpense
        
        // Initialize tags display
        updateTagsDisplay()
    }

    private fun setupClickListeners() {
        // Header buttons
        binding.btnBack.setOnClickListener { dismiss() }
        binding.btnClose.setOnClickListener { dismiss() }
        
        // Transaction type buttons
        binding.btnSpend.setOnClickListener {
            isSpendSelected = true
            isExpense = true
            updateTransactionTypeButtons()
            binding.switchExpense.isChecked = true
        }
        
        binding.btnIncome.setOnClickListener {
            isSpendSelected = false
            isExpense = false
            updateTransactionTypeButtons()
            binding.switchExpense.isChecked = false
        }
        
        // Date & Time picker
        binding.tvDateTime.setOnClickListener {
            showDateTimePicker()
        }
        
        // Account selector
        binding.tvAccount.setOnClickListener {
            showAccountSelector()
        }
        
        // Expense toggle
        binding.switchExpense.setOnCheckedChangeListener { _, isChecked ->
            isExpense = isChecked
            // Update transaction type based on toggle
            if (isChecked) {
                isSpendSelected = true
                updateTransactionTypeButtons()
            } else {
                isSpendSelected = false
                updateTransactionTypeButtons()
            }
        }
        
        // Category selector
        binding.tvCategory.setOnClickListener {
            showCategorySelector()
        }
        
        binding.btnAddCategory.setOnClickListener {
            showCategorySelector()
        }
        
        // Tags selector
        binding.tvTags.setOnClickListener {
            showTagsSelector()
        }
        
        binding.btnAddTag.setOnClickListener {
            showTagsSelector()
        }
        
        // Attachment button
        binding.btnAddAttachment.setOnClickListener {
            showAttachmentPicker()
        }
        
        // Calculator button (optional - can open calculator or do nothing)
        binding.btnCalculator.setOnClickListener {
            // Optional: Open calculator or show amount input dialog
        }
        
        // Save buttons
        binding.btnSave.setOnClickListener {
            shouldAddAnother = false
            if (validateInput()) {
                saveTransaction()
            }
        }
        
        binding.btnSaveAndAdd.setOnClickListener {
            shouldAddAnother = true
            if (validateInput()) {
                saveTransaction()
            }
        }
    }

    private fun updateTransactionTypeButtons() {
        val primaryColor = requireContext().getColor(R.color.primary)
        val whiteColor = requireContext().getColor(R.color.white)
        val textGrayColor = 0xFFA0AEC0.toInt()
        
        if (isSpendSelected) {
            // Spend selected - blue background with checkmark icon
            binding.btnSpend.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            binding.btnSpend.setTextColor(whiteColor)
            binding.btnSpend.icon = requireContext().getDrawable(R.drawable.ic_check_circle)
            binding.btnSpend.iconTint = android.content.res.ColorStateList.valueOf(whiteColor)
            // Income unselected - outlined
            binding.btnIncome.backgroundTintList = android.content.res.ColorStateList.valueOf(0x1A1F2E)
            binding.btnIncome.setTextColor(whiteColor)
            binding.btnIncome.icon = null
        } else {
            // Income selected - blue background with checkmark icon
            binding.btnIncome.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            binding.btnIncome.setTextColor(whiteColor)
            binding.btnIncome.icon = requireContext().getDrawable(R.drawable.ic_check_circle)
            binding.btnIncome.iconTint = android.content.res.ColorStateList.valueOf(whiteColor)
            // Spend unselected - outlined
            binding.btnSpend.backgroundTintList = android.content.res.ColorStateList.valueOf(0x1A1F2E)
            binding.btnSpend.setTextColor(whiteColor)
            binding.btnSpend.icon = null
        }
    }

    private fun updateDateTimeDisplay() {
        val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        val dateStr = dateFormat.format(selectedDate.time)
        val timeStr = timeFormat.format(selectedDate.time)
        
        // Check if today
        val today = Calendar.getInstance()
        val isToday = selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        
        if (isToday) {
            binding.tvDateTime.text = "Today, $timeStr"
        } else {
            binding.tvDateTime.text = "$dateStr, $timeStr"
        }
    }

    private fun showDateTimePicker() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                // After date is selected, show time picker
                val timePicker = TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDate.set(Calendar.MINUTE, minute)
                        updateDateTimeDisplay()
                    },
                    selectedDate.get(Calendar.HOUR_OF_DAY),
                    selectedDate.get(Calendar.MINUTE),
                    false
                )
                timePicker.show()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showAccountSelector() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Account")
            .setItems(accounts.toTypedArray()) { _, which ->
                selectedAccount = accounts[which]
                binding.tvAccount.text = selectedAccount
            }
            .show()
    }

    private fun showCategorySelector() {
        val dialog = CategoriesSelectionDialog.newInstance { category ->
            selectedCategory = category.name
            selectedCategoryId = category.id
            binding.tvCategory.text = selectedCategory
        }
        dialog.show(parentFragmentManager, CategoriesSelectionDialog.TAG)
    }

    private fun showTagsSelector() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_tags, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val etCustomTag = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCustomTag)
        val btnAddCustomTag = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnAddCustomTag)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
        val btnDone = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDone)
        val chipFriends = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipFriends)
        val chipOffice = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipOffice)
        val chipFamily = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipFamily)
        val chipSchool = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipSchool)
        val chipOnline = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipOnline)
        val rvExistingTags = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvExistingTags)
        
        // Load existing tags from selectedTags string
        currentTags.clear()
        selectedTags?.let { tagsString ->
            currentTags.addAll(tagsString.split(",").filter { it.isNotBlank() })
        }
        
        // Setup RecyclerView for existing tags
        var dialogTagsAdapter: TagsAdapter? = null
        dialogTagsAdapter = TagsAdapter(
            tags = currentTags.toMutableList(),
            onTagRemoved = { tag ->
                currentTags.remove(tag)
                val updatedTags = currentTags.toMutableList()
                dialogTagsAdapter?.updateTags(updatedTags)
                // Show/hide existing tags section
                rvExistingTags.visibility = if (currentTags.isNotEmpty()) View.VISIBLE else View.GONE
                // Update chip states
                chipFriends.isChecked = currentTags.contains("Friends")
                chipOffice.isChecked = currentTags.contains("Office")
                chipFamily.isChecked = currentTags.contains("Family")
                chipSchool.isChecked = currentTags.contains("School")
                chipOnline.isChecked = currentTags.contains("Online")
                // Update selectedTags string
                selectedTags = if (currentTags.isNotEmpty()) currentTags.joinToString(",") else null
                updateTagsDisplay()
            }
        )
        
        rvExistingTags.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvExistingTags.adapter = dialogTagsAdapter
        
        // Show/hide existing tags section
        if (currentTags.isNotEmpty()) {
            rvExistingTags.visibility = View.VISIBLE
        } else {
            rvExistingTags.visibility = View.GONE
        }
        
        // Set selected state for existing tags
        chipFriends.isChecked = currentTags.contains("Friends")
        chipOffice.isChecked = currentTags.contains("Office")
        chipFamily.isChecked = currentTags.contains("Family")
        chipSchool.isChecked = currentTags.contains("School")
        chipOnline.isChecked = currentTags.contains("Online")
        
        // Handle predefined tag clicks
        val onTagClick: (String) -> Unit = { tagName ->
            if (currentTags.contains(tagName)) {
                currentTags.remove(tagName)
            } else {
                currentTags.add(tagName)
            }
            dialogTagsAdapter?.updateTags(currentTags)
            // Show/hide existing tags section
            rvExistingTags.visibility = if (currentTags.isNotEmpty()) View.VISIBLE else View.GONE
            // Update chip state
            when (tagName) {
                "Friends" -> chipFriends.isChecked = currentTags.contains("Friends")
                "Office" -> chipOffice.isChecked = currentTags.contains("Office")
                "Family" -> chipFamily.isChecked = currentTags.contains("Family")
                "School" -> chipSchool.isChecked = currentTags.contains("School")
                "Online" -> chipOnline.isChecked = currentTags.contains("Online")
            }
            // Update selectedTags string
            selectedTags = if (currentTags.isNotEmpty()) currentTags.joinToString(",") else null
            updateTagsDisplay()
        }
        
        chipFriends.setOnClickListener { onTagClick("Friends") }
        chipOffice.setOnClickListener { onTagClick("Office") }
        chipFamily.setOnClickListener { onTagClick("Family") }
        chipSchool.setOnClickListener { onTagClick("School") }
        chipOnline.setOnClickListener { onTagClick("Online") }
        
        // Handle custom tag input
        etCustomTag.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val customTag = etCustomTag.text.toString().trim().removePrefix("#").trim()
                if (customTag.isNotEmpty() && !currentTags.contains(customTag)) {
                    currentTags.add(customTag)
                    dialogTagsAdapter?.updateTags(currentTags)
                    rvExistingTags.visibility = View.VISIBLE
                    etCustomTag.text?.clear()
                    // Update selectedTags string
                    selectedTags = if (currentTags.isNotEmpty()) currentTags.joinToString(",") else null
                    updateTagsDisplay()
                }
                true
            } else {
                false
            }
        }
        
        btnAddCustomTag.setOnClickListener {
            val customTag = etCustomTag.text.toString().trim().removePrefix("#").trim()
            if (customTag.isNotEmpty() && !currentTags.contains(customTag)) {
                currentTags.add(customTag)
                dialogTagsAdapter?.updateTags(currentTags)
                rvExistingTags.visibility = View.VISIBLE
                etCustomTag.text?.clear()
                // Update selectedTags string
                selectedTags = if (currentTags.isNotEmpty()) currentTags.joinToString(",") else null
                updateTagsDisplay()
            }
        }
        
        btnClose.setOnClickListener {
            // Save tags before closing
            selectedTags = if (currentTags.isNotEmpty()) currentTags.joinToString(",") else null
            updateTagsDisplay()
            dialog.dismiss()
        }
        
        btnDone.setOnClickListener {
            // Save tags and close dialog
            selectedTags = if (currentTags.isNotEmpty()) currentTags.joinToString(",") else null
            updateTagsDisplay()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun updateTagsDisplay() {
        if (currentTags.isNotEmpty()) {
            binding.tvTags.text = currentTags.joinToString(", ")
            binding.tvTags.setTextColor(requireContext().getColor(R.color.primary))
        } else {
            binding.tvTags.text = "Tags"
            binding.tvTags.setTextColor(requireContext().getColor(android.R.color.darker_gray))
        }
    }

    private fun showAttachmentPicker() {
        // Open image picker
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private fun validateInput(): Boolean {
        var isValid = true

        // Validate amount
        val amountText = binding.etAmount.text.toString().trim()
        if (amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show()
            isValid = false
        } else {
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                isValid = false
            }
        }

        // Validate category
        if (selectedCategory == null || selectedCategory!!.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun saveTransaction() {
        val amountText = binding.etAmount.text.toString().trim()
        val amount = amountText.toDoubleOrNull() ?: 0.0
        
        val paidTo = binding.etPaidTo.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()
        val category = selectedCategory ?: categories[0]
        
        val isCash = selectedAccount == "CASH"
        val bankName = if (isCash) "Cash" else selectedAccount
        
        val transactionType = if (isExpense) {
            TransactionType.DEBIT
        } else {
            TransactionType.CREDIT
        }

        // Get category ID - use selectedCategoryId if available, otherwise try to find it
        val categoryId = selectedCategoryId ?: getCategoryId(category)

        // Prepare tags string
        val tagsString = if (currentTags.isNotEmpty()) {
            currentTags.joinToString(",")
        } else {
            selectedTags
        }
        
        val transaction = Transaction(
            id = java.util.UUID.randomUUID().toString(),
            amount = amount,
            description = paidTo.ifEmpty { notes.ifEmpty { "Manual Entry" } },
            merchant = paidTo.ifEmpty { "Manual Entry" },
            categoryId = categoryId,
            type = transactionType,
            date = selectedDate.timeInMillis,
            isProcessed = true,
            isManuallySet = true,
            bankName = bankName,
            isCashFlow = isCash,
            confidence = 1.0f,
            isBankEnabled = true,
            tags = tagsString // Include tags in transaction
        )

        // Save to database
        lifecycleScope.launch {
            try {
                android.util.Log.d("AddTransactionDialog", "💾 Saving transaction with category: $category -> $categoryId")
                
                // First, ensure default categories exist
                ensureDefaultCategoriesExist()
                
                // Verify category exists
                val categoryExists = database.categoryDao().getCategoryById(categoryId)
                if (categoryExists == null) {
                    throw Exception("Category '$category' (ID: $categoryId) does not exist in database")
                }
                
                android.util.Log.d("AddTransactionDialog", "✅ Category verified: ${categoryExists.name}")
                
                // Then insert the transaction
                database.transactionDao().insertTransaction(transaction)
                
                android.util.Log.d("AddTransactionDialog", "✅ Transaction saved successfully: ₹$amount - $paidTo")
                
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Transaction added successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    onTransactionAddedListener?.invoke()
                    
                    if (shouldAddAnother) {
                        // Clear form and keep dialog open
                        clearForm()
                    } else {
                        dismiss()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AddTransactionDialog", "❌ Error saving transaction: ${e.message}", e)
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun clearForm() {
        binding.etAmount.text?.clear()
        binding.etPaidTo.text?.clear()
        binding.etNotes.text?.clear()
        selectedDate = Calendar.getInstance()
        updateDateTimeDisplay()
        selectedCategory = categories[0]
        selectedCategoryId = getCategoryId(categories[0])
        binding.tvCategory.text = selectedCategory
        selectedAccount = "CASH"
        binding.tvAccount.text = selectedAccount
        isSpendSelected = true
        isExpense = true
        updateTransactionTypeButtons()
        binding.switchExpense.isChecked = true
        selectedTags = null
        currentTags.clear()
        updateTagsDisplay()
        selectedAttachmentUri = null
        binding.tvAttachmentHint.text = "Photo of a receipt/warranty"
        binding.tvAttachmentHint.setTextColor(0xFF5A6B7F.toInt())
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
        // Map category names to actual database IDs (MUST match TransactionCategory.getDefaultCategories())
        return when (categoryName) {
            "Food & Dining" -> "food"
            "Grocery" -> "grocery"
            "Transportation" -> "transport"
            "Bills & Utilities" -> "bills"
            "Education" -> "education"
            "Entertainment" -> "entertainment"
            "Healthcare" -> "healthcare"
            "Shopping" -> "shopping"
            "Salary & Income" -> "salary"
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

