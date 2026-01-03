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
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import java.util.Calendar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
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
import com.koshpal_android.koshpalapp.ui.profile.ProfileActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.ui.home.compose.HomeScreen
import com.koshpal_android.koshpalapp.ui.home.compose.AddGoalDialog
import com.koshpal_android.koshpalapp.ui.goals.GoalsViewModel
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionCategorizationDialog
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionDetailsDialog
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.ui.home.model.FeatureItem
import com.koshpal_android.koshpalapp.ui.home.model.HomeUiState
import com.koshpal_android.koshpalapp.ui.home.model.MonthlySpendingData
import com.koshpal_android.koshpalapp.ui.theme.KoshpalTheme
import com.koshpal_android.koshpalapp.utils.SMSReader
import com.koshpal_android.koshpalapp.utils.SMSTestHelper
import com.koshpal_android.koshpalapp.utils.DebugHelper
import com.koshpal_android.koshpalapp.utils.SMSManager
import com.koshpal_android.koshpalapp.utils.DebugDataManager
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.BankSpending
import com.koshpal_android.koshpalapp.model.PaymentSms
import com.koshpal_android.koshpalapp.model.Transaction
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

    private val viewModel: HomeViewModel by viewModels()
    private val goalsViewModel: GoalsViewModel by viewModels()
    private var isFirstLoad = true

    @Inject
    lateinit var smsReader: SMSReader

    @Inject
    lateinit var transactionProcessingService: TransactionProcessingService

    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsPermissionGranted = permissions[Manifest.permission.READ_SMS] == true
        val receivePermissionGranted = permissions[Manifest.permission.RECEIVE_SMS] == true

        if (smsPermissionGranted && receivePermissionGranted) {
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
        // Set status bar color
        setStatusBarColor()
        
        return ComposeView(requireContext()).apply {
            setContent {
                KoshpalTheme {
                    HomeScreenContent()
                }
            }
        }
    }

    @Composable
    private fun HomeScreenContent() {
        // Observe ViewModel state
        val uiState by viewModel.uiState.collectAsState()
        val recentTransactions by viewModel.recentTransactions.collectAsState()
        
        // Bank spending state
        var bankCards by remember { mutableStateOf<List<BankSpending>>(emptyList()) }
        var isLoadingBankData by remember { mutableStateOf(true) }

        // Goals dialog state
        var showAddGoalDialog by remember { mutableStateOf(false) }
        
        // Calculate greeting text
        val greetingText = remember {
            when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 0..11 -> "Good Morning"
                in 12..16 -> "Good Afternoon"
                else -> "Good Evening"
            }
        }
        
        // Load bank spending data
        LaunchedEffect(Unit) {
            try {
                android.util.Log.d("HomeFragment", "💳 ========== LOADING BANK SPENDING ==========")

                // Get ALL-TIME transaction data (no month filtering)
                val bankSpending = transactionRepository.getBankWiseSpending().toMutableList()

                android.util.Log.d("HomeFragment", "💳 Loaded ${bankSpending.size} bank cards with spending data")

                // Detailed logging for each bank
                bankSpending.forEach { bank ->
                    android.util.Log.d("HomeFragment", "💳 ${bank.bankName}: ₹${bank.totalSpending} (${bank.transactionCount} transactions, isCash=${bank.isCash})")
                }

                // Special check for cash cards
                val cashCards = bankSpending.filter { it.isCash }
                if (cashCards.isNotEmpty()) {
                    android.util.Log.d("HomeFragment", "💰 CASH CARDS FOUND: ${cashCards.size}")
                    cashCards.forEach { cash ->
                        android.util.Log.d("HomeFragment", "💰 Cash: ₹${cash.totalSpending} (${cash.transactionCount} transactions)")
                    }
                } else {
                    android.util.Log.w("HomeFragment", "⚠️ NO CASH CARDS - cash transactions may not be detected")
                }

                android.util.Log.d("HomeFragment", "💳 Submitting ${bankSpending.size} cards")
                bankCards = bankSpending
                isLoadingBankData = false
                
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "❌ Failed to load bank spending: ${e.message}", e)
                e.printStackTrace()

                // Show empty list on error (no fallback cash card)
                bankCards = emptyList()
                isLoadingBankData = false
            }
        }
        
        // Reload bank data when transactions change
        LaunchedEffect(recentTransactions.size) {
            if (!isLoadingBankData && recentTransactions.isNotEmpty()) {
                try {
                    val bankSpending = transactionRepository.getBankWiseSpending().toMutableList()
                    bankCards = bankSpending
                } catch (e: Exception) {
                    android.util.Log.e("HomeFragment", "❌ Failed to reload bank spending: ${e.message}", e)
                }
            }
        }
        
        // Calculate percentage changes (simplified - you can enhance this)
        val incomeChangePercentage = remember(uiState.currentMonthIncome) {
            // TODO: Calculate actual percentage change from last month
            if (uiState.currentMonthIncome > 0) "+12%" else null
        }
        
        val expenseChangePercentage = remember(uiState.currentMonthExpenses) {
            // TODO: Calculate actual percentage change from last month
            if (uiState.currentMonthExpenses > 0) "-8%" else null
        }
        
        // Memoize callbacks
        val context = requireContext()
        val onProfileClick: () -> Unit = remember {
            {
                android.util.Log.d("HomeFragment", "👤 Profile icon clicked - navigating to profile")
                val intent = Intent(context, ProfileActivity::class.java)
                startActivity(intent)
            }
        }
        
        val onNotificationClick: () -> Unit = remember {
            {
                // TODO: Navigate to notifications
                Toast.makeText(context, "Notifications coming soon", Toast.LENGTH_SHORT).show()
            }
        }
        
        val onViewDetailsClick: () -> Unit = remember {
            {
                // Show monthly spending details
                showMonthlySpendingDetails(uiState.last3MonthsData)
            }
        }
        
        val onBankCardClick: (String) -> Unit = remember {
            { bankName ->
                (activity as? HomeActivity)?.showBankTransactionsFragment(bankName)
            }
        }
        
        val onAddCashClick: () -> Unit = remember {
            {
                showAddCashDialog()
            }
        }
        
        val onAddPaymentClick: () -> Unit = remember {
            {
                showAddTransactionDialog()
            }
        }
        
        val onTransactionClick: (Transaction) -> Unit = remember {
            { transaction ->
                android.util.Log.d("HomeFragment", "📱 Transaction clicked: ${transaction.merchant}")
                showTransactionDetailsDialog(transaction)
            }
        }
        
        val onViewAllTransactionsClick: () -> Unit = remember {
            {
                (activity as? HomeActivity)?.showTransactionsFragment()
            }
        }

        val onAddGoalClick: () -> Unit = remember {
            {
                showAddGoalDialog = true
            }
        }
        
        // Show HomeScreen
        HomeScreen(
            greetingText = greetingText,
            userName = uiState.userName,
            currentMonthIncome = uiState.currentMonthIncome,
            currentMonthExpenses = uiState.currentMonthExpenses,
            incomeChangePercentage = incomeChangePercentage,
            expenseChangePercentage = expenseChangePercentage,
            bankCards = bankCards,
            recentTransactions = recentTransactions.take(5), // Limit to 5 for display
            onProfileClick = onProfileClick,
            onNotificationClick = onNotificationClick,
            onViewDetailsClick = onViewDetailsClick,
            onBankCardClick = onBankCardClick,
            onAddCashClick = onAddCashClick,
            onAddPaymentClick = onAddPaymentClick,
            onTransactionClick = onTransactionClick,
            onViewAllTransactionsClick = onViewAllTransactionsClick,
            goalsViewModel = goalsViewModel,
            onAddGoalClick = onAddGoalClick
        )

        // Add Goal Dialog
        if (showAddGoalDialog) {
            AddGoalDialog(
                viewModel = goalsViewModel,
                onDismiss = { showAddGoalDialog = false },
                onGoalCreated = {
                    showAddGoalDialog = false
                    // Goals list will auto-refresh via ViewModel
                }
            )
        }
    }
    

    private fun setStatusBarColor() {
        activity?.window?.let { window ->
            // Set status bar and navigation bar to dark/black for dark theme
            window.statusBarColor = ContextCompat.getColor(requireContext(), android.R.color.black)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                window.navigationBarColor = ContextCompat.getColor(requireContext(), android.R.color.black)
            }
            
            // Make status bar and navigation bar icons light (white) for dark background
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                var flags = window.decorView.systemUiVisibility
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() // Clear light status bar flag
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv() // Clear light nav bar flag
                }
                window.decorView.systemUiVisibility = flags
            }
            
            android.util.Log.d("HomeFragment", "🎨 Status bar and navigation bar set to dark")
        }
    }


    private fun showAddCashDialog() {
        // TODO: Implement add cash transaction dialog
        Toast.makeText(requireContext(), "Add Cash Transaction - Coming Soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showFeedbackDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_feedback, null)
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
        dialog.setContentView(dialogView)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        val btnBack = dialogView.findViewById<ImageView>(R.id.btnBack)
        val etFeedback = dialogView.findViewById<EditText>(R.id.etFeedback)
        val btnSubmit = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSubmit)

        // Enable/disable submit button based on text input
        etFeedback.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val hasText = !s.isNullOrBlank()
                btnSubmit.isEnabled = hasText
                btnSubmit.alpha = if (hasText) 1f else 0.5f
            }
        })

        btnBack.setOnClickListener {
            dialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            val feedbackText = etFeedback.text.toString().trim()
            if (feedbackText.isNotBlank()) {
                sendFeedbackEmail(feedbackText)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun sendFeedbackEmail(feedback: String) {
        try {
            if (isGmailInstalled()) {
                val gmailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:koshpal@koshpal.com")
                    setPackage("com.google.android.gm")
                    putExtra(Intent.EXTRA_SUBJECT, "Koshpal App Feedback")
                    putExtra(Intent.EXTRA_TEXT, feedback)
                }
                startActivity(gmailIntent)
                Toast.makeText(requireContext(), "Opening Gmail...", Toast.LENGTH_SHORT).show()
            } else {
                // Fallback: Open chooser with any available email app
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:koshpal@koshpal.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Koshpal App Feedback")
                    putExtra(Intent.EXTRA_TEXT, feedback)
                }
                startActivity(Intent.createChooser(emailIntent, "Send Feedback via"))
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error sending feedback", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }


    private fun isGmailInstalled(): Boolean {
        val pm = requireContext().packageManager
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            setPackage("com.google.android.gm")
        }
        val resolveInfo = pm.queryIntentActivities(intent, 0)
        return resolveInfo.isNotEmpty()
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
            // Permissions not granted - user can request them when needed
            android.util.Log.d("HomeFragment", "⚠️ SMS permissions not granted")
        } else {
            viewModel.onPermissionsGranted()
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
        // Compose UI will automatically update via ViewModel state
        viewModel.forceRefreshNow()
    }

    override fun onResume() {
        super.onResume()
        
        // Skip refresh on first load
        if (isFirstLoad) {
            isFirstLoad = false
            android.util.Log.d("HomeFragment", "⏭️ Skipping onResume refresh (first load)")
            return
        }
        
        android.util.Log.d("HomeFragment", "🔄 onResume - refreshing data...")
        // Refresh ViewModel - Compose UI will automatically update
        viewModel.refreshData()
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
            // Bank cards will auto-refresh via LaunchedEffect when transactions change
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
                
                // Refresh UI - Compose will auto-update via ViewModel
                viewModel.refreshData()
                // Bank cards will auto-refresh via LaunchedEffect when transactions change
                
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
            // Bank cards will auto-refresh via LaunchedEffect when transactions change
            
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
        // Restore status bar and navigation bar to dark when leaving HomeFragment
        activity?.window?.let { window ->
            window.statusBarColor = ContextCompat.getColor(requireContext(), android.R.color.black)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                window.navigationBarColor = ContextCompat.getColor(requireContext(), android.R.color.black)
            }
            
            // Make status bar and navigation bar icons light (white) for dark background
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                var flags = window.decorView.systemUiVisibility
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() // Clear light status bar flag
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv() // Clear light nav bar flag
                }
                window.decorView.systemUiVisibility = flags
            }
            
            android.util.Log.d("HomeFragment", "🎨 Status bar and navigation bar restored to dark")
        }
    }
}