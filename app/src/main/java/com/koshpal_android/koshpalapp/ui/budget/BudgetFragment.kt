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
        val entries = categories.map { com.github.mikephil.charting.data.PieEntry(it.allocatedAmount.toFloat(), it.name) }
        val colors = listOf(
                android.graphics.Color.parseColor("#8B5CF6"),
                android.graphics.Color.parseColor("#10B981"),
                android.graphics.Color.parseColor("#F59E0B"),
                android.graphics.Color.parseColor("#EF4444"),
                android.graphics.Color.parseColor("#6366F1"),
                android.graphics.Color.parseColor("#06B6D4")
            )
        val set = com.github.mikephil.charting.data.PieDataSet(entries, "").apply {
            setDrawValues(false)
            this.colors = colors
            sliceSpace = 2f
        }
        val data = com.github.mikephil.charting.data.PieData(set)
        chart.data = data
        chart.description.isEnabled = false
        chart.setUsePercentValues(true)
        chart.setDrawEntryLabels(false)
        chart.legend.isEnabled = false
        chart.setHoleColor(android.graphics.Color.TRANSPARENT)
        chart.transparentCircleRadius = 52f
        chart.holeRadius = 45f
        chart.invalidate()

        val legendItems = state.categories.mapIndexed { idx, c ->
            com.koshpal_android.koshpalapp.ui.budget.adapter.LegendItem(
                label = c.name,
                amount = c.allocatedAmount,
                color = colors[idx % colors.size]
            )
        }
        legendAdapter.submitList(legendItems)
    }

    private fun renderDonut(spent: Float, remaining: Float) {
        val chart = binding.donutUsage
        val entries = listOf(
            com.github.mikephil.charting.data.PieEntry(spent, "Spent"),
            com.github.mikephil.charting.data.PieEntry(remaining, "Remaining")
        )
        val set = com.github.mikephil.charting.data.PieDataSet(entries, "").apply {
            colors = listOf(
                android.graphics.Color.parseColor("#EF4444"),
                android.graphics.Color.parseColor("#10B981")
            )
            setDrawValues(false)
            sliceSpace = 2f
        }
        val data = com.github.mikephil.charting.data.PieData(set)
        chart.data = data
        chart.description.isEnabled = false
        chart.setUsePercentValues(true)
        chart.setDrawEntryLabels(false)
        chart.legend.isEnabled = false
        chart.setHoleColor(android.graphics.Color.TRANSPARENT)
        chart.transparentCircleRadius = 58f
        chart.holeRadius = 52f
        chart.invalidate()
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


