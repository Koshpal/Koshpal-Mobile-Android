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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = window.decorView.systemUiVisibility
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() // Clear light status bar flag
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv() // Clear light nav bar flag
            }
            window.decorView.systemUiVisibility = flags
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

        // Set bottom navigation background to null (required for BottomAppBar)
        binding.bottomNavigation.background = null
        
        // Disable the placeholder menu item (center position for FAB cradle)
        binding.bottomNavigation.menu.getItem(2).isEnabled = false
        // While Home is showing, make group not checkable and clear selection
        binding.bottomNavigation.menu.setGroupCheckable(0, false, true)
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            binding.bottomNavigation.menu.getItem(i).isChecked = false
        }
        
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Re-enable selection when user picks a tab
            binding.bottomNavigation.menu.setGroupCheckable(0, true, true)
            when (item.itemId) {
                R.id.transactions -> {
                    showFragment(transactionsFragment)
                    true
                }
                R.id.reminders -> {
                    showFragment(fregmentReminders)
                    true
                }
                R.id.categories -> {
                    showFragment(categoriesFragment)
                    true
                }
                R.id.insights -> {
                    showFragment(insightsFragment)
                    true
                }
                else -> false
            }
        }

        binding.fabCenter.setOnClickListener {
            // Center FAB goes Home
            showFragment(homeFragment)
            // Clear selection highlight to reflect Home (not in bottom nav)
            binding.bottomNavigation.menu.setGroupCheckable(0, false, true)
            for (i in 0 until binding.bottomNavigation.menu.size()) {
                binding.bottomNavigation.menu.getItem(i).isChecked = false
            }
        }

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

        binding.bottomNavigation.visibility = android.view.View.GONE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, categoryDetailsFragment)
            .addToBackStack("category_details")
            .commit()
    }

    fun navigateBackFromCategoryDetails() {
        android.util.Log.d("HomeActivity", "🔙 Navigating back from category details")

        binding.bottomAppBar.visibility = android.view.View.VISIBLE
        binding.bottomNavigation.visibility = android.view.View.VISIBLE
        binding.fabCenter.visibility = android.view.View.VISIBLE
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

        binding.bottomNavigation.visibility = android.view.View.GONE
    }

    fun showSetMonthlyBudgetFragment() {
        android.util.Log.d("HomeActivity", "💰 Navigating to Set Monthly Budget")

        val setMonthlyBudgetFragment = com.koshpal_android.koshpalapp.ui.categories.SetMonthlyBudgetFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, setMonthlyBudgetFragment)
            .addToBackStack("set_monthly_budget")
            .commit()

        // Hide bottom app bar, bottom nav and FAB after fragment transaction
        binding.root.post {
            binding.bottomAppBar.visibility = android.view.View.GONE
            binding.bottomNavigation.visibility = android.view.View.GONE
            binding.fabCenter.visibility = android.view.View.GONE
            android.util.Log.d("HomeActivity", "🚫 Bottom app bar, nav and FAB hidden")
        }
    }

    fun showRemindersListFragment() {
        android.util.Log.d("HomeActivity", "🔔 Navigating to Reminders List")

        val remindersListFragment = com.koshpal_android.koshpalapp.ui.reminders.RemindersListFragment.newInstance()

        binding.bottomNavigation.visibility = android.view.View.GONE

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
                        val dialog = TransactionDetailsDialog.newInstance(transaction) { updatedTransaction ->
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
            binding.bottomAppBar.visibility = android.view.View.VISIBLE
            binding.bottomNavigation.visibility = android.view.View.VISIBLE
            binding.fabCenter.visibility = android.view.View.VISIBLE
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}