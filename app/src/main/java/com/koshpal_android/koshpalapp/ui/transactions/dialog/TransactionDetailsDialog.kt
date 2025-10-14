package com.koshpal_android.koshpalapp.ui.transactions.dialog

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.DialogTransactionDetailsBinding
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.ui.transactions.TransactionsViewModel
import com.koshpal_android.koshpalapp.ui.transactions.adapter.TagsAdapter
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TransactionDetailsDialog : BottomSheetDialogFragment() {

    private var _binding: DialogTransactionDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by viewModels()
    private lateinit var tagsAdapter: TagsAdapter
    
    private var transaction: Transaction? = null
    private var onTransactionUpdated: ((Transaction) -> Unit)? = null
    
    private var selectedImageUri: Uri? = null
    private var currentTags = mutableListOf<String>()
    
    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    // Predefined tags
    private val predefinedTags = listOf(
        "family", "friends", "online", "cash", "office"
    )

    // Permission launcher for gallery access
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Permission denied to access gallery", Toast.LENGTH_SHORT).show()
        }
    }

    // Multiple permissions launcher for Android 13+
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Permission denied to access gallery", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                showPhotoPreview(uri)
            }
        }
    }

    companion object {
        fun newInstance(
            transaction: Transaction,
            onTransactionUpdated: (Transaction) -> Unit
        ): TransactionDetailsDialog {
            return TransactionDetailsDialog().apply {
                this.transaction = transaction
                this.onTransactionUpdated = onTransactionUpdated
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTransactionDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupAdapters()
        setupClickListeners()
        loadTransactionData()
    }

    private fun setupViews() {
        transaction?.let { txn ->
            binding.apply {
                // Set transaction data
                etMerchant.setText(txn.merchant)
                etAmount.setText(txn.amount.toString())
                etNotes.setText(txn.notes)
                
                // Set date
                val dateFormat = SimpleDateFormat("EEE, d MMM, h:mm a", Locale.getDefault())
                tvDate.text = dateFormat.format(Date(txn.timestamp))
                
                // Set cash flow toggle - check if transaction is in cash flow table
                lifecycleScope.launch {
                    val isCashFlow = transactionRepository.isCashFlowTransaction(txn.id)
                    switchCashFlow.isChecked = isCashFlow
                }
                
                // Set star status
                updateStarButton(txn.isStarred)
                
                // Set category icon and name
                loadCategoryInfo(txn.categoryId)
                
                // Set UPI reference
                tvUpiRef.text = extractUpiReference(txn.smsBody)
                
                // Set SMS
                tvSms.text = txn.smsBody ?: "No SMS data available"
                
                // Load existing tags
                txn.tags?.let { tagsString ->
                    currentTags.clear()
                    currentTags.addAll(tagsString.split(",").filter { it.isNotBlank() })
                }
                
                // Show photo if exists
                txn.attachmentPath?.let { path ->
                    // Load and show photo preview
                    showPhotoPreview(Uri.parse(path))
                }
            }
        }
    }

    private fun setupAdapters() {
        // Setup tags adapter
        tagsAdapter = TagsAdapter(
            tags = currentTags,
            onTagRemoved = { tag ->
                currentTags.remove(tag)
                updateTagsDisplay()
            }
        )
        
        binding.rvTags.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = tagsAdapter
        }
    }

    private fun updateTagsDisplay() {
        tagsAdapter.updateTags(currentTags)
    }

    private fun loadCategoryInfo(categoryId: String?) {
        lifecycleScope.launch {
            try {
                val categories = TransactionCategory.getDefaultCategories()
                val category = categories.find { it.id == categoryId }
                
                category?.let {
                    binding.tvCategory.text = it.name
                    binding.ivCategoryIcon.setImageResource(it.icon)
                    
                    // Set category icon background color
                    try {
                        val color = Color.parseColor(it.color)
                        binding.cardCategoryIcon.setCardBackgroundColor(color)
                        binding.ivCategoryIcon.setColorFilter(Color.WHITE)
                    } catch (e: Exception) {
                        // Fallback to default colors
                        binding.cardCategoryIcon.setCardBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.primary_light)
                        )
                        binding.ivCategoryIcon.setColorFilter(
                            ContextCompat.getColor(requireContext(), R.color.primary)
                        )
                    }
                } ?: run {
                    // Default category if not found
                    binding.tvCategory.text = "Uncategorized"
                    binding.ivCategoryIcon.setImageResource(R.drawable.ic_category_default)
                    binding.cardCategoryIcon.setCardBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.surface_gray)
                    )
                    binding.ivCategoryIcon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.text_secondary)
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("TransactionDetails", "Failed to load category info: ${e.message}")
                binding.tvCategory.text = "Uncategorized"
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_category)
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnClose.setOnClickListener { dismiss() }
            
            btnStar.setOnClickListener {
                transaction?.let { txn ->
                    val newStarStatus = !txn.isStarred
                    updateStarButton(newStarStatus)
                    transaction = txn.copy(isStarred = newStarStatus)
                    
                    // Immediately save starred status to database
                    lifecycleScope.launch {
                        try {
                            val updatedTxn = txn.copy(isStarred = newStarStatus, updatedAt = System.currentTimeMillis())
                            transactionRepository.updateTransaction(updatedTxn)
                            android.util.Log.d("TransactionDetailsDialog", "⭐ Starred status updated: ${updatedTxn.id} -> $newStarStatus")
                            
                            val message = if (newStarStatus) "Transaction starred" else "Transaction unstarred"
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            android.util.Log.e("TransactionDetailsDialog", "❌ Failed to update starred status: ${e.message}")
                            // Revert UI change if database update failed
                            updateStarButton(!newStarStatus)
                            transaction = txn.copy(isStarred = !newStarStatus)
                        }
                    }
                }
            }
            
            cardAttach.setOnClickListener {
                checkGalleryPermissionAndOpen()
            }
            
            btnRemovePhoto.setOnClickListener {
                removePhoto()
            }
            
            btnMenu.setOnClickListener {
                showOptionsMenu(it)
            }
            
            // Make category section clickable in the blue card
            cardCategorySelection.setOnClickListener {
                showCategorySelectionDialog()
            }
            
            // Handle add tag button clicks
            btnAddTag.setOnClickListener {
                showAddTagDialog()
            }
            
            // Also handle clicks on the parent MaterialCardView
            (btnAddTag.parent as? View)?.setOnClickListener {
                showAddTagDialog()
            }
            
            switchCashFlow.setOnCheckedChangeListener { _, isChecked ->
                transaction?.let { txn ->
                    lifecycleScope.launch {
                        try {
                            if (isChecked) {
                                // Add to cash flow database
                                transactionRepository.addToCashFlow(txn.id)
                                android.util.Log.d("TransactionDetailsDialog", "💰 Added transaction ${txn.id} to cash flow")
                                Toast.makeText(requireContext(), "Added to Cash Flow", Toast.LENGTH_SHORT).show()
                            } else {
                                // Remove from cash flow database
                                transactionRepository.removeFromCashFlow(txn.id)
                                android.util.Log.d("TransactionDetailsDialog", "💰 Removed transaction ${txn.id} from cash flow")
                                Toast.makeText(requireContext(), "Removed from Cash Flow", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TransactionDetailsDialog", "❌ Failed to update cash flow status: ${e.message}")
                            // Revert the switch state
                            switchCashFlow.isChecked = !isChecked
                            Toast.makeText(requireContext(), "Failed to update cash flow status", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            
            btnSave.setOnClickListener {
                saveTransaction()
            }
        }
    }


    private fun showCategorySelectionDialog() {
        transaction?.let { txn ->
            val categorizationDialog = TransactionCategorizationDialog.newInstance(txn) { updatedTxn, selectedCategory ->
                // Update the category in the blue card
                binding.tvCategory.text = selectedCategory.name
                binding.ivCategoryIcon.setImageResource(selectedCategory.icon)
                
                // Set category icon background color
                try {
                    val color = Color.parseColor(selectedCategory.color)
                    binding.cardCategoryIcon.setCardBackgroundColor(color)
                    binding.ivCategoryIcon.setColorFilter(Color.WHITE)
                } catch (e: Exception) {
                    // Fallback to default colors
                    binding.cardCategoryIcon.setCardBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.primary_light)
                    )
                    binding.ivCategoryIcon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.primary)
                    )
                }
                
                // Update local transaction
                transaction = updatedTxn.copy(categoryId = selectedCategory.id)
                
                // Immediately save to database (like HomeFragment does)
                lifecycleScope.launch {
                    try {
                        transactionRepository.updateTransactionCategory(txn.id, selectedCategory.id)
                        android.util.Log.d("TransactionDetailsDialog", "✅ Category updated in database: ${txn.id} -> ${selectedCategory.name}")
                        
                        // ✅ FIX: Refresh Categories fragment so categorized transactions appear there
                        (activity as? com.koshpal_android.koshpalapp.ui.home.HomeActivity)?.refreshCategoriesData()
                        android.util.Log.d("TransactionDetailsDialog", "🔄 Categories fragment refresh triggered")
                        
                        Toast.makeText(requireContext(), "Category updated to ${selectedCategory.name}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.util.Log.e("TransactionDetailsDialog", "❌ Failed to update category in database: ${e.message}")
                        Toast.makeText(requireContext(), "Failed to update category", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            categorizationDialog.show(parentFragmentManager, "CategorySelection")
        }
    }

    private fun updateTransactionCategory(category: TransactionCategory) {
        binding.tvCategory.text = category.name
        transaction = transaction?.copy(categoryId = category.id)
        // TODO: Update category icon
    }

    private fun checkGalleryPermissionAndOpen() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ uses READ_MEDIA_IMAGES
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
                    }
                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
            }
            else -> {
                // Below Android 13 uses READ_EXTERNAL_STORAGE
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
                    }
                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun showPhotoPreview(uri: Uri) {
        binding.apply {
            cardPhotoPreview.visibility = View.VISIBLE
            ivPhotoPreview.setImageURI(uri)
            
            // TODO: Calculate and show file size
            tvPhotoSize.text = "Image attached"
        }
    }

    private fun removePhoto() {
        selectedImageUri = null
        binding.cardPhotoPreview.visibility = View.GONE
        transaction = transaction?.copy(attachmentPath = null)
    }

    private fun showAddTagDialog() {
        val availableTags = predefinedTags.filter { !currentTags.contains(it) }
        
        if (availableTags.isEmpty()) {
            Toast.makeText(requireContext(), "All predefined tags are already added", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create a simple dialog with predefined tags
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Add Tag")
        
        val tagNames = availableTags.toTypedArray()
        builder.setItems(tagNames) { _, which ->
            val selectedTag = tagNames[which]
            if (!currentTags.contains(selectedTag)) {
                currentTags.add(selectedTag)
                tagsAdapter.updateTags(currentTags)
            }
        }
        
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun updateStarButton(isStarred: Boolean) {
        binding.btnStar.setImageResource(
            if (isStarred) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )
        binding.btnStar.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                if (isStarred) R.color.warning else R.color.text_secondary
            )
        )
    }

    private fun extractUpiReference(smsBody: String?): String {
        if (smsBody == null) return "N/A"
        
        // Extract UPI reference number from SMS
        val regex = "Refno\\s+(\\d+)".toRegex()
        val match = regex.find(smsBody)
        return match?.groupValues?.get(1) ?: "N/A"
    }

    private fun showOptionsMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.transaction_options_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete -> {
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction() {
        transaction?.let { txn ->
            lifecycleScope.launch {
                try {
                    // Delete from repository using injected instance
                    transactionRepository.deleteTransaction(txn)
                    
                    Toast.makeText(requireContext(), "Transaction deleted successfully", Toast.LENGTH_SHORT).show()
                    dismiss()
                    
                    // Refresh the parent fragment
                    (parentFragment as? com.koshpal_android.koshpalapp.ui.transactions.TransactionsFragment)?.let {
                        // Trigger refresh
                    }
                    
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to delete transaction", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveTransaction() {
        val updatedTransaction = transaction?.copy(
            merchant = binding.etMerchant.text.toString().trim(),
            amount = binding.etAmount.text.toString().toDoubleOrNull() ?: transaction?.amount ?: 0.0,
            notes = binding.etNotes.text.toString().trim().takeIf { it.isNotEmpty() },
            tags = currentTags.joinToString(",").takeIf { it.isNotEmpty() },
            attachmentPath = selectedImageUri?.toString(),
            updatedAt = System.currentTimeMillis()
        )
        
        updatedTransaction?.let { txn ->
            // Save to database using repository
            lifecycleScope.launch {
                try {
                    transactionRepository.updateTransaction(txn)
                    android.util.Log.d("TransactionDetailsDialog", "✅ Transaction updated in database: ${txn.id}")
                    
                    onTransactionUpdated?.invoke(txn)
                    dismiss()
                    
                    Toast.makeText(requireContext(), "Transaction updated successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.util.Log.e("TransactionDetailsDialog", "❌ Failed to update transaction: ${e.message}")
                    Toast.makeText(requireContext(), "Failed to update transaction", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadTransactionData() {
        // Load any additional transaction data if needed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
