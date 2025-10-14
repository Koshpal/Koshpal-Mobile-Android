package com.koshpal_android.koshpalapp.ui.budget

import android.graphics.Color
import android.graphics.Typeface
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.koshpal_android.koshpalapp.ui.budget.adapter.LegendItem

object BudgetDetailsChartHelper {
    
    /**
     * Configures a modern donut chart with labels displayed outside the chart
     * @param chart The PieChart to configure
     * @param data List of category data with label, amount, and color
     * @return List of LegendItem for the RecyclerView
     */
    fun setupModernDonutChart(
        chart: PieChart,
        data: List<CategoryData>
    ): List<LegendItem> {
        if (data.isEmpty()) {
            chart.clear()
            return emptyList()
        }

        // Create pie entries
        val entries = data.map { PieEntry(it.amount.toFloat(), it.label) }

        // Create dataset
        val dataSet = PieDataSet(entries, "").apply {
            colors = data.map { it.color }
            sliceSpace = 3f
            selectionShift = 5f
            
            // Configure value labels to appear outside
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.3f
            valueLinePart2Length = 0.4f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            
            // Style the connecting lines
            valueLineColor = Color.parseColor("#9CA3AF")
            valueLineWidth = 1.5f
            
            // Enable value labels
            setDrawValues(true)
        }

        // Create custom value formatter to show currency amounts
        val valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "₹${String.format("%,.0f", value)}"
            }
        }

        // Create pie data
        val pieData = PieData(dataSet).apply {
            setValueFormatter(valueFormatter)
            setValueTextSize(12f)
            setValueTextColor(Color.parseColor("#1F2937"))
            setValueTypeface(Typeface.DEFAULT_BOLD)
        }

        // Configure chart appearance
        chart.apply {
            this.data = pieData
            
            // Disable description
            description.isEnabled = false
            
            // Disable percentage values (we're showing actual amounts)
            setUsePercentValues(false)
            
            // Disable entry labels (category names)
            setDrawEntryLabels(false)
            
            // Disable legend (we have custom legend below)
            legend.isEnabled = false
            
            // Configure the donut hole
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 65f
            transparentCircleRadius = 70f
            
            // Rotation settings
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            
            // Extra offsets for labels outside
            setExtraOffsets(20f, 10f, 20f, 10f)
            
            // Animation
            animateY(1000)
            
            // Refresh
            invalidate()
        }

        // Return legend items for RecyclerView
        return data.map { LegendItem(it.label, it.amount, it.color) }
    }

    /**
     * Sample colors matching the reference image
     */
    fun getModernColors(): List<Int> = listOf(
        Color.parseColor("#5EEAD4"), // Teal (like in reference)
        Color.parseColor("#FFC1CC"), // Light pink
        Color.parseColor("#FCD34D"), // Yellow/Gold
        Color.parseColor("#A78BFA"), // Purple
        Color.parseColor("#34D399"), // Green
        Color.parseColor("#F87171")  // Red
    )

    data class CategoryData(
        val label: String,
        val amount: Double,
        val color: Int
    )
}
