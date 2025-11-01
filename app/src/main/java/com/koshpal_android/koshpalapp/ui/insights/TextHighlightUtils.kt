package com.koshpal_android.koshpalapp.ui.insights

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.koshpal_android.koshpalapp.R

/**
 * Utility for highlighting amounts and percentages in insight text
 * Creates AI-like conversational tone with visual emphasis
 */
object TextHighlightUtils {

    /**
     * Highlights currency amounts (₹xxx) in text with accent color
     */
    fun highlightAmounts(textView: TextView, text: String) {
        val spannableString = SpannableString(text)
        val amountPattern = Regex("₹[\\d,]+")
        val color = ContextCompat.getColor(textView.context, R.color.text_highlight)
        
        amountPattern.findAll(text).forEach { matchResult ->
            spannableString.setSpan(
                ForegroundColorSpan(color),
                matchResult.range.first,
                matchResult.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                matchResult.range.first,
                matchResult.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        textView.text = spannableString
    }
    
    /**
     * Highlights percentages with color based on direction (increase/decrease)
     */
    fun highlightPercentages(textView: TextView, text: String) {
        val spannableString = SpannableString(text)
        val percentagePattern = Regex("([↑↓]?\\s*\\d+\\.?\\d*%)")
        
        percentagePattern.findAll(text).forEach { matchResult ->
            val matchText = matchResult.value
            val color = when {
                matchText.contains("↑") -> ContextCompat.getColor(textView.context, R.color.text_error_emphasis)
                matchText.contains("↓") -> ContextCompat.getColor(textView.context, R.color.text_success_emphasis)
                else -> ContextCompat.getColor(textView.context, R.color.text_highlight)
            }
            
            spannableString.setSpan(
                ForegroundColorSpan(color),
                matchResult.range.first,
                matchResult.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                matchResult.range.first,
                matchResult.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        textView.text = spannableString
    }
    
    /**
     * Highlights both amounts and percentages in a single text
     */
    fun highlightInsightText(textView: TextView, text: String) {
        var spannableString = SpannableString(text)
        val highlightColor = ContextCompat.getColor(textView.context, R.color.text_highlight)
        val increaseColor = ContextCompat.getColor(textView.context, R.color.text_error_emphasis)
        val decreaseColor = ContextCompat.getColor(textView.context, R.color.text_success_emphasis)
        
        // Highlight amounts (₹xxx)
        val amountPattern = Regex("₹[\\d,]+")
        amountPattern.findAll(text).forEach { matchResult ->
            spannableString.setSpan(
                ForegroundColorSpan(highlightColor),
                matchResult.range.first,
                matchResult.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                matchResult.range.first,
                matchResult.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        // Highlight percentages with direction
        val percentagePattern = Regex("([↑↓]?\\s*\\d+\\.?\\d*%)")
        percentagePattern.findAll(text).forEach { matchResult ->
            val matchText = matchResult.value
            val color = when {
                matchText.contains("↑") -> increaseColor
                matchText.contains("↓") -> decreaseColor
                else -> highlightColor
            }
            
            spannableString.setSpan(
                ForegroundColorSpan(color),
                matchResult.range.first,
                matchResult.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                matchResult.range.first,
                matchResult.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        // Highlight merchant/category names (capitalized words)
        val merchantPattern = Regex("\\b[A-Z][a-z]+\\b")
        merchantPattern.findAll(text).forEach { matchResult ->
            // Skip if already highlighted (amounts/percentages)
            val alreadyHighlighted = spannableString.getSpans(
                matchResult.range.first,
                matchResult.range.last + 1,
                ForegroundColorSpan::class.java
            ).isNotEmpty()
            
            if (!alreadyHighlighted) {
                spannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    matchResult.range.first,
                    matchResult.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        
        textView.text = spannableString
    }
    
    /**
     * Creates conversational insight text with smart suggestions
     */
    fun buildConversationalInsight(
        increases: List<Pair<String, Float>>,
        decreases: List<Pair<String, Float>>,
        overallChange: Double
    ): String {
        val parts = mutableListOf<String>()
        
        // Opening
        parts.add("Here's what changed:")
        
        // Increases
        if (increases.isNotEmpty()) {
            val top = increases.first()
            parts.add("${top.first} spending went ↑ ${top.second.toInt()}% vs last month.")
        }
        
        // Decreases
        if (decreases.isNotEmpty()) {
            val top = decreases.first()
            parts.add("${top.first} decreased ↓ ${kotlin.math.abs(top.second).toInt()}%.")
        }
        
        // Overall
        if (overallChange > 0) {
            parts.add("Overall, you spent ₹${overallChange.toInt()} more this month.")
        } else if (overallChange < 0) {
            parts.add("Great! You saved ₹${kotlin.math.abs(overallChange).toInt()} this month.")
        }
        
        // Suggestion
        if (increases.isNotEmpty()) {
            val category = increases.first().first
            parts.add("Consider reducing $category expenses next month.")
        }
        
        return parts.joinToString(" ")
    }
}
