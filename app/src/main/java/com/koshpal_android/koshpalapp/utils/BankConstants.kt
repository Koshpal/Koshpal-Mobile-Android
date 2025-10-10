package com.koshpal_android.koshpalapp.utils

/**
 * Centralized bank and payment service constants
 * Comprehensive list of 80+ Indian banks and payment services
 */
object BankConstants {
    
    // ALL INDIAN BANK SENDERS (80+ banks)
    val BANK_SENDERS = listOf(
        // Major Private Banks
        "SBIINB", "SBISMS", "SBIUPI", "SBIBNK",  // State Bank of India
        "HDFCBK", "HDFCSM", "HDFCCC", "HDFCBN",   // HDFC Bank
        "ICICIB", "ICICIC", "ICICBN", "ICICIS",   // ICICI Bank
        "AXISBK", "AXISCC", "AXISBN", "AXISSM",   // Axis Bank
        "KOTAKB", "KOTAKS", "KOTAKM", "KOTAK",    // Kotak Mahindra
        "YESBNK", "YESCARD", "YESBAN", "YESSMS",  // Yes Bank
        "IDFCFB", "IDFCBN", "IDFCSM",             // IDFC First Bank
        "RBLBNK", "RBLCARD", "RBLBAN",            // RBL Bank
        "INDUSB", "INDUSIND", "INDUSL",           // IndusInd Bank
        
        // Major Public Sector Banks
        "PNBSMS", "PNBBNK", "PUNJAB",             // Punjab National Bank
        "BOBSMS", "BOBBNK", "BARODA",             // Bank of Baroda
        "CANBKS", "CANARA", "CNRBNK",             // Canara Bank
        "UNISBI", "UNION", "UNIBNK",              // Union Bank of India
        "IOBNET", "IOBSMS", "IOBBNK",             // Indian Overseas Bank
        "CORPBK", "CORPBN", "CORPOR",             // Corporation Bank
        "ANDHBK", "ANDHRA", "ANDHBN",             // Andhra Bank
        "ALHABK", "ALLAHABAD", "ALHABK",          // Allahabad Bank
        "CENTBK", "CENTRAL", "CENBAN",            // Central Bank of India
        "INDBKS", "INDIAN", "INDBNK",             // Indian Bank
        "MHABKS", "MAHARA", "MHABNK",             // Bank of Maharashtra
        "SYNDBK", "SYNDIC", "SYNDBN",             // Syndicate Bank
        "UCOBKS", "UCOBAN", "UCOBNK",             // UCO Bank
        "VIJBKS", "VIJAYA", "VIJBNK",             // Vijaya Bank
        "BOIMSG", "BOIBKS", "BOIBNK",             // Bank of India
        "ORBNKS", "ORIENT", "ORBBNK",             // Oriental Bank
        
        // Foreign Banks
        "SCBANK", "STANCH", "SCBSMS",             // Standard Chartered
        "CITIBK", "CITIBN", "CITISMS",            // Citibank
        "HSBCIN", "HSBCSM", "HSBCBN",             // HSBC India
        "DEUTSC", "DBSIND", "DBSSMS",             // Deutsche Bank / DBS
        
        // Small Finance Banks
        "UJJIVN", "UJJBNK",                       // Ujjivan Small Finance Bank
        "EQUITB", "EQUITS",                       // Equitas Small Finance Bank
        "AUBSMS", "AUBBNK",                       // AU Small Finance Bank
        "FINCBN", "FINCARE",                      // Fincare Small Finance Bank
        "ESFBKS", "ESFBSM",                       // ESAF Small Finance Bank
        "CAPFIN", "CAPITAL",                      // Capital Small Finance Bank
        "NORTHEAST", "NESF",                      // North East Small Finance Bank
        "SURYOD", "SURYBN",                       // Suryoday Small Finance Bank
        
        // Payment Banks
        "AIRPAY", "AIRTEL", "AIRTLP",             // Airtel Payments Bank
        "PAYTMB", "PAYTPB", "PAYTMP",             // Paytm Payments Bank
        "FINDPB", "FINOPB",                       // Fino Payments Bank
        "JIOPPB", "JIOPAY",                       // Jio Payments Bank
        
        // Credit Cards
        "SBCARD", "SBICRD", "SBICAR",             // SBI Card
        "HDFCCC", "HDFCRD",                       // HDFC Credit Card
        "ICICIC", "ICICRD",                       // ICICI Credit Card
        "AXISCC", "AXICRD",                       // Axis Credit Card
        "AMEXIN", "AMEXCD", "AMEXCR",             // American Express
        "YESCARD", "YESCRD",                      // Yes Bank Card
        "RBLCARD", "RBLCRD",                      // RBL Credit Card
        "SCBCRD", "STNCRD",                       // Standard Chartered Card
        "CITICR", "CITICD",                       // Citi Credit Card
        "HSBCCC", "HSBCRD",                       // HSBC Credit Card
        
        // UPI & Payment Apps
        "PAYTM", "PAYTMS", "PAYTMW",              // Paytm
        "GPAY", "GOOGPAY", "GOOGLEPAY",           // Google Pay
        "PHONEPE", "PHONPE", "PHNEPE",            // PhonePe
        "AMAZONP", "AMZPAY", "AMAZONPAY",         // Amazon Pay
        "BHARTP", "BHRTPE", "BHARAT",             // BharatPe
        "MOBIKW", "MOBIWK", "MOBIKWIK",           // Mobikwik
        "FREECHARGE", "FREECH", "FREECHRG",       // Freecharge
        "OXIGEN", "OXYGN",                        // Oxigen Wallet
        "JIOPAY", "JIOWLT",                       // Jio Money
        "WHATSAPP", "WHTAPP", "FBPAY",            // WhatsApp Pay
        
        // Regional Banks
        "KARVYB", "KARVY",                        // Karvy Bank
        "JKBANK", "JKBSMS", "JKBBNK",             // Jammu & Kashmir Bank
        "DHANLA", "DHANBN",                       // Dhanlaxmi Bank
        "KARNBK", "KARNBN",                       // Karnataka Bank
        "KARVYB", "KVBBNK",                       // Karur Vysya Bank
        "LCBSMS", "LAKSHMI",                      // Lakshmi Vilas Bank
        "TMIBNK", "TAMILNAD",                     // Tamilnad Mercantile Bank
        "CITYBN", "CITYUB",                       // City Union Bank
        "SARASB", "SARASWAT",                     // Saraswat Bank
        "FEDERAL", "FEDBNK",                      // Federal Bank
        "SOUTHI", "SOUTHBN",                      // South Indian Bank
        "DCBBKS", "DCBBAN",                       // DCB Bank
        "KTKBAN", "KATAK",                        // Karnataka Bank
        
        // Co-operative Banks
        "APEXCO", "APEXBN",                       // Apex Bank
        "NKGSB", "NKGBAN",                        // NKGSB Co-op Bank
        "COSBAN", "COSMOS",                       // Cosmos Bank
        
        // Additional variants and aliases
        "BANKSMS", "BNKSMS", "MYBANKAPP",
        "UPIAPP", "UPISMS", "BHIMSMS",
        "WALLET", "EWALET", "DIGIWLT"
    )
    
    // Transaction keywords
    val TRANSACTION_KEYWORDS = listOf(
        "debited", "credited", "debit", "credit", 
        "withdrawn", "deposited", "paid", "received",
        "spent", "transferred", "transaction", "txn",
        "purchase", "refund", "cashback", "reward", 
        "charges", "fee", "payment"
    )
    
    // Amount patterns
    val AMOUNT_PATTERNS = listOf(
        "rs.", "rs ", "rs-", "inr", "₹", "rupees", 
        "amount", "amt", "debited by", "credited by",
        "debited of", "credited of", "withdrawn of"
    )
    
    // Banking terms
    val BANKING_TERMS = listOf(
        "account", "a/c", "ac", "acct",
        "available balance", "avbl bal", "bal", "balance",
        "upi", "imps", "neft", "rtgs", "atm", "pos", 
        "card", "wallet", "mandate", "autopay",
        "trf to", "transferred to", "towards"
    )
}

