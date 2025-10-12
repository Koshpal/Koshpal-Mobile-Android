package com.koshpal_android.koshpalapp.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    private val homeFragment = HomeFragment()
    private val insightsFragment = InsightsFragment()
    private val categoriesFragment = CategoriesFragment()
    private val profileFragment = ProfileFragment()
    private val transactionsFragment = TransactionsFragment()
    private val budgetFragment = BudgetFragment()

    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(homeFragment)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    replaceFragment(homeFragment)
                    true
                }
                R.id.shorts -> {
                    replaceFragment(insightsFragment) // Map to a fragment (e.g., Shorts)
                    true
                }
                R.id.subscriptions -> {
                    replaceFragment(categoriesFragment) // Map to a fragment (e.g., Subscriptions)
                    true
                }
                R.id.library -> {
                    replaceFragment(profileFragment) // Map to a fragment (e.g., Library)
                    true
                }
                else -> false
            }
        }

        binding.fabCenter.setOnClickListener {
            // Handle FAB action (e.g., open a new fragment or dialog)
            replaceFragment(transactionsFragment) // Example action
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
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, fragment)
        fragmentTransaction.commit()
    }

    fun showTransactionsFragment() {
        replaceFragment(transactionsFragment)
    }

    fun showCategoriesFragment() {
        replaceFragment(categoriesFragment)
    }

    fun showHomeFragment() {
        replaceFragment(homeFragment)
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

        binding.bottomNavigationContainer.visibility = android.view.View.GONE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, categoryDetailsFragment)
            .addToBackStack("category_details")
            .commit()
    }

    fun navigateBackFromCategoryDetails() {
        android.util.Log.d("HomeActivity", "🔙 Navigating back from category details")

        binding.bottomNavigationContainer.visibility = android.view.View.VISIBLE
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

        binding.bottomNavigationContainer.visibility = android.view.View.GONE
    }

    fun showSetMonthlyBudgetFragment() {
        android.util.Log.d("HomeActivity", "💰 Navigating to Set Monthly Budget")

        val setMonthlyBudgetFragment = com.koshpal_android.koshpalapp.ui.categories.SetMonthlyBudgetFragment()

        binding.bottomNavigationContainer.visibility = android.view.View.GONE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, setMonthlyBudgetFragment)
            .addToBackStack("set_monthly_budget")
            .commit()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            binding.bottomNavigationContainer.visibility = android.view.View.VISIBLE
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}