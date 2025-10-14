package com.koshpal_android.koshpalapp.ui.budget

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshpal_android.koshpalapp.databinding.FragmentBudgetBinding
import com.koshpal_android.koshpalapp.ui.budget.adapter.BudgetCategoryListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.graphics.Color

@AndroidEntryPoint
class BudgetFragment : Fragment() {
    
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BudgetViewModel by viewModels()
    private lateinit var adapter: BudgetCategoryListAdapter
    private lateinit var legendAdapter: com.koshpal_android.koshpalapp.ui.budget.adapter.PieLegendAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupClicks()
        observeState()
        viewModel.load()
    }

    private fun setupList() {
        adapter = BudgetCategoryListAdapter()
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter

        legendAdapter = com.koshpal_android.koshpalapp.ui.budget.adapter.PieLegendAdapter()
        binding.rvLegend.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLegend.adapter = legendAdapter
    }

    private fun setupClicks() {
        binding.btnCreateBudget.setOnClickListener {
            startActivity(Intent(requireContext(), BudgetActivity::class.java))
        }
        binding.btnUpdateBudget.setOnClickListener {
            startActivity(Intent(requireContext(), BudgetActivity::class.java))
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.groupEmpty.visibility = if (state.budget == null) View.VISIBLE else View.GONE
                binding.groupContent.visibility = if (state.budget != null) View.VISIBLE else View.GONE

                state.budget?.let { b ->
                    // progress: spent vs total (spent = total - savings)
                    val spent = (b.totalBudget - b.savings).coerceAtLeast(0.0)
                    val pct = if (b.totalBudget > 0) ((spent / b.totalBudget) * 100).toInt() else 0
                    renderDonut(spent.toFloat(), (b.totalBudget - spent).toFloat())
                    binding.tvSavings.text = "₹${String.format("%,.0f", b.savings)} savings"
                    binding.tvTotalBudget.text = "₹${String.format("%,.0f", b.totalBudget)}"
                    binding.tvTotalSpent.text = "₹${String.format("%,.0f", spent)}"
                    binding.tvRemaining.text = "₹${String.format("%,.0f", (b.totalBudget - spent))}"
                    binding.tvBudgetTitle.text = "Your Budget"
                }

                adapter.submitList(state.categories)

                renderPie(state)
            }
        }
    }

    private fun renderPie(state: BudgetUiState) {
        val chart = binding.pieChart
        val categories = state.categories
        if (categories.isEmpty()) {
            chart.clear()
            return
        }
        
        // Use modern colors
        val colors = BudgetDetailsChartHelper.getModernColors()
        
        // Convert to chart data
        val categoryData = categories.mapIndexed { idx, cat ->
            BudgetDetailsChartHelper.CategoryData(
                label = cat.name,
                amount = cat.allocatedAmount,
                color = colors[idx % colors.size]
            )
        }
        
        // Setup modern donut chart with outside labels
        val legendItems = BudgetDetailsChartHelper.setupModernDonutChart(
            chart = chart,
            data = categoryData
        )
        
        // Update legend
        legendAdapter.submitList(legendItems)
    }

    private fun renderDonut(spent: Float, remaining: Float) {
        val chart = binding.donutUsage
        
        // Use modern chart helper for consistent styling
        val categoryData = listOf(
            BudgetDetailsChartHelper.CategoryData("Spent", spent.toDouble(), Color.parseColor("#EF4444")),
            BudgetDetailsChartHelper.CategoryData("Remaining", remaining.toDouble(), Color.parseColor("#10B981"))
        )
        
        BudgetDetailsChartHelper.setupModernDonutChart(
            chart = chart,
            data = categoryData
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


