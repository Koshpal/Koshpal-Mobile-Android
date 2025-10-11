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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    // FIXED: Keep fragment instances to prevent recreation
    private val homeFragment = HomeFragment()
    private val budgetFragment = BudgetFragment()
    private val transactionsFragment = TransactionsFragment()
    private val categoriesFragment = CategoriesFragment()
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        
        // Load default fragment only once
        if (savedInstanceState == null) {
            setupFragments()
        }
        
        // Check if coming from SMS processing - refresh categories data
        val smsProcessingCompleted = intent.getBooleanExtra("SMS_PROCESSING_COMPLETED", false)
        if (smsProcessingCompleted) {
            android.util.Log.d("HomeActivity", "✅ ===== SMS PROCESSING COMPLETED FLAG DETECTED =====")
            android.util.Log.d("HomeActivity", "🕐 Scheduling categories refresh after 1.5 second delay")
            // Post with delay to ensure fragments are ready and categorization is complete
            binding.root.postDelayed({
                android.util.Log.d("HomeActivity", "🔄 ===== NOW REFRESHING CATEGORIES DATA =====")
                try {
                    refreshCategoriesData()
                    android.util.Log.d("HomeActivity", "✅ Categories refresh initiated successfully")
                } catch (e: Exception) {
                    android.util.Log.e("HomeActivity", "❌ Error refreshing categories: ${e.message}", e)
                }
            }, 1500)  // Increased delay to ensure categorization is complete and fragments are ready
        }
    }

    private fun setupFragments() {
        // Add all fragments but hide them initially - use commitNow for synchronous execution
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, homeFragment, "HOME")
            .add(R.id.fragmentContainer, transactionsFragment, "TRANSACTIONS")
            .add(R.id.fragmentContainer, categoriesFragment, "CATEGORIES")
            .add(R.id.fragmentContainer, budgetFragment, "BUDGET")  // Keep for future use
            .hide(transactionsFragment)
            .hide(categoriesFragment)
            .hide(budgetFragment)
            .commitNow()  // Use commitNow() to ensure fragments are added immediately

        android.util.Log.d("HomeActivity", "✅ Fragments setup complete")
        
        // Ensure Home is selected by default
        binding.bottomNavigation.selectedItemId = R.id.homeFragment
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    showFragment(homeFragment)
                    true
                }
                R.id.transactionsFragment -> {
                    showFragment(transactionsFragment)
                    true
                }
                R.id.budgetFragment -> {
                    // Show Categories fragment when Budget is tapped
                    showFragment(categoriesFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        // FIXED: Show/hide fragments instead of recreating them
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .commit()
        activeFragment = fragment
    }

    fun showTransactionsFragment() {
        // Show the existing transactions fragment and update bottom nav
        showFragment(transactionsFragment)
        binding.bottomNavigation.selectedItemId = R.id.transactionsFragment
    }

    fun showCategoriesFragment() {
        // Show the existing categories fragment
        showFragment(categoriesFragment)
        // Do not change bottom navigation selection (no tab for categories)
    }

    fun showHomeFragment() {
        // Show the home fragment and update bottom nav
        showFragment(homeFragment)
        binding.bottomNavigation.selectedItemId = R.id.homeFragment
        android.util.Log.d("HomeActivity", "🏠 Navigated back to Home fragment")
    }
    
    fun refreshCategoriesData() {
        // Force refresh categories fragment data
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

        // Create the CategoryDetailsFragment with parameters
        val categoryDetailsFragment = CategoryDetailsFragment.newInstance(
            categoryId = categoryId,
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            month = month,
            year = year
        )

        // Hide bottom navigation when showing detail view
        binding.bottomNavigation.visibility = android.view.View.GONE

        // Add the fragment over the current view
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .add(R.id.fragmentContainer, categoryDetailsFragment, "CATEGORY_DETAILS")
            .addToBackStack("category_details")
            .commit()
    }

    fun navigateBackFromCategoryDetails() {
        android.util.Log.d("HomeActivity", "🔙 Navigating back from category details")
        
        // Show bottom navigation again
        binding.bottomNavigation.visibility = android.view.View.VISIBLE
        
        // Pop back stack to return to categories fragment
        supportFragmentManager.popBackStack()
        
        // Refresh categories data to show any updates
        refreshCategoriesData()
    }
}