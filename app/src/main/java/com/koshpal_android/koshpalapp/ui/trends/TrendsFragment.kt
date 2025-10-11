package com.koshpal_android.koshpalapp.ui.trends

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.koshpal_android.koshpalapp.databinding.FragmentTrendsBinding
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TrendsFragment : Fragment() {

    private var _binding: FragmentTrendsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var transactionRepository: TransactionRepository

    // Current selected month for detailed view
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    
    // Store month data for click handling
    private val monthDataList = mutableListOf<MonthData>()
    
    data class MonthData(
        val year: Int,
        val month: Int,
        val spending: Double,
        val income: Double,
        val label: String
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBarChart()
        setupBackButton()
        updateSelectedMonthDisplay()
        loadTrendsData()
    }

    private fun setupBackButton() {
        // Handle back button click to go to Home
        binding.btnBack.setOnClickListener {
            android.util.Log.d("TrendsFragment", "🔙 Back button clicked - navigating to Home")
            (activity as? HomeActivity)?.showHomeFragment()
        }
    }

    private fun setupBarChart() {
        val chart = binding.barChart
        
        // Chart styling to match the screenshot
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.setPinchZoom(false)
        chart.setScaleEnabled(false)
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        
        // Set up click listener for bars
        chart.setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
            override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                if (e != null) {
                    val monthIndex = e.x.toInt()
                    android.util.Log.d("TrendsFragment", "📊 Bar clicked: month index $monthIndex, amount: ${e.y}")
                    onMonthBarClicked(monthIndex)
                }
            }
            
            override fun onNothingSelected() {
                // Reset to current month when nothing is selected
                updateSelectedMonthDisplay()
                lifecycleScope.launch { loadCurrentMonthDetails() }
            }
        })
        
        // Remove right axis
        chart.axisRight.isEnabled = false
        
        // Style left axis
        chart.axisLeft.apply {
            setDrawGridLines(true)
            gridColor = Color.parseColor("#333333")
            textColor = Color.parseColor("#888888")
            textSize = 10f
            axisMinimum = 0f
            setDrawAxisLine(false)
        }
        
        // Style X axis
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(true)
            axisLineColor = Color.parseColor("#333333")
            textColor = Color.parseColor("#888888")
            textSize = 10f
            granularity = 1f
        }
        
        android.util.Log.d("TrendsFragment", "📊 Bar chart setup complete")
    }

    private fun updateSelectedMonthDisplay() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, selectedYear)
        calendar.set(Calendar.MONTH, selectedMonth)
        
        val monthFormat = SimpleDateFormat("MMM''yy", Locale.getDefault())
        binding.tvSelectedMonth.text = monthFormat.format(calendar.time)
        
        android.util.Log.d("TrendsFragment", "📅 Selected month display: ${binding.tvSelectedMonth.text}")
    }

    private fun loadTrendsData() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TrendsFragment", "📊 Loading trends data...")
                
                // Load last 6 months data for the chart
                loadMonthlyTrendsChart()
                
                // Load current month details
                loadCurrentMonthDetails()
                
            } catch (e: Exception) {
                android.util.Log.e("TrendsFragment", "Failed to load trends data: ${e.message}")
            }
        }
    }

    private suspend fun loadMonthlyTrendsChart() {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        monthDataList.clear()
        
        android.util.Log.d("TrendsFragment", "📊 Loading REAL transaction data for last 6 months...")
        
        // Get ALL transactions first
        val allTransactions = transactionRepository.getAllTransactionsOnce()
        android.util.Log.d("TrendsFragment", "📊 Total transactions in database: ${allTransactions.size}")
        
        // Debug: Print all transactions
        allTransactions.forEachIndexed { index, transaction ->
            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(transaction.date))
            android.util.Log.d("TrendsFragment", "📊 Transaction $index: $dateStr - ${transaction.type} - ₹${transaction.amount} - ${transaction.merchant}")
        }
        
        // Get last 6 months data
        for (i in 5 downTo 0) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -i)
            
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            
            // Calculate month start and end
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.timeInMillis
            
            // Filter transactions for this month
            val monthTransactions = allTransactions.filter { transaction ->
                transaction.date >= startOfMonth && transaction.date <= endOfMonth
            }
            
            android.util.Log.d("TrendsFragment", "📊 Month range: ${java.util.Date(startOfMonth)} to ${java.util.Date(endOfMonth)}")
            android.util.Log.d("TrendsFragment", "📊 Found ${monthTransactions.size} transactions in this month")
            
            // Calculate spending using same method as Categories
            val categorySpending = transactionRepository.getCurrentMonthCategorySpending(startOfMonth, endOfMonth)
            val monthSpending = categorySpending.sumOf { it.totalAmount }
            
            // Calculate income (CREDIT transactions) 
            val monthIncome = monthTransactions
                .filter { it.type == TransactionType.CREDIT }
                .sumOf { it.amount }
            
            android.util.Log.d("TrendsFragment", "📊 Categories method: ₹$monthSpending vs Direct method: ₹${monthTransactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }}")
            
            // Format label
            calendar.timeInMillis = startOfMonth
            val monthLabel = SimpleDateFormat("MMM'yy", Locale.getDefault()).format(calendar.time)
            labels.add(monthLabel)
            
            // Store month data for click handling
            monthDataList.add(MonthData(year, month, monthSpending, monthIncome, monthLabel))
            
            // Add to chart entries
            entries.add(BarEntry((5 - i).toFloat(), monthSpending.toFloat()))
            
            android.util.Log.d("TrendsFragment", "📊 $monthLabel: ${monthTransactions.size} transactions, Spending: ₹$monthSpending, Income: ₹$monthIncome")
        }
        
        android.util.Log.d("TrendsFragment", "📊 Chart entries created: ${entries.size}, Month data stored: ${monthDataList.size}")
        
        // Debug: Print all chart entries
        entries.forEachIndexed { index, entry ->
            android.util.Log.d("TrendsFragment", "📊 Chart Entry $index: X=${entry.x}, Y=₹${entry.y}")
        }
        
        // NO DUMMY DATA - Only show real transaction data
        android.util.Log.d("TrendsFragment", "📊 Using ONLY real transaction data - Total entries: ${entries.size}")
        
        // Create bar data set
        val dataSet = BarDataSet(entries, "Monthly Spending").apply {
            color = Color.parseColor("#4285F4") // Blue color like in screenshot
            setDrawValues(false)
            isHighlightEnabled = true
            highLightColor = Color.parseColor("#6200EE")
        }
        
        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        
        // Update chart
        binding.barChart.apply {
            data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size
            
            // Force refresh the chart
            notifyDataSetChanged()
            invalidate()
            
            // Animate after data is set
            animateY(1000)
            
            android.util.Log.d("TrendsFragment", "📊 Chart updated with ${entries.size} bars")
        }
        
        android.util.Log.d("TrendsFragment", "📊 Monthly trends chart updated with ${entries.size} months")
    }

    private fun onMonthBarClicked(monthIndex: Int) {
        if (monthIndex >= 0 && monthIndex < monthDataList.size) {
            val monthData = monthDataList[monthIndex]
            android.util.Log.d("TrendsFragment", "📊 Month bar clicked: ${monthData.label}")
            
            // Update selected month
            selectedYear = monthData.year
            selectedMonth = monthData.month
            
            // Update UI to show clicked month's data
            updateSelectedMonthDisplay()
            
            // Update spending and income cards with clicked month's data
            binding.tvSpendingAmount.text = "₹${String.format("%.2f", monthData.spending)}"
            binding.tvIncomeAmount.text = "₹${String.format("%.0f", monthData.income)}"
            
            android.util.Log.d("TrendsFragment", "💰 Clicked month details - Spending: ₹${monthData.spending}, Income: ₹${monthData.income}")
        }
    }

    private suspend fun loadCurrentMonthDetails() {
        android.util.Log.d("TrendsFragment", "💰 Loading current month details for ${selectedYear}-${selectedMonth + 1}")
        
        // Find current month data from stored list
        val currentMonthData = monthDataList.find { it.year == selectedYear && it.month == selectedMonth }
        
        if (currentMonthData != null) {
            // Use stored data
            binding.tvSpendingAmount.text = "₹${String.format("%.2f", currentMonthData.spending)}"
            binding.tvIncomeAmount.text = "₹${String.format("%.0f", currentMonthData.income)}"
            android.util.Log.d("TrendsFragment", "💰 Using stored data - Spending: ₹${currentMonthData.spending}, Income: ₹${currentMonthData.income}")
        } else {
            // Fallback: calculate for current month if not in stored data
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, selectedYear)
            calendar.set(Calendar.MONTH, selectedMonth)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.timeInMillis
            
            val allTransactions = transactionRepository.getAllTransactionsOnce()
            val monthTransactions = allTransactions.filter { it.date >= startOfMonth && it.date <= endOfMonth }
            
            val monthSpending = monthTransactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
            val monthIncome = monthTransactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
            
            binding.tvSpendingAmount.text = "₹${String.format("%.2f", monthSpending)}"
            binding.tvIncomeAmount.text = "₹${String.format("%.0f", monthIncome)}"
            android.util.Log.d("TrendsFragment", "💰 Calculated data - Spending: ₹$monthSpending, Income: ₹$monthIncome")
        }
    }

    override fun onResume() {
        super.onResume()
        loadTrendsData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
