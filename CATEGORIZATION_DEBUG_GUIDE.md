# Transaction Categorization Debug Guide

## 🔍 Problem Analysis

User reports that **transactions categorized in TransactionsFragment are not appearing in the Categories section**.

## ✅ Enhanced Debugging Added

I've added comprehensive logging to trace the entire categorization flow from save to display.

---

## 📊 Debug Logs Added

### 1. **TransactionsFragment.kt** - Categorization Process

When you categorize a transaction, you'll now see:

```
🔄 ===== STARTING CATEGORIZATION =====
📝 Transaction ID: {id}
📝 Merchant: {merchant name}
📝 Old Category: {old category}
📝 New Category: {new category} ({category name})
📝 Amount: ₹{amount}
📝 Date: {date}
✅ Update completed - Rows affected: {1 or 0}
🔍 Verification - categoryId after update: {category id}
🔄 Categories fragment refresh triggered
```

**What to Check:**
- ❌ If "Rows affected: 0" → **Update is FAILING**
- ✅ If "Rows affected: 1" → Update successful
- Check if the verified categoryId matches what you set

---

### 2. **CategoriesFragment.kt** - Data Loading

When Categories fragment loads, you'll now see:

```
🚀 ===== loadCategoryData() STARTED =====
📊 DEBUG: Total transactions in DB: {count}
📊 DEBUG: Categorized transactions: {count}
   📌 Category 'food': {count} txns, ₹{amount}
   📌 Category 'transport': {count} txns, ₹{amount}
📊 ===== QUERYING CATEGORY SPENDING =====
📅 Date Range: {start} to {end}
📊 Selected month category spending: {count} categories
   💰 food ('Food') -> ₹{amount}
```

**What to Check:**
- Look at **"Total transactions in DB"** vs **"Categorized transactions"**
- If categorized count is 0 but you've categorized transactions → categoryId not being saved
- Check if your categorized transaction appears in the category breakdown
- Check if the date range includes your transaction's date

---

## 🧪 How to Test

### Step-by-Step Testing:

1. **Open Logcat** with filter: `CategoriesFragment|TransactionsFragment|TransactionRepository`

2. **Go to Transactions screen**

3. **Select a transaction** and categorize it (e.g., "Food")

4. **Watch the logs for:**
   ```
   TransactionsFragment: 🔄 ===== STARTING CATEGORIZATION =====
   TransactionRepository: 🔄 Starting updateTransactionCategory
   TransactionRepository: ✅ Update result: 1 rows affected
   TransactionsFragment: ✅ Update completed - Rows affected: 1
   TransactionsFragment: 🔍 Verification - categoryId after update: food
   TransactionsFragment: 🔄 Categories fragment refresh triggered
   ```

5. **Categories fragment should reload:**
   ```
   CategoriesFragment: 🚀 ===== loadCategoryData() STARTED =====
   CategoriesFragment: 📊 DEBUG: Categorized transactions: {should increase}
   CategoriesFragment: 📌 Category 'food': X txns, ₹{amount}
   ```

6. **Navigate to Categories screen**

7. **Check if your transaction appears**

---

## 🐛 Common Issues & Solutions

### Issue 1: Rows Affected = 0

**Symptom:** Log shows `Rows affected: 0`

**Possible Causes:**
- Transaction ID doesn't exist in database
- Transaction was deleted
- Database constraint issue

**Solution:**
```
Check logs for:
TransactionRepository: ❌ Transaction {id} does not exist!
```

---

### Issue 2: CategoryId Not Updating

**Symptom:** Verification shows old categoryId

**Possible Causes:**
- Foreign key constraint failing
- Category doesn't exist in database
- Database write not completing

**Solution:**
Look for errors in `TransactionRepository` logs

---

### Issue 3: Transaction Not in Selected Month

**Symptom:** Transaction saved successfully but doesn't appear in Categories

**Possible Causes:**
- Transaction date is outside the selected month range
- Categories fragment is showing different month

**Solution:**
```
Compare these in logs:
- Transaction Date: {date}
- Categories Date Range: {start} to {end}
```

Make sure transaction date falls within the range!

---

### Issue 4: Query Not Finding Data

**Symptom:** "Categorized transactions: X" but "Selected month category spending: 0"

**Possible Causes:**
- SQL query filtering incorrectly
- Date range wrong
- Transaction type is CREDIT not DEBIT

**Solution:**
Check the transaction type - Categories only shows DEBIT (expense) transactions

---

## 📋 What to Share

If the issue persists, share these log excerpts:

1. **From Categorization:**
```
TransactionsFragment: 📝 Transaction ID: ...
TransactionsFragment: 📝 Date: ...
TransactionsFragment: ✅ Update completed - Rows affected: ...
TransactionsFragment: 🔍 Verification - categoryId after update: ...
```

2. **From Categories Loading:**
```
CategoriesFragment: 📊 DEBUG: Total transactions in DB: ...
CategoriesFragment: 📊 DEBUG: Categorized transactions: ...
CategoriesFragment: 📅 Date Range: ...
CategoriesFragment: 📊 Selected month category spending: ...
```

3. **From Repository:**
```
TransactionRepository: 🔄 Starting updateTransactionCategory: ...
TransactionRepository: ✅ Update result: ... rows affected
TransactionRepository: 🔍 After update: CategoryId='...'
```

---

## ✨ Expected Behavior

**After categorizing a transaction:**

1. ✅ TransactionsFragment logs show successful update (rows = 1)
2. ✅ Verification confirms categoryId is saved
3. ✅ CategoriesFragment refresh is triggered
4. ✅ Categories screen shows updated data immediately
5. ✅ Pie chart updates with new category/amount

---

## 🔧 Quick Fixes

### Force Refresh Categories

If categorization works but Categories doesn't update:

```kotlin
// Already implemented - should trigger automatically
(activity as? HomeActivity)?.refreshCategoriesData()
```

### Check Database Directly

You can verify data in Database Inspector:
1. View → Tool Windows → App Inspection
2. Select "transactions" table
3. Check the "categoryId" column for your transaction

---

## 📱 Testing Commands

Run the app and filter logcat:

```bash
# PowerShell
adb logcat -s "CategoriesFragment:D" "TransactionsFragment:D" "TransactionRepository:D"
```

This will show only the relevant debug logs.

---

**Debug Version**: October 14, 2025  
**Purpose**: Diagnose transaction categorization issues  
**Status**: Enhanced logging active
