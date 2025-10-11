package com.koshpal_android.koshpalapp.utils

import android.util.Log

object MerchantCategorizer {

    private const val TAG = "MerchantCategorizer"

    // Category ID constants (MUST match TransactionCategory.getDefaultCategories())
    private const val CATEGORY_FOOD = "food"
    private const val CATEGORY_GROCERY = "grocery"
    private const val CATEGORY_TRANSPORT = "transport"
    private const val CATEGORY_BILLS = "bills"
    private const val CATEGORY_EDUCATION = "education"
    private const val CATEGORY_ENTERTAINMENT = "entertainment"
    private const val CATEGORY_HEALTHCARE = "healthcare"
    private const val CATEGORY_SHOPPING = "shopping"
    private const val CATEGORY_SALARY = "salary"
    private const val CATEGORY_OTHERS = "others"

    // Merchant keyword mappings - Only 20-30 highly specific brand names per category
    private val categoryKeywords = mapOf(
        CATEGORY_FOOD to listOf(
            // Major food delivery apps & restaurants only
            "zomato", "swiggy", "ubereats", "foodpanda",
            "dominos", "pizzahut", "mcdonalds", "kfc", "burgerking", "subway",
            "starbucks", "ccd", "barista", "dunkin",
            "barbeque nation", "haldiram", "bikanervala"
        ),
        
        CATEGORY_TRANSPORT to listOf(
            // Only major ride-sharing, fuel, and travel brands
            "uber", "ola", "rapido", "nammayatri", "meru",
            "iocl", "indianoil", "bpcl", "bharatpetroleum", "hpcl", "shell",
            "fastag", "makemytrip", "goibibo", "cleartrip", "yatra",
            "redbus", "irctc", "indigo", "spicejet", "airindia", "vistara", "oyo"
        ),
        
        CATEGORY_SHOPPING to listOf(
            // Only major e-commerce and retail brands
            "amazon", "flipkart", "myntra", "ajio", "meesho", "snapdeal",
            "nykaa", "croma", "reliancedigital", "tanishq", "lifestyle",
            "westside", "pantaloons", "shoppersstop", "decathlon",
            "nike", "adidas", "zara", "h&m", "uniqlo"
        ),
        
        CATEGORY_ENTERTAINMENT to listOf(
            // Only major streaming and entertainment brands
            "netflix", "amazonprime", "hotstar", "disney", "zee5", "sonyliv",
            "spotify", "gaana", "wynk", "jiosaavn",
            "bookmyshow", "pvr", "inox", "cinepolis",
            "steam", "playstation", "xbox", "googleplay", "appstore"
        ),
        
        CATEGORY_BILLS to listOf(
            // Only major telecom, utilities, and bill payment brands
            "airtel", "jio", "vi", "vodafone", "bsnl",
            "tatasky", "tataplay", "dishtv", "actfibernet",
            "bescom", "msedcl", "tatapower", "adanielectricity"
        ),
        
        CATEGORY_HEALTHCARE to listOf(
            // Only major hospitals, pharmacies, and fitness brands
            "apollo", "fortis", "maxhealthcare", "medanta", "manipal", "narayana",
            "netmeds", "1mg", "pharmeasy", "apollopharmacy", "medplus",
            "thyrocare", "lalpathlabs", "metropolis", "cultfit", "goldsgym"
        ),
        
        CATEGORY_EDUCATION to listOf(
            // Only major education platforms and institutions
            "udemy", "coursera", "unacademy", "byjus", "vedantu",
            "upgrad", "simplilearn", "scaler", "physicswallah",
            "allen", "aakash", "fiitjee"
        ),
        
        CATEGORY_GROCERY to listOf(
            // Only major grocery delivery and supermarket brands
            "bigbasket", "blinkit", "zepto", "dunzo", "swiggyinstamart",
            "jiomart", "dmart", "reliancefresh", "more", "spencer",
            "bigbazaar", "licious"
        ),
        
        CATEGORY_SALARY to listOf(
            // Salary & Income Keywords
            "salary", "credited", "income", "bonus", "incentive", "refund", 
            "cashback", "reward", "credit", "deposit", "received", "payment received"
        )
    )

    /**
     * Automatically categorize a transaction based on merchant name or SMS body
     * Matches if first 3-4 characters of merchant match keyword
     */
    fun categorizeTransaction(merchant: String, smsBody: String? = null): String {
        val merchantLower = merchant.lowercase().trim()
        val smsBodyLower = smsBody?.lowercase()?.trim() ?: ""

        Log.d(TAG, "🔍 ===== CATEGORIZING =====")
        Log.d(TAG, "📝 Merchant: '$merchant'")
        Log.d(TAG, "📝 Merchant (lowercase): '$merchantLower'")
        if (smsBody != null) {
            Log.d(TAG, "📝 SMS Body: ${smsBody.take(100)}...")
        }

        // Try to match with category keywords
        for ((category, keywords) in categoryKeywords) {
            for (keyword in keywords) {
                val keywordLower = keyword.lowercase().trim()
                
                // Skip very short keywords
                if (keywordLower.length < 3) continue
                
                // Method 1: Check if merchant name matches first 3-4 characters of keyword
                if (merchantLower.length >= 3) {
                    val merchantFirst4 = merchantLower.take(4)
                    val keywordFirst4 = keywordLower.take(4)
                    
                    // If first 4 chars match
                    if (merchantFirst4 == keywordFirst4) {
                        Log.d(TAG, "✅ Match: '$merchant' first 4 chars match '$keyword' → $category")
                        return category
                    }
                    
                    // If first 3 chars match (fallback)
                    val merchantFirst3 = merchantLower.take(3)
                    val keywordFirst3 = keywordLower.take(3)
                    if (merchantFirst3 == keywordFirst3) {
                        Log.d(TAG, "✅ Match: '$merchant' first 3 chars match '$keyword' → $category")
                        return category
                    }
                }
                
                // Method 2: Full keyword match in merchant or SMS body
                if (merchantLower.contains(keywordLower) || smsBodyLower.contains(keywordLower)) {
                    Log.d(TAG, "✅ Full match: '$keyword' found → $category")
                    return category
                }
            }
        }

        // Default to others if no match found
        Log.d(TAG, "⚠️ No match → $CATEGORY_OTHERS")
        return CATEGORY_OTHERS
    }

    /**
     * Get category name for display
     */
    fun getCategoryDisplayName(categoryId: String): String {
        return when (categoryId) {
            CATEGORY_FOOD -> "Food & Dining"
            CATEGORY_GROCERY -> "Grocery"
            CATEGORY_TRANSPORT -> "Transportation"
            CATEGORY_BILLS -> "Bills & Utilities"
            CATEGORY_EDUCATION -> "Education"
            CATEGORY_ENTERTAINMENT -> "Entertainment"
            CATEGORY_HEALTHCARE -> "Healthcare"
            CATEGORY_SHOPPING -> "Shopping"
            CATEGORY_SALARY -> "Salary & Income"
            else -> "Others"
        }
    }

    /**
     * Add custom merchant-category mapping
     */
    fun addCustomMapping(merchant: String, categoryId: String) {
        // This can be extended to support user-defined mappings
        Log.d(TAG, "📝 Custom mapping added: $merchant → $categoryId")
    }
}
