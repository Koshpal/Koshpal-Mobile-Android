package com.koshpal_android.koshpalapp.ui.transactions.dialog

import android.Manifest
import android.app.Activity
import android.app.Dialog
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
import android.widget.ImageButton
import android.widget.ImageView
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
    private var inlineTagsAdapter: TagsAdapter? = null
    
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

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        return com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), theme).apply {
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
        _binding = DialogTransactionDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAdapters() // Initialize adapters first
        setupViews()
        setupClickListeners()
        loadTransactionData()
    }

    private fun setupViews() {
        transaction?.let { txn ->
            binding.apply {
                // Set title based on transaction type
                val titleText = when (txn.type) {
                    com.koshpal_android.koshpalapp.model.TransactionType.DEBIT -> "Debit transaction"
                    com.koshpal_android.koshpalapp.model.TransactionType.CREDIT -> "Credit transaction"
                    else -> "Transaction Details"
                }
                tvTitle.text = titleText
                
                // Set amount with + or - prefix
                val amountPrefix = if (txn.type == com.koshpal_android.koshpalapp.model.TransactionType.CREDIT) "+" else "-"
                tvAmount.text = "$amountPrefix₹${txn.amount}"
                
                // Set Credited/Debited label - use blue color instead of green
                val typeLabel = if (txn.type == com.koshpal_android.koshpalapp.model.TransactionType.CREDIT) "Credited" else "Debited"
                tvTransactionTypeLabel.text = typeLabel
                tvTransactionTypeLabel.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.primary)
                )
                
                // Set description (merchant or description)
                val description = txn.description.ifEmpty { txn.merchant }
                if (description.isNotEmpty()) {
                    binding.tvDescription.text = description
                    binding.tvDescription.visibility = View.VISIBLE
                } else {
                    binding.tvDescription.visibility = View.GONE
                }
                
                // Set date and time - format: "Sat, 5th, 12:28 pm"
                val dateStr = formatDateWithOrdinal(txn.date)
                tvDate.text = dateStr
                tvDateTop.text = dateStr
                
                // Display notes if they exist
                if (txn.notes.isNullOrEmpty()) {
                    binding.tvNotesDisplay.visibility = View.GONE
                    btnAddNotes.text = "Tap to add"
                } else {
                    binding.tvNotesDisplay.text = txn.notes
                    binding.tvNotesDisplay.visibility = View.VISIBLE
                    btnAddNotes.text = "Edit notes"
                }
                
                // Set cash flow toggle - check if transaction is in cash flow table
                lifecycleScope.launch {
                    val isCashFlow = transactionRepository.isCashFlowTransaction(txn.id)
                    // Temporarily remove listener to avoid triggering toast on initial load
                    switchCashFlow.setOnCheckedChangeListener(null)
                    switchCashFlow.isChecked = isCashFlow
                    // Re-attach listener after setting initial state
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
                                    switchCashFlow.setOnCheckedChangeListener(null)
                                    switchCashFlow.isChecked = !isChecked
                                    switchCashFlow.setOnCheckedChangeListener { _, isChecked ->
                                        // Re-attach listener
                                    }
                                    Toast.makeText(requireContext(), "Failed to update cash flow status", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                
                // Set star status
                updateStarButton(txn.isStarred)
                
                // Set category icon and name
                loadCategoryInfo(txn.categoryId)
                
                // Set transaction type toggle (always visible)
                setupTransactionTypeToggle()
                
                // Set UPI reference
                val upiRef = extractUpiReference(txn.smsBody)
                tvUpiRef.text = if (upiRef == "N/A") "N/A" else upiRef
                
                // Set SMS
                tvSms.text = txn.smsBody ?: "No SMS data available"
                
                // Load existing tags (if any)
                currentTags.clear()
                txn.tags?.let { tagsString ->
                    if (tagsString.isNotBlank()) {
                        val parsedTags = tagsString.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        currentTags.addAll(parsedTags)
                        android.util.Log.d("TransactionDetailsDialog", "📋 Loaded ${currentTags.size} tags: $currentTags")
                    }
                }
                android.util.Log.d("TransactionDetailsDialog", "📋 Current tags after loading: $currentTags (size: ${currentTags.size})")
                // Update tags display after adapters are initialized
                updateTagsDisplay()
                
                // Show photo if exists
                txn.attachmentPath?.let { path ->
                    try {
                        val uri = Uri.parse(path)
                        selectedImageUri = uri
                        // Load and show photo preview
                        showPhotoPreview(uri)
                    } catch (e: Exception) {
                        android.util.Log.e("TransactionDetailsDialog", "❌ Failed to load image: ${e.message}")
                    }
                }
            }
        }
    }

    private fun setupAdapters() {
        // Setup tags adapter for main tags section
        tagsAdapter = TagsAdapter(
            tags = currentTags,
            onTagRemoved = { tag ->
                currentTags.remove(tag)
                updateTagsDisplay()
                // Save tags immediately when removed
                saveTagsToTransaction()
            }
        )
        
        binding.rvTags.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = tagsAdapter
        }
        
        // Setup inline tags adapter (next to date)
        inlineTagsAdapter = TagsAdapter(
            tags = currentTags,
            onTagRemoved = { tag ->
                currentTags.remove(tag)
                updateTagsDisplay()
                // Save tags immediately when removed
                saveTagsToTransaction()
            }
        )
        
        binding.rvTagsInline.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = inlineTagsAdapter
        }
    }

    private fun updateTagsDisplay() {
        // Check if tagsAdapter is initialized
        if (::tagsAdapter.isInitialized) {
            android.util.Log.d("TransactionDetailsDialog", "🔄 Updating tags display: $currentTags (size: ${currentTags.size})")
            tagsAdapter.updateTags(currentTags)
            // Update inline tags adapter
            inlineTagsAdapter?.let { adapter ->
                adapter.updateTags(currentTags)
                android.util.Log.d("TransactionDetailsDialog", "✅ Updated inline tags adapter with ${currentTags.size} tags")
            } ?: run {
                android.util.Log.e("TransactionDetailsDialog", "❌ Inline tags adapter is null")
            }
            // Show/hide inline tags and spacer
            if (currentTags.isNotEmpty()) {
                binding.rvTagsInline.visibility = View.VISIBLE
                binding.tvTagSpacer.visibility = View.VISIBLE
                android.util.Log.d("TransactionDetailsDialog", "👁️ Showing inline tags (${currentTags.size} tags)")
            } else {
                binding.rvTagsInline.visibility = View.GONE
                binding.tvTagSpacer.visibility = View.GONE
                android.util.Log.d("TransactionDetailsDialog", "👁️ Hiding inline tags (no tags)")
            }
            // Also update main tags section
            if (currentTags.isNotEmpty()) {
                binding.rvTags.visibility = View.VISIBLE
            } else {
                binding.rvTags.visibility = View.GONE
            }
        } else {
            android.util.Log.e("TransactionDetailsDialog", "❌ TagsAdapter not initialized yet")
        }
    }

    private fun loadCategoryInfo(categoryId: String?) {
        lifecycleScope.launch {
            try {
                // Load from database first (includes custom categories)
                val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                val category = database.categoryDao().getCategoryById(categoryId ?: "others")
                
                category?.let {
                    binding.tvCategory.text = it.name
                    binding.ivCategoryIcon.setImageResource(it.icon)
                    
                    // Set category icon color (pill shape, no background)
                    try {
                        val color = Color.parseColor(it.color)
                        binding.ivCategoryIcon.setColorFilter(color)
                    } catch (e: Exception) {
                        // Fallback to default color
                        binding.ivCategoryIcon.setColorFilter(
                            ContextCompat.getColor(requireContext(), R.color.primary)
                        )
                    }
                } ?: run {
                    // Default category if not found
                    binding.tvCategory.text = "Uncategorized"
                    binding.ivCategoryIcon.setImageResource(R.drawable.ic_category_default)
                    binding.ivCategoryIcon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.text_secondary)
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("TransactionDetails", "Failed to load category info: ${e.message}")
                binding.tvCategory.text = "Uncategorized"
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_category_default)
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
            
            btnAddNotes.setOnClickListener {
                showNotesDialog()
            }
            
            cardNotes.setOnClickListener {
                showNotesDialog()
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
            
            
            btnSave.setOnClickListener {
                saveTransaction()
            }
        }
    }


    private fun showCategorySelectionDialog() {
        transaction?.let { txn ->
            val categoriesDialog = com.koshpal_android.koshpalapp.ui.home.dialog.CategoriesSelectionDialog.newInstance { selectedCategory ->
                // Update the category in the pill shape
                binding.tvCategory.text = selectedCategory.name
                binding.ivCategoryIcon.setImageResource(selectedCategory.icon)
                
                // Set category icon color (pill shape, no background)
                try {
                    val color = Color.parseColor(selectedCategory.color)
                    binding.ivCategoryIcon.setColorFilter(color)
                } catch (e: Exception) {
                    // Fallback to default color
                    binding.ivCategoryIcon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.primary)
                    )
                }
                
                // Update local transaction
                transaction = txn.copy(categoryId = selectedCategory.id)
                
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
            categoriesDialog.show(parentFragmentManager, com.koshpal_android.koshpalapp.ui.home.dialog.CategoriesSelectionDialog.TAG)
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
            
            // Add click listener to open image in full screen
            ivPhotoPreview.setOnClickListener {
                showFullScreenImage(uri)
            }
            
            // Also make the card clickable
            cardPhotoPreview.setOnClickListener {
                showFullScreenImage(uri)
            }
        }
    }
    
    private fun showFullScreenImage(uri: Uri) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_fullscreen_image, null)
        dialog.setContentView(dialogView)
        
        val ivFullScreen = dialogView.findViewById<android.widget.ImageView>(R.id.ivFullScreen)
        val btnCloseFullScreen = dialogView.findViewById<ImageButton>(R.id.btnCloseFullScreen)
        
        ivFullScreen.setImageURI(uri)
        ivFullScreen.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        
        btnCloseFullScreen.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun removePhoto() {
        selectedImageUri = null
        binding.cardPhotoPreview.visibility = View.GONE
        transaction = transaction?.copy(attachmentPath = null)
    }

    private fun showAddTagDialog() {
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
        
        // Setup RecyclerView for existing tags - create adapter first
        var dialogTagsAdapter: TagsAdapter? = null
        dialogTagsAdapter = TagsAdapter(
            tags = currentTags.toMutableList(),
            onTagRemoved = { tag ->
                currentTags.remove(tag)
                // Update all adapters
                val updatedTags = currentTags.toMutableList()
                dialogTagsAdapter?.updateTags(updatedTags)
                tagsAdapter.updateTags(updatedTags)
                inlineTagsAdapter?.updateTags(updatedTags)
                // Show/hide existing tags section
                rvExistingTags.visibility = if (currentTags.isNotEmpty()) View.VISIBLE else View.GONE
                // Update chip states
                chipFriends.isChecked = currentTags.contains("Friends")
                chipOffice.isChecked = currentTags.contains("Office")
                chipFamily.isChecked = currentTags.contains("Family")
                chipSchool.isChecked = currentTags.contains("School")
                chipOnline.isChecked = currentTags.contains("Online")
                // Save tags immediately when removed
                saveTagsToTransaction()
            }
        )
        
        rvExistingTags.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
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
            tagsAdapter.updateTags(currentTags)
            inlineTagsAdapter?.updateTags(currentTags)
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
            // Save tags immediately to transaction
            saveTagsToTransaction()
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
                    tagsAdapter.updateTags(currentTags)
                    rvExistingTags.visibility = View.VISIBLE
                    etCustomTag.text?.clear()
                    // Save tags immediately to transaction
                    saveTagsToTransaction()
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
                tagsAdapter.updateTags(currentTags)
                rvExistingTags.visibility = View.VISIBLE
                etCustomTag.text?.clear()
                // Save tags immediately to transaction
                saveTagsToTransaction()
            }
        }
        
        btnClose.setOnClickListener {
            // Save tags before closing
            saveTagsToTransaction()
            dialog.dismiss()
        }
        
        btnDone.setOnClickListener {
            // Save tags and close dialog
            saveTagsToTransaction()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showNotesDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_notes, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val etNotes = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNotes)
        val btnCloseNotes = dialogView.findViewById<ImageButton>(R.id.btnCloseNotes)
        val btnCancelNotes = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelNotes)
        val btnSaveNotes = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveNotes)
        
        // Set existing notes if any
        val notesText = transaction?.notes ?: ""
        etNotes.setText(notesText)
        if (notesText.isNotEmpty()) {
            etNotes.setSelection(notesText.length) // Move cursor to end
        }
        
        btnCloseNotes.setOnClickListener {
            dialog.dismiss()
        }
        
        btnCancelNotes.setOnClickListener {
            dialog.dismiss()
        }
        
        btnSaveNotes.setOnClickListener {
            val notes = etNotes.text.toString().trim()
            transaction?.let { txn ->
                val updatedTransaction = txn.copy(
                    notes = notes.takeIf { it.isNotEmpty() },
                    updatedAt = System.currentTimeMillis()
                )
                transaction = updatedTransaction
                
                // Save notes to database immediately
                lifecycleScope.launch {
                    try {
                        transactionRepository.updateTransaction(updatedTransaction)
                        android.util.Log.d("TransactionDetailsDialog", "✅ Notes saved to Room DB: ${txn.id} -> ${notes.takeIf { it.isNotEmpty() } ?: "empty"}")
                        Toast.makeText(requireContext(), "Notes saved", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.util.Log.e("TransactionDetailsDialog", "❌ Failed to save notes to Room DB: ${e.message}", e)
                        Toast.makeText(requireContext(), "Failed to save notes", Toast.LENGTH_SHORT).show()
                    }
                }
                
                // Update notes display and button text
                if (notes.isNotEmpty()) {
                    binding.tvNotesDisplay.text = notes
                    binding.tvNotesDisplay.visibility = View.VISIBLE
                    binding.btnAddNotes.text = "Edit notes"
                } else {
                    binding.tvNotesDisplay.visibility = View.GONE
                    binding.btnAddNotes.text = "Tap to add"
                }
            }
            
            dialog.dismiss()
        }
        
        dialog.show()
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

    private fun setupTransactionTypeToggle() {
        transaction?.let { txn ->
            val isExpense = txn.type == com.koshpal_android.koshpalapp.model.TransactionType.DEBIT
            binding.switchExpense.setOnCheckedChangeListener(null)
            binding.switchExpense.isChecked = isExpense
            updateTransactionTypeLabel(isExpense)
            
            binding.switchExpense.setOnCheckedChangeListener { _, isChecked ->
                val newType = if (isChecked) {
                    com.koshpal_android.koshpalapp.model.TransactionType.DEBIT
                } else {
                    com.koshpal_android.koshpalapp.model.TransactionType.CREDIT
                }
                
                // Update transaction type
                transaction = txn.copy(type = newType)
                
                // Update title
                val titleText = when (newType) {
                    com.koshpal_android.koshpalapp.model.TransactionType.DEBIT -> "Debit transaction"
                    com.koshpal_android.koshpalapp.model.TransactionType.CREDIT -> "Credit transaction"
                    else -> "Transaction Details"
                }
                binding.tvTitle.text = titleText
                
                // Update label
                updateTransactionTypeLabel(isChecked)
                
                // Save immediately to database
                lifecycleScope.launch {
                    try {
                        transactionRepository.updateTransaction(transaction!!)
                        android.util.Log.d("TransactionDetailsDialog", "✅ Transaction type updated: ${txn.id} -> $newType")
                        Toast.makeText(requireContext(), "Transaction type updated", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.util.Log.e("TransactionDetailsDialog", "❌ Failed to update transaction type: ${e.message}")
                        // Revert switch
                        binding.switchExpense.setOnCheckedChangeListener(null)
                        binding.switchExpense.isChecked = !isChecked
                        updateTransactionTypeLabel(!isChecked)
                        // Re-attach listener
                        setupTransactionTypeToggle()
                        Toast.makeText(requireContext(), "Failed to update transaction type", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateTransactionTypeLabel(isExpense: Boolean) {
        binding.tvIncomeExpenseLabel.text = if (isExpense) "Expense" else "Income"
        
        // Update amount prefix and label
        transaction?.let { txn ->
            val amountPrefix = if (isExpense) "-" else "+"
            binding.tvAmount.text = "$amountPrefix₹${txn.amount}"
            
            val typeLabel = if (isExpense) "Debited" else "Credited"
            binding.tvTransactionTypeLabel.text = typeLabel
            binding.tvTransactionTypeLabel.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.primary)
            )
        }
    }

    private fun formatDateWithOrdinal(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val ordinal = when {
            day % 10 == 1 && day % 100 != 11 -> "st"
            day % 10 == 2 && day % 100 != 12 -> "nd"
            day % 10 == 3 && day % 100 != 13 -> "rd"
            else -> "th"
        }
        val dateFormat = SimpleDateFormat("EEE, d'$ordinal', h:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    private fun extractUpiReference(smsBody: String?): String {
        if (smsBody == null) return "N/A"
        
        // Try multiple patterns to extract UPI reference number from SMS
        val patterns = listOf(
            "Refno\\s+(\\d+)".toRegex(RegexOption.IGNORE_CASE),
            "Ref\\s+No[.:]?\\s*(\\d+)".toRegex(RegexOption.IGNORE_CASE),
            "Txn\\s+ID[.:]?\\s*(\\d+)".toRegex(RegexOption.IGNORE_CASE),
            "Ref[.:]?\\s*(\\d+)".toRegex(RegexOption.IGNORE_CASE),
            "ID[.:]?\\s*(\\d{12,})".toRegex(RegexOption.IGNORE_CASE) // Long numeric ID
        )
        
        for (pattern in patterns) {
            val match = pattern.find(smsBody)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        return "N/A"
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

    private fun saveTagsToTransaction() {
        transaction?.let { txn ->
            val tagsString = currentTags.joinToString(",").takeIf { it.isNotEmpty() }
            // Also save attachment path if image was selected
            val attachmentPath = selectedImageUri?.toString() ?: txn.attachmentPath
            transaction = txn.copy(
                tags = tagsString,
                attachmentPath = attachmentPath,
                updatedAt = System.currentTimeMillis()
            )
            // Save to database immediately
            lifecycleScope.launch {
                try {
                    transactionRepository.updateTransaction(transaction!!)
                    android.util.Log.d("TransactionDetailsDialog", "✅ Tags and attachment saved to Room DB: ${txn.id} -> tags: $tagsString, attachment: $attachmentPath")
                } catch (e: Exception) {
                    android.util.Log.e("TransactionDetailsDialog", "❌ Failed to save tags to Room DB: ${e.message}", e)
                    Toast.makeText(requireContext(), "Failed to save tags", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveTransaction() {
        val updatedTransaction = transaction?.copy(
            amount = transaction?.amount ?: 0.0, // Amount is displayed but not editable in this design
            notes = transaction?.notes, // Notes are saved via dialog
            tags = currentTags.joinToString(",").takeIf { it.isNotEmpty() },
            attachmentPath = selectedImageUri?.toString() ?: transaction?.attachmentPath,
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
        // Reload transaction from database to get latest notes and tags
        transaction?.let { txn ->
            lifecycleScope.launch {
                try {
                    val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                    val latestTransaction = database.transactionDao().getTransactionById(txn.id)
                    
                    latestTransaction?.let { latest ->
                        transaction = latest
                        android.util.Log.d("TransactionDetailsDialog", "🔄 Reloaded transaction from DB: notes=${latest.notes}, tags=${latest.tags}")
                        
                        // Update notes display
                        if (latest.notes.isNullOrEmpty()) {
                            binding.tvNotesDisplay.visibility = View.GONE
                            binding.btnAddNotes.text = "Tap to add"
                        } else {
                            binding.tvNotesDisplay.text = latest.notes
                            binding.tvNotesDisplay.visibility = View.VISIBLE
                            binding.btnAddNotes.text = "Edit notes"
                        }
                        
                        // Reload tags
                        currentTags.clear()
                        latest.tags?.let { tagsString ->
                            if (tagsString.isNotBlank()) {
                                val parsedTags = tagsString.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                currentTags.addAll(parsedTags)
                                android.util.Log.d("TransactionDetailsDialog", "📋 Reloaded ${currentTags.size} tags: $currentTags")
                            }
                        }
                        updateTagsDisplay()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TransactionDetailsDialog", "❌ Failed to reload transaction: ${e.message}", e)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


