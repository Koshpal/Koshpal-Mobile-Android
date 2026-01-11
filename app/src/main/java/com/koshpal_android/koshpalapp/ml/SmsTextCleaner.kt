package com.koshpal_android.koshpalapp.ml

import java.util.regex.Pattern

/**
 * SMS Text Cleaner
 *
 * BIT-EXACT Python cleaning logic implementation.
 * Rules applied in EXACT Python order:
 * 1. lowercase()
 * 2. URL replacement → <URL>
 * 3. Currency normalization: ₹, rs., rs, inr → "rs"
 * 4. Mask numeric patterns:
 *    - x{4,}\d+ → xxxx<NUM>
 *    - \bx\s*\d+\b → x<NUM>
 *    - \b\d{1,3}(?:,\d{3})+(?:\.\d+)?\b → <NUM>
 *    - \b\d+(?:\.\d+)?\b → <NUM>
 * 5. Long tokens: \b[a-z0-9]{10,}\b → <UTR>
 * 6. Whitespace normalization → single spaces
 * 7. trim()
 */
object SmsTextCleaner {
    
    // Regex patterns (compiled once for performance)
    private val URL_PATTERN_HTTP = Pattern.compile("https?://[^\\s]+")
    private val URL_PATTERN_WWW = Pattern.compile("www\\.[^\\s]+")
    private val URL_PATTERN_DOMAIN = Pattern.compile("\\b[a-z0-9][a-z0-9-]*\\.(com|in|org|net|co|io|me|tv|app|dev)[^\\s]*")
    
    private val CURRENCY_RUPEE = Pattern.compile("₹")
    private val CURRENCY_RS_DOT = Pattern.compile("\\brs\\.\\s*")
    private val CURRENCY_RS_DOT_END = Pattern.compile("\\brs\\.\\b")
    private val CURRENCY_INR = Pattern.compile("\\binr\\b")
    private val CURRENCY_MULTIPLE_RS = Pattern.compile("\\brs\\s+rs\\b")
    
    private val MASKED_ACCOUNT_XXXX = Pattern.compile("x{4,}\\d+")
    private val MASKED_ACCOUNT_X = Pattern.compile("\\bx\\s*\\d+\\b")
    
    private val NUMBER_WITH_COMMAS = Pattern.compile("\\b\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?\\b")
    private val NUMBER_WITHOUT_COMMAS = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b")
    
    private val LONG_ALPHANUMERIC = Pattern.compile("\\b[a-z0-9]{10,}\\b")  // EXACT Python: \b[a-z0-9]{10,}\b
    
    private val MULTIPLE_SPACES = Pattern.compile("\\s+")
    
    /**
     * Clean SMS text according to specified rules.
     * 
     * @param text Raw SMS text (can be null or empty)
     * @return Cleaned SMS text
     */
    fun clean(text: String?): String {
        // Handle null/empty values
        if (text.isNullOrEmpty()) {
            return ""
        }
        
        var cleaned: String = text
        
        // Rule 1: Lowercase text
        cleaned = cleaned.lowercase()
        
        // Rule 2: Replace URLs with <URL>
        cleaned = URL_PATTERN_HTTP.matcher(cleaned as CharSequence).replaceAll("<URL>")
        cleaned = URL_PATTERN_WWW.matcher(cleaned as CharSequence).replaceAll("<URL>")
        cleaned = URL_PATTERN_DOMAIN.matcher(cleaned as CharSequence).replaceAll("<URL>")
        
        // Rule 3: Normalize currency
        cleaned = CURRENCY_RUPEE.matcher(cleaned as CharSequence).replaceAll("rs")
        cleaned = CURRENCY_RS_DOT.matcher(cleaned as CharSequence).replaceAll("rs ")
        cleaned = CURRENCY_RS_DOT_END.matcher(cleaned as CharSequence).replaceAll("rs")
        cleaned = CURRENCY_INR.matcher(cleaned as CharSequence).replaceAll("rs")
        cleaned = CURRENCY_MULTIPLE_RS.matcher(cleaned as CharSequence).replaceAll("rs")
        
        // Rule 4: Mask numeric patterns (EXACT Python order)
        cleaned = MASKED_ACCOUNT_XXXX.matcher(cleaned as CharSequence).replaceAll("xxxx<NUM>")
        cleaned = MASKED_ACCOUNT_X.matcher(cleaned as CharSequence).replaceAll("x<NUM>")
        cleaned = NUMBER_WITH_COMMAS.matcher(cleaned as CharSequence).replaceAll("<NUM>")
        cleaned = NUMBER_WITHOUT_COMMAS.matcher(cleaned as CharSequence).replaceAll("<NUM>")
        
        // Rule 5: Replace long alphanumeric tokens (≥10 characters)
        cleaned = replaceLongTokens(cleaned)
        
        // Rule 6: Normalize whitespace
        cleaned = MULTIPLE_SPACES.matcher(cleaned as CharSequence).replaceAll(" ")
        cleaned = cleaned.trim()
        
        return cleaned
    }
    
    /**
     * Rule 5: Replace long alphanumeric tokens with <UTR>.
     * Python: \b[a-z0-9]{10,}\b → <UTR>
     * Android: Same logic, skips already-processed tags (<NUM>, <URL>, etc.)
     */
    private fun replaceLongTokens(text: String): String {
        val matcher = LONG_ALPHANUMERIC.matcher(text)
        val buffer = StringBuffer()
        
        while (matcher.find()) {
            val token = matcher.group()
            
            // Skip if it's already a tag
            if (token.startsWith("<") && token.endsWith(">")) {
                matcher.appendReplacement(buffer, token)
                continue
            }
            
            // Only replace pure alphanumeric tokens
            if (token.all { it.isLetterOrDigit() } && token.length >= 10) {
                matcher.appendReplacement(buffer, "<UTR>")
            } else {
                matcher.appendReplacement(buffer, token)
            }
        }
        matcher.appendTail(buffer)
        
        return buffer.toString()
    }
}

