package com.koshpal_android.koshpalapp.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ActivityHomeBinding
import com.koshpal_android.koshpalapp.ui.transactions.TransactionsFragment
import com.koshpal_android.koshpalapp.ui.budget.BudgetFragment
import com.koshpal_android.koshpalapp.ui.categories.CategoriesFragment
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
    }

    private fun setupFragments() {
        // Add all fragments but hide them initially
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, homeFragment, "HOME")
            .add(R.id.fragmentContainer, transactionsFragment, "TRANSACTIONS")
            .add(R.id.fragmentContainer, categoriesFragment, "CATEGORIES")
            .add(R.id.fragmentContainer, budgetFragment, "BUDGET")  // Keep for future use
            .hide(transactionsFragment)
            .hide(categoriesFragment)
            .hide(budgetFragment)
            .commit()

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
                R.id.insightsFragment -> {
                    // TODO: Create InsightsFragment
                    showFragment(homeFragment) // Temporary fallback
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
        // Simple refresh - the fragment will auto-refresh when it becomes visible
        android.util.Log.d("HomeActivity", "🔄 Categories data refresh requested")
    }
}