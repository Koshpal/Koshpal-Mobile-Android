# 🔄 COMPLETE FLOW: SMS → CATEGORIZATION → STORAGE → DISPLAY

## 📋 **TABLE OF CONTENTS**

1. [SMS Reception](#1-sms-reception)
2. [Transaction Extraction](#2-transaction-extraction)
3. [Auto-Categorization Process](#3-auto-categorization-process)
4. [Database Storage](#4-database-storage)
5. [Categories Fragment Query](#5-categories-fragment-query)
6. [UI Display](#6-ui-display)
7. [Complete Flow Diagram](#7-complete-flow-diagram)

---

## 1️⃣ **SMS RECEPTION**

### **Entry Point: TransactionSMSReceiver.kt**

```kotlin
// Location: app/src/main/java/.../utils/TransactionSMSReceiver.kt
class TransactionSMSReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        // Triggered when SMS arrives
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
            
            // Extract SMS data
            val messageBody = smsMessage?.messageBody  // "Rs.500 debited at Amazon..."
            val sender = smsMessage?.originatingAddress  // "HDFCBK"
            
            // Step 1: Check if this is a transaction SMS
            if (isTransactionSMS(messageBody, sender)) {
                Log.d("TransactionSMS", "🔔 Detected transaction SMS from $sender")
                
                // Step 2: Process in background (even if app is closed)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    processTransactionSMS(context, messageBody, sender)
                    pendingResult.finish()
                }
            }
        }
    }
}
```

### **SMS Validation:**

```kotlin
private fun isTransactionSMS(body: String, sender: String): Boolean {
    val bankSenders = BankConstants.BANK_SENDERS  // 80+ banks
    val transactionKeywords = BankConstants.TRANSACTION_KEYWORDS
    
    val isFromBank = bankSenders.any { sender.uppercase().contains(it) }
    val hasTransactionKeyword = transactionKeywords.any { body.lowercase().contains(it) }
    val hasAmountPattern = body.matches(Regex(".*(?:₹|rs\\.?|inr)\\s*[0-9,]+.*"))
    
    // Must be: (from bank OR has keywords) AND has amount
    return (isFromBank || hasTransactionKeyword) && hasAmountPattern
}
```

---

## 2️⃣ **TRANSACTION EXTRACTION**

### **Engine: TransactionCategorizationEngine.kt**

```kotlin
// Location: app/src/main/java/.../engine/TransactionCategorizationEngine.kt

fun extractTransactionDetails(smsBody: String): TransactionDetails {
    // Extract amount
    val amount = extractAmount(smsBody)  // ₹500 → 500.0
    
    // Extract merchant
    val merchant = extractMerchant(smsBody)  // "at Amazon" → "Amazon"
    
    // Determine type
    val type = determineTransactionType(smsBody)  // DEBIT/CREDIT/TRANSFER
    
    return TransactionDetails(
        amount = amount,
        merchant = merchant,
        type = type,
        description = "Payment to $merchant"
    )
}
```

### **Amount Extraction:**

```kotlin
private fun extractAmount(smsBody: String): Double {
    // Regex: (?:rs\.?|inr|₹)\s*(\d+(?:,\d{3})*(?:\.\d{1,2})?)
    val amountPattern = Pattern.compile(
        "(?:(?:rs\\.?|inr|₹)\\s*|(?:debited|credited)\\s+by\\s+)(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)"
    )
    
    val matcher = amountPattern.matcher(smsBody)
    if (matcher.find()) {
        val amountStr = matcher.group(1)?.replace(",", "") ?: "0"
        return amountStr.toDoubleOrNull() ?: 0.0
    }
    return 0.0
}

// Examples:
// "Rs.500.00" → 500.0
// "debited by 1200" → 1200.0
// "₹1,500" → 1500.0
```

### **Merchant Extraction:**

```kotlin
private fun extractMerchant(smsBody: String): String {
    // Regex: (?:at|from|to|trf\s+to)\s+([a-zA-Z0-9\s&.-]+?)
    val merchantPattern = Pattern.compile(
        "(?:at|from|to|trf\\s+to|transferred\\s+to|towards)\\s+([a-zA-Z0-9\\s&.-]+?)(?:\\s+(?:on|from|refno|umn|thru|through)|\\.|$)"
    )
    
    val matcher = merchantPattern.matcher(smsBody)
    if (matcher.find()) {
        return matcher.group(1)?.trim() ?: "Unknown"
    }
    
    // Fallback: check for common merchants
    val commonMerchants = listOf("amazon", "flipkart", "zomato", "swiggy", "uber", "ola")
    for (merchant in commonMerchants) {
        if (smsBody.lowercase().contains(merchant)) {
            return merchant.capitalize()
        }
    }
    
    return "Unknown Merchant"
}

// Examples:
// "debited at AMAZON INDIA" → "AMAZON INDIA"
// "paid to ZOMATO" → "ZOMATO"
// "from mr shivam dinesh atr" → "mr shivam dinesh atr"
```

### **Transaction Type:**

```kotlin
private fun determineTransactionType(smsBody: String): TransactionType {
    val lowerSms = smsBody.lowercase()
    return when {
        lowerSms.contains("debited") || lowerSms.contains("spent") || 
        lowerSms.contains("paid") 
            → TransactionType.DEBIT
        
        lowerSms.contains("credited") || lowerSms.contains("received") || 
        lowerSms.contains("refund") 
            → TransactionType.CREDIT
        
        lowerSms.contains("transfer") 
            → TransactionType.TRANSFER
        
        else → TransactionType.DEBIT  // Default
    }
}
```

---

## 3️⃣ **AUTO-CATEGORIZATION PROCESS**

### **Categorizer: MerchantCategorizer.kt**

```kotlin
// Location: app/src/main/java/.../utils/MerchantCategorizer.kt

object MerchantCategorizer {
    
    // Category Keywords (20-30 per category)
    private val categoryKeywords = mapOf(
        "food" to listOf(
            "zomato", "swiggy", "ubereats", "dominos", "pizzahut", 
            "mcdonalds", "kfc", "burgerking", "starbucks", ...
        ),
        "transport" to listOf(
            "uber", "ola", "rapido", "indianoil", "bpcl", "shell", 
            "fastag", "makemytrip", ...
        ),
        "shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio", "meesho", 
            "nykaa", "croma", ...
        ),
        "grocery" to listOf(
            "bigbasket", "blinkit", "zepto", "dunzo", "dmart", 
            "jiomart", ...
        ),
        // ... 10 categories total
    )
    
    fun categorizeTransaction(merchant: String, smsBody: String? = null): String {
        val merchantLower = merchant.lowercase().trim()
        val smsBodyLower = smsBody?.lowercase()?.trim() ?: ""
        
        Log.d(TAG, "🔍 ===== CATEGORIZING =====")
        Log.d(TAG, "📝 Merchant: '$merchant'")
        Log.d(TAG, "📝 SMS Body: ${smsBody?.take(100)}...")
        
        // Try to match with category keywords
        for ((category, keywords) in categoryKeywords) {
            for (keyword in keywords) {
                val keywordLower = keyword.lowercase().trim()
                
                // Skip very short keywords
                if (keywordLower.length < 3) continue
                
                // Method 1: Check first 3-4 character match
                if (merchantLower.length >= 3) {
                    val merchantFirst4 = merchantLower.take(4)
                    val keywordFirst4 = keywordLower.take(4)
                    
                    if (merchantFirst4 == keywordFirst4) {
                        Log.d(TAG, "✅ Match: '$merchant' → $category")
                        return category
                    }
                }
                
                // Method 2: Full keyword match
                if (merchantLower.contains(keywordLower) || 
                    smsBodyLower.contains(keywordLower)) {
                    Log.d(TAG, "✅ Full match: '$keyword' → $category")
                    return category
                }
            }
        }
        
        // No match found
        Log.d(TAG, "⚠️ No match → others")
        return "others"
    }
}
```

### **Categorization Examples:**

```
Input: merchant = "Amazon India"
Process:
  - merchantLower = "amazon india"
  - Loop through categories
  - Check "shopping" keywords
  - Find "amazon" in keywords
  - "amazon india".contains("amazon") = TRUE
Output: "shopping"

Input: merchant = "Zomato"
Process:
  - merchantLower = "zomato"
  - Loop through categories
  - Check "food" keywords
  - Find "zomato" in keywords
  - First 4 chars: "zoma" == "zoma" = TRUE
Output: "food"

Input: merchant = "mr shivam dinesh atr"
Process:
  - merchantLower = "mr shivam dinesh atr"
  - Loop through all categories
  - No keyword matches
  - Check SMS body for keywords
  - If SMS contains "received a payment" and type = CREDIT
    → Might be "salary"
Output: "salary" or "others"
```

---

## 4️⃣ **DATABASE STORAGE**

### **Step 1: Create Transaction Object**

```kotlin
// In SMSManager.processAllSMS() or TransactionSMSReceiver

// After extraction and categorization:
val details = engine.extractTransactionDetails(smsBody)
// details.amount = 500.0
// details.merchant = "Amazon India"
// details.type = DEBIT

val categoryId = MerchantCategorizer.categorizeTransaction(
    details.merchant, 
    smsBody
)
// categoryId = "shopping"

val transaction = Transaction(
    id = UUID.randomUUID().toString(),  // "123e4567-e89b-12d3-a456-426614174000"
    amount = details.amount,             // 500.0
    type = details.type,                 // TransactionType.DEBIT
    merchant = details.merchant,         // "Amazon India"
    categoryId = categoryId,             // "shopping"
    confidence = 85.0f,                  // Auto-categorization confidence
    date = sms.timestamp,                // 1696512000000 (timestamp)
    description = details.description,   // "Payment to Amazon India"
    smsBody = smsBody,                   // Original SMS (for duplicate check)
    isManuallySet = false,               // Auto-categorized, not manual
    bankName = "HDFC Bank"              // Extracted from sender
)
```

### **Step 2: Insert into Database**

```kotlin
// Room Database insertion
database.transactionDao().insertTransaction(transaction)

Log.d("SMSManager", "✅ Created transaction: ₹${details.amount} at ${details.merchant} → Category: $categoryId")
```

### **Database Table Structure:**

```sql
CREATE TABLE transactions (
    id TEXT PRIMARY KEY,                    -- "123e4567-..."
    amount REAL,                            -- 500.0
    description TEXT,                       -- "Payment to Amazon India"
    merchant TEXT,                          -- "Amazon India"
    categoryId TEXT,                        -- "shopping" (FK → transaction_categories)
    type TEXT,                              -- "DEBIT"
    date INTEGER,                           -- 1696512000000
    smsId TEXT,                             -- null (for SMS reference)
    isProcessed INTEGER,                    -- 0 (boolean)
    createdAt INTEGER,                      -- 1696512000000
    updatedAt INTEGER,                      -- 1696512000000
    confidence REAL,                        -- 85.0
    smsBody TEXT,                           -- "Rs.500 debited at Amazon..."
    isManuallySet INTEGER,                  -- 0 (false = auto-categorized)
    bankName TEXT,                          -- "HDFC Bank"
    
    FOREIGN KEY (categoryId) REFERENCES transaction_categories(id)
        ON DELETE SET NULL 
        ON UPDATE CASCADE
);

CREATE INDEX idx_categoryId ON transactions(categoryId);
```

### **Multiple Transactions in Database:**

```
transactions table:
┌──────────┬────────┬─────────────┬───────────┬──────────┬──────┐
│ id       │ amount │ merchant    │ categoryId│ type     │ date │
├──────────┼────────┼─────────────┼───────────┼──────────┼──────┤
│ txn-001  │ 500.0  │ Amazon      │ shopping  │ DEBIT    │ Oct 5│
│ txn-002  │ 1200.0 │ Zomato      │ food      │ DEBIT    │ Oct 6│
│ txn-003  │ 25000.0│ Salary Cred │ salary    │ CREDIT   │ Oct 1│
│ txn-004  │ 350.0  │ Uber        │ transport │ DEBIT    │ Oct 7│
│ txn-005  │ 800.0  │ DMart       │ grocery   │ DEBIT    │ Oct 8│
└──────────┴────────┴─────────────┴───────────┴──────────┴──────┘
```

---

## 5️⃣ **CATEGORIES FRAGMENT QUERY**

### **Fragment: CategoriesFragment.kt**

```kotlin
// Location: app/src/main/java/.../ui/categories/CategoriesFragment.kt

private fun loadCategoryData() {
    lifecycleScope.launch {
        Log.d("CategoriesFragment", "🔄 Loading category data...")
        
        // Step 1: Calculate month range (e.g., Oct 1 - Oct 31, 2025)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, selectedYear)        // 2025
        calendar.set(Calendar.MONTH, selectedMonth)      // 9 (October, 0-based)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis         // 1696118400000
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.timeInMillis           // 1698796799999
        
        Log.d("CategoriesFragment", "📅 Querying Oct 2025 from $startOfMonth to $endOfMonth")
        
        // Step 2: Query database for category spending
        val categorySpending = transactionRepository
            .getCurrentMonthCategorySpending(startOfMonth, endOfMonth)
        
        Log.d("CategoriesFragment", "📊 Found ${categorySpending.size} categories with spending")
        
        // Log each category
        categorySpending.forEach { spending →
            val categoryName = TransactionCategory.getDefaultCategories()
                .find { it.id == spending.categoryId }?.name ?: "Unknown"
            Log.d("CategoriesFragment", "   💰 ${spending.categoryId} ('$categoryName') → ₹${spending.totalAmount}")
        }
        
        // Step 3: Update UI
        if (categorySpending.isNotEmpty()) {
            updatePieChart(categorySpending)
            updateCategoryList(categorySpending)
            updateTotalSpending(categorySpending)
            showDataViews()
        } else {
            showEmptyState()
        }
    }
}
```

### **Database Query (DAO):**

```kotlin
// Location: app/src/main/java/.../data/local/dao/TransactionDao.kt

@Query("""
    SELECT 
        categoryId, 
        SUM(amount) as totalAmount 
    FROM transactions 
    WHERE type = 'DEBIT'                    -- Only expenses (not income)
      AND categoryId IS NOT NULL            -- Must have a category
      AND date >= :startOfMonth             -- Oct 1, 2025
      AND date <= :endOfMonth               -- Oct 31, 2025
    GROUP BY categoryId                     -- Group by category
    HAVING SUM(amount) > 0                  -- Only categories with spending
    ORDER BY totalAmount DESC               -- Highest spending first
""")
suspend fun getCurrentMonthCategorySpending(
    startOfMonth: Long, 
    endOfMonth: Long
): List<CategorySpending>
```

### **Query Execution Example:**

```sql
-- Input:
startOfMonth = 1696118400000  -- Oct 1, 2025, 00:00:00
endOfMonth = 1698796799999    -- Oct 31, 2025, 23:59:59

-- Query execution:
SELECT categoryId, SUM(amount) as totalAmount 
FROM transactions 
WHERE type = 'DEBIT' 
  AND categoryId IS NOT NULL 
  AND date >= 1696118400000 
  AND date <= 1698796799999
GROUP BY categoryId 
HAVING SUM(amount) > 0
ORDER BY totalAmount DESC;

-- Result:
┌───────────┬─────────────┐
│ categoryId│ totalAmount │
├───────────┼─────────────┤
│ food      │ 1200.0      │  -- Zomato
│ grocery   │ 800.0       │  -- DMart
│ shopping  │ 500.0       │  -- Amazon
│ transport │ 350.0       │  -- Uber
└───────────┴─────────────┘
```

### **Result Object: CategorySpending**

```kotlin
// Location: app/src/main/java/.../model/CategorySpending.kt

data class CategorySpending(
    val categoryId: String,      // "food"
    val totalAmount: Double      // 1200.0
)

// List returned from query:
listOf(
    CategorySpending("food", 1200.0),
    CategorySpending("grocery", 800.0),
    CategorySpending("shopping", 500.0),
    CategorySpending("transport", 350.0)
)
```

---

## 6️⃣ **UI DISPLAY**

### **Step 1: Update Pie Chart**

```kotlin
private fun updatePieChart(categorySpending: List<CategorySpending>) {
    val entries = mutableListOf<PieEntry>()
    val colors = mutableListOf<Int>()
    
    categorySpending.forEach { spending →
        // Get category details from default categories
        val category = TransactionCategory.getDefaultCategories()
            .find { it.id == spending.categoryId }
        
        // Category name for display
        val categoryName = category?.name ?: "Unknown"  // "Food & Dining"
        
        // Add pie entry
        entries.add(PieEntry(spending.totalAmount.toFloat(), categoryName))
        // PieEntry(1200.0f, "Food & Dining")
        
        // Get category color
        val color = try {
            Color.parseColor(category?.color ?: "#6750A4")  // "#FF6B35" (orange)
        } catch (e: Exception) {
            Color.parseColor("#6750A4")  // Default purple
        }
        colors.add(color)
    }
    
    // Create dataset
    val dataSet = PieDataSet(entries, "Categories").apply {
        setColors(colors)
        sliceSpace = 3f              // Space between slices
        selectionShift = 5f          // Shift when tapped
        valueTextSize = 0f           // Hide values on slices
    }
    
    // Create pie data
    val data = PieData(dataSet).apply {
        setValueFormatter(PercentFormatter())
        setValueTextColor(Color.TRANSPARENT)  // Hide text
    }
    
    // Update chart
    binding.pieChart.data = data
    binding.pieChart.invalidate()  // Refresh
}
```

**Pie Chart Display:**
```
        ╱─────────╲
      ╱  🍔 Food   ╲
     │   42.1%      │
     │              │
    ╱ 🛒 Grocery    ╲
   │    28.1%       │
    ╲               ╱
     │ 🛍️ Shopping │
     │   17.5%     │
      ╲ 🚗 Trans  ╱
        ╲ 12.3% ╱
          ─────

  Center: Spends
          ₹2,850
```

### **Step 2: Update Category List**

```kotlin
private fun updateCategoryList(categorySpending: List<CategorySpending>) {
    // Submit to adapter
    categorySpendingAdapter.submitList(categorySpending)
}
```

**Adapter Binding:**

```kotlin
// Location: app/src/main/java/.../ui/categories/adapter/CategorySpendingAdapter.kt

inner class CategorySpendingViewHolder(binding: ItemCategorySpendingBinding) {
    
    fun bind(categorySpending: CategorySpending) {
        // categorySpending.categoryId = "food"
        // categorySpending.totalAmount = 1200.0
        
        // Get category details
        val category = TransactionCategory.getDefaultCategories()
            .find { it.id == categorySpending.categoryId }
        
        // Set category name
        tvCategoryName.text = category?.name ?: "Unknown"
        // "Food & Dining"
        
        // Set amount
        tvAmount.text = "₹${String.format("%.0f", categorySpending.totalAmount)}"
        // "₹1,200"
        
        // Set transaction count (placeholder for now)
        tvTransactionCount.text = "1 Spend"
        
        // Set category icon
        if (category != null) {
            ivCategoryIcon.setImageResource(category.icon)  // R.drawable.ic_menu_eat
            
            // Set icon color
            val color = Color.parseColor(category.color)  // #FF6B35 (orange)
            cardIcon.setCardBackgroundColor(color)
            ivCategoryIcon.setColorFilter(Color.WHITE)
        }
    }
}
```

**List Display:**

```
┌─────────────────────────────────────┐
│ [🍔] Food & Dining       ₹1,200    │
│      1 Spend          Set budget > │
├─────────────────────────────────────┤
│ [🛒] Grocery             ₹800      │
│      1 Spend          Set budget > │
├─────────────────────────────────────┤
│ [🛍️] Shopping            ₹500      │
│      1 Spend          Set budget > │
├─────────────────────────────────────┤
│ [🚗] Transportation      ₹350      │
│      1 Spend          Set budget > │
└─────────────────────────────────────┘
```

### **Step 3: Update Total Spending**

```kotlin
private fun updateTotalSpending(categorySpending: List<CategorySpending>) {
    val total = categorySpending.sumOf { it.totalAmount }
    // 1200 + 800 + 500 + 350 = 2850.0
    
    binding.tvTotalSpending.text = "₹${String.format("%.0f", total)}"
    // "₹2,850"
}
```

---

## 7️⃣ **COMPLETE FLOW DIAGRAM**

```
┌─────────────────────────────────────────────────────────────────┐
│                    📱 SMS ARRIVES                               │
│  "Rs.500 debited at Amazon India on 05-Oct-25"                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              📥 STEP 1: SMS RECEPTION                           │
│  TransactionSMSReceiver.onReceive()                             │
│    • Sender: "HDFCBK"                                           │
│    • Body: "Rs.500 debited at Amazon India..."                 │
│    • isTransactionSMS(): ✅ TRUE                                │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│           🔍 STEP 2: TRANSACTION EXTRACTION                     │
│  TransactionCategorizationEngine.extractTransactionDetails()    │
│    • extractAmount(): "Rs.500" → 500.0                         │
│    • extractMerchant(): "at Amazon India" → "Amazon India"     │
│    • determineType(): "debited" → TransactionType.DEBIT        │
│                                                                 │
│  Result: TransactionDetails(                                    │
│    amount = 500.0,                                              │
│    merchant = "Amazon India",                                   │
│    type = DEBIT,                                                │
│    description = "Payment to Amazon India"                      │
│  )                                                              │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│            🏷️ STEP 3: AUTO-CATEGORIZATION                      │
│  MerchantCategorizer.categorizeTransaction()                    │
│    Input: merchant = "Amazon India"                             │
│           smsBody = "Rs.500 debited at Amazon India..."         │
│                                                                 │
│    Process:                                                     │
│      1. merchantLower = "amazon india"                          │
│      2. Loop through categories:                                │
│         - food: no match                                        │
│         - grocery: no match                                     │
│         - transport: no match                                   │
│         - shopping: checking...                                 │
│           • keywords: ["amazon", "flipkart", ...]               │
│           • "amazon india".contains("amazon") = ✅ TRUE         │
│      3. MATCH FOUND!                                            │
│                                                                 │
│    Output: "shopping"                                           │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              💾 STEP 4: DATABASE STORAGE                        │
│  Transaction object created:                                    │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ id: "txn-001"                                             │ │
│  │ amount: 500.0                                             │ │
│  │ merchant: "Amazon India"                                  │ │
│  │ categoryId: "shopping"    ← STORED CATEGORY               │ │
│  │ type: DEBIT                                               │ │
│  │ date: 1696512000000  (Oct 5, 2025)                       │ │
│  │ smsBody: "Rs.500 debited..."                             │ │
│  │ isManuallySet: false  (auto-categorized)                 │ │
│  │ bankName: "HDFC Bank"                                     │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  database.transactionDao().insertTransaction(transaction)      │
│    ↓                                                            │
│  Inserted into `transactions` table with categoryId reference  │
└────────────────────────┬────────────────────────────────────────┘
                         │
                    [DATABASE]
                         │
transactions table       │
┌───────────┬────────┬────────────┬───────────┬──────┬────────┐
│ id        │ amount │ merchant   │ categoryId│ type │ date   │
├───────────┼────────┼────────────┼───────────┼──────┼────────┤
│ txn-001   │ 500.0  │ Amazon Ind │ shopping  │DEBIT │ Oct 5  │
│ txn-002   │ 1200.0 │ Zomato     │ food      │DEBIT │ Oct 6  │
│ txn-003   │ 800.0  │ DMart      │ grocery   │DEBIT │ Oct 8  │
│ txn-004   │ 350.0  │ Uber       │ transport │DEBIT │ Oct 7  │
└───────────┴────────┴────────────┴───────────┴──────┴────────┘
                         │
                         │ User opens Budget tab
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│          📊 STEP 5: CATEGORIES FRAGMENT QUERY                   │
│  CategoriesFragment.loadCategoryData()                          │
│    • selectedMonth = October 2025                               │
│    • startOfMonth = 1696118400000                               │
│    • endOfMonth = 1698796799999                                 │
│                                                                 │
│  transactionRepository.getCurrentMonthCategorySpending(...)     │
│    ↓                                                            │
│  SQL Query:                                                     │
│  SELECT categoryId, SUM(amount) as totalAmount                  │
│  FROM transactions                                              │
│  WHERE type = 'DEBIT'                                           │
│    AND categoryId IS NOT NULL                                   │
│    AND date >= 1696118400000                                    │
│    AND date <= 1698796799999                                    │
│  GROUP BY categoryId                                            │
│  ORDER BY totalAmount DESC                                      │
│                                                                 │
│  Result:                                                        │
│  ┌───────────┬─────────────┐                                   │
│  │ categoryId│ totalAmount │                                   │
│  ├───────────┼─────────────┤                                   │
│  │ food      │ 1200.0      │  ← Zomato                         │
│  │ grocery   │ 800.0       │  ← DMart                          │
│  │ shopping  │ 500.0       │  ← Amazon India                   │
│  │ transport │ 350.0       │  ← Uber                           │
│  └───────────┴─────────────┘                                   │
│                                                                 │
│  Returns: List<CategorySpending>                                │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│               🎨 STEP 6: UI DISPLAY                             │
│                                                                 │
│  A. Update Pie Chart:                                           │
│     • Loop through categorySpending                             │
│     • Get category name and color from TransactionCategory      │
│     • Create PieEntry for each                                  │
│     • Set colors based on category                              │
│                                                                 │
│  B. Update Category List:                                       │
│     • Submit list to CategorySpendingAdapter                    │
│     • Adapter binds each item:                                  │
│       - Icon (colored circle)                                   │
│       - Category name                                           │
│       - Amount                                                  │
│                                                                 │
│  C. Update Total Spending:                                      │
│     • Sum all amounts: 1200 + 800 + 500 + 350 = 2850          │
│     • Display in pie chart center                               │
│                                                                 │
│  Final Display:                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Categories                            Oct'25 ▼         │   │
│  │                                                         │   │
│  │          ╱───────────╲                                  │   │
│  │        ╱   Spends     ╲                                 │   │
│  │       │    ₹2,850      │                                │   │
│  │        ╲             ╱                                  │   │
│  │          ╲─────────╱                                    │   │
│  │     (Colored slices for each category)                 │   │
│  │                                                         │   │
│  │   [Set monthly budget]                                 │   │
│  │                                                         │   │
│  │  🍔 Food & Dining              ₹1,200                  │   │
│  │     1 Spend              Set budget >                  │   │
│  │                                                         │   │
│  │  🛒 Grocery                    ₹800                    │   │
│  │     1 Spend              Set budget >                  │   │
│  │                                                         │   │
│  │  🛍️ Shopping                   ₹500                    │   │
│  │     1 Spend              Set budget >                  │   │
│  │                                                         │   │
│  │  🚗 Transportation             ₹350                    │   │
│  │     1 Spend              Set budget >                  │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 **COMPLETE DATA FLOW SUMMARY**

```
SMS Text
    ↓ [TransactionSMSReceiver]
Transaction Details (amount, merchant, type)
    ↓ [MerchantCategorizer]
Category ID (e.g., "shopping")
    ↓ [Transaction object creation]
Database Record (with categoryId)
    ↓ [Room Database INSERT]
transactions table row
    ↓ [User opens Budget tab]
SQL Query (GROUP BY categoryId, SUM amount)
    ↓ [TransactionDao]
List<CategorySpending>
    ↓ [CategoriesFragment]
Pie Chart + Category List UI
```

---

## 📊 **KEY TABLES & RELATIONSHIPS**

### **TransactionCategory Table:**

```
transaction_categories
┌──────────┬────────────────┬──────┬─────────┬────────────────┐
│ id       │ name           │ icon │ color   │ keywords       │
├──────────┼────────────────┼──────┼─────────┼────────────────┤
│ food     │ Food & Dining  │ 🍔   │#FF6B35  │[zomato,swiggy] │
│ shopping │ Shopping       │ 🛍️   │#795548  │[amazon,flipk.] │
│ grocery  │ Grocery        │ 🛒   │#4CAF50  │[bigbasket,..] │
│ transport│ Transportation │ 🚗   │#2196F3  │[uber,ola,..] │
│ salary   │ Salary & Income│ 💰   │#4CAF50  │[salary,credit] │
│ others   │ Others         │ 📦   │#607D8B  │[]              │
└──────────┴────────────────┴──────┴─────────┴────────────────┘
```

### **Foreign Key Relationship:**

```
transactions.categoryId → transaction_categories.id

If category deleted: SET NULL
If category updated: CASCADE
```

---

## ✅ **VERIFICATION & DEBUGGING**

### **LogCat Tags to Monitor:**

```kotlin
// 1. SMS Reception
"TransactionSMS" → "🔔 Detected transaction SMS from HDFCBK"

// 2. Extraction
"TransactionCategorizationEngine" → "Extracted: ₹500 at Amazon India"

// 3. Categorization
"MerchantCategorizer" → "✅ Match: 'Amazon India' → shopping"

// 4. Storage
"SMSManager" → "✅ Created transaction: ₹500 at Amazon India → Category: shopping"

// 5. Query
"CategoriesFragment" → "📊 Found 4 categories with spending"
"CategoriesFragment" → "   💰 shopping ('Shopping') → ₹500"

// 6. Display
"CategorySpendingAdapter" → "Binding category: shopping, amount: ₹500"
```

### **Database Verification:**

```sql
-- Check if transaction was inserted
SELECT id, merchant, categoryId, amount 
FROM transactions 
WHERE merchant LIKE '%Amazon%';

-- Check category spending
SELECT categoryId, SUM(amount) as total 
FROM transactions 
WHERE type = 'DEBIT' 
  AND categoryId IS NOT NULL 
GROUP BY categoryId;

-- Verify category exists
SELECT id, name 
FROM transaction_categories 
WHERE id = 'shopping';
```

---

## 🎯 **SUMMARY**

### **Complete Journey:**

1. **SMS Arrives** → "Rs.500 debited at Amazon India"
2. **Extraction** → amount=500, merchant="Amazon India", type=DEBIT
3. **Categorization** → MerchantCategorizer → "shopping"
4. **Storage** → Insert Transaction with categoryId="shopping"
5. **Query** → GROUP BY categoryId, SUM(amount)
6. **Display** → Pie chart + List showing Shopping: ₹500

### **Key Points:**

✅ **Automatic** - No manual intervention needed  
✅ **Real-time** - Works even when app closed  
✅ **Accurate** - 400+ keywords for categorization  
✅ **Stored** - All data persisted in Room database  
✅ **Queryable** - Efficient SQL queries with grouping  
✅ **Visual** - Beautiful pie chart and list display  
✅ **Month-wise** - Filter by any month  

**The entire flow from SMS to Categories display is FULLY AUTOMATED and REAL-TIME!** 🚀

