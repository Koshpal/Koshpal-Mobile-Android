package com.koshpal_android.koshpalapp.ui.insights

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.FragmentInsightsBinding
import com.koshpal_android.koshpalapp.model.FinancialInsight
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class InsightsFragment : Fragment() {
    
    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: InsightsViewModel by viewModels()
    private lateinit var recommendationsAdapter: RecommendationsAdapter
    
    private val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupLineChart()
        observeViewModel()
        
        viewModel.loadFinancialInsights()
    }
    
    private fun setupRecyclerView() {
        recommendationsAdapter = RecommendationsAdapter()
        binding.rvRecommendations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recommendationsAdapter
        }
    }
    
    private fun setupLineChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            
            // X-axis configuration
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.parseColor("#666666")
                textSize = 10f
            }
            
            // Y-axis configuration
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#666666")
                textSize = 10f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = true
            
            animateX(1000)
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentInsight.collect { insight ->
                insight?.let { updateCurrentInsight(it) }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.trendData.collect { trends ->
                updateTrendChart(trends)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recommendations.collect { recommendations ->
                recommendationsAdapter.submitList(recommendations)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.monthlyComparison.collect { comparison ->
                updateMonthlyComparison(comparison)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun updateCurrentInsight(insight: FinancialInsight) {
        binding.apply {
            // Health Score
            tvHealthScore.text = insight.healthScore.toString()
            progressHealthScore.progress = insight.healthScore
            
            val (status, statusColor) = when {
                insight.healthScore >= 80 -> "Excellent" to R.color.success
                insight.healthScore >= 60 -> "Good" to R.color.primary
                insight.healthScore >= 40 -> "Fair" to R.color.warning
                else -> "Poor" to R.color.error
            }
            
            tvHealthStatus.text = status
            tvHealthStatus.setTextColor(resources.getColor(statusColor, null))
            tvHealthDescription.text = getHealthDescription(insight.healthScore)
            
            // Income vs Expense
            tvTotalIncome.text = insight.getFormattedIncome()
            tvTotalExpense.text = insight.getFormattedExpense()
            tvNetSavings.text = insight.getFormattedSavings()
            tvSavingsRate.text = "(${String.format("%.1f", insight.savingsRate)}%)"
            
            // Progress bars (normalized to 100)
            val maxAmount = maxOf(insight.totalIncome, insight.totalExpense)
            if (maxAmount > 0) {
                progressIncome.progress = (insight.totalIncome / maxAmount * 100).toInt()
                progressExpense.progress = (insight.totalExpense / maxAmount * 100).toInt()
            }
        }
    }
    
    private fun updateTrendChart(trends: List<MonthlyTrend>) {
        if (trends.isEmpty()) return
        
        val incomeEntries = mutableListOf<Entry>()
        val expenseEntries = mutableListOf<Entry>()
        val months = mutableListOf<String>()
        
        trends.forEachIndexed { index, trend ->
            incomeEntries.add(Entry(index.toFloat(), trend.income.toFloat()))
            expenseEntries.add(Entry(index.toFloat(), trend.expense.toFloat()))
            
            val calendar = Calendar.getInstance()
            calendar.set(trend.year, trend.month - 1, 1)
            months.add(monthFormatter.format(calendar.time))
        }
        
        val incomeDataSet = LineDataSet(incomeEntries, "Income").apply {
            color = Color.parseColor("#4CAF50")
            setCircleColor(Color.parseColor("#4CAF50"))
            lineWidth = 3f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 0f
        }
        
        val expenseDataSet = LineDataSet(expenseEntries, "Expenses").apply {
            color = Color.parseColor("#F44336")
            setCircleColor(Color.parseColor("#F44336"))
            lineWidth = 3f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 0f
        }
        
        val lineData = LineData(incomeDataSet, expenseDataSet)
        
        binding.lineChart.apply {
            data = lineData
            xAxis.valueFormatter = IndexAxisValueFormatter(months)
            invalidate()
        }
    }
    
    private fun updateMonthlyComparison(comparison: MonthlyComparison) {
        binding.apply {
            tvThisMonthSpending.text = "₹${String.format("%.0f", comparison.thisMonth)}"
            tvLastMonthSpending.text = "₹${String.format("%.0f", comparison.lastMonth)}"
            
            val changePercentage = if (comparison.lastMonth > 0) {
                ((comparison.thisMonth - comparison.lastMonth) / comparison.lastMonth * 100)
            } else 0.0
            
            val isIncrease = changePercentage > 0
            
            tvComparisonPercentage.text = "${if (isIncrease) "+" else ""}${String.format("%.1f", changePercentage)}%"
            
            val (arrowIcon, color) = if (isIncrease) {
                R.drawable.ic_trending_up to R.color.error
            } else {
                R.drawable.ic_trending_down to R.color.success
            }
            
            ivComparisonArrow.setImageResource(arrowIcon)
            ivComparisonArrow.setColorFilter(resources.getColor(color, null))
            tvComparisonPercentage.setTextColor(resources.getColor(color, null))
        }
    }
    
    private fun getHealthDescription(score: Int): String {
        return when {
            score >= 80 -> "You're managing your finances excellently! Keep up the great work."
            score >= 60 -> "Good financial management. Consider optimizing your spending in some areas."
            score >= 40 -> "Your finances need attention. Focus on reducing expenses and increasing savings."
            else -> "Your financial health needs immediate improvement. Consider creating a budget plan."
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
