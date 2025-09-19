package com.koshpal_android.koshpalapp.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.koshpal_android.koshpalapp.databinding.FragmentDashboardBinding
import com.koshpal_android.koshpalapp.model.CategorySpendingData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class DashboardFragment : Fragment(), OnChartValueSelectedListener {
    
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DashboardViewModel by viewModels()
    
    private lateinit var categoryBreakdownAdapter: CategoryBreakdownAdapter
    private lateinit var topMerchantsAdapter: TopMerchantsAdapter
    
    private val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupPieChart()
        setupClickListeners()
        observeViewModel()
        
        // Load current month data
        viewModel.loadCurrentMonthData()
    }
    
    private fun setupRecyclerViews() {
        categoryBreakdownAdapter = CategoryBreakdownAdapter()
        binding.rvCategoryBreakdown.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryBreakdownAdapter
        }
        
        topMerchantsAdapter = TopMerchantsAdapter()
        binding.rvTopMerchants.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = topMerchantsAdapter
        }
    }
    
    private fun setupPieChart() {
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            
            dragDecelerationFrictionCoef = 0.95f
            
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            
            holeRadius = 58f
            transparentCircleRadius = 61f
            
            setDrawCenterText(true)
            centerText = "Monthly\nSpending"
            setCenterTextSize(16f)
            setCenterTextColor(Color.parseColor("#333333"))
            
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            
            setOnChartValueSelectedListener(this@DashboardFragment)
            
            animateY(1400, Easing.EaseInOutQuad)
            
            legend.isEnabled = false
        }
    }
    
    private fun setupClickListeners() {
        binding.btnPreviousMonth.setOnClickListener {
            viewModel.navigateToPreviousMonth()
        }
        
        binding.btnNextMonth.setOnClickListener {
            viewModel.navigateToNextMonth()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentMonth.collect { (month, year) ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month - 1, 1)
                binding.tvCurrentMonth.text = monthFormatter.format(calendar.time)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.monthlyData.collect { data ->
                updateUI(data)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categorySpending.collect { categories ->
                updatePieChart(categories)
                categoryBreakdownAdapter.submitList(categories)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.topMerchants.collect { merchants ->
                topMerchantsAdapter.submitList(merchants)
            }
        }
    }
    
    private fun updateUI(data: MonthlyDashboardData) {
        binding.apply {
            tvTotalSpending.text = "₹${String.format("%.2f", data.totalExpense)}"
            tvTotalIncome.text = "₹${String.format("%.2f", data.totalIncome)}"
            tvTotalSavings.text = "₹${String.format("%.2f", data.totalSavings)}"
        }
    }
    
    private fun updatePieChart(categories: List<CategorySpendingData>) {
        if (categories.isEmpty()) {
            binding.pieChart.clear()
            return
        }
        
        val entries = categories.map { category ->
            PieEntry(category.amount.toFloat(), category.categoryName)
        }
        
        val colors = categories.map { category ->
            try {
                Color.parseColor(category.color)
            } catch (e: IllegalArgumentException) {
                Color.parseColor("#607D8B") // Default color
            }
        }
        
        val dataSet = PieDataSet(entries, "Spending Categories").apply {
            setDrawIcons(false)
            sliceSpace = 3f
            iconsOffset = com.github.mikephil.charting.utils.MPPointF.getInstance(0f, 40f)
            selectionShift = 5f
            setColors(colors)
            
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.2f
            valueLinePart2Length = 0.4f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        }
        
        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter())
            setValueTextSize(11f)
            setValueTextColor(Color.parseColor("#333333"))
        }
        
        binding.pieChart.data = data
        binding.pieChart.highlightValues(null)
        binding.pieChart.invalidate()
    }
    
    override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: Highlight?) {
        if (e == null) return
        
        val pieEntry = e as PieEntry
        val categoryName = pieEntry.label
        val amount = pieEntry.value
        
        // Update center text with selected category info
        binding.pieChart.centerText = "$categoryName\n₹${String.format("%.0f", amount)}"
    }
    
    override fun onNothingSelected() {
        // Reset center text
        binding.pieChart.centerText = "Monthly\nSpending"
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
