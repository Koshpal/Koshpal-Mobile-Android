# Month-over-Month Spending Comparison Feature

## Overview
Implemented a comprehensive "Previous Month vs Current Month Spending Comparison" feature in InsightsFragment using Room database, MPAndroidChart, and MVVM architecture.

## 🎯 Features Implemented

### 1. **Data Fetching (Room DB)**
- ✅ Added DAO queries in `TransactionDao.kt`:
  - `getMonthlySpendingByCategory()` - Groups transactions by category and sums amounts
  - `getTransactionsByCategoryAndMonth()` - Fetches detailed transactions for drill-down
- ✅ Queries filter by DEBIT type, date range, and category
- ✅ Returns aggregated spending data for current and previous months

### 2. **Visualization**
- ✅ **Grouped Bar Chart** using MPAndroidChart:
  - Two bars per category (Current Month in blue, Previous Month in gray)
  - Smooth fade-in animation (800ms)
  - Top 6 categories displayed
  - Interactive chart - tap bars to drill down
- ✅ **Toggle Button**: Switch between absolute ₹ values and % difference
- ✅ **Color-coded percentage labels**:
  - Red (↑) for increases
  - Green (↓) for decreases

### 3. **Interactive Features**
- ✅ **Click to Drill-down**: Opens `CategoryDrilldownDialog` with:
  - Tabbed interface (Current Month / Previous Month)
  - Transaction list with totals
  - Swipe between months
- ✅ **Swipe-to-refresh**: Reloads all data from Room DB
- ✅ **Component-level shimmer**: Only chart shows loading placeholder (not fullscreen)

### 4. **Smart Insights Section**
- ✅ Auto-generated insights card with:
  - Top 3 categories with largest increases
  - Top 3 categories with largest decreases
  - Overall savings comparison
- ✅ Example: *"Food spending ↑ 22% this month, Travel ↓ 18%, Overall saving ↑ ₹1,200 compared to last month."*
- ✅ Interactive insight items - tap to view detailed transactions

### 5. **UI/UX (Material 3 Design)**
- ✅ Rounded corners (20dp), soft shadows, pastel colors
- ✅ Smooth fade-in animations (200ms shimmer fade-out, 300ms content fade-in)
- ✅ Component-level shimmer (follows existing pattern)
- ✅ Responsive layout with proper spacing
- ✅ Category icons and color-coded badges

### 6. **Code Structure (MVVM Pattern)**
- ✅ **ViewModel**: `InsightsViewModel.kt`
  - Fetches data from DAO
  - Processes month-over-month comparison
  - Generates smart insights
  - Exposes StateFlows for reactive UI
- ✅ **Fragment**: `InsightsFragment.kt`
  - Observes ViewModel data
  - Renders chart and insights
  - Handles user interactions
- ✅ **Repository Pattern**: Uses existing `TransactionRepository`

---

## 📁 Files Created

### Data Layer
1. **`TransactionDao.kt`** (Modified)
   - Added `getMonthlySpendingByCategory()`
   - Added `getTransactionsByCategoryAndMonth()`

### Model Classes
2. **`MonthComparisonData.kt`**
   - Data class for category comparison
   - Includes percentage change, absolute change, colors, icons

3. **`MonthComparisonInsight.kt`**
   - Smart insights data model
   - Top increases/decreases, overall savings

### ViewModel
4. **`InsightsViewModel.kt`**
   - MVVM architecture
   - Room DB queries
   - Data processing logic
   - Insights generation

### UI Layouts
5. **`shimmer_month_comparison.xml`**
   - Component-level shimmer placeholder
   - Matches bar chart structure

6. **`card_month_comparison.xml`**
   - Main comparison card with:
     - Shimmer frame layout
     - Bar chart
     - Toggle button
     - Smart insights section

7. **`item_top_change.xml`**
   - List item for top increases/decreases
   - Category icon, name, amount, percentage badge

8. **`dialog_category_drilldown.xml`**
   - Full-screen dialog with tabs
   - ViewPager2 for month switching

9. **`fragment_transaction_list.xml`**
   - Transaction list for drill-down
   - Summary card with totals
   - Empty state

### Fragment/Dialog
10. **`CategoryDrilldownDialog.kt`**
    - Dialog with ViewPager2
    - Shows current/previous month transactions
    - Includes `TransactionListFragment` inner class

### Updated Files
11. **`fragment_insights.xml`** (Modified)
    - Added SwipeRefreshLayout
    - Included comparison card section
    - Updated layout structure

12. **`InsightsFragment.kt`** (Modified)
    - Added ViewModel integration
    - Chart rendering logic
    - Shimmer control methods
    - Drill-down handlers
    - Swipe-to-refresh

---

## 🔧 Technical Details

### Architecture
```
┌─────────────────────────────────────────┐
│          InsightsFragment               │
│  (Observes ViewModel, Renders UI)       │
└──────────────┬──────────────────────────┘
               │
               │ observes StateFlows
               ▼
┌─────────────────────────────────────────┐
│        InsightsViewModel                │
│  (Processes data, generates insights)   │
└──────────────┬──────────────────────────┘
               │
               │ queries
               ▼
┌─────────────────────────────────────────┐
│         TransactionDao                  │
│  (Room DB queries)                      │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│      Room Database (transactions)       │
└─────────────────────────────────────────┘
```

### Data Flow
1. **ViewModel** calls `loadMonthComparisonData()`
2. Fetches current and previous month spending from Room DB
3. Processes data into `MonthComparisonData` objects
4. Generates `MonthComparisonInsight` with smart text
5. Emits via StateFlows
6. **Fragment** observes and renders chart + insights
7. User taps chart bar → opens drill-down dialog
8. Dialog fetches detailed transactions from Room DB

### Performance Optimizations
- ✅ Parallel queries using coroutines
- ✅ Data caching (5-minute TTL for transactions)
- ✅ Component-level loading (not fullscreen)
- ✅ Efficient Room queries with aggregations
- ✅ Lazy loading in ViewPager2

---

## 🎨 UI Components

### Bar Chart Configuration
```kotlin
- Chart Library: MPAndroidChart v3.1.0
- Chart Type: Grouped Bar Chart
- Bars per Group: 2 (Previous/Current)
- Group Space: 0.3f
- Bar Space: 0.05f
- Bar Width: 0.3f
- Animation: 800ms ease-in-out-quad
- Colors:
  - Previous Month: #B0BEC5 (Gray)
  - Current Month: #5C6BC0 (Blue)
```

### Shimmer Configuration
```kotlin
- Duration: 1500ms
- Direction: Left to Right
- Base Alpha: 0.7
- Highlight Alpha: 0.6
- Shape: Linear
```

### Color Scheme
```
Increases (Red):
  - Background: #FFEBEE
  - Text: #D32F2F

Decreases (Green):
  - Background: #E8F5E9
  - Text: #388E3C
```

---

## 🚀 How to Use

### For Users
1. Open **Insights** tab
2. Scroll to **"Spending Trends"** section
3. View bar chart comparing current vs previous month
4. Tap **₹** button to toggle between amounts and percentages
5. **Tap any bar** to see detailed transactions for that category
6. View **Smart Insights** card for automatic analysis
7. **Pull down** to refresh data

### For Developers
```kotlin
// Load comparison data
viewModel.loadMonthComparisonData()

// Observe data
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.monthComparisonData.collect { data ->
        // Render chart
        renderMonthComparisonChart(data, showPercentages)
    }
}

// Show drill-down
showCategoryDrilldown(monthComparisonData)
```

---

## 📊 Smart Insights Logic

### Algorithm
1. **Calculate totals**: Sum all category spending for both months
2. **Identify top changes**:
   - Filter categories with ≥5% change
   - Sort by absolute percentage change
   - Take top 3 increases and top 3 decreases
3. **Generate insight text**:
   - Top increase: "Food spending ↑ 22%"
   - Top decrease: "Travel ↓ 18%"
   - Overall: "Overall saving ↑ ₹1,200 compared to last month"

### Example Output
```
"Food spending ↑ 22% this month, Travel ↓ 18%, Overall saving ↑ ₹1,200 compared to last month."

Top Increases:
- Food: ₹1,500 more (↑ 22%)
- Entertainment: ₹800 more (↑ 15%)
- Shopping: ₹600 more (↑ 12%)

Top Decreases:
- Travel: ₹1,200 less (↓ 18%)
- Bills: ₹400 less (↓ 8%)
```

---

## 🐛 Error Handling
- ✅ Try-catch blocks around Room queries
- ✅ Null safety for empty data states
- ✅ Graceful fallback for missing categories
- ✅ Empty state UI when no transactions
- ✅ Loading states with shimmer

---

## ✅ Requirements Checklist

| Requirement | Status | Implementation |
|------------|--------|----------------|
| Room DB queries | ✅ | `getMonthlySpendingByCategory()` |
| Grouped bar chart | ✅ | MPAndroidChart with 2 bars/category |
| Toggle ₹ vs % | ✅ | Button in header |
| Percentage labels | ✅ | Color-coded (red ↑ / green ↓) |
| Click to drill-down | ✅ | `CategoryDrilldownDialog` |
| Swipe-to-refresh | ✅ | `SwipeRefreshLayout` |
| Shimmer loading | ✅ | Component-level shimmer |
| Smart insights | ✅ | Auto-generated text + top changes |
| Material 3 UI | ✅ | Rounded corners, shadows, animations |
| MVVM pattern | ✅ | ViewModel + StateFlows |

---

## 🎯 Key Benefits

1. **Data-Driven**: Uses actual Room DB transactions (not hardcoded)
2. **Reactive**: StateFlows ensure UI always reflects latest data
3. **Performant**: Component-level shimmer, caching, parallel queries
4. **Interactive**: Tap to drill down, swipe to refresh, toggle views
5. **Insightful**: Auto-generated analysis saves user time
6. **Modern UI**: Follows Material 3 design guidelines
7. **Maintainable**: Clean MVVM architecture, separation of concerns

---

## 📝 Notes

- **Dependencies**: MPAndroidChart v3.1.0 already in `build.gradle.kts`
- **Database**: Uses existing `transactions` table in Room DB
- **Category Mapping**: Uses existing category icons/colors from app
- **Shimmer Pattern**: Follows existing component-level shimmer approach
- **Navigation**: Drill-down uses DialogFragment (not new Activity)

---

## 🔮 Future Enhancements (Optional)

- Add month selector to compare any two months
- Export comparison as PDF/image
- Add trend lines showing 3+ months
- Category-wise budget vs actual overlay
- Push notifications for significant spending changes
- Machine learning predictions for next month

---

**Implementation Complete! 🎉**
All features working as specified with modern Material 3 UI and MVVM architecture.
