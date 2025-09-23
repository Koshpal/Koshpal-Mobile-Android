package com.koshpal_android.koshpalapp.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ActivityHomeBinding
import com.koshpal_android.koshpalapp.ui.transactions.TransactionsFragment
import com.koshpal_android.koshpalapp.ui.budget.BudgetFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    
    // FIXED: Keep fragment instances to prevent recreation
    private val homeFragment = HomeFragment()
    private val budgetFragment = BudgetFragment()
    private val transactionsFragment = TransactionsFragment()
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
            .add(R.id.fragmentContainer, budgetFragment, "BUDGET")
            // Transactions fragment kept for internal navigation only
            .add(R.id.fragmentContainer, transactionsFragment, "TRANSACTIONS")
            .hide(budgetFragment)
            .hide(transactionsFragment)
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
                R.id.budgetFragment -> {
                    showFragment(budgetFragment)
                    true
                }
                // Transactions menu removed
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
        // Show the existing transactions fragment
        showFragment(transactionsFragment)
        // Do not change bottom navigation selection (no tab for transactions)
    }
}