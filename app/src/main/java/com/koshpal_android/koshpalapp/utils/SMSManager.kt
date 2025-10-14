package com.koshpal_android.koshpalapp.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.engine.TransactionCategorizationEngine
import com.koshpal_android.koshpalapp.model.PaymentSms
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SMSManager(private val context: Context) {
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    suspend fun processAllSMS(): ProcessResult {
        return withContext(Dispatchers.IO) {
            val result = ProcessResult()
            
            try {
                // Check permissions first
                if (!hasPermissions()) {
                    result.error = "SMS permissions not granted"
                    return@withContext result
                }
                
                val database = KoshpalDatabase.getDatabase(context)
                val paymentSmsDao = database.paymentSmsDao()
                val transactionDao = database.transactionDao()
                
                // Step 1: Read SMS from device
                Log.d("SMSManager", "🚀 Starting SMS processing...")
                val smsMessages = readSMSFromDevice()
                result.smsFound = smsMessages.size
                Log.d("SMSManager", "📱 Found ${smsMessages.size} SMS messages from device")
                
                // Step 2: Filter transaction SMS
                Log.d("SMSManager", "🔍 Filtering transaction SMS...")
                val transactionSMS = smsMessages.filter { sms ->
                    isTransactionSMS(sms.body, sms.address)
                }
                result.transactionSmsFound = transactionSMS.size
                Log.d("SMSManager", "💳 Found ${transactionSMS.size} transaction SMS out of ${smsMessages.size} total SMS")
                
                // Log some examples for debugging
                transactionSMS.take(3).forEach { sms ->
                    Log.d("SMSManager", "📄 Example transaction SMS from ${sms.address}: ${sms.body.take(100)}...")
                }
                
                // Step 3: Save SMS to database (avoid duplicates)
                Log.d("SMSManager", "💾 Saving SMS to database...")
                transactionSMS.forEach { sms ->
                    try {
                        // Check if SMS already exists to avoid duplicates
                        val existing = paymentSmsDao.getSMSByBodyAndSender(sms.body, sms.address)
                        if (existing == null) {
                            paymentSmsDao.insertSms(sms)
                            result.smsProcessed++
                        } else {
                            Log.d("SMSManager", "⏭️ SMS already exists, skipping duplicate")
                        }
                    } catch (e: Exception) {
                        Log.e("SMSManager", "❌ Error saving SMS: ${e.message}", e)
                    }
                }
                Log.d("SMSManager", "💾 Saved ${result.smsProcessed} new SMS to database")
                
                // Step 4: Ensure default categories exist
                Log.d("SMSManager", "📂 Ensuring default categories exist...")
                val categoryDao = database.categoryDao()
                val existingCategories = categoryDao.getDefaultCategories()
                
                if (existingCategories.isEmpty()) {
                    Log.d("SMSManager", "📂 No categories found, inserting default categories...")
                    val defaultCategories = TransactionCategory.getDefaultCategories()
                    try {
                        categoryDao.insertCategories(defaultCategories)
                        Log.d("SMSManager", "✅ Inserted ${defaultCategories.size} default categories")
                    } catch (e: Exception) {
                        Log.e("SMSManager", "❌ Error inserting default categories: ${e.message}")
                    }
                } else {
                    Log.d("SMSManager", "📂 Found ${existingCategories.size} existing categories")
                }
                
                // Step 5: Process SMS into transactions
                Log.d("SMSManager", "⚙️ Processing SMS into transactions...")
                val engine = TransactionCategorizationEngine()
                
                // Get categories directly without Flow collection to avoid hanging
                val categoryList = try {
                    database.categoryDao().getAllActiveCategoriesList() // Use direct list method
                } catch (e: Exception) {
                    Log.e("SMSManager", "❌ Error getting categories: ${e.message}")
                    emptyList()
                }
                
                Log.d("SMSManager", "📂 Found ${categoryList.size} categories for processing")
                
                transactionSMS.forEach { sms ->
                    try {
                        val details = engine.extractTransactionDetails(sms.smsBody)
                        
                        if (details.amount > 0 && details.merchant.isNotBlank()) {
                            // ROBUST DUPLICATE PREVENTION - Check multiple ways
                            // 1. Check by exact SMS body
                            val existingBySms = database.transactionDao().getTransactionsBySmsBody(sms.smsBody)
                            if (existingBySms != null) {
                                Log.d("SMSManager", "⏭️ Duplicate: Transaction exists with same SMS body, skipping")
                                return@forEach
                            }
                            
                            // 2. Check by amount + timestamp + merchant (within 1 minute tolerance)
                            val timeWindow = 60000L // 1 minute in milliseconds
                            val existingByDetails = database.transactionDao().getTransactionByAmountAndTime(
                                details.amount,
                                sms.timestamp - timeWindow,
                                sms.timestamp + timeWindow
                            )
                            if (existingByDetails != null && existingByDetails.merchant == details.merchant) {
                                Log.d("SMSManager", "⏭️ Duplicate: Similar transaction exists (amount + time + merchant), skipping")
                                return@forEach
                            }
                            
                            // Validate merchant - skip if it's too generic or suspicious
                            if (isValidMerchant(details.merchant)) {
                                Log.d("SMSManager", "🔍 Processing merchant: '${details.merchant}'")
                                Log.d("SMSManager", "📄 SMS body: ${sms.smsBody.take(100)}...")
                                
                                // Get category using MerchantCategorizer with FULL SMS body
                                val suggestedCategory = determineCategoryId(details, sms.smsBody, categoryList)
                                
                                // Verify category exists in database, fallback to "others" if not
                                val validCategory = if (categoryList.any { it.id == suggestedCategory }) {
                                    suggestedCategory
                                } else {
                                    Log.w("SMSManager", "⚠️ Category '$suggestedCategory' not found in database, using 'others'")
                                    "others"
                                }
                                
                                Log.d("SMSManager", "🎯 Final category for '${details.merchant}': $validCategory")
                                
                                // Extract bank name from SMS
                                val bankName = extractBankNameFromSMS(sms.smsBody, sms.sender)
                                
                                val transaction = Transaction(
                                    id = UUID.randomUUID().toString(),
                                    amount = details.amount,
                                    type = details.type,
                                    merchant = details.merchant,
                                    categoryId = validCategory,
                                    confidence = 85.0f, // High confidence for SMS-based transactions
                                    date = sms.timestamp,
                                    description = details.description,
                                    smsBody = sms.smsBody,
                                    bankName = bankName,
                                    isManuallySet = false // Mark as auto-categorized
                                )
                                
                                Log.d("SMSManager", "💾 ===== SAVING TRANSACTION =====")
                                Log.d("SMSManager", "📝 Merchant: ${details.merchant}")
                                Log.d("SMSManager", "📝 Amount: ₹${details.amount}")
                                Log.d("SMSManager", "📝 Type: ${details.type}")
                                Log.d("SMSManager", "📝 CategoryId: '$validCategory' (length: ${validCategory.length})")
                                Log.d("SMSManager", "📝 Date: ${java.util.Date(sms.timestamp)}")
                                Log.d("SMSManager", "📝 isManuallySet: false")
                                
                                database.transactionDao().insertTransaction(transaction)
                                result.transactionsCreated++
                                
                                // Verify it was saved correctly
                                val savedTransaction = database.transactionDao().getTransactionById(transaction.id)
                                Log.d("SMSManager", "✅ ===== VERIFICATION =====")
                                Log.d("SMSManager", "✅ Saved categoryId: '${savedTransaction?.categoryId}' (length: ${savedTransaction?.categoryId?.length})")
                                Log.d("SMSManager", "✅ Is null? ${savedTransaction?.categoryId == null}")
                                Log.d("SMSManager", "✅ Is empty? ${savedTransaction?.categoryId?.isEmpty()}")
                                Log.d("SMSManager", "✅ Created & Verified: ₹${details.amount} at ${details.merchant} → Category: ${savedTransaction?.categoryId}")
                            } else {
                                Log.d("SMSManager", "⚠️ Skipping invalid merchant: ${details.merchant}")
                            }
                        } else {
                            Log.d("SMSManager", "⚠️ Could not extract valid data from SMS: ${sms.body.take(50)}...")
                        }
                    } catch (e: Exception) {
                        Log.e("SMSManager", "❌ Error processing SMS: ${e.message}", e)
                    }
                }
                
                Log.d("SMSManager", "✅ SMS processing completed successfully!")
                Log.d("SMSManager", "📊 FINAL RESULTS:")
                Log.d("SMSManager", "   📱 Total SMS found: ${result.smsFound}")
                Log.d("SMSManager", "   💳 Transaction SMS: ${result.transactionSmsFound}")
                Log.d("SMSManager", "   💾 SMS processed: ${result.smsProcessed}")
                Log.d("SMSManager", "   ✅ Transactions created: ${result.transactionsCreated}")
                
                result.success = true
                
            } catch (e: Exception) {
                result.error = "Error: ${e.message}"
                Log.e("SMSManager", "SMS processing failed", e)
            }
            
            result
        }
    }
    
    private fun readSMSFromDevice(): List<PaymentSms> {
        val smsList = mutableListOf<PaymentSms>()
        
        try {
            // Read from all SMS (inbox + sent + drafts)
            val uri = Uri.parse("content://sms")
            val projection = arrayOf("_id", "address", "body", "date")
            
            // Read ALL SMS messages from last 6 months, ordered by date DESC
            val sixMonthsAgo = System.currentTimeMillis() - (6 * 30 * 24 * 60 * 60 * 1000L)
            val selection = "date >= ?"
            val selectionArgs = arrayOf(sixMonthsAgo.toString())
            
            Log.d("SMSManager", "🔍 Reading SMS from last 6 months (since ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(sixMonthsAgo))})")
            
            val cursor = context.contentResolver.query(
                uri, 
                projection, 
                selection,
                selectionArgs,
                "date DESC"
            )
            
            cursor?.use { c ->
                val idIndex = c.getColumnIndexOrThrow("_id")
                val addressIndex = c.getColumnIndexOrThrow("address")
                val bodyIndex = c.getColumnIndexOrThrow("body")
                val dateIndex = c.getColumnIndexOrThrow("date")
                
                while (c.moveToNext()) {
                    val id = c.getString(idIndex)
                    val address = c.getString(addressIndex) ?: "Unknown"
                    val body = c.getString(bodyIndex) ?: ""
                    val timestamp = c.getLong(dateIndex)
                    val formattedDate = dateFormatter.format(Date(timestamp))
                    
                    val sms = PaymentSms(
                        id = UUID.randomUUID().toString(), // Use UUID to avoid conflicts
                        sender = address,
                        smsBody = body,
                        timestamp = timestamp,
                        isProcessed = false
                    )
                    
                    smsList.add(sms)
                }
            }
            
        } catch (e: Exception) {
            Log.e("SMSManager", "❌ Error reading SMS: ${e.message}", e)
        }
        
        Log.d("SMSManager", "📱 Total SMS read from device: ${smsList.size}")
        
        return smsList
    }
    
    private fun isTransactionSMS(body: String, sender: String): Boolean {
        val lowerCaseBody = body.lowercase()
        val senderUpper = sender.uppercase()
        
        Log.d("SMSManager", "🔍 Checking SMS from $sender: ${body.take(50)}...")
        
        // Known bank/payment service senders (80+ banks)
        val bankSenders = BankConstants.BANK_SENDERS
        
        // Transaction keywords (comprehensive list)
        val transactionKeywords = BankConstants.TRANSACTION_KEYWORDS
        
        // Amount patterns (comprehensive list)
        val amountPatterns = BankConstants.AMOUNT_PATTERNS
        
        // Banking/Payment terms
        val bankingTerms = BankConstants.BANKING_TERMS
        
        // Check conditions
        val isFromBank = bankSenders.any { senderUpper.contains(it) }
        val hasTransactionKeyword = transactionKeywords.any { lowerCaseBody.contains(it) }
        val hasAmountPattern = amountPatterns.any { lowerCaseBody.contains(it) } || 
                              body.matches(Regex(".*(?:(?:₹|rs\\.?|inr)\\s*[0-9,]+(?:\\.[0-9]{1,2})?|(?:debited|credited)\\s+by\\s+[0-9,]+(?:\\.[0-9]{1,2})?).*", RegexOption.IGNORE_CASE))
        val hasBankingTerm = bankingTerms.any { lowerCaseBody.contains(it) }
        
        // More lenient logic: (bank sender OR transaction keyword) AND amount pattern
        val isTransaction = (isFromBank || hasTransactionKeyword) && hasAmountPattern
        
        if (isTransaction) {
            Log.d("SMSManager", "✅ TRANSACTION SMS detected from $sender")
        } else {
            Log.d("SMSManager", "❌ Not a transaction SMS from $sender (bank:$isFromBank, keyword:$hasTransactionKeyword, amount:$hasAmountPattern)")
        }
        
        return isTransaction
    }
    
    private fun extractBankNameFromSMS(smsBody: String, sender: String): String {
        val text = smsBody.uppercase()
        val senderUpper = sender.uppercase()
        
        // First try to identify from sender
        return when {
            senderUpper.contains("SBI") || senderUpper.contains("STATE BANK") -> "SBI"
            senderUpper.contains("HDFC") -> "HDFC Bank"
            senderUpper.contains("ICICI") -> "ICICI Bank"
            senderUpper.contains("AXIS") -> "Axis Bank"
            senderUpper.contains("KOTAK") -> "Kotak Mahindra"
            senderUpper.contains("IPPB") || senderUpper.contains("INDIA POST") -> "IPPB"
            senderUpper.contains("PAYTM") -> "Paytm"
            senderUpper.contains("PHONEPE") -> "PhonePe"
            senderUpper.contains("GPAY") || senderUpper.contains("GOOGLE PAY") -> "Google Pay"
            senderUpper.contains("BOB") || senderUpper.contains("BANK OF BARODA") -> "Bank of Baroda"
            senderUpper.contains("PNB") || senderUpper.contains("PUNJAB NATIONAL") -> "PNB"
            senderUpper.contains("CANARA") -> "Canara Bank"
            senderUpper.contains("UNION BANK") -> "Union Bank"
            senderUpper.contains("IDBI") -> "IDBI Bank"
            senderUpper.contains("YES BANK") -> "Yes Bank"
            
            // Then try to identify from SMS body content
            text.contains("SBI") || text.contains("STATE BANK") -> "SBI"
            text.contains("HDFC") -> "HDFC Bank"
            text.contains("ICICI") -> "ICICI Bank"
            text.contains("AXIS") -> "Axis Bank"
            text.contains("KOTAK") -> "Kotak Mahindra"
            text.contains("IPPB") || text.contains("INDIA POST") -> "IPPB"
            text.contains("PAYTM") -> "Paytm"
            text.contains("PHONEPE") -> "PhonePe"
            text.contains("GPAY") || text.contains("GOOGLE PAY") -> "Google Pay"
            text.contains("BOB") || text.contains("BANK OF BARODA") -> "Bank of Baroda"
            text.contains("PNB") || text.contains("PUNJAB NATIONAL") -> "PNB"
            text.contains("CANARA") -> "Canara Bank"
            text.contains("UNION BANK") -> "Union Bank"
            text.contains("IDBI") -> "IDBI Bank"
            text.contains("YES BANK") -> "Yes Bank"
            
            else -> "Other Banks"
        }
    }
    
    private fun determineCategoryId(
        details: com.koshpal_android.koshpalapp.engine.TransactionDetails,
        smsBody: String,
        categories: List<com.koshpal_android.koshpalapp.model.TransactionCategory>
    ): String {
        // Use MerchantCategorizer with 400+ keywords for accurate categorization
        // Pass the FULL SMS body (not just description) for better keyword matching
        val categoryId = MerchantCategorizer.categorizeTransaction(
            details.merchant,
            smsBody  // ✅ Full SMS body with all keywords
        )
        
        Log.d("SMSManager", "🏷️ Auto-categorized '${details.merchant}' → $categoryId (${MerchantCategorizer.getCategoryDisplayName(categoryId)})")
        return categoryId
    }
    
    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun isValidMerchant(merchant: String): Boolean {
        val cleanMerchant = merchant.trim().lowercase()
        
        // Skip if merchant is too short or generic
        if (cleanMerchant.length < 3) return false
        
        // Skip generic/suspicious merchants (WHOLE WORD matching only)
        val invalidMerchants = listOf(
            "unknown", "merchant", "payment", "transaction", "transfer", 
            "debit", "credit", "bank", "upi", "imps", "neft", "rtgs",
            "pos", "atm", "cash", "withdrawal", "deposit", "balance",
            "sms", "alert", "notification", "service", "charge", "fee"
        )
        
        // FIXED: Match whole words only, not substrings
        // This prevents "bankar" (surname) from being rejected due to "bank"
        val words = cleanMerchant.split("\\s+".toRegex())
        for (word in words) {
            if (word in invalidMerchants) {
                Log.d("SMSManager", "❌ Invalid merchant word detected: '$word' in '$merchant'")
                return false
            }
        }
        
        // Must contain at least one letter (not just numbers/symbols)
        if (!cleanMerchant.any { it.isLetter() }) return false
        
        Log.d("SMSManager", "✅ Valid merchant: $merchant")
        return true
    }
    
    suspend fun createSampleData(): ProcessResult {
        return withContext(Dispatchers.IO) {
            val result = ProcessResult()
            
            try {
                Log.d("SMSManager", "🧪 Starting sample data creation...")
                val database = KoshpalDatabase.getDatabase(context)
                val transactionDao = database.transactionDao()
                val categoryDao = database.categoryDao()
                
                // Ensure categories exist first
                Log.d("SMSManager", "📂 Ensuring default categories exist...")
                val existingCategories = categoryDao.getDefaultCategories()
                if (existingCategories.isEmpty()) {
                    val defaultCategories = TransactionCategory.getDefaultCategories()
                    categoryDao.insertCategories(defaultCategories)
                    Log.d("SMSManager", "✅ Inserted ${defaultCategories.size} default categories")
                }
                
                val sampleTransactions = createSampleTransactions()
                Log.d("SMSManager", "📝 Created ${sampleTransactions.size} sample transactions")
                
                // Check for duplicates before inserting
                sampleTransactions.forEach { transaction ->
                    try {
                        val existing = transactionDao.getTransactionsBySmsBody(transaction.smsBody ?: "")
                        if (existing == null) {
                            transactionDao.insertTransaction(transaction)
                            result.transactionsCreated++
                            Log.d("SMSManager", "✅ Inserted transaction: ${transaction.merchant} - ₹${transaction.amount}")
                        } else {
                            Log.d("SMSManager", "⏭️ Transaction already exists: ${transaction.merchant}")
                        }
                    } catch (e: Exception) {
                        Log.e("SMSManager", "❌ Error inserting transaction ${transaction.merchant}: ${e.message}")
                        throw e
                    }
                }
                
                result.success = true
                result.smsFound = sampleTransactions.size
                result.transactionSmsFound = sampleTransactions.size
                result.smsProcessed = sampleTransactions.size
                
                Log.d("SMSManager", "🎉 Sample data creation completed! Created ${result.transactionsCreated} transactions")
                
            } catch (e: Exception) {
                Log.e("SMSManager", "❌ Error creating sample data: ${e.message}", e)
                result.error = "Error creating sample data: ${e.message}"
                result.success = false
            }
            
            result
        }
    }
    
    private fun createSampleTransactions(): List<Transaction> {
        val currentTime = System.currentTimeMillis()
        
        // Create transactions with timestamps spread across current month
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Set to beginning of current month
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        val monthStart = calendar.timeInMillis
        
        Log.d("SMSManager", "📅 Creating sample transactions for ${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}")
        
        return listOf(
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 500.0,
                type = TransactionType.DEBIT,
                merchant = "Amazon India",
                categoryId = "shopping",
                confidence = 0.95f,
                date = monthStart + (1 * 24 * 60 * 60 * 1000), // 1 day into month
                description = "Online shopping",
                smsBody = "Your A/c debited by Rs.500.00 at AMAZON INDIA"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 1200.0,
                type = TransactionType.DEBIT,
                merchant = "Zomato",
                categoryId = "food",
                confidence = 0.90f,
                date = monthStart + (5 * 24 * 60 * 60 * 1000), // 5 days into month
                description = "Food delivery",
                smsBody = "Rs.1200 debited for UPI/ZOMATO"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 25000.0,
                type = TransactionType.CREDIT,
                merchant = "Salary Credit",
                categoryId = "salary",
                confidence = 0.98f,
                date = monthStart + (10 * 24 * 60 * 60 * 1000), // 10 days into month
                description = "Monthly salary",
                smsBody = "Your account credited with Rs.25000.00 Salary credit"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 350.0,
                type = TransactionType.DEBIT,
                merchant = "Uber",
                categoryId = "transport",
                confidence = 0.85f,
                date = monthStart + (12 * 24 * 60 * 60 * 1000), // 12 days into month
                description = "Cab ride",
                smsBody = "INR 350.00 debited for UBER TRIP"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 800.0,
                type = TransactionType.DEBIT,
                merchant = "DMart",
                categoryId = "grocery",
                confidence = 0.88f,
                date = monthStart + (15 * 24 * 60 * 60 * 1000), // 15 days into month
                description = "Grocery shopping",
                smsBody = "Rs.800 spent at DMART GROCERY"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 2500.0,
                type = TransactionType.DEBIT,
                merchant = "Flipkart",
                categoryId = "shopping",
                confidence = 0.92f,
                date = monthStart + (18 * 24 * 60 * 60 * 1000), // 18 days into month
                description = "Online shopping",
                smsBody = "₹2500 spent at FLIPKART"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 150.0,
                type = TransactionType.DEBIT,
                merchant = "Swiggy",
                categoryId = "food",
                confidence = 0.89f,
                date = currentTime - 604800000,
                description = "Food delivery",
                smsBody = "You paid ₹150 to SWIGGY via UPI"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 45000.0,
                type = TransactionType.CREDIT,
                merchant = "Salary Credit",
                categoryId = "salary",
                confidence = 0.98f,
                date = currentTime - 2592000000,
                description = "Monthly salary",
                smsBody = "Your salary Rs.45000 credited to account"
            )
        )
    }
}

data class ProcessResult(
    var success: Boolean = false,
    var smsFound: Int = 0,
    var transactionSmsFound: Int = 0,
    var smsProcessed: Int = 0,
    var transactionsCreated: Int = 0,
    var error: String? = null
)

