package com.koshpal_android.koshpalapp.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshpal_android.koshpalapp.databinding.FragmentBudgetDetailsBinding
import com.koshpal_android.koshpalapp.ui.budget.adapter.PieLegendAdapter

/**
 * Fragment to display budget details with a modern donut chart
 * 
 * Usage Example:
 * ```
 * val fragment = BudgetDetailsFragment.newInstance(
 *     totalAmount = 94475.0,
 *     categories = listOf(
 *         CategoryData("Food & Dining", 56685.0, Color.parseColor("#5EEAD4")),
 *         CategoryData("Shopping", 19839.0, Color.parseColor("#FFC1CC")),
 *         CategoryData("Transport", 17950.0, Color.parseColor("#FCD34D"))
 *     )
 * )
 * ```
 */
class BudgetDetailsFragment : Fragment() {

    private var _binding: FragmentBudgetDetailsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var legendAdapter: PieLegendAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLegendRecyclerView()
        loadSampleData() // Replace with actual data loading
    }

    private fun setupLegendRecyclerView() {
        legendAdapter = PieLegendAdapter()
        binding.rvCategoryLegend.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = legendAdapter
        }
    }

    /**
     * Load and display budget data
     * Replace this with your actual data loading logic (ViewModel, Repository, etc.)
     */
    private fun loadSampleData() {
        // Get modern colors
        val colors = BudgetDetailsChartHelper.getModernColors()
        
        // Sample data - Replace with actual data from your ViewModel
        val categoryData = listOf(
            BudgetDetailsChartHelper.CategoryData("Food & Dining", 56685.0, colors[0]),
            BudgetDetailsChartHelper.CategoryData("Shopping", 19839.0, colors[1]),
            BudgetDetailsChartHelper.CategoryData("Transport", 17950.0, colors[2])
        )
        
        // Calculate total
        val total = categoryData.sumOf { it.amount }
        
        // Update total amount in center
        binding.tvTotalAmount.text = "₹${String.format("%,d", total.toInt())}"
        
        // Setup chart and get legend items
        val legendItems = BudgetDetailsChartHelper.setupModernDonutChart(
            chart = binding.chartDetails,
            data = categoryData
        )
        
        // Update legend RecyclerView
        legendAdapter.submitList(legendItems)
        
        // Optional: Update summary text
        binding.tvSummary.text = "Total spending across ${categoryData.size} categories"
    }

    /**
     * Call this method to update the chart with new data
     */
    fun updateChartData(categories: List<BudgetDetailsChartHelper.CategoryData>) {
        val total = categories.sumOf { it.amount }
        binding.tvTotalAmount.text = "₹${String.format("%,d", total.toInt())}"
        
        val legendItems = BudgetDetailsChartHelper.setupModernDonutChart(
            chart = binding.chartDetails,
            data = categories
        )
        
        legendAdapter.submitList(legendItems)
        binding.tvSummary.text = "Total spending across ${categories.size} categories"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = BudgetDetailsFragment()
    }
}
