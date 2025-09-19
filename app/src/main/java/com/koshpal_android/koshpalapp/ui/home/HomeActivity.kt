package com.koshpal_android.koshpalapp.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ActivityHomeBinding
import com.koshpal_android.koshpalapp.ui.budget.BudgetFragment
import com.koshpal_android.koshpalapp.ui.dashboard.DashboardFragment
import com.koshpal_android.koshpalapp.ui.payments.PaymentsFragment
import com.koshpal_android.koshpalapp.ui.profile.ProfileFragment
import com.koshpal_android.koshpalapp.ui.savings.SavingsGoalsFragment
import com.koshpal_android.koshpalapp.ui.transactions.TransactionsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_budget -> {
                    loadFragment(BudgetFragment())
                    true
                }
                R.id.nav_savings -> {
                    loadFragment(SavingsGoalsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // Helper methods for HomeFragment navigation
    fun switchToDashboardTab() {
        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
    }

    fun switchToBudgetTab() {
        binding.bottomNavigation.selectedItemId = R.id.nav_budget
    }

    fun switchToSavingsTab() {
        binding.bottomNavigation.selectedItemId = R.id.nav_savings
    }
    
    fun showTransactionsFragment() {
        // Load the transactions fragment directly
        loadFragment(TransactionsFragment())
    }
}