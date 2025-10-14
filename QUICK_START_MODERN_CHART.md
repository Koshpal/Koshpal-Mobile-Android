# 🚀 Quick Start: Modern Donut Chart

## Copy-Paste Ready Code

### 1️⃣ Basic Usage (3 lines of code)

```kotlin
val colors = BudgetDetailsChartHelper.getModernColors()
val data = listOf(
    BudgetDetailsChartHelper.CategoryData("Food", 56685.0, colors[0]),
    BudgetDetailsChartHelper.CategoryData("Shopping", 19839.0, colors[1]),
    BudgetDetailsChartHelper.CategoryData("Transport", 17950.0, colors[2])
)
BudgetDetailsChartHelper.setupModernDonutChart(binding.chartDetails, data)
```

### 2️⃣ With Legend (Add 2 more lines)

```kotlin
val colors = BudgetDetailsChartHelper.getModernColors()
val data = listOf(
    BudgetDetailsChartHelper.CategoryData("Food", 56685.0, colors[0]),
    BudgetDetailsChartHelper.CategoryData("Shopping", 19839.0, colors[1]),
    BudgetDetailsChartHelper.CategoryData("Transport", 17950.0, colors[2])
)

// Setup chart and get legend items
val legendItems = BudgetDetailsChartHelper.setupModernDonutChart(
    chart = binding.chartDetails,
    data = data
)

// Display legend
legendAdapter.submitList(legendItems)

// Update total in center
val total = data.sumOf { it.amount }
binding.tvTotalAmount.text = "₹${String.format("%,d", total.toInt())}"
```

### 3️⃣ Update Existing BudgetFragment.kt

**Find this method:**
```kotlin
private fun renderPie(state: BudgetUiState) {
    val chart = binding.pieChart
    val categories = state.categories
    // ... existing code ...
}
```

**Replace with:**
```kotlin
private fun renderPie(state: BudgetUiState) {
    val chart = binding.pieChart
    val categories = state.categories
    if (categories.isEmpty()) {
        chart.clear()
        return
    }
    
    val colors = BudgetDetailsChartHelper.getModernColors()
    val categoryData = categories.mapIndexed { idx, cat ->
        BudgetDetailsChartHelper.CategoryData(
            label = cat.name,
            amount = cat.allocatedAmount,
            color = colors[idx % colors.size]
        )
    }
    
    val legendItems = BudgetDetailsChartHelper.setupModernDonutChart(
        chart = chart,
        data = categoryData
    )
    
    legendAdapter.submitList(legendItems)
}
```

**Add this import at the top:**
```kotlin
import com.koshpal_android.koshpalapp.ui.budget.BudgetDetailsChartHelper
```

### 4️⃣ Show as New Fragment

```kotlin
// Create and show the fragment
val fragment = BudgetDetailsFragment.newInstance()
supportFragmentManager.beginTransaction()
    .replace(R.id.container, fragment)
    .commit()
```

---

## 📦 What You Get

✅ **Modern donut chart** with colored segments  
✅ **Values displayed outside** with connecting lines  
✅ **Center total display** in large, bold text  
✅ **Card-based legend** below the chart  
✅ **Smooth animations** on load  
✅ **Professional colors** (teal, pink, gold, etc.)  

---

## 🎨 Color Reference

```kotlin
val colors = BudgetDetailsChartHelper.getModernColors()
// Returns:
// [0] #5EEAD4 - Teal
// [1] #FFC1CC - Light Pink  
// [2] #FCD34D - Gold
// [3] #A78BFA - Purple
// [4] #34D399 - Green
// [5] #F87171 - Red
```

---

## 🔧 Common Customizations

### Change Chart Size
In `fragment_budget_details.xml`:
```xml
<com.github.mikephil.charting.charts.PieChart
    android:layout_width="350dp"   <!-- Change this -->
    android:layout_height="350dp"  <!-- Change this -->
    .../>
```

### Change Donut Hole Size
In `BudgetDetailsChartHelper.kt`:
```kotlin
holeRadius = 70f  // Bigger number = larger hole
```

### Custom Colors
```kotlin
val myColors = listOf(
    Color.parseColor("#FF6B6B"),  // Your color 1
    Color.parseColor("#4ECDC4"),  // Your color 2
    Color.parseColor("#45B7D1")   // Your color 3
)

val data = listOf(
    BudgetDetailsChartHelper.CategoryData("Cat1", 1000.0, myColors[0]),
    BudgetDetailsChartHelper.CategoryData("Cat2", 2000.0, myColors[1])
)
```

---

## ⚡ Complete Example

```kotlin
class MyFragment : Fragment() {
    private lateinit var binding: FragmentBudgetDetailsBinding
    private lateinit var legendAdapter: PieLegendAdapter
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup legend
        legendAdapter = PieLegendAdapter()
        binding.rvCategoryLegend.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategoryLegend.adapter = legendAdapter
        
        // Load data
        loadChartData()
    }
    
    private fun loadChartData() {
        // Your data (from ViewModel, database, etc.)
        val categories = listOf(
            Triple("Food & Dining", 56685.0, 0),
            Triple("Shopping", 19839.0, 1),
            Triple("Transport", 17950.0, 2)
        )
        
        // Convert to chart data
        val colors = BudgetDetailsChartHelper.getModernColors()
        val chartData = categories.map { (name, amount, colorIdx) ->
            BudgetDetailsChartHelper.CategoryData(name, amount, colors[colorIdx])
        }
        
        // Calculate total
        val total = chartData.sumOf { it.amount }
        binding.tvTotalAmount.text = "₹${String.format("%,d", total.toInt())}"
        
        // Setup chart
        val legendItems = BudgetDetailsChartHelper.setupModernDonutChart(
            chart = binding.chartDetails,
            data = chartData
        )
        
        // Update legend
        legendAdapter.submitList(legendItems)
    }
}
```

---

## 📱 Test It Now

1. **Copy** the `BudgetDetailsFragment.kt` file (already created)
2. **Add** it to your layout/navigation
3. **Run** the app
4. **See** the modern chart with sample data

That's it! 🎉

---

## 📚 Need More Details?

- **Full Guide**: `MODERN_DONUT_CHART_GUIDE.md`
- **Summary**: `MODERN_CHART_IMPLEMENTATION_SUMMARY.md`
- **Integration**: `README_CHART_INTEGRATION.md`
