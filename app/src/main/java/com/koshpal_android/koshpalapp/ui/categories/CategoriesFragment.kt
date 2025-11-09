package com.koshpal_android.koshpalapp.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.koshpal_android.koshpalapp.ui.categories.compose.CategoriesScreen
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.theme.KoshpalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoriesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                KoshpalTheme {
                    CategoriesScreen(
                        onSetBudgetClick = {
                            (activity as? HomeActivity)?.showSetMonthlyBudgetFragment()
                        },
                        onCategoryClick = { categoryId, categoryName, categoryIcon, month, year ->
                            (activity as? HomeActivity)?.showCategoryDetailsFragment(
                                categoryId = categoryId,
                                categoryName = categoryName,
                                categoryIcon = categoryIcon,
                                month = month,
                                year = year
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("CategoriesFragment", "🔄 onResume - Compose UI will auto-refresh via ViewModel")
    }

    /**
     * Public method to force refresh category data
     * The Compose ViewModel will handle the refresh automatically
     */
    fun refreshCategoryData() {
        android.util.Log.d("CategoriesFragment", "🔄 Manual refresh requested - Compose ViewModel handles this")
        // The ViewModel in Compose screen will automatically refresh when needed
        // If you need to force refresh, you can access the ViewModel here if needed
    }
}
