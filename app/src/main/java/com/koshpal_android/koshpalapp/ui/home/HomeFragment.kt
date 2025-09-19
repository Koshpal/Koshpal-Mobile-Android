package com.koshpal_android.koshpalapp.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.FragmentHomeBinding
import com.koshpal_android.koshpalapp.ui.home.adapter.FeatureAdapter
import com.koshpal_android.koshpalapp.ui.home.adapter.RecentTransactionAdapter
import com.koshpal_android.koshpalapp.ui.home.model.FeatureItem
import com.koshpal_android.koshpalapp.ui.home.model.HomeUiState
import com.koshpal_android.koshpalapp.ui.home.model.MonthlySpendingData
import com.koshpal_android.koshpalapp.utils.SMSReader
import com.koshpal_android.koshpalapp.utils.SMSTestHelper
import com.koshpal_android.koshpalapp.utils.DebugHelper
import com.koshpal_android.koshpalapp.utils.SMSManager
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.PaymentSms
import com.koshpal_android.koshpalapp.service.TransactionProcessingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    
    @Inject
    lateinit var smsReader: SMSReader
    
    @Inject
    lateinit var transactionProcessingService: TransactionProcessingService
    
    private lateinit var featureAdapter: FeatureAdapter

    // Permission launcher for SMS permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsPermissionGranted = permissions[Manifest.permission.READ_SMS] == true
        val receivePermissionGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        
        if (smsPermissionGranted && receivePermissionGranted) {
            binding.cardPermissions.visibility = View.GONE
            showManualSmsParsingOption()
            viewModel.onPermissionsGranted()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
        checkPermissions()
        
        // Load initial data
        android.util.Log.d("HomeFragment", "🚀 Loading initial dashboard data...")
        viewModel.loadDashboardData()
        
        // Also manually trigger refresh after a short delay to ensure data is loaded
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000) // Wait 2 seconds
            android.util.Log.d("HomeFragment", "🔄 Manual refresh after delay...")
            viewModel.refreshData()
        }
    }

    private fun setupRecyclerViews() {
        // RecyclerViews removed for new Figma design
        // Features are now integrated into the main layout
    }

    private fun setupClickListeners() {
        binding.apply {
            // Import button click
            btnImport.setOnClickListener {
                createSampleTransactions()
            }
            
            // Add budget button click
            btnAddBudget.setOnClickListener {
                // Switch to Budget tab in bottom navigation
                (activity as? HomeActivity)?.switchToBudgetTab()
            }
            
            tvViewAllTransactions.setOnClickListener {
                // Navigate to transactions screen
                (activity as? HomeActivity)?.showTransactionsFragment()
            }
            
            btnEnablePermissions.setOnClickListener {
                requestSmsPermissions()
            }
            
            btnParseSms.setOnClickListener {
                createSampleTransactions()
            }
            
            // Add test button for immediate data (temporary)
            cardFinancialOverview.setOnLongClickListener {
                createSampleTransactions()
                true
            }
            
            cardFinancialOverview.setOnClickListener {
                // Show current transaction count and hint
                lifecycleScope.launch {
                    try {
                        val database = KoshpalDatabase.getDatabase(requireContext())
                        val transactionDao = database.transactionDao()
                        val allTransactions = transactionDao.getRecentTransactions(100)
                        
                        showMessage("📊 CURRENT STATUS:\n\n💳 Total Transactions: ${allTransactions.size}\n\n🔍 DEBUG MODE: Long press this card to create sample transactions!\n\nThis will:\n✅ Create 8 sample transactions\n✅ Show detailed results\n✅ Enable all app features")
                    } catch (e: Exception) {
                        showMessage("🔍 DEBUG MODE: Long press this card to create sample transactions!\n\nError checking current data: ${e.message}")
                    }
                }
            }
            
            // Month selector click listener
            layoutMonthSelector.setOnClickListener {
                showMonthSelectionDialog()
            }
        }
    }

    private fun observeViewModel() {
        android.util.Log.d("HomeFragment", "🔍 Setting up ViewModel observation...")
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                android.util.Log.d("HomeFragment", "🔍 Starting to collect uiState...")
                viewModel.uiState.collect { state ->
                    android.util.Log.d("HomeFragment", "🔍 Received new state in collect block")
                    updateUI(state)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentTransactions.collect { transactions ->
                    // Update the "View All" text to show transaction count
                    binding.tvViewAllTransactions.text = if (transactions.isEmpty()) {
                        "View All (0)"
                    } else {
                        "View All (${transactions.size})"
                    }
                    
                    // Show/hide the no transactions card based on transaction count
                    binding.cardNoTransactions.visibility = if (transactions.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }
    }

    private fun updateUI(state: HomeUiState) {
        android.util.Log.d("HomeFragment", "🎯 updateUI called with state:")
        android.util.Log.d("HomeFragment", "   hasTransactions: ${state.hasTransactions}")
        android.util.Log.d("HomeFragment", "   totalIncome: ₹${state.totalIncome}")
        android.util.Log.d("HomeFragment", "   totalExpenses: ₹${state.totalExpenses}")
        android.util.Log.d("HomeFragment", "   transactionCount: ${state.transactionCount}")
        
        binding.apply {
            tvUserName.text = state.userName
            
            // Show real balance data or prompt to import SMS
            if (state.hasTransactions) {
                // Show selected month data prominently
                val currentBalance = state.currentMonthBalance
                
                tvCurrentBalance.text = "₹${String.format("%.0f", currentBalance)}"
                tvTotalBalance.text = "₹${String.format("%.0f", state.totalBalance)}"
                
                // Show current month data prominently
                tvTotalIncome.text = "₹${String.format("%.0f", state.currentMonthIncome)}"
                tvTotalExpenses.text = "₹${String.format("%.0f", state.currentMonthExpenses)}"
                
                // Update the month display to selected month
                val selectedDate = java.util.Calendar.getInstance()
                selectedDate.set(state.selectedYear, state.selectedMonth, 1)
                val monthFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
                tvCurrentMonth.text = "${monthFormat.format(selectedDate.time)} ${state.selectedYear}"
                
                // Debug logging
                android.util.Log.d("HomeFragment", "📊 UI UPDATED - Income: ₹${state.totalIncome}, Expenses: ₹${state.totalExpenses}, Balance: ₹${currentBalance}")
                android.util.Log.d("HomeFragment", "📊 UI Elements Set:")
                android.util.Log.d("HomeFragment", "   tvTotalIncome.text = ${tvTotalIncome.text}")
                android.util.Log.d("HomeFragment", "   tvTotalExpenses.text = ${tvTotalExpenses.text}")
                android.util.Log.d("HomeFragment", "   tvCurrentBalance.text = ${tvCurrentBalance.text}")
                android.util.Log.d("HomeFragment", "📊 Has transactions: ${state.hasTransactions}, Transaction count: ${state.transactionCount}")
                
                // Update budget progress
                val budgetProgress = if (state.budgetLimit > 0) {
                    (state.budgetSpent / state.budgetLimit).toFloat()
                } else 0f
                progressBudget.progress = (budgetProgress * 100).toInt()
                tvBudgetSpent.text = "₹${String.format("%.2f", state.budgetSpent)} spent"
                
                // Hide no transactions card, show transaction data
                cardNoTransactions.visibility = View.GONE
                cardSmsParser.visibility = View.GONE
                
                // Update monthly spending card with real data
                updateMonthlySpendingCard(state.last3MonthsData)
                
            } else {
                // First time user - show import SMS prompt
                tvCurrentBalance.text = "₹0.00"
                tvTotalBalance.text = "₹0.00"
                tvTotalIncome.text = "₹0.00"
                tvTotalExpenses.text = "₹0.00"
                tvBudgetSpent.text = "₹0.00 spent"
                progressBudget.progress = 0
                
                // Show SMS import card
                cardSmsParser.visibility = View.VISIBLE
                cardNoTransactions.visibility = View.VISIBLE
            }
            
            // Show error if any
            state.errorMessage?.let { error ->
                showMessage(error)
            }
        }
    }
    
    private fun updateMonthlySpendingCard(monthlyData: List<MonthlySpendingData>) {
        if (monthlyData.isNotEmpty()) {
            val currentMonth = monthlyData.lastOrNull()
            currentMonth?.let { month ->
                binding.apply {
                    // Update the current month display
                    tvCurrentMonth.text = "${month.month} ${month.year}"
                    
                    // Make the financial overview card clickable to show detailed view
                    cardFinancialOverview.setOnClickListener {
                        showMonthlySpendingDetails(monthlyData)
                    }
                }
            }
        }
    }
    
    private fun showMonthlySpendingDetails(monthlyData: List<MonthlySpendingData>) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Last 3 Months Spending")
            .setMessage(buildMonthlyDataMessage(monthlyData))
            .setPositiveButton("OK", null)
            .create()
        dialog.show()
    }
    
    private fun buildMonthlyDataMessage(monthlyData: List<MonthlySpendingData>): String {
        return monthlyData.joinToString("\n\n") { month ->
            "${month.month} ${month.year}:\n" +
            "Income: ₹${String.format("%.2f", month.totalIncome)}\n" +
            "Expenses: ₹${String.format("%.2f", month.totalSpent)}\n" +
            "Transactions: ${month.transactionCount}"
        }
    }

    private fun getFeaturesList(): List<FeatureItem> {
        return listOf(
            FeatureItem(
                id = "dashboard",
                title = "Dashboard",
                description = "Monthly insights & charts",
                icon = R.drawable.ic_summary,
                color = R.color.primary
            ),
            FeatureItem(
                id = "budget",
                title = "Budget Planner",
                description = "Set spending limits",
                icon = R.drawable.ic_budget_empty,
                color = R.color.success
            ),
            FeatureItem(
                id = "savings",
                title = "Savings Goals",
                description = "Track your goals",
                icon = R.drawable.ic_savings,
                color = R.color.warning
            ),
            FeatureItem(
                id = "insights",
                title = "Financial Insights",
                description = "Smart recommendations",
                icon = R.drawable.ic_lightbulb,
                color = R.color.primary_dark
            ),
            FeatureItem(
                id = "categorization",
                title = "Categorize",
                description = "Organize transactions",
                icon = R.drawable.ic_category_default,
                color = R.color.secondary
            ),
            FeatureItem(
                id = "alerts",
                title = "Spending Alerts",
                description = "Budget notifications",
                icon = R.drawable.ic_warning,
                color = R.color.error
            )
        )
    }

    private fun navigateToFeature(feature: FeatureItem) {
        when (feature.id) {
            "dashboard" -> (activity as? HomeActivity)?.switchToDashboardTab()
            "budget" -> (activity as? HomeActivity)?.switchToBudgetTab()
            "savings" -> (activity as? HomeActivity)?.switchToSavingsTab()
            "insights" -> showMessage("Financial Insights feature coming soon!")
            "categorization" -> viewModel.onCategorizationClick()
            "alerts" -> showMessage("Spending Alerts feature coming soon!")
        }
    }

    private fun checkPermissions() {
        val smsPermission = ContextCompat.checkSelfPermission(
            requireContext(), 
            Manifest.permission.READ_SMS
        )
        val receivePermission = ContextCompat.checkSelfPermission(
            requireContext(), 
            Manifest.permission.RECEIVE_SMS
        )
        
        if (smsPermission != PackageManager.PERMISSION_GRANTED || 
            receivePermission != PackageManager.PERMISSION_GRANTED) {
            binding.cardPermissions.visibility = View.VISIBLE
        } else {
            binding.cardPermissions.visibility = View.GONE
            // Show manual SMS parsing option
            showManualSmsParsingOption()
        }
    }
    
    private fun showManualSmsParsingOption() {
        // Only show if no transactions exist yet
        viewLifecycleOwner.lifecycleScope.launch {
            val transactionCount = viewModel.getTransactionCount()
            // Check if binding is still available before accessing it
            _binding?.let { binding ->
                if (transactionCount == 0) {
                    binding.cardSmsParser.visibility = View.VISIBLE
                } else {
                    binding.cardSmsParser.visibility = View.GONE
                }
            }
        }
    }

    private fun requestSmsPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS
            )
        )
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("SMS permissions are required for automatic transaction tracking. You can enable them in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    private fun showErrorMessage(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showMessage(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Info")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun createSampleTransactions() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("HomeFragment", "🚀 Starting sample transaction creation...")
                showMessage("🔄 Creating sample transactions... Please wait...")
                
                val smsManager = SMSManager(requireContext())
                
                // Try real SMS first, then fallback to sample data
                val realResult = smsManager.processAllSMS()
                
                if (realResult.success && realResult.transactionsCreated > 0) {
                    // Real SMS processing worked
                    val message = buildString {
                        append("✅ REAL SMS PROCESSED!\n\n")
                        append("📊 DETAILED RESULTS:\n")
                        append("📱 Total SMS Found: ${realResult.smsFound}\n")
                        append("💳 Transaction SMS: ${realResult.transactionSmsFound}\n")
                        append("💾 SMS Saved: ${realResult.smsProcessed}\n")
                        append("✅ Transactions Created: ${realResult.transactionsCreated}\n\n")
                        append("🎉 Your actual financial data is now available!\n")
                        append("Check Dashboard, Budget, and other tabs to see your data.")
                    }
                    showMessage(message)
                    android.util.Log.d("HomeFragment", "✅ Real SMS processing successful: ${realResult.transactionsCreated} transactions")
                } else {
                    // Fallback to sample data
                    android.util.Log.d("HomeFragment", "📝 Falling back to sample data creation...")
                    val sampleResult = smsManager.createSampleData()
                    
                    if (sampleResult.success) {
                        val message = "✅ SAMPLE DATA CREATED!\n\n🧪 Sample Transactions: ${sampleResult.transactionsCreated}\n\nIncludes:\n• Amazon ₹500 (Shopping)\n• Zomato ₹1,200 (Food)\n• Salary ₹25,000 (Income)\n• Uber ₹350 (Transport)\n• DMart ₹800 (Grocery)\n\nCheck Dashboard, Budget, and other tabs!"
                        showMessage(message)
                        android.util.Log.d("HomeFragment", "✅ Sample data creation successful: ${sampleResult.transactionsCreated} transactions")
                    } else {
                        val errorMsg = "Failed to create data: ${sampleResult.error}"
                        showErrorMessage(errorMsg)
                        android.util.Log.e("HomeFragment", "❌ Sample data creation failed: ${sampleResult.error}")
                    }
                }
                
                // Force refresh UI after data creation
                android.util.Log.d("HomeFragment", "🔄 Refreshing UI data...")
                viewModel.refreshData()
                
                // Small delay to ensure data is loaded
                kotlinx.coroutines.delay(1000)
                viewModel.refreshData()
                
            } catch (e: Exception) {
                val errorMsg = "Error: ${e.message}"
                showErrorMessage(errorMsg)
                android.util.Log.e("HomeFragment", "❌ Error creating transactions: ${e.message}", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        viewModel.refreshData()
    }

    private fun showMonthSelectionDialog() {
        val currentState = viewModel.uiState.value
        val availableMonths = currentState.availableMonths
        
        if (availableMonths.isEmpty()) {
            Toast.makeText(requireContext(), "No transaction data available for month selection", Toast.LENGTH_SHORT).show()
            return
        }
        
        val monthNames = availableMonths.map { it.displayName }.toTypedArray()
        val currentSelection = availableMonths.indexOfFirst { 
            it.month == currentState.selectedMonth && it.year == currentState.selectedYear 
        }.takeIf { it >= 0 } ?: 0
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("📅 Select Month")
            .setSingleChoiceItems(monthNames, currentSelection) { dialog, which ->
                val selectedOption = availableMonths[which]
                android.util.Log.d("HomeFragment", "📅 User selected: ${selectedOption.displayName}")
                viewModel.selectMonth(selectedOption.month, selectedOption.year)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}