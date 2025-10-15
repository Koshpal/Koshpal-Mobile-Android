package com.koshpal_android.koshpalapp.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import java.util.Calendar
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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.FragmentHomeBinding
import com.koshpal_android.koshpalapp.ui.home.adapter.FeatureAdapter
import com.koshpal_android.koshpalapp.ui.home.adapter.RecentTransactionAdapter
import com.koshpal_android.koshpalapp.ui.home.adapter.BankCardAdapter
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionCategorizationDialog
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionDetailsDialog
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.ui.home.model.FeatureItem
import com.koshpal_android.koshpalapp.ui.home.model.HomeUiState
import com.koshpal_android.koshpalapp.ui.home.model.MonthlySpendingData
import com.koshpal_android.koshpalapp.utils.SMSReader
import com.koshpal_android.koshpalapp.utils.SMSTestHelper
import com.koshpal_android.koshpalapp.utils.DebugHelper
import com.koshpal_android.koshpalapp.utils.SMSManager
import com.koshpal_android.koshpalapp.utils.DebugDataManager
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.PaymentSms
import com.koshpal_android.koshpalapp.service.TransactionProcessingService
import dagger.hilt.android.AndroidEntryPoint
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
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

    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    private lateinit var recentTransactionsAdapter: RecentTransactionAdapter
    private lateinit var bankCardAdapter: BankCardAdapter
    
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

        // Set status bar color to primary blue
        setStatusBarColor()

        setupRecyclerViews()
        setupClickListeners()
        setupRecentTransactionsRecyclerView()

        // REMOVED: Reset logic that was interfering with automatic categorization
        // Transactions are now correctly categorized during SMS parsing and don't need reset
        android.util.Log.d("HomeFragment", "✅ onViewCreated - skipping reset logic (transactions auto-categorize correctly)")

        // FIXED: Use single data source - ViewModel only
        android.util.Log.d("HomeFragment", "🚀 Setting up ViewModel observation...")
        observeViewModel()

        val greetingText = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning ☀️"
            in 12..16 -> "Good Afternoon 🌤️"
            else -> "Good Evening 🌙"
        }
        binding.tvGreeting.text = greetingText

    }

    private fun setStatusBarColor() {
        activity?.window?.let { window ->
            // Set status bar color to primary blue
            window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.primary)
            
            // Make status bar icons white (for dark background)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = 0 // Clear light status bar flag for white icons
            }
            
            android.util.Log.d("HomeFragment", "🎨 Status bar color set to primary blue")
        }
    }

    private fun setupRecyclerViews() {
        // RecyclerViews removed for new Figma design
        // Features are now integrated into the main layout
    }

    private fun setupRecentTransactionsRecyclerView() {
        recentTransactionsAdapter = RecentTransactionAdapter { transaction ->
            // Handle transaction click - show full transaction details dialog (like TransactionsFragment)
            android.util.Log.d("HomeFragment", "📱 Transaction clicked: ${transaction.merchant}")
            // Show transaction details dialog when transaction is clicked
            showTransactionDetailsDialog(transaction)
        }

        binding.rvRecentTransactions.apply {
            adapter = recentTransactionsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }

        android.util.Log.d("HomeFragment", "📱 Recent transactions RecyclerView setup complete")
        
        // Setup bank cards
        setupBankCards()
    }

    private fun setupBankCards() {
        bankCardAdapter = BankCardAdapter(
            onAddCashClick = {
                // Handle add cash button click
                showAddCashDialog()
            },
            onBankCardClick = { bankName ->
                // Handle bank card click - navigate to bank transactions
                (activity as? HomeActivity)?.showBankTransactionsFragment(bankName)
            }
        )

        binding.rvBankCards.apply {
            adapter = bankCardAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        // Load bank spending data
        loadBankSpending()
    }

    private fun loadBankSpending() {
        lifecycleScope.launch {
            try {
                // Get ONLY real SMS transaction data for current month
                val bankSpending = transactionRepository.getBankWiseSpending().toMutableList()
                
                android.util.Log.d("HomeFragment", "💳 Loaded ${bankSpending.size} bank cards with REAL data")
                
                // Log each bank's spending
                bankSpending.forEach { bank ->
                    android.util.Log.d("HomeFragment", "💳 ${bank.bankName}: ₹${bank.totalSpending} (${bank.transactionCount} transactions)")
                }
                
                // Add Cash card at the end (always show for manual entry)
                bankSpending.add(
                    com.koshpal_android.koshpalapp.model.BankSpending(
                        bankName = "Cash",
                        totalSpending = 0.0,
                        transactionCount = 0,
                        isCash = true
                    )
                )
                
                bankCardAdapter.submitList(bankSpending)
                
                // Hide section if no banks found
                if (bankSpending.size == 1) { // Only Cash card
                    android.util.Log.d("HomeFragment", "⚠️ No bank transactions found for current month")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "❌ Failed to load bank spending: ${e.message}", e)
            }
        }
    }

    private fun showAddCashDialog() {
        // TODO: Implement add cash transaction dialog
        Toast.makeText(requireContext(), "Add Cash Transaction - Coming Soon!", Toast.LENGTH_SHORT).show()
    }

    private fun setupClickListeners() {
        binding.apply {
            // Real SMS parsing button
            cardSmsParser.setOnClickListener {
                android.util.Log.d(
                    "HomeFragment",
                    "📱 SMS Parser card clicked - starting real SMS parsing"
                )
                createSampleTransactions()
            }

            // Import button click - CLEAR DATA FIRST then parse real SMS
            btnImport.setOnClickListener {
                clearAllDataAndParseRealSMS()
            }


            tvViewAllTransactions.setOnClickListener {
                // Navigate to transactions screen
                (activity as? HomeActivity)?.showTransactionsFragment()
            }

            // Add Transaction button
            btnAddTransaction.setOnClickListener {
                showAddTransactionDialog()
            }

            // Reminders button
            btnReminders.setOnClickListener {
                android.util.Log.d("HomeFragment", "🔔 Reminders button clicked")
                (activity as? HomeActivity)?.showRemindersListFragment()
            }

            btnEnablePermissions.setOnClickListener {
                requestSmsPermissions()
            }

            btnParseSms.setOnClickListener {
                createSampleTransactions()
            }
            
            btnTestBudget.setOnClickListener {
                testBudgetNotifications()
            }
            
            btnCheckBudget.setOnClickListener {
                checkBudgetNow()
            }
            
            btnTestNotification.setOnClickListener {
                testBudgetNotification()
            }
            
            btnTestAllThresholds.setOnClickListener {
                testAllThresholdsAndCategories()
            }

            // LONG PRESS financial card to trigger auto-categorization + show debug info
            layoutFinancialOverview.setOnLongClickListener {
                android.util.Log.d("HomeFragment", "🤖 Long press detected - showing transaction debug info")
                lifecycleScope.launch {
                    try {
                        // First show what transactions exist
                        val allTransactions = transactionRepository.getAllTransactionsOnce()
                        android.util.Log.d("HomeFragment", "📊 ===== TRANSACTION DEBUG =====")
                        android.util.Log.d("HomeFragment", "📊 Total transactions: ${allTransactions.size}")
                        
                        allTransactions.take(10).forEach { txn ->
                            android.util.Log.d("HomeFragment", "💳 Merchant: '${txn.merchant}' | Category: ${txn.categoryId} | Amount: ₹${txn.amount}")
                        }
                        
                        // Now auto-categorize
                        val count = transactionRepository.autoCategorizeExistingTransactions()
                        
                        Toast.makeText(
                            requireContext(),
                            "🤖 Auto-categorized $count transactions\nCheck logcat for details",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        viewModel.refreshData()
                        loadBankSpending()
                        
                        // Refresh categories fragment data
                        (activity as? HomeActivity)?.refreshCategoriesData()
                    } catch (e: Exception) {
                        android.util.Log.e("HomeFragment", "❌ Error: ${e.message}", e)
                        Toast.makeText(
                            requireContext(),
                            "❌ Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                true
            }

            layoutFinancialOverview.setOnClickListener {
                // Perform comprehensive debugging check
                lifecycleScope.launch {
                    try {
                        val debugManager = DebugDataManager(requireContext())
                        val checkResult = debugManager.performCompleteDataCheck()
                        val homeDebugResult = debugManager.debugHomeScreenData()

                        val message = buildString {
                            append("🔍 COMPREHENSIVE DEBUG STATUS:\n\n")
                            append("📊 DATABASE STATUS:\n")
                            append("Connected: ${checkResult.databaseConnected}\n")
                            append("Categories: ${checkResult.categoriesCount}\n")
                            append("Transactions: ${checkResult.transactionsCount}\n")
                            append("Orphaned: ${checkResult.orphanedTransactions}\n\n")
                            append("💰 FINANCIAL DATA:\n")
                            append(
                                "Income: ₹${
                                    String.format(
                                        "%.2f",
                                        homeDebugResult.totalIncome
                                    )
                                }\n"
                            )
                            append(
                                "Expenses: ₹${
                                    String.format(
                                        "%.2f",
                                        homeDebugResult.totalExpenses
                                    )
                                }\n"
                            )
                            append(
                                "Balance: ₹${
                                    String.format(
                                        "%.2f",
                                        homeDebugResult.balance
                                    )
                                }\n\n"
                            )
                            append("🔍 ACTIONS:\n")
                            append("• Long press: Create sample data\n")
                            append("• Import button: Process SMS/Create data")
                        }

                        showMessage(message)
                    } catch (e: Exception) {
                        showMessage("❌ Debug check failed: ${e.message}")
                    }
                }
            }

            // Month selector removed - no longer in layout
            // layoutMonthSelector.setOnClickListener {
            //     showMonthSelectionDialog()
            // }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                android.util.Log.d(
                    "HomeFragment",
                    "🔄 UI State received: hasTransactions=${uiState.hasTransactions}, balance=₹${uiState.currentBalance}"
                )
                updateUI(uiState)
                updateCurrentMonthDisplay()

            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentTransactions.collect { transactions ->
                    android.util.Log.d(
                        "HomeFragment",
                        "📱 Recent transactions received: ${transactions.size}"
                    )

                    // Update the "View All" text to show transaction count
                    // Note: tvViewAllTransactions is now a LinearLayout, text updates handled in layout

                    // Show recent transactions (limit to 4)
                    val recentTransactions = transactions.take(4)
                    recentTransactionsAdapter.submitList(recentTransactions)

                    // Show/hide recent transactions card and no transactions card
                    if (transactions.isNotEmpty()) {
                        binding.cardRecentTransactions.visibility = View.VISIBLE
                        binding.cardNoTransactions.visibility = View.GONE
                    } else {
                        binding.cardRecentTransactions.visibility = View.GONE
                        binding.cardNoTransactions.visibility = View.VISIBLE
                    }

                    android.util.Log.d(
                        "HomeFragment",
                        "📱 Displaying ${recentTransactions.size} recent transactions"
                    )
                }
            }
        }
    }



    private fun updateCurrentMonthDisplay() {
        val calendar = java.util.Calendar.getInstance()
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val currentMonth = monthNames[calendar.get(java.util.Calendar.MONTH)]
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        // tvCurrentMonth removed from layout - no longer displaying month selector
        // _binding?.tvCurrentMonth?.text = "$currentMonth $currentYear"
        android.util.Log.d("HomeFragment", "📅 Month display removed from new design")
    }


    private fun updateUI(state: HomeUiState) {
        android.util.Log.d("HomeFragment", "🎯 updateUI called with state:")
        android.util.Log.d("HomeFragment", "   hasTransactions: ${state.hasTransactions}")
        android.util.Log.d("HomeFragment", "   totalIncome: ₹${state.totalIncome}")
        android.util.Log.d("HomeFragment", "   totalExpenses: ₹${state.totalExpenses}")
        android.util.Log.d("HomeFragment", "   transactionCount: ${state.transactionCount}")

        binding.apply {
            // User name removed from UI - using logo with headline instead

            // Show real balance data or prompt to import SMS
            if (state.hasTransactions) {
                // Show current month data prominently (removed balance display)
                tvTotalIncome.text = "₹${String.format("%.0f", state.currentMonthIncome)}"
                tvTotalExpenses.text = "₹${String.format("%.0f", state.currentMonthExpenses)}"

                // Month display removed from new design
                // val selectedDate = java.util.Calendar.getInstance()
                // selectedDate.set(state.selectedYear, state.selectedMonth, 1)
                // val monthFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
                // tvCurrentMonth.text = "${monthFormat.format(selectedDate.time)} ${state.selectedYear}"

                // Debug logging
                android.util.Log.d(
                    "HomeFragment",
                    "📊 UI UPDATED - Month Income: ₹${state.currentMonthIncome}, Month Expenses: ₹${state.currentMonthExpenses}"
                )
                android.util.Log.d("HomeFragment", "📊 UI Elements Set:")
                android.util.Log.d("HomeFragment", "   tvTotalIncome.text = ${tvTotalIncome.text}")
                android.util.Log.d(
                    "HomeFragment",
                    "   tvTotalExpenses.text = ${tvTotalExpenses.text}"
                )
                android.util.Log.d(
                    "HomeFragment",
                    "📊 Has transactions: ${state.hasTransactions}, Transaction count: ${state.transactionCount}"
                )


                // Hide no transactions card, show transaction data
                cardNoTransactions.visibility = View.GONE
                cardSmsParser.visibility = View.GONE

                // Update monthly spending card with real data
                updateMonthlySpendingCard(state.last3MonthsData)

            } else {
                // First time user - show import SMS prompt (removed balance display)
                tvTotalIncome.text = "₹0.00"
                tvTotalExpenses.text = "₹0.00"

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
                    // Month display removed from new design
                    // tvCurrentMonth.text = "${month.month} ${month.year}"

                    // Make the financial overview card clickable to show detailed view
                    layoutFinancialOverview.setOnClickListener {
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
                color = R.color.primary_darkest
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
            "transactions" -> (activity as? HomeActivity)?.showTransactionsFragment()
            else -> showMessage("Feature coming soon!")
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
            receivePermission != PackageManager.PERMISSION_GRANTED
        ) {
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
                android.util.Log.d("HomeFragment", "📱 ===== STARTING REAL SMS PARSING =====")

                val debugManager = DebugDataManager(requireContext())

                // Step 1: Perform complete data check
                android.util.Log.d("HomeFragment", "🔍 Step 1: Performing complete data check...")
                val checkResult = debugManager.performCompleteDataCheck()

                if (!checkResult.success) {
                    showErrorMessage("Data check failed: ${checkResult.error}")
                    return@launch
                }

                // Step 2: Parse real SMS data
                android.util.Log.d("HomeFragment", "📱 Step 2: Parsing real SMS messages...")
                showMessage("📱 Processing your SMS messages...\nThis may take a few moments.")

                val createResult = debugManager.parseRealSMSAndCreateData()

                if (!createResult.success) {
                    showErrorMessage("Failed to parse SMS data: ${createResult.error}")
                    return@launch
                }

                // Step 3: Debug home screen data
                android.util.Log.d("HomeFragment", "🏠 Step 3: Debugging home screen data...")
                val homeDebugResult = debugManager.debugHomeScreenData()

                // Step 4: Show comprehensive results
                val message = buildString {
                    if (createResult.transactionsCreated > 0) {
                        append("✅ REAL SMS PARSING SUCCESSFUL!\n\n")
                        append("📱 SMS PROCESSING RESULTS:\n")
                        append("💾 Real Transactions Created: ${createResult.transactionsCreated}\n")
                        append("💳 Total Transaction Count: ${createResult.finalTransactionCount}\n")
                        append(
                            "💰 Total Income: ₹${
                                String.format(
                                    "%.2f",
                                    homeDebugResult.totalIncome
                                )
                            }\n"
                        )
                        append(
                            "💸 Total Expenses: ₹${
                                String.format(
                                    "%.2f",
                                    homeDebugResult.totalExpenses
                                )
                            }\n"
                        )
                        append(
                            "🏦 Current Balance: ₹${
                                String.format(
                                    "%.2f",
                                    homeDebugResult.balance
                                )
                            }\n\n"
                        )
                        append("🎉 Your actual financial data is now displayed!\n")
                        append("📅 Home screen shows CURRENT MONTH data only.\n")
                        append("📱 Recent 5 transactions are shown below.\n")
                        append("Check Transactions tab to see your complete history.")
                    } else {
                        append("📝 SAMPLE DATA CREATED\n\n")
                        append("No transaction SMS found on your device.\n")
                        append("Sample data has been created for demonstration.\n\n")
                        append(
                            "💰 Sample Balance: ₹${
                                String.format(
                                    "%.2f",
                                    homeDebugResult.balance
                                )
                            }\n"
                        )
                        append("You can now explore all app features!")
                    }
                }

                showMessage(message)
                android.util.Log.d(
                    "HomeFragment", "✅ ===== SMS PARSING COMPLETED SUCCESSFULLY =====\n" +
                            "Created: ${createResult.transactionsCreated} transactions\n" +
                            "Balance: ₹${homeDebugResult.balance}"
                )

                // Step 5: Force refresh UI with new data
                android.util.Log.d("HomeFragment", "🔄 Step 5: Force refreshing UI with new data...")
                refreshUIWithDebugData(homeDebugResult)

            } catch (e: Exception) {
                val errorMsg = "SMS parsing failed: ${e.message}"
                showErrorMessage(errorMsg)
                android.util.Log.e(
                    "HomeFragment",
                    "❌ ===== SMS PARSING FAILED =====\n${e.message}",
                    e
                )
            }
        }
    }

    private fun refreshUIWithDebugData(debugResult: com.koshpal_android.koshpalapp.utils.HomeScreenDebugResult) {
        android.util.Log.d("HomeFragment", "🔄 Refreshing UI with debug data...")

        _binding?.let { binding ->
            binding.apply {
                // Do not overwrite ViewModel-driven month figures here.
                // Let HomeViewModel compute and bind current month income/expenses/balance.

                // Update transaction count
                // Note: tvViewAllTransactions is now a LinearLayout, text updates handled in layout

                // Show/hide appropriate cards
                if (debugResult.hasTransactions) {
                    cardRecentTransactions.visibility = View.VISIBLE
                    cardNoTransactions.visibility = View.GONE
                    cardSmsParser.visibility = View.GONE
                    layoutFinancialOverview.visibility = View.VISIBLE
                } else {
                    cardRecentTransactions.visibility = View.GONE
                    cardNoTransactions.visibility = View.VISIBLE
                    cardSmsParser.visibility = View.VISIBLE
                    layoutFinancialOverview.visibility = View.VISIBLE
                }

                android.util.Log.d(
                    "HomeFragment",
                    "✅ UI refreshed with debug data (counts/visibility only) - Transactions: ${debugResult.transactionCount}"
                )
            }
        }

        // Also refresh ViewModel to keep it in sync
        viewModel.forceRefreshNow()
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("HomeFragment", "🔄 onResume - performing comprehensive data refresh...")

        // Perform comprehensive data check and refresh
        lifecycleScope.launch {
            try {
                val debugManager = DebugDataManager(requireContext())
                val homeDebugResult = debugManager.debugHomeScreenData()

                if (homeDebugResult.success) {
                    refreshUIWithDebugData(homeDebugResult)
                } else {
                    android.util.Log.e(
                        "HomeFragment",
                        "Home screen debug failed: ${homeDebugResult.error}"
                    )
                    // Fallback to ViewModel refresh
                    viewModel.refreshData()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "onResume data refresh failed: ${e.message}", e)
                // Fallback to ViewModel refresh
                viewModel.refreshData()
            }
        }
    }

    private fun showMonthSelectionDialog() {
        val currentState = viewModel.uiState.value
        val availableMonths = currentState.availableMonths

        if (availableMonths.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No transaction data available for month selection",
                Toast.LENGTH_SHORT
            ).show()
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

    private fun navigateToBudget() {
        Toast.makeText(requireContext(), "Budget feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private var isUpdatingTransaction = false
    
    private fun showTransactionCategorizationDialog(transaction: com.koshpal_android.koshpalapp.model.Transaction) {
        val dialog = TransactionCategorizationDialog.newInstance(transaction) { txn, category ->
            // Prevent multiple simultaneous updates
            if (isUpdatingTransaction) {
                android.util.Log.w("HomeFragment", "⚠️ Transaction update already in progress, ignoring...")
                return@newInstance
            }
            
            // Update transaction with selected category
            lifecycleScope.launch {
                isUpdatingTransaction = true
                try {
                    transactionRepository.updateTransactionCategory(txn.id, category.id)
                    android.util.Log.d(
                        "HomeFragment",
                        "Transaction ${txn.id} categorized as ${category.name}"
                    )
                    
                    // Refresh data to show updated transaction
                    viewModel.refreshData()
                    
                    // ✅ FIX: Refresh Categories fragment so categorized transactions appear there
                    (activity as? HomeActivity)?.refreshCategoriesData()
                    android.util.Log.d("HomeFragment", "🔄 Categories fragment refresh triggered")
                    
                    // Show success message
                    Toast.makeText(
                        requireContext(),
                        "Transaction categorized as ${category.name}",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (e: Exception) {
                    android.util.Log.e(
                        "HomeFragment",
                        "Failed to categorize transaction: ${e.message}"
                    )
                    Toast.makeText(
                        requireContext(),
                        "Failed to categorize transaction",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    // Always reset the flag
                    isUpdatingTransaction = false
                }
            }
        }

        dialog.show(parentFragmentManager, "TransactionCategorizationDialog")
    }

    private fun showTransactionDetailsDialog(transaction: com.koshpal_android.koshpalapp.model.Transaction) {
        val dialog = TransactionDetailsDialog.newInstance(transaction) { updatedTransaction ->
            // Reload data to show updated transaction
            android.util.Log.d("HomeFragment", "🔄 Refreshing data after transaction update...")
            viewModel.refreshData()
            loadBankSpending()
        }
        
        dialog.show(parentFragmentManager, "TransactionDetailsDialog")
    }

    private fun checkBudgetNow() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("HomeFragment", "💰 ===== MANUAL BUDGET CHECK =====")
                
                // First, let's check what's in the database directly
                val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                val budgetDao = database.budgetNewDao()
                val budgetCategoryDao = database.budgetCategoryNewDao()
                
                val budget = budgetDao.getSingleBudget()
                android.util.Log.d("HomeFragment", "🔍 Direct DB check - Budget: $budget")
                
                if (budget != null) {
                    val categories = budgetCategoryDao.getCategoriesForBudget(budget.id)
                    android.util.Log.d("HomeFragment", "🔍 Direct DB check - Categories: ${categories.size}")
                    categories.forEach { category ->
                        android.util.Log.d("HomeFragment", "   - ${category.name}: ₹${category.allocatedAmount}")
                    }
                } else {
                    android.util.Log.d("HomeFragment", "🔍 Direct DB check - NO BUDGET FOUND!")
                }
                
                // Get budget info first
                val budgetMonitor = com.koshpal_android.koshpalapp.utils.BudgetMonitor.getInstance(requireContext())
                val budgetInfo = budgetMonitor.getBudgetInfo()
                
                // Trigger budget monitoring manually
                transactionRepository.triggerBudgetMonitoring()
                
                val message = buildString {
                    append("💰 BUDGET CHECK COMPLETED:\n\n")
                    append("📊 Current Budget Info:\n")
                    append(budgetInfo)
                    append("\n\n✅ Budget monitoring triggered!\n\n")
                    append("🔍 Checking all budget categories:\n")
                    append("• Calculating current spending\n")
                    append("• Checking 50%, 90%, 100% thresholds\n")
                    append("• Sending notifications if needed\n\n")
                    append("Check your notifications for any budget alerts!")
                }
                
                showMessage(message)
                
            } catch (e: Exception) {
                showErrorMessage("Budget check failed: ${e.message}")
                android.util.Log.e("HomeFragment", "Budget check error: ${e.message}", e)
            }
        }
    }
    
    private fun testBudgetNotification() {
        try {
            android.util.Log.d("HomeFragment", "🧪 ===== TESTING BUDGET NOTIFICATION =====")
            
            val budgetMonitor = com.koshpal_android.koshpalapp.utils.BudgetMonitor.getInstance(requireContext())
            budgetMonitor.sendTestBudgetNotification()
            
            val message = buildString {
                append("🧪 TEST NOTIFICATION SENT:\n\n")
                append("✅ Test budget notification triggered!\n\n")
                append("📱 Check your notification panel\n")
                append("🔔 You should see a test budget alert\n\n")
                append("If you don't see the notification:\n")
                append("• Check notification permissions\n")
                append("• Check if notifications are enabled\n")
                append("• Check notification settings")
            }
            
            showMessage(message)
            
        } catch (e: Exception) {
            showErrorMessage("Test notification failed: ${e.message}")
            android.util.Log.e("HomeFragment", "Test notification error: ${e.message}", e)
        }
    }

    private fun testAllThresholdsAndCategories() {
        try {
            android.util.Log.d("HomeFragment", "🧪 ===== TESTING ALL THRESHOLDS & CATEGORIES =====")
            
            val budgetMonitor = com.koshpal_android.koshpalapp.utils.BudgetMonitor.getInstance(requireContext())
            budgetMonitor.testAllThresholdsAndCategories()
            
            val message = buildString {
                append("🧪 COMPREHENSIVE TEST STARTED:\n\n")
                append("✅ Testing all thresholds & categories!\n\n")
                append("📊 Testing 9 categories × 3 thresholds = 27 notifications\n\n")
                append("🔔 Check your notification panel for:\n")
                append("• 40% warnings (9 notifications)\n")
                append("• 90% alerts (9 notifications)\n")
                append("• 100% exceeded (9 notifications)\n\n")
                append("Categories tested:\n")
                append("• Food & Dining, Grocery, Transportation\n")
                append("• Entertainment, Bills & Utilities, Education\n")
                append("• Healthcare, Shopping, Others")
            }
            
            showMessage(message)
            
        } catch (e: Exception) {
            showErrorMessage("Comprehensive test failed: ${e.message}")
            android.util.Log.e("HomeFragment", "Comprehensive test error: ${e.message}", e)
        }
    }

    private fun testBudgetNotifications() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("HomeFragment", "💰 ===== TESTING BUDGET NOTIFICATIONS =====")
                
                val budgetTester = com.koshpal_android.koshpalapp.utils.BudgetNotificationTester.getInstance(requireContext())
                budgetTester.createTestBudgetScenario()
                
                val message = buildString {
                    append("💰 BUDGET NOTIFICATION TEST:\n\n")
                    append("✅ Test budget scenario created!\n\n")
                    append("📊 Test Data:\n")
                    append("• Grocery: ₹3,000 budget\n")
                    append("• Entertainment: ₹2,000 budget\n")
                    append("• Transport: ₹1,500 budget\n")
                    append("• Food: ₹2,500 budget\n\n")
                    append("🔔 Expected Notifications:\n")
                    append("• Grocery: 50% alert (₹1,500 spent)\n")
                    append("• Entertainment: 90% alert (₹1,700 spent)\n")
                    append("• Transport: 100% alert (₹1,600 spent)\n\n")
                    append("Check your notifications!")
                }
                
                showMessage(message)
                
                // Refresh UI to show new budget data
                viewModel.refreshData()
                
            } catch (e: Exception) {
                showErrorMessage("Budget notification test failed: ${e.message}")
                android.util.Log.e("HomeFragment", "Budget notification test error: ${e.message}", e)
            }
        }
    }

    private fun forceRealSmsParsingOnly() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("HomeFragment", "🚀 ===== FORCING REAL SMS PARSING ONLY =====")
                
                val smsManager = SMSManager(requireContext())
                val smsResult = smsManager.processAllSMS()
                
                val message = buildString {
                    append("📱 REAL SMS PARSING RESULTS:\n\n")
                    append("Success: ${smsResult.success}\n")
                    append("Total SMS Found: ${smsResult.smsFound}\n")
                    append("Transaction SMS: ${smsResult.transactionSmsFound}\n")
                    append("Transactions Created: ${smsResult.transactionsCreated}\n\n")
                    
                    if (smsResult.transactionsCreated > 0) {
                        append("✅ Real transactions created from your SMS!\n")
                        append("Check the transaction list to see them.")
                    } else {
                        append("❌ No transaction SMS found on your device.\n")
                        append("Make sure you have:\n")
                        append("• Bank transaction SMS\n")
                        append("• UPI payment notifications\n")
                        append("• Credit/debit card alerts")
                    }
                }
                
                showMessage(message)
                
                // Refresh UI if transactions were created
                if (smsResult.transactionsCreated > 0) {
                    viewModel.refreshData()
                }
                
            } catch (e: Exception) {
                showErrorMessage("Real SMS parsing failed: ${e.message}")
                android.util.Log.e("HomeFragment", "Real SMS parsing error: ${e.message}", e)
            }
        }
    }

    private fun clearAllDataAndParseRealSMS() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("HomeFragment", "🧽 ===== CLEARING ALL DATA AND PARSING REAL SMS =====")
                
                // Step 1: Clear all existing data
                val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(requireContext())
                database.transactionDao().deleteAllTransactions()
                android.util.Log.d("HomeFragment", "✅ Cleared all existing transactions")
                
                // Step 2: Check SMS permissions
                val smsPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
                val receivePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS)
                
                if (smsPermission != PackageManager.PERMISSION_GRANTED || receivePermission != PackageManager.PERMISSION_GRANTED) {
                    showErrorMessage("❌ SMS permissions not granted!\nPlease grant SMS permissions first.")
                    return@launch
                }
                
                android.util.Log.d("HomeFragment", "✅ SMS permissions granted")
                
                // Step 3: Try to parse real SMS directly
                val smsManager = SMSManager(requireContext())
                val smsResult = smsManager.processAllSMS()
                
                // Step 4: Show detailed diagnosis
                val message = buildString {
                    append("🔍 SMS PARSING DIAGNOSIS:\n\n")
                    append("📱 SMS PERMISSIONS: ✅ Granted\n")
                    append("📊 SMS SCAN RESULTS:\n")
                    append("• Total SMS Found: ${smsResult.smsFound}\n")
                    append("• Transaction SMS: ${smsResult.transactionSmsFound}\n")
                    append("• Transactions Created: ${smsResult.transactionsCreated}\n")
                    append("• Success: ${smsResult.success}\n\n")
                    
                    if (smsResult.transactionsCreated > 0) {
                        append("✅ SUCCESS! Real transactions created from SMS.\n")
                        append("Check your transaction list now.")
                    } else if (smsResult.smsFound == 0) {
                        append("❌ ISSUE: No SMS found at all!\n")
                        append("• Check if SMS permission is really granted\n")
                        append("• Try restarting the app")
                    } else if (smsResult.transactionSmsFound == 0) {
                        append("❌ ISSUE: SMS found but no transaction SMS!\n")
                        append("• Found ${smsResult.smsFound} SMS total\n")
                        append("• But none match transaction patterns\n")
                        append("• Do you have bank/UPI transaction SMS?")
                    } else {
                        append("❌ ISSUE: Transaction SMS found but failed to create transactions!\n")
                        append("• Found ${smsResult.transactionSmsFound} transaction SMS\n")
                        append("• But parsing failed - check SMS format")
                    }
                }
                
                showMessage(message)
                
                // Step 5: Auto-categorize all transactions based on keywords
                if (smsResult.transactionsCreated > 0) {
                    android.util.Log.d("HomeFragment", "🤖 Starting auto-categorization...")
                    val categorizedCount = transactionRepository.autoCategorizeExistingTransactions()
                    android.util.Log.d("HomeFragment", "✅ Auto-categorized $categorizedCount transactions")
                    
                    Toast.makeText(
                        requireContext(),
                        "✅ Imported ${smsResult.transactionsCreated} transactions\n🤖 Auto-categorized $categorizedCount transactions",
                        Toast.LENGTH_LONG
                    ).show()
                }
                
                // Refresh UI
                viewModel.refreshData()
                loadBankSpending() // Refresh bank cards
                
                // Refresh categories fragment data
                (activity as? HomeActivity)?.refreshCategoriesData()
                
            } catch (e: Exception) {
                showErrorMessage("❌ SMS parsing failed: ${e.message}")
                android.util.Log.e("HomeFragment", "SMS parsing error: ${e.message}", e)
            }
        }
    }

    private fun showAddTransactionDialog() {
        val dialog = com.koshpal_android.koshpalapp.ui.home.dialog.AddTransactionDialog()
        dialog.setOnTransactionAddedListener {
            // Refresh data after transaction is added
            viewModel.refreshData()
            loadBankSpending() // Refresh bank cards to show updated amounts
            
            // Refresh categories fragment data
            (activity as? HomeActivity)?.refreshCategoriesData()
            
            Toast.makeText(
                requireContext(),
                "Transaction added successfully!",
                Toast.LENGTH_SHORT
            ).show()
        }
        dialog.show(childFragmentManager, com.koshpal_android.koshpalapp.ui.home.dialog.AddTransactionDialog.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore status bar color to white when leaving HomeFragment
        activity?.window?.let { window ->
            window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.white)
            
            // Make status bar icons dark (for light background)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            
            android.util.Log.d("HomeFragment", "🎨 Status bar color restored to white")
        }
        _binding = null
    }
}