package com.koshpal_android.koshpalapp.engine

import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.model.TransactionType
import java.util.regex.Pattern
import kotlin.math.max

class TransactionCategorizationEngine {
    
    private val categoryRules = mapOf(
        "food" to listOf(
            "zomato", "swiggy", "restaurant", "cafe", "food", "dining", "pizza", "burger", 
            "dominos", "kfc", "mcdonalds", "subway", "starbucks", "ccd", "barista", 
            "food panda", "uber eats", "dunzo", "grofers food"
        ),
        "grocery" to listOf(
            "bigbasket", "grofers", "blinkit", "zepto", "dunzo", "dmart", "grocery", 
            "supermarket", "vegetables", "fruits", "reliance fresh", "spencer's", 
            "more supermarket", "nature's basket", "godrej nature's basket"
        ),
        "transport" to listOf(
            "uber", "ola", "metro", "bus", "petrol", "fuel", "taxi", "auto", "rapido", 
            "namma yatri", "quick ride", "bounce", "vogo", "yulu", "lime", "bird", 
            "indian oil", "bharat petroleum", "hp petrol", "shell", "essar"
        ),
        "bills" to listOf(
            "electricity", "water", "gas", "internet", "mobile", "recharge", "broadband", 
            "wifi", "postpaid", "prepaid", "airtel", "jio", "vi", "bsnl", "act fibernet", 
            "hathway", "tikona", "you broadband", "spectranet", "railwire"
        ),
        "education" to listOf(
            "fees", "course", "book", "education", "school", "college", "university", 
            "tuition", "coaching", "byju's", "unacademy", "vedantu", "white hat jr", 
            "coursera", "udemy", "skillshare", "khan academy"
        ),
        "entertainment" to listOf(
            "netflix", "amazon prime", "hotstar", "spotify", "movie", "cinema", "theatre", 
            "gaming", "youtube premium", "zee5", "sonyliv", "voot", "alt balaji", 
            "mx player", "jio cinema", "book my show", "paytm movies", "pvr", "inox"
        ),
        "healthcare" to listOf(
            "hospital", "doctor", "medicine", "pharmacy", "medical", "health", "clinic", 
            "apollo", "fortis", "max healthcare", "manipal", "narayana", "aster", 
            "medplus", "apollo pharmacy", "netmeds", "1mg", "pharmeasy"
        ),
        "shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio", "shopping", "clothes", "fashion", 
            "electronics", "gadgets", "nykaa", "jabong", "snapdeal", "paytm mall", 
            "tata cliq", "shoppers stop", "lifestyle", "pantaloons", "westside"
        ),
        "salary" to listOf(
            "salary", "credited", "income", "bonus", "incentive", "refund", "cashback", 
            "interest credited", "dividend", "commission", "freelance", "consulting"
        )
    )
    
    // Enhanced amount pattern - supports multiple formats
    // Format 1: Rs.500, ₹500, INR 500
    // Format 2: debited by 2000.0, credited by 5000.0 (SBI UPI format)
    private val amountPattern = Pattern.compile("(?:(?:rs\\.?|inr|₹)\\s*|(?:debited|credited)\\s+by\\s+)(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    
    // Enhanced merchant pattern - supports multiple formats
    // Format 1: at AMAZON, from ZOMATO, to SWIGGY
    // Format 2: trf to NAME, transferred to NAME (UPI transfers)
    // Format 3: towards GOOGLE (UPI mandate)
    // Format 4: from NAME thru BANK (IPPB format)
    private val merchantPattern = Pattern.compile("(?:at|from|to|trf\\s+to|transferred\\s+to|towards)\\s+([a-zA-Z0-9\\s&.-]+?)(?:\\s+(?:on|from|refno|umn|thru|through)|\\.|$)", Pattern.CASE_INSENSITIVE)
    
    fun categorizeTransaction(
        smsBody: String, 
        merchant: String, 
        amount: Double,
        type: TransactionType,
        categories: List<TransactionCategory>
    ): Pair<TransactionCategory, Float> {
        
        val cleanSms = smsBody.lowercase().trim()
        val cleanMerchant = merchant.lowercase().trim()
        
        // Special handling for salary/income transactions
        if (type == TransactionType.CREDIT) {
            val salaryCategory = categories.find { it.id == "salary" }
            if (salaryCategory != null) {
                val salaryConfidence = calculateSalaryConfidence(cleanSms, amount)
                if (salaryConfidence > 0.6f) {
                    return Pair(salaryCategory, salaryConfidence)
                }
            }
        }
        
        var bestMatch: TransactionCategory? = null
        var bestScore = 0f
        
        for (category in categories.filter { it.id != "salary" && it.id != "others" }) {
            val score = calculateCategoryScore(cleanSms, cleanMerchant, category)
            if (score > bestScore) {
                bestScore = score
                bestMatch = category
            }
        }
        
        // If no good match found, use "others" category
        if (bestScore < 0.3f || bestMatch == null) {
            val othersCategory = categories.find { it.id == "others" }
                ?: categories.first() // Fallback
            return Pair(othersCategory, 0.2f)
        }
        
        return Pair(bestMatch, bestScore)
    }
    
    private fun calculateCategoryScore(smsBody: String, merchant: String, category: TransactionCategory): Float {
        var score = 0f
        val keywords = categoryRules[category.id] ?: category.keywords
        
        // Check SMS body for keywords
        for (keyword in keywords) {
            if (smsBody.contains(keyword.lowercase())) {
                score += when {
                    keyword.length > 8 -> 0.4f // Longer, more specific keywords get higher score
                    keyword.length > 5 -> 0.3f
                    else -> 0.2f
                }
            }
        }
        
        // Check merchant name for keywords
        for (keyword in keywords) {
            if (merchant.contains(keyword.lowercase())) {
                score += 0.5f // Merchant match is more reliable
            }
        }
        
        // Bonus for exact merchant matches
        if (keywords.any { merchant.contains(it.lowercase()) && it.length > 4 }) {
            score += 0.3f
        }
        
        // Cap the score at 1.0
        return minOf(score, 1.0f)
    }
    
    private fun calculateSalaryConfidence(smsBody: String, amount: Double): Float {
        var confidence = 0f
        
        val salaryKeywords = listOf("salary", "credited", "income", "bonus", "incentive")
        for (keyword in salaryKeywords) {
            if (smsBody.contains(keyword)) {
                confidence += 0.3f
            }
        }
        
        // Higher amounts are more likely to be salary
        when {
            amount > 50000 -> confidence += 0.4f
            amount > 20000 -> confidence += 0.3f
            amount > 10000 -> confidence += 0.2f
        }
        
        // Check for typical salary patterns
        if (smsBody.contains("monthly") || smsBody.contains("payroll")) {
            confidence += 0.3f
        }
        
        return minOf(confidence, 1.0f)
    }
    
    fun extractTransactionDetails(smsBody: String): TransactionDetails {
        val amount = extractAmount(smsBody)
        val merchant = extractMerchant(smsBody)
        val type = determineTransactionType(smsBody)
        
        return TransactionDetails(
            amount = amount,
            merchant = merchant,
            type = type,
            description = generateDescription(smsBody, merchant, type)
        )
    }
    
    private fun extractAmount(smsBody: String): Double {
        val matcher = amountPattern.matcher(smsBody)
        if (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", "") ?: "0"
            return amountStr.toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }
    
    private fun extractMerchant(smsBody: String): String {
        val matcher = merchantPattern.matcher(smsBody)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: "Unknown"
        }
        
        // Fallback: look for common merchant patterns
        val commonMerchants = listOf("amazon", "flipkart", "zomato", "swiggy", "uber", "ola")
        for (merchant in commonMerchants) {
            if (smsBody.lowercase().contains(merchant)) {
                return merchant.capitalize()
            }
        }
        
        return "Unknown Merchant"
    }
    
    private fun determineTransactionType(smsBody: String): TransactionType {
        val lowerSms = smsBody.lowercase()
        return when {
            lowerSms.contains("debited") || lowerSms.contains("debit") || 
            lowerSms.contains("spent") || lowerSms.contains("paid") -> TransactionType.DEBIT
            
            lowerSms.contains("credited") || lowerSms.contains("credit") || 
            lowerSms.contains("received") || lowerSms.contains("refund") -> TransactionType.CREDIT
            
            lowerSms.contains("transfer") -> TransactionType.TRANSFER
            
            else -> TransactionType.DEBIT // Default assumption
        }
    }
    
    private fun generateDescription(smsBody: String, merchant: String, type: TransactionType): String {
        val action = when (type) {
            TransactionType.DEBIT -> "Payment to"
            TransactionType.CREDIT -> "Payment from"
            TransactionType.TRANSFER -> "Transfer"
        }
        
        return if (merchant != "Unknown Merchant") {
            "$action $merchant"
        } else {
            "Transaction via SMS"
        }
    }
    
    fun updateCategoryRules(transaction: com.koshpal_android.koshpalapp.model.Transaction, newCategory: TransactionCategory) {
        // This method can be used to learn from user corrections
        // For now, it's a placeholder for future ML implementation
        // In a production app, you might want to store these corrections
        // and use them to improve future categorizations
    }
}

data class TransactionDetails(
    val amount: Double,
    val merchant: String,
    val type: TransactionType,
    val description: String
)
