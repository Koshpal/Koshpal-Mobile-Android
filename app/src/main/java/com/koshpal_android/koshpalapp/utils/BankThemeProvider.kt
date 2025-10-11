package com.koshpal_android.koshpalapp.utils

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * Provides brand-specific themes for banks and payment apps
 * Based on actual brand colors and designs
 */
object BankThemeProvider {

    data class BankTheme(
        val displayName: String,
        @ColorInt val primaryColor: Int,
        @ColorInt val secondaryColor: Int,
        @ColorInt val accentColor: Int? = null, // For 3-color gradients
        val gradientAngle: GradientAngle = GradientAngle.DIAGONAL,
        val iconInitials: String,
        val iconDrawable: Int? = null, // Actual drawable resource
        val iconStyle: IconStyle = IconStyle.TEXT,
        val textColor: Int = Color.WHITE
    )
    
    enum class GradientAngle {
        DIAGONAL,      // Top-left to bottom-right (payment apps)
        VERTICAL,      // Top to bottom (traditional)
        HORIZONTAL,    // Left to right
        RADIAL         // Center spread
    }
    
    enum class IconStyle {
        TEXT,          // Styled text initials
        ROUNDED,       // Rounded shape background
        CIRCLE_SOLID,  // Solid circle
        GRADIENT       // Gradient circle
    }

    // Popular Indian Banks (Top 20) - ALL with beautiful gradients
    private val bankThemes = mapOf(
        // State Bank of India - Blue to dark blue gradient
        "SBI" to BankTheme(
            displayName = "SBI", 
            primaryColor = Color.parseColor("#1E88E5"), 
            secondaryColor = Color.parseColor("#0D47A1"), 
            accentColor = null,
            gradientAngle = GradientAngle.VERTICAL,
            iconInitials = "S",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_sbi,
            iconStyle = IconStyle.ROUNDED
        ),
        "STATE BANK" to BankTheme(
            displayName = "SBI", 
            primaryColor = Color.parseColor("#1E88E5"), 
            secondaryColor = Color.parseColor("#0D47A1"),
            accentColor = null,
            gradientAngle = GradientAngle.VERTICAL,
            iconInitials = "S",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_sbi,
            iconStyle = IconStyle.ROUNDED
        ),
        
        // HDFC Bank - Red to orange gradient (matches HDFC branding)
        "HDFC" to BankTheme(
            displayName = "HDFC Bank", 
            primaryColor = Color.parseColor("#D32F2F"), 
            secondaryColor = Color.parseColor("#FF6F00"),
            accentColor = Color.parseColor("#004C8F"),
            gradientAngle = GradientAngle.DIAGONAL,
            iconInitials = "H",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_hdfc,
            iconStyle = IconStyle.GRADIENT
        ),
        
        // ICICI Bank - Orange to brown gradient
        "ICICI" to BankTheme(
            displayName = "ICICI Bank", 
            primaryColor = Color.parseColor("#FF6F00"), 
            secondaryColor = Color.parseColor("#E65100"),
            accentColor = Color.parseColor("#BF360C"),
            gradientAngle = GradientAngle.DIAGONAL,
            iconInitials = "I",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_icici,
            iconStyle = IconStyle.GRADIENT
        ),
        
        // Axis Bank - Burgundy to dark red gradient
        "AXIS" to BankTheme(
            displayName = "Axis Bank", 
            primaryColor = Color.parseColor("#880E4F"), 
            secondaryColor = Color.parseColor("#4A0027"),
            accentColor = null,
            gradientAngle = GradientAngle.VERTICAL,
            iconInitials = "A",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_axis,
            iconStyle = IconStyle.CIRCLE_SOLID
        ),
        
        // IPPB - India Post Payments Bank - Orange gradient
        "IPPB" to BankTheme(
            displayName = "India Post", 
            primaryColor = Color.parseColor("#FF6F00"), 
            secondaryColor = Color.parseColor("#E65100"),
            accentColor = null,
            gradientAngle = GradientAngle.VERTICAL,
            iconInitials = "I",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_ippb,
            iconStyle = IconStyle.ROUNDED
        ),
        "INDIA POST" to BankTheme(
            displayName = "India Post", 
            primaryColor = Color.parseColor("#FF6F00"), 
            secondaryColor = Color.parseColor("#E65100"),
            accentColor = null,
            gradientAngle = GradientAngle.VERTICAL,
            iconInitials = "I",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_ippb,
            iconStyle = IconStyle.ROUNDED
        ),
    )

    // Popular Payment Apps (Top 10) - Vibrant diagonal gradients
    private val paymentAppThemes = mapOf(
        // PhonePe - Purple gradient (iconic PhonePe colors)
        "PHONEPE" to BankTheme(
            displayName = "PhonePe", 
            primaryColor = Color.parseColor("#5F259F"), 
            secondaryColor = Color.parseColor("#8E24AA"),
            accentColor = Color.parseColor("#3C1361"),
            gradientAngle = GradientAngle.DIAGONAL,
            iconInitials = "P",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_phonepay,
            iconStyle = IconStyle.GRADIENT
        ),
        
        // Paytm - Blue gradient (Paytm brand colors)
        "PAYTM" to BankTheme(
            displayName = "Paytm", 
            primaryColor = Color.parseColor("#00BAF2"), 
            secondaryColor = Color.parseColor("#0098C9"),
            accentColor = Color.parseColor("#0277BD"),
            gradientAngle = GradientAngle.DIAGONAL,
            iconInitials = "P",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_paytm,
            iconStyle = IconStyle.GRADIENT
        ),
        
        // Google Pay - Multi-color gradient (Google colors)
        "GOOGLE PAY" to BankTheme(
            displayName = "Google Pay", 
            primaryColor = Color.parseColor("#4285F4"), 
            secondaryColor = Color.parseColor("#34A853"),
            accentColor = Color.parseColor("#FBBC05"),
            gradientAngle = GradientAngle.DIAGONAL,
            iconInitials = "G",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_google_pay,
            iconStyle = IconStyle.GRADIENT
        ),
        "GOOGLEPAY" to BankTheme(
            displayName = "Google Pay", 
            primaryColor = Color.parseColor("#4285F4"), 
            secondaryColor = Color.parseColor("#34A853"),
            accentColor = Color.parseColor("#FBBC05"),
            gradientAngle = GradientAngle.DIAGONAL,
            iconInitials = "G",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_google_pay,
            iconStyle = IconStyle.GRADIENT
        ),
        "GPAY" to BankTheme(
            displayName = "Google Pay", 
            primaryColor = Color.parseColor("#4285F4"), 
            secondaryColor = Color.parseColor("#34A853"),
            accentColor = Color.parseColor("#FBBC05"),
            gradientAngle = GradientAngle.DIAGONAL,
            iconInitials = "G",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_google_pay,
            iconStyle = IconStyle.GRADIENT
        ),
        
        // Amazon Pay - Orange gradient (Amazon brand)
        "AMAZON PAY" to BankTheme(
            displayName = "Amazon Pay", 
            primaryColor = Color.parseColor("#FF9900"), 
            secondaryColor = Color.parseColor("#FF6600"),
            accentColor = Color.parseColor("#E65100"),
            gradientAngle = GradientAngle.DIAGONAL,
            iconInitials = "A",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_amazon_pay,
            iconStyle = IconStyle.GRADIENT
        ),
        "AMAZONPAY" to BankTheme(
            displayName = "Amazon Pay", 
            primaryColor = Color.parseColor("#FF9900"), 
            secondaryColor = Color.parseColor("#FF6600"),
            accentColor = Color.parseColor("#E65100"),
            gradientAngle = GradientAngle.DIAGONAL,
            iconInitials = "A",
            iconDrawable = com.koshpal_android.koshpalapp.R.drawable.ic_amazon_pay,
            iconStyle = IconStyle.GRADIENT
        ),
    )

    // Default themes for unmatched banks - All with beautiful gradients
    private val defaultThemes = listOf(
        BankTheme("Bank", Color.parseColor("#2196F3"), Color.parseColor("#1565C0"), null, GradientAngle.DIAGONAL, "B", null, IconStyle.ROUNDED),
        BankTheme("Bank", Color.parseColor("#4CAF50"), Color.parseColor("#2E7D32"), null, GradientAngle.DIAGONAL, "B", null, IconStyle.ROUNDED),
        BankTheme("Bank", Color.parseColor("#FF9800"), Color.parseColor("#EF6C00"), null, GradientAngle.DIAGONAL, "B", null, IconStyle.ROUNDED),
        BankTheme("Bank", Color.parseColor("#9C27B0"), Color.parseColor("#6A1B9A"), null, GradientAngle.DIAGONAL, "B", null, IconStyle.ROUNDED),
        BankTheme("Bank", Color.parseColor("#00BCD4"), Color.parseColor("#00838F"), null, GradientAngle.DIAGONAL, "B", null, IconStyle.ROUNDED),
        BankTheme("Bank", Color.parseColor("#E91E63"), Color.parseColor("#AD1457"), null, GradientAngle.DIAGONAL, "B", null, IconStyle.ROUNDED),
        BankTheme("Bank", Color.parseColor("#3F51B5"), Color.parseColor("#283593"), null, GradientAngle.DIAGONAL, "B", null, IconStyle.ROUNDED),
        BankTheme("Bank", Color.parseColor("#009688"), Color.parseColor("#00695C"), null, GradientAngle.DIAGONAL, "B", null, IconStyle.ROUNDED),
    )

    /**
     * Get theme for a bank based on bank name
     * Performs fuzzy matching on bank name
     */
    fun getThemeForBankConsistent(bankName: String): BankTheme {
        val upperBankName = bankName.uppercase()
        
        // First try exact match
        bankThemes[upperBankName]?.let { return it }
        paymentAppThemes[upperBankName]?.let { return it }
        
        // Then try partial match
        bankThemes.keys.find { upperBankName.contains(it) }?.let { 
            return bankThemes[it]!! 
        }
        paymentAppThemes.keys.find { upperBankName.contains(it) }?.let { 
            return paymentAppThemes[it]!! 
        }
        
        // Fallback to default theme based on hash
        val hash = upperBankName.hashCode()
        val index = kotlin.math.abs(hash) % defaultThemes.size
        return defaultThemes[index]
    }
}