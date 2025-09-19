package com.koshpal_android.koshpalapp.utils

import android.util.Log
import com.koshpal_android.koshpalapp.engine.TransactionCategorizationEngine
import com.koshpal_android.koshpalapp.model.TransactionType

object SMSTestHelper {
    
    fun testSMSParsing() {
        val engine = TransactionCategorizationEngine()
        
        // Test common SMS formats
        val testSMS = listOf(
            "Your A/c XX1234 debited by Rs.500.00 on 15-Dec-23 at AMAZON INDIA. Avl Bal: Rs.10000.00",
            "Rs.1200 debited from A/c XX5678 for UPI/ZOMATO/123456789 on 15-Dec-23. Bal: Rs.8800",
            "Your account credited with Rs.25000.00 on 15-Dec-23. Salary credit. Available balance Rs.35000.00",
            "INR 350.00 debited from your account for UBER TRIP on 15-Dec-23",
            "₹2500 spent at FLIPKART on 15-Dec-23 using card ending 1234"
        )
        
        testSMS.forEach { sms ->
            Log.d("SMSTest", "Testing SMS: $sms")
            val details = engine.extractTransactionDetails(sms)
            Log.d("SMSTest", "Amount: ${details.amount}, Merchant: ${details.merchant}, Type: ${details.type}")
            Log.d("SMSTest", "---")
        }
    }
    
    fun createSampleTransactions(): List<SampleTransaction> {
        return listOf(
            SampleTransaction("SBIINB", "Your A/c XX1234 debited by Rs.500.00 on 15-Dec-23 at AMAZON INDIA. Avl Bal: Rs.10000.00"),
            SampleTransaction("HDFCBK", "Rs.1200 debited from A/c XX5678 for UPI/ZOMATO/123456789 on 15-Dec-23. Bal: Rs.8800"),
            SampleTransaction("ICICIB", "Your account credited with Rs.25000.00 on 15-Dec-23. Salary credit. Available balance Rs.35000.00"),
            SampleTransaction("AXISBK", "INR 350.00 debited from your account for UBER TRIP on 15-Dec-23"),
            SampleTransaction("PAYTM", "₹2500 spent at FLIPKART on 15-Dec-23 using card ending 1234"),
            SampleTransaction("GPAY", "You paid ₹150 to SWIGGY via Google Pay UPI"),
            SampleTransaction("PHONEPE", "₹300 debited for METRO CARD recharge via PhonePe"),
            SampleTransaction("KOTAKB", "Rs.5000.00 debited for RENT payment on 01-Dec-23"),
            SampleTransaction("PNBSMS", "₹800 spent at DMART GROCERY on 14-Dec-23"),
            SampleTransaction("BOBSMS", "Your salary Rs.45000 credited to account on 01-Dec-23")
        )
    }
}

data class SampleTransaction(
    val sender: String,
    val message: String
)
