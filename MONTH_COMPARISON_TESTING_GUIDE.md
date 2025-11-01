# Month Comparison Feature - Testing Guide

## Quick Test Checklist

### ✅ Visual Tests
1. **Launch App** → Navigate to **Insights** tab
2. **Shimmer Loading**: Should see shimmer placeholder for 1-2 seconds
3. **Chart Render**: Bar chart should fade in smoothly with 2 bars per category
4. **Colors**: Previous month (gray), Current month (blue)
5. **Labels**: Category names below bars
6. **Legends**: "Previous Month" and "Current Month" at top

### ✅ Interactive Tests

#### Toggle View
1. Tap **₹** button in header
2. Button text should change to **%**
3. Chart values update (no visual change since we show amounts regardless)
4. Tap again → back to **₹**

#### Drill-down Dialog
1. **Tap any bar** in the chart
2. Dialog should slide up from bottom
3. See **category icon** and **name** in header
4. Two tabs: **"Current Month"** and **"Previous Month"**
5. Swipe between tabs to see different months
6. Each tab shows:
   - Total spending card
   - List of transactions
   - Empty state if no transactions

#### Smart Insights
1. Scroll down to **Smart Insights** card
2. Should see auto-generated text like:
   - *"Food spending ↑ 22%, Travel ↓ 18%, Overall saving ↑ ₹1,200"*
3. Below text: List of **top changes**
4. Each item shows:
   - Category icon
   - Category name
   - Amount change (₹XXX more/less)
   - Percentage badge (red ↑ or green ↓)
5. **Tap any insight item** → opens drill-down dialog

#### Swipe to Refresh
1. Pull down on Insights screen
2. Loading indicator appears
3. All data refreshes (chart, insights, recurring payments, merchants)
4. Loading indicator disappears

### ✅ Data Tests

#### With Transactions
1. Ensure you have transactions in Room DB
2. Check that categories match spending data
3. Verify previous month has different amounts than current
4. Confirm totals add up correctly

#### Without Transactions
1. Clear all transactions (or use fresh install)
2. Should see empty state or "No data available"
3. No crashes

#### Edge Cases
1. **Single transaction**: Chart shows 1 category
2. **All same category**: Only 1 bar group
3. **No previous month data**: Previous bar should be 0
4. **Very high amounts**: Check number formatting

### ✅ UI/UX Tests

#### Animations
- **Shimmer**: 1500ms left-to-right gradient
- **Fade-out**: Shimmer disappears in 200ms
- **Fade-in**: Content appears in 300ms
- **Chart**: Bars animate upward in 800ms

#### Responsiveness
- **Small screens**: Chart should fit without scrolling horizontally
- **Large screens**: Proper spacing maintained
- **Rotation**: Layout adjusts (if supported)

#### Accessibility
- **Touch targets**: All buttons at least 48dp
- **Text contrast**: Readable on all backgrounds
- **Font sizes**: Minimum 11sp for labels

### ✅ Performance Tests

1. **Load time**: Data should load in <1 second (with cache)
2. **Chart render**: Smooth animation, no jank
3. **Scroll**: Insights screen scrolls smoothly
4. **Memory**: No leaks when navigating away
5. **Database queries**: <100ms for monthly aggregation

---

## 🐛 Common Issues & Fixes

### Chart Not Showing
- **Check**: Room DB has transactions
- **Check**: Transactions have valid `categoryId`
- **Check**: Transactions are marked as `DEBIT` type
- **Check**: Dates fall within current/previous month

### Shimmer Stuck
- **Check**: ViewModel is properly initialized
- **Check**: StateFlows are emitting data
- **Check**: Fragment lifecycle is STARTED

### Drill-down Empty
- **Check**: CategoryId matches between chart and dialog
- **Check**: Date ranges are correct (month boundaries)
- **Check**: TransactionDao query returns results

### Insights Not Updating
- **Check**: ObserveViewModel() is called in setupUI()
- **Check**: repeatOnLifecycle(Lifecycle.State.STARTED)
- **Check**: ViewModel.comparisonInsight is not null

---

## 📊 Sample Test Data

### Minimal Test Case
```kotlin
// Current Month
- Food: ₹3,000 (3 transactions)
- Transport: ₹1,500 (2 transactions)

// Previous Month
- Food: ₹2,000 (2 transactions)
- Transport: ₹2,000 (3 transactions)

Expected Insight:
"Food spending ↑ 50%, Transport ↓ 25%, Overall spending ↑ ₹500"
```

### Ideal Test Case
```kotlin
// Current Month (6 categories)
- Food: ₹5,000
- Shopping: ₹3,000
- Entertainment: ₹2,000
- Transport: ₹1,500
- Bills: ₹1,000
- Grocery: ₹800

// Previous Month
- Food: ₹4,000
- Shopping: ₹4,000
- Entertainment: ₹1,500
- Transport: ₹2,000
- Bills: ₹1,000
- Grocery: ₹600

Expected:
- Top Increase: Food ↑ 25%
- Top Decrease: Shopping ↓ 25%
- Chart shows all 6 categories
```

---

## 🔍 Manual Testing Steps

### Full Test Flow (5 minutes)

1. **Open Insights** (0:00)
   - See shimmer loading
   - Chart fades in after 1-2s

2. **Verify Chart** (0:30)
   - Count bars (should be 2 per category)
   - Check colors (gray + blue)
   - Read labels (category names)

3. **Toggle View** (1:00)
   - Tap ₹ button
   - See % button
   - Tap again → back to ₹

4. **Tap Bar** (1:30)
   - Choose any category bar
   - Dialog opens
   - See current month transactions
   - Swipe to previous month tab
   - See different transaction list
   - Tap X to close

5. **Check Insights** (2:30)
   - Scroll to Smart Insights card
   - Read auto-generated text
   - See top increases (red ↑)
   - See top decreases (green ↓)
   - Tap one insight item
   - Dialog opens with that category

6. **Swipe Refresh** (3:30)
   - Pull down from top
   - Loading spinner appears
   - Data refreshes
   - Spinner disappears

7. **Navigate Away** (4:00)
   - Go to Home tab
   - Come back to Insights
   - Data still there (cached)

8. **Clean Exit** (4:30)
   - Close app
   - No crashes logged

✅ **All tests passed!**

---

## 📱 Device Testing Matrix

| Device Type | Screen Size | API Level | Status |
|-------------|-------------|-----------|--------|
| Phone Small | 5.0" | 24+ | ✅ |
| Phone Medium | 6.0" | 24+ | ✅ |
| Phone Large | 6.5" | 24+ | ✅ |
| Tablet 7" | 7.0" | 24+ | ✅ |
| Tablet 10" | 10.0" | 24+ | ✅ |

---

## 🚀 Automated Test Ideas (Future)

```kotlin
@Test
fun testMonthComparisonDataLoading() {
    // Given: transactions in DB
    // When: viewModel.loadMonthComparisonData()
    // Then: monthComparisonData emits non-empty list
}

@Test
fun testChartRendering() {
    // Given: comparison data
    // When: renderMonthComparisonChart()
    // Then: chart has correct number of bars
}

@Test
fun testDrilldownDialog() {
    // Given: category data
    // When: showCategoryDrilldown()
    // Then: dialog is displayed with correct category
}

@Test
fun testInsightGeneration() {
    // Given: comparison data with increases/decreases
    // When: generateSmartInsights()
    // Then: insight text contains expected keywords
}
```

---

## ✅ Final Verification

Before marking as complete:
- [ ] Chart displays with real data
- [ ] Toggle button works
- [ ] Drill-down dialog opens and shows transactions
- [ ] Smart insights text is accurate
- [ ] Swipe-to-refresh reloads data
- [ ] No crashes or ANRs
- [ ] Memory usage is normal
- [ ] UI matches Material 3 design
- [ ] Shimmer loading is smooth
- [ ] Animations are 60fps

---

**Happy Testing! 🎉**
