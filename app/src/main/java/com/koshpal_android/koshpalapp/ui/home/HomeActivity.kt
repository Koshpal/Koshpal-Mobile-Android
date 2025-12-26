package com.koshpal_android.koshpalapp.ui.home

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ActivityHomeBinding
import com.koshpal_android.koshpalapp.ui.transactions.TransactionsFragment
import com.koshpal_android.koshpalapp.ui.budget.BudgetFragment
import com.koshpal_android.koshpalapp.ui.categories.CategoriesFragment
import com.koshpal_android.koshpalapp.ui.categories.CategoryDetailsFragment
import com.koshpal_android.koshpalapp.ui.bank.BankTransactionsFragment
import com.koshpal_android.koshpalapp.ui.insights.InsightsFragment
import com.koshpal_android.koshpalapp.ui.profile.ProfileFragment
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionDetailsDialog
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.ui.reminders.RemindersListFragment
import com.koshpal_android.koshpalapp.ui.home.compose.CustomBottomNavigation
import com.koshpal_android.koshpalapp.ui.home.compose.NavigationItem
import com.koshpal_android.koshpalapp.ui.theme.KoshpalTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    private val homeFragment = HomeFragment()
    private val insightsFragment = InsightsFragment()
    private val categoriesFragment = CategoriesFragment()
    private val profileFragment = ProfileFragment()
    private val transactionsFragment = TransactionsFragment()
    private val budgetFragment = BudgetFragment()
    private val fregmentReminders = RemindersListFragment()

    private var activeFragment: Fragment = homeFragment
    private var selectedNavItemId by mutableStateOf(R.id.home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val window = window
        // Set status bar and navigation bar to dark/black for dark theme
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.black)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = ContextCompat.getColor(this, android.R.color.black)
        }

        // Make status bar and navigation bar icons light (white) for dark background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsBehavior(
                android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = 0 // Clear all flags for dark background
        }
        // Pre-add all primary fragments and show Home by default for faster nav
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, insightsFragment, "insights").hide(insightsFragment)
            .add(R.id.fragmentContainer, categoriesFragment, "categories").hide(categoriesFragment)
            .add(R.id.fragmentContainer, fregmentReminders, "reminders").hide(fregmentReminders)
            .add(R.id.fragmentContainer, transactionsFragment, "transactions").hide(transactionsFragment)
            .add(R.id.fragmentContainer, homeFragment, "home")
            .commit()
        activeFragment = homeFragment

        // Setup custom Compose bottom navigation
        setupBottomNavigation()

        // Check if coming from SMS processing - refresh categories data
        val smsProcessingCompleted = intent.getBooleanExtra("SMS_PROCESSING_COMPLETED", false)
        if (smsProcessingCompleted) {
            android.util.Log.d("HomeActivity", "✅ ===== SMS PROCESSING COMPLETED FLAG DETECTED =====")
            android.util.Log.d("HomeActivity", "🕐 Scheduling categories refresh after 1.5 second delay")
            binding.root.postDelayed({
                android.util.Log.d("HomeActivity", "🔄 ===== NOW REFRESHING CATEGORIES DATA =====")
                try {
                    refreshCategoriesData()
                    android.util.Log.d("HomeActivity", "✅ Categories refresh initiated successfully")
                } catch (e: Exception) {
                    android.util.Log.e("HomeActivity", "❌ Error refreshing categories: ${e.message}", e)
                }
            }, 1500)
        }

        // Check if coming from notification click - open transaction dialog
        val openTransactionDialog = intent.getBooleanExtra("open_transaction_dialog", false)
        val transactionId = intent.getStringExtra("transaction_id")
        if (openTransactionDialog && transactionId != null) {
            android.util.Log.d("HomeActivity", "🔔 ===== NOTIFICATION CLICK DETECTED =====")
            android.util.Log.d("HomeActivity", "📱 Opening transaction dialog for ID: $transactionId")

            // Navigate to transactions fragment first
            showFragment(transactionsFragment)

            // Then open the transaction dialog after a short delay
            binding.root.postDelayed({
                try {
                    openTransactionDetailsDialog(transactionId)
                } catch (e: Exception) {
                    android.util.Log.e("HomeActivity", "❌ Error opening transaction dialog: ${e.message}", e)
                }
            }, 500)
        }
        
        // Check if coming from budget notification click - open budget fragment
        val openBudgetFragment = intent.getBooleanExtra("open_budget_fragment", false)
        if (openBudgetFragment) {
            android.util.Log.d("HomeActivity", "💰 ===== BUDGET NOTIFICATION CLICK DETECTED =====")
            android.util.Log.d("HomeActivity", "📱 Opening budget fragment")

            // Navigate to budget fragment
            showFragment(categoriesFragment)
        }
    }

    private fun setupBottomNavigation() {
        val navigationItems = listOf(
            NavigationItem(R.id.home, R.drawable.ic_home, "Home"),
            NavigationItem(R.id.transactions, R.drawable.ic_rup, "Payments"),
            NavigationItem(R.id.categories, R.drawable.ic_categ, "Categories"),
            //NavigationItem(R.id.insights, R.drawable.ic_insig, "Insights"),
            NavigationItem(R.id.reminders, R.drawable.ic_notifications, "Reminders")
        )

        val composeView = binding.root.findViewById<ComposeView>(R.id.bottomNavigationCompose)
        composeView?.setContent {
            KoshpalTheme {
                CustomBottomNavigation(
                    items = navigationItems,
                    selectedItemId = selectedNavItemId,
                    onItemSelected = { itemId ->
                        selectedNavItemId = itemId
                        when (itemId) {
                            R.id.home -> showFragment(homeFragment)
                            R.id.transactions -> showFragment(transactionsFragment)
                            R.id.categories -> showFragment(categoriesFragment)
                            //R.id.insights -> showFragment(insightsFragment)
                            R.id.reminders -> showFragment(fregmentReminders)
                        }
                    }
                )
            }
        }
    }

    private fun showFragment(target: Fragment) {
        if (target === activeFragment) return
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commit()
        activeFragment = target
    }

    fun showTransactionsFragment() {
        showFragment(transactionsFragment)
    }

    fun showCategoriesFragment() {
        showFragment(categoriesFragment)
    }

    fun showHomeFragment() {
        showFragment(homeFragment)
        android.util.Log.d("HomeActivity", "🏠 Navigated back to Home fragment")
    }

    fun refreshCategoriesData() {
        android.util.Log.d("HomeActivity", "🔄 Categories data refresh requested - forcing reload")
        android.util.Log.d("HomeActivity", "🔍 categoriesFragment reference: ${categoriesFragment}")
        android.util.Log.d("HomeActivity", "🔍 categoriesFragment.isAdded: ${categoriesFragment.isAdded}")

        try {
            categoriesFragment.refreshCategoryData()
            android.util.Log.d("HomeActivity", "✅ Successfully called refreshCategoryData()")
        } catch (e: Exception) {
            android.util.Log.e("HomeActivity", "❌ Error calling refreshCategoryData(): ${e.message}", e)
        }
    }

    fun showCategoryDetailsFragment(
        categoryId: String,
        categoryName: String,
        categoryIcon: Int,
        month: Int,
        year: Int
    ) {
        android.util.Log.d(
            "HomeActivity",
            "📊 Navigating to category details: $categoryName (month: $month, year: $year)"
        )

        val categoryDetailsFragment = CategoryDetailsFragment.newInstance(
            categoryId = categoryId,
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            month = month,
            year = year
        )

        binding.root.findViewById<ComposeView>(R.id.bottomNavigationCompose)?.visibility = android.view.View.GONE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, categoryDetailsFragment)
            .addToBackStack("category_details")
            .commit()
    }

    fun navigateBackFromCategoryDetails() {
        android.util.Log.d("HomeActivity", "🔙 Navigating back from category details")

        binding.root.findViewById<ComposeView>(R.id.bottomNavigationCompose)?.visibility = android.view.View.VISIBLE
        supportFragmentManager.popBackStack()
        refreshCategoriesData()
    }

    fun showBankTransactionsFragment(bankName: String) {
        android.util.Log.d("HomeActivity", "🏦 Navigating to bank transactions for: $bankName")

        val bankTransactionsFragment = BankTransactionsFragment.newInstance(bankName)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, bankTransactionsFragment)
            .addToBackStack("bank_transactions")
            .commit()

        binding.root.findViewById<ComposeView>(R.id.bottomNavigationCompose)?.visibility = android.view.View.GONE
    }

    fun showSetMonthlyBudgetFragment() {
        android.util.Log.d("HomeActivity", "💰 Navigating to Set Monthly Budget")

        val setMonthlyBudgetFragment = com.koshpal_android.koshpalapp.ui.categories.SetMonthlyBudgetFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, setMonthlyBudgetFragment)
            .addToBackStack("set_monthly_budget")
            .commit()

        // Hide bottom nav after fragment transaction
        binding.root.post {
            binding.root.findViewById<ComposeView>(R.id.bottomNavigationCompose)?.visibility = android.view.View.GONE
            android.util.Log.d("HomeActivity", "🚫 Bottom navigation hidden")
        }
    }

    fun showRemindersListFragment() {
        android.util.Log.d("HomeActivity", "🔔 Navigating to Reminders List")

        val remindersListFragment = com.koshpal_android.koshpalapp.ui.reminders.RemindersListFragment.newInstance()

        binding.root.findViewById<ComposeView>(R.id.bottomNavigationCompose)?.visibility = android.view.View.GONE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, remindersListFragment)
            .addToBackStack("reminders_list")
            .commit()
    }

    private fun openTransactionDetailsDialog(transactionId: String) {
        android.util.Log.d("HomeActivity", "🔍 Opening transaction dialog for ID: $transactionId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = KoshpalDatabase.getDatabase(this@HomeActivity)
                val transactionDao = database.transactionDao()
                val transaction = transactionDao.getTransactionById(transactionId)
                
                if (transaction != null) {
                    android.util.Log.d("HomeActivity", "✅ Found transaction: ${transaction.merchant} - ₹${transaction.amount}")
                    
                    // Switch to main thread to show dialog
                    runOnUiThread {
                        val dialog = TransactionDetailsDialog.newInstance(transaction) { _ ->
                            android.util.Log.d("HomeActivity", "📝 Transaction updated from notification dialog")
                            // Optionally refresh the transactions fragment
                            if (transactionsFragment.isAdded) {
                                // Refresh transactions fragment if needed
                                android.util.Log.d("HomeActivity", "🔄 Transaction updated, fragment refresh may be needed")
                            }
                        }
                        dialog.show(supportFragmentManager, "TransactionDetailsDialog")
                    }
                } else {
                    android.util.Log.w("HomeActivity", "⚠️ Transaction not found with ID: $transactionId")
                    runOnUiThread {
                        android.widget.Toast.makeText(
                            this@HomeActivity,
                            "Transaction not found",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeActivity", "❌ Error opening transaction dialog", e)
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this@HomeActivity,
                        "Error opening transaction details",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            binding.root.findViewById<ComposeView>(R.id.bottomNavigationCompose)?.visibility = android.view.View.VISIBLE
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}