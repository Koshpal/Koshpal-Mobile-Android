package com.koshpal_android.koshpalapp.utils

/**
 * Centralized bank and payment service constants
 * Comprehensive list of 80+ Indian banks and payment services
 */
object BankConstants {
    
    // ALL INDIAN BANK SENDERS (80+ banks)
    val BANK_SENDERS = listOf(
        // Major Private Banks (core codes for flexible matching)
        "SBI",                                    // State Bank of India
        "HDFC",                                   // HDFC Bank
        "ICICI",                                  // ICICI Bank
        "AXIS",                                   // Axis Bank
        "KOTAK",                                  // Kotak Mahindra
        "YESBNK", "YESBANK",                      // Yes Bank
        "IDFC",                                   // IDFC First Bank
        "RBL",                                    // RBL Bank
        "INDUSIND",                               // IndusInd Bank
        
        // Major Public Sector Banks (core codes)
        "PNB", "PUNJAB",                          // Punjab National Bank
        "BOB", "BARODA",                          // Bank of Baroda
        "CANARA",                                 // Canara Bank
        "UNION",                                  // Union Bank of India
        "IOB",                                    // Indian Overseas Bank
        "CORPOR",                                 // Corporation Bank
        "ANDHRA",                                 // Andhra Bank
        "ALLAHABAD",                              // Allahabad Bank
        "CENTRAL",                                // Central Bank of India
        "INDIAN",                                 // Indian Bank
        "MAHARA",                                 // Bank of Maharashtra
        "SYNDIC",                                 // Syndicate Bank
        "UCO",                                    // UCO Bank
        "VIJAYA",                                 // Vijaya Bank
        "BOI",                                    // Bank of India
        "ORIENT",                                 // Oriental Bank
        
        // Foreign Banks (core codes)
        "STANCHART", "STANCH",                    // Standard Chartered
        "CITI",                                   // Citibank
        "HSBC",                                   // HSBC India
        "DEUTSCH", "DBS",                         // Deutsche Bank / DBS
        
        // Small Finance Banks (core codes)
        "UJJIVAN",                                // Ujjivan Small Finance Bank
        "EQUITAS",                                // Equitas Small Finance Bank
        "AUBANK",                                 // AU Small Finance Bank
        "FINCARE",                                // Fincare Small Finance Bank
        "ESAF",                                   // ESAF Small Finance Bank
        "CAPITAL",                                // Capital Small Finance Bank
        "NORTHEAST",                              // North East Small Finance Bank
        "SURYODAY",                               // Suryoday Small Finance Bank
        
        // Payment Banks
        "AIRPAY", "AIRTEL", "AIRTLP",             // Airtel Payments Bank
        "PAYTMB", "PAYTPB", "PAYTMP",             // Paytm Payments Bank
        "FINDPB", "FINOPB",                       // Fino Payments Bank
        "JIOPPB", "JIOPAY",                       // Jio Payments Bank
        "IPPB", "IPB",                            // India Post Payments Bank (flexible matching)
        
        // Credit Cards (already covered by bank codes above)
        "AMEX",                                   // American Express
        
        // UPI & Payment Apps (core codes)
        "PAYTM",                                  // Paytm
        "GPAY", "GOOGLEPAY",                      // Google Pay
        "PHONEPE",                                // PhonePe
        "AMAZONPAY",                              // Amazon Pay
        "BHARATPE",                               // BharatPe
        "MOBIKWIK",                               // Mobikwik
        "FREECHARGE",                             // Freecharge
        "OXIGEN",                                 // Oxigen Wallet
        "JIOMONEY",                               // Jio Money
        "WHATSAPP",                               // WhatsApp Pay
        
        // Regional Banks (core codes)
        "KARVY",                                  // Karvy Bank
        "JKBANK",                                 // Jammu & Kashmir Bank
        "DHANLAXMI",                              // Dhanlaxmi Bank
        "KARNATAKA",                              // Karnataka Bank
        "KARURVYSYA", "KVB",                      // Karur Vysya Bank
        "LAKSHMI",                                // Lakshmi Vilas Bank
        "TAMILNAD", "TMB",                        // Tamilnad Mercantile Bank
        "CITYUNION",                              // City Union Bank
        "SARASWAT",                               // Saraswat Bank
        "FEDERAL",                                // Federal Bank
        "SOUTHINDIAN",                            // South Indian Bank
        "DCB",                                    // DCB Bank
        
        // Co-operative Banks
        "NKGSB",                                  // NKGSB Co-op Bank
        "COSMOS",                                 // Cosmos Bank
        
        // Generic terms (last resort)
        "BANK", "UPI", "BHIM"
    )
    
    // Transaction keywords
    val TRANSACTION_KEYWORDS = listOf(
        "debited", "credited", "debit", "credit", 
        "withdrawn", "deposited", "paid", "received",
        "spent", "transferred", "transaction", "txn",
        "purchase", "refund", "cashback", "reward", 
        "charges", "fee", "payment",
        "received a payment", "sent a payment"  // IPPB format
    )
    
    // Amount patterns
    val AMOUNT_PATTERNS = listOf(
        "rs.", "rs ", "rs-", "inr", "₹", "rupees", 
        "amount", "amt", "debited by", "credited by",
        "debited of", "credited of", "withdrawn of",
        "payment of rs", "payment of inr"  // IPPB specific
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

