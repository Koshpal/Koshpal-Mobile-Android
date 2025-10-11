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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
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

        setupRecyclerViews()
        setupClickListeners()
        setupRecentTransactionsRecyclerView()

        // Only reset transactions to "others" on very first app install (one-time only)
        // This prevents overriding user categorizations on every app launch
        lifecycleScope.launch {
            try {
                // Check if this is the first time the app is running
                val sharedPrefs = requireContext().getSharedPreferences("koshpal_prefs", Context.MODE_PRIVATE)
                val hasResetTransactions = sharedPrefs.getBoolean("has_reset_transactions", false)
                
                if (!hasResetTransactions) {
                    val resetCount = transactionRepository.resetAllTransactionsToOthers()
                    android.util.Log.d("HomeFragment", "🔄 First-time reset: $resetCount transactions set to 'others'")
                    
                    // Mark that we've done the reset so we don't do it again
                    sharedPrefs.edit().putBoolean("has_reset_transactions", true).apply()
                } else {
                    android.util.Log.d("HomeFragment", "✅ Skipping reset - user categorizations preserved")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Failed to reset transactions: ${e.message}")
            }
        }

        // FIXED: Use single data source - ViewModel only
        android.util.Log.d("HomeFragment", "🚀 Setting up ViewModel observation...")
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // RecyclerViews removed for new Figma design
        // Features are now integrated into the main layout
    }

    private fun setupRecentTransactionsRecyclerView() {
        recentTransactionsAdapter = RecentTransactionAdapter { transaction ->
            // Handle transaction click - could navigate to transaction details
            android.util.Log.d("HomeFragment", "📱 Transaction clicked: ${transaction.merchant}")
            // Show categorization dialog when transaction is clicked
            showTransactionCategorizationDialog(transaction)
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
        bankCardAdapter = BankCardAdapter {
            // Handle add cash button click
            showAddCashDialog()
        }

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

            // Add budget button click
            btnAddBudget.setOnClickListener {
                navigateToBudget()
            }

            // Budget card click (feature removed)
            cardBudget.setOnClickListener {
                Toast.makeText(requireContext(), "Budget feature coming soon!", Toast.LENGTH_SHORT)
                    .show()
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

            // LONG PRESS financial card to trigger auto-categorization + show debug info
            cardFinancialOverview.setOnLongClickListener {
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

            cardFinancialOverview.setOnClickListener {
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

            // Month selector click listener
            layoutMonthSelector.setOnClickListener {
                showMonthSelectionDialog()
            }
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
                renderLast4MonthsChart(uiState)
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
                    binding.tvViewAllTransactions.text = if (transactions.isEmpty()) {
                        "View All (0)"
                    } else {
                        "View All (${transactions.size})"
                    }

                    // Show recent transactions (limit to 5)
                    val recentTransactions = transactions.take(5)
                    recentTransactionsAdapter.submitList(recentTransactions)

                    // Show/hide recent transactions RecyclerView
                    binding.rvRecentTransactions.visibility = if (recentTransactions.isNotEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    // Show/hide the no transactions card based on transaction count
                    binding.cardNoTransactions.visibility = if (transactions.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
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

        _binding?.tvCurrentMonth?.text = "$currentMonth $currentYear"
        android.util.Log.d("HomeFragment", "📅 Updated month display: $currentMonth $currentYear")
    }

    private fun renderLast4MonthsChart(state: HomeUiState) {
        val chart: BarChart = binding.chartLast4Months
        // Basic styling
        chart.description.isEnabled = false
        chart.setPinchZoom(false)
        chart.setScaleEnabled(false)
        chart.axisRight.isEnabled = false
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.textColor = android.graphics.Color.parseColor("#6B7280")
        chart.setNoDataText("No data yet")
        chart.setExtraOffsets(8f, 8f, 8f, 8f)

        val months = state.last4MonthsComparison.map { it.month }
        if (months.isEmpty()) {
            chart.clear()
            return
        }

        val incomeEntries = mutableListOf<BarEntry>()
        val expenseEntries = mutableListOf<BarEntry>()
        state.last4MonthsComparison.forEachIndexed { index, item ->
            incomeEntries.add(BarEntry(index.toFloat(), item.totalIncome.toFloat()))
            expenseEntries.add(BarEntry(index.toFloat(), item.totalSpent.toFloat()))
        }

        val incomeSet = BarDataSet(incomeEntries, "Income").apply {
            color = android.graphics.Color.parseColor("#10B981")
            valueTextColor = android.graphics.Color.parseColor("#374151")
        }
        val expenseSet = BarDataSet(expenseEntries, "Expenses").apply {
            color = android.graphics.Color.parseColor("#EF4444")
            valueTextColor = android.graphics.Color.parseColor("#374151")
        }

        val data = BarData(incomeSet, expenseSet).apply {
            setValueTextSize(10f)
            barWidth = 0.32f
        }

        val groupSpace = 0.36f
        val barSpace = 0.0f

        chart.data = data
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            valueFormatter = IndexAxisValueFormatter(months)
            granularity = 1f
            textColor = android.graphics.Color.parseColor("#6B7280")
        }

        chart.legend.apply {
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            orientation = Legend.LegendOrientation.HORIZONTAL
            textColor = android.graphics.Color.parseColor("#374151")
            setDrawInside(false)
        }

        // Ensure proper grouping across x range [0, count)
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.axisMaximum = 0f + data.getGroupWidth(groupSpace, barSpace) * months.size
        chart.groupBars(0f, groupSpace, barSpace)
        chart.invalidate()

        // Budget details navigation removed
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
                // Show current month data prominently (removed balance display)
                tvTotalIncome.text = "₹${String.format("%.0f", state.currentMonthIncome)}"
                tvTotalExpenses.text = "₹${String.format("%.0f", state.currentMonthExpenses)}"

                // Update the month display to selected month
                val selectedDate = java.util.Calendar.getInstance()
                selectedDate.set(state.selectedYear, state.selectedMonth, 1)
                val monthFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
                tvCurrentMonth.text =
                    "${monthFormat.format(selectedDate.time)} ${state.selectedYear}"

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
                // First time user - show import SMS prompt (removed balance display)
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
                tvViewAllTransactions.text = if (debugResult.hasTransactions) {
                    "View All (${debugResult.transactionCount})"
                } else {
                    "No transactions"
                }

                // Show/hide appropriate cards
                if (debugResult.hasTransactions) {
                    cardNoTransactions.visibility = View.GONE
                    cardSmsParser.visibility = View.GONE
                    cardFinancialOverview.visibility = View.VISIBLE
                } else {
                    cardNoTransactions.visibility = View.VISIBLE
                    cardSmsParser.visibility = View.VISIBLE
                    cardFinancialOverview.visibility = View.VISIBLE
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
                
            } catch (e: Exception) {
                showErrorMessage("❌ SMS parsing failed: ${e.message}")
                android.util.Log.e("HomeFragment", "SMS parsing error: ${e.message}", e)
            }
        }
    }
}