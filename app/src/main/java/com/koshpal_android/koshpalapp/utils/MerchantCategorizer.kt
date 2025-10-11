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

    // Merchant keyword mappings - 400+ keywords
    private val categoryKeywords = mapOf(
        CATEGORY_FOOD to listOf(
            // Food Delivery Apps
            "zomato", "swiggy", "uber eats", "ubereats", "foodpanda", "box8", "faasos", 
            "behrouz", "oven story", "lunch box", "the good bowl", "mandarin oak",
            // Pizza Chains
            "dominos", "domino's", "pizza hut", "pizzahut", "papa johns", "papajohns",
            "chicago pizza", "smokin joes", "la pinoz", "pizza corner",
            // Fast Food
            "mcdonald", "mcdonalds", "kfc", "burger king", "burgerking", "subway",
            "taco bell", "wendy's", "hardees", "popeyes", "five guys",
            // Coffee Chains
            "starbucks", "cafe coffee day", "ccd", "barista", "costa coffee", 
            "blue tokai", "third wave", "sleepy owl", "chai point", "chaayos",
            "tea post", "tea villa", "tea trails",
            // Cloud Kitchens
            "rebel foods", "eatfit", "fresh menu", "freshmenu", "innerchef",
            // Restaurants & Cafes
            "restaurant", "cafe", "bistro", "dine", "eatery", "dhaba", "mess",
            "canteen", "food court", "food", "kitchen", "barbeque", "bbq", "grill",
            // Indian Food
            "biryani", "haldiram", "bikanervala", "sagar ratna", "saravana bhavan",
            "moti mahal", "punjabi dhaba", "karim", "tunday kababi", "paradise biryani",
            // Bakery & Sweets
            "bakery", "cake", "monginis", "karachi bakery", "theobroma", "sweet",
            "mithai", "haldirams", "bikaji", "bikanerwala"
        ),
        
        CATEGORY_TRANSPORT to listOf(
            // Ride Sharing & Taxi
            "uber", "ola", "rapido", "namma yatri", "meru", "mega cabs", "savaari",
            "auto", "taxi", "cab", "rideshare", "carpool", "quick ride",
            "blu smart", "yulu", "bounce", "vogo", "mobycy", "bike rental",
            // Fuel Stations
            "petrol", "diesel", "fuel", "cng", "gas station",
            "hp", "hindustan petroleum", "iocl", "indian oil", "bharat petroleum", "bpcl",
            "shell", "essar", "reliance petroleum", "nayara energy",
            // Parking & Tolls
            "parking", "toll", "fastag", "paytm fastag", "parking lot",
            // Vehicle Services
            "car wash", "car service", "mechanic", "garage", "tyre", "battery",
            // Travel & Transportation (merged from TRAVEL category)
            "makemytrip", "mmt", "goibibo", "cleartrip", "yatra", "ixigo", "easemytrip",
            "redbus", "abhibus", "irctc", "train", "flight", "airline", "bus",
            "indigo", "spicejet", "air india", "vistara", "hotel", "oyo"
        ),
        
        CATEGORY_SHOPPING to listOf(
            // E-commerce Giants
            "amazon", "flipkart", "myntra", "ajio", "meesho", "snapdeal", "shopclues",
            "paytm mall", "tata cliq", "jiomart", "reliance digital", "croma",
            // Fashion & Apparel
            "nykaa", "purplle", "bewakoof", "koovs", "limeroad", "shein", "urbanic",
            "h&m", "zara", "uniqlo", "forever 21", "mango", "vero moda",
            "allen solly", "van heusen", "peter england", "louis philippe",
            "fabindia", "biba", "w for woman", "global desi",
            // Footwear & Sports
            "nike", "adidas", "puma", "reebok", "skechers", "woodland", "bata",
            "liberty", "red tape", "hush puppies", "clarks", "crocs",
            "decathlon", "sports station", "nike store", "adidas store",
            // Electronics & Gadgets
            "croma", "reliance digital", "vijay sales", "poorvika", "sangeetha",
            "apple store", "samsung", "mi store", "oneplus", "realme",
            // Jewelry & Accessories
            "tanishq", "malabar gold", "kalyan jewellers", "joyalukkas", "pc jeweller",
            "caratlane", "bluestone", "giva", "melorra",
            // Retail Stores
            "dmart", "more", "spencer", "reliance fresh", "reliance smart",
            "lifestyle", "westside", "pantaloons", "max fashion", "brand factory",
            "shoppers stop", "central", "big bazaar", "vishal mega mart",
            // Books & Stationery
            "amazon books", "flipkart books", "crossword", "landmark", "odyssey",
            "stationery", "office depot"
        ),
        
        CATEGORY_ENTERTAINMENT to listOf(
            // Streaming Services
            "netflix", "prime video", "amazon prime", "hotstar", "disney", "disney+",
            "zee5", "sonyliv", "voot", "alt balaji", "mx player", "eros now",
            "jiocinema", "youtube premium", "youtube", "apple tv",
            // Music Streaming
            "spotify", "apple music", "gaana", "wynk", "jiosaavn", "hungama",
            "amazon music", "youtube music", "tidal", "soundcloud",
            // Gaming Platforms
            "steam", "epic games", "playstation", "xbox", "nintendo", "origin",
            "google play", "app store", "pubg", "free fire", "cod", "valorant",
            "roblox", "minecraft", "fortnite", "genshin impact",
            // Movies & Theaters
            "bookmyshow", "paytm insider", "pvr", "inox", "cinepolis", "carnival",
            "movie", "cinema", "multiplex", "theater", "film",
            // Events & Concerts
            "insider", "townscript", "allevents", "meraevents", "concert",
            "show", "event", "ticket", "live show"
        ),
        
        CATEGORY_BILLS to listOf(
            // Electricity & Utilities
            "electricity", "electric bill", "power", "bescom", "msedcl", "tata power",
            "adani electricity", "cesc", "dhbvn", "bses", "torrent power",
            "water", "water bill", "municipal", "corporation",
            "gas", "lpg", "cylinder", "indane", "hp gas", "bharat gas",
            // Telecom & Mobile
            "airtel", "jio", "reliance jio", "vi", "vodafone", "idea", "vodafone idea",
            "bsnl", "mtnl", "mobile", "recharge", "prepaid", "postpaid",
            "mobile bill", "phone bill", "sim", "data pack",
            // Internet & Broadband
            "broadband", "internet", "wifi", "fiber", "airtel xstream", "jio fiber",
            "act fibernet", "hathway", "tikona", "spectranet", "you broadband",
            // DTH & Cable
            "tata sky", "tata play", "dish tv", "sun direct", "d2h", "airtel digital tv",
            "dth", "cable", "cable tv", "den", "hathway cable",
            // Insurance
            "insurance", "premium", "policy", "lic", "life insurance",
            "health insurance", "car insurance", "bike insurance",
            // Loan EMI
            "emi", "loan", "home loan", "car loan", "personal loan", "credit card bill",
            // Municipal & Government
            "property tax", "house tax", "municipal tax", "challan", "fine"
        ),
        
        CATEGORY_HEALTHCARE to listOf(
            // Hospitals & Clinics
            "hospital", "clinic", "nursing home", "medical center", "health center",
            "apollo", "fortis", "max healthcare", "medanta", "manipal", "narayana",
            "columbia asia", "cloudnine", "rainbow", "motherhood", "nova",
            "aiims", "safdarjung", "pgimer", "government hospital",
            // Doctors & Specialists
            "doctor", "dr", "physician", "surgeon", "dentist", "dental",
            "orthodontist", "dermatologist", "cardiologist", "neurologist",
            "pediatrician", "gynecologist", "ophthalmologist", "ent",
            // Pharmacy & Medicine
            "pharmacy", "medicine", "medical store", "chemist", "drug store",
            "netmeds", "1mg", "pharmeasy", "medlife", "apollo pharmacy",
            "medplus", "wellness forever", "guardian pharmacy",
            // Diagnostics & Labs
            "lab", "laboratory", "pathology", "diagnostic", "test", "scan",
            "thyrocare", "dr lal pathlabs", "metropolis", "srl diagnostics",
            "vijaya diagnostics", "quest diagnostics",
            // Health & Wellness
            "gym", "fitness", "yoga", "cult fit", "cultfit", "gold's gym",
            "anytime fitness", "fitness first", "talwalkars", "snap fitness"
        ),
        
        CATEGORY_EDUCATION to listOf(
            // Online Learning Platforms
            "udemy", "coursera", "edx", "udacity", "pluralsight", "skillshare",
            "linkedin learning", "masterclass", "domestika", "treehouse",
            // Indian EdTech
            "unacademy", "byju", "vedantu", "toppr", "white hat jr", "whitehatjr",
            "upgrad", "great learning", "simplilearn", "scaler", "coding ninjas",
            "physics wallah", "pw", "allen", "aakash", "fiitjee", "resonance",
            // Schools & Colleges
            "school", "college", "university", "institute", "academy",
            "iit", "nit", "iim", "bits", "vit", "srm", "amity",
            "dps", "ryan", "kendriya vidyalaya", "navodaya",
            // Coaching & Tuition
            "tuition", "coaching", "classes", "tutorial", "training center",
            "ielts", "toefl", "gre", "gmat", "cat", "jee", "neet",
            // Books & Study Material
            "textbook", "study material", "notes", "question bank",
            // Exam Fees
            "exam fee", "application fee", "admission fee", "registration"
        ),
        
        CATEGORY_GROCERY to listOf(
            // Online Grocery Delivery
            "bigbasket", "grofers", "blinkit", "zepto", "dunzo", "swiggy instamart",
            "jiomart", "amazon fresh", "amazon pantry", "flipkart grocery",
            "milk basket", "country delight", "licious", "fresho",
            // Supermarkets & Hypermarkets
            "dmart", "reliance fresh", "reliance smart", "more", "spencer",
            "big bazaar", "star bazaar", "hypercity", "easyday",
            "nature's basket", "foodhall", "le marche",
            // General Terms
            "grocery", "supermarket", "kirana", "provision store",
            "vegetables", "fruits", "dairy", "milk", "bread"
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
