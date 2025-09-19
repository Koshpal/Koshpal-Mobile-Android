package com.koshpal_android.koshpalapp.ui.budget.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.ui.budget.model.BudgetTier
import com.koshpal_android.koshpalapp.ui.budget.model.TierData
import kotlin.math.sin

/**
 * Beautiful tier card with animated progress and micro-interactions
 */
class TierCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var tierData: TierData? = null
    private var animatedProgress = 0f
    private var wobbleAnimation = 0f
    private var isPressed = false
    
    // Animation
    private var progressAnimator: ValueAnimator? = null
    private var wobbleAnimator: ValueAnimator? = null
    private var pressAnimator: ValueAnimator? = null
    
    // Paint objects
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Dimensions
    private val cardHeight = 120f
    private val cornerRadius = 20f
    private val progressHeight = 8f
    private val shadowOffset = 4f
    
    // Interaction callback
    var onTierClickListener: ((BudgetTier) -> Unit)? = null

    init {
        setupPaints()
        isClickable = true
        isFocusable = true
    }

    private fun setupPaints() {
        backgroundPaint.apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        progressBackgroundPaint.apply {
            color = Color.parseColor("#33FFFFFF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        progressPaint.apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        textPaint.apply {
            color = Color.WHITE
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        
        iconPaint.apply {
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        shadowPaint.apply {
            color = Color.BLACK
            alpha = 50
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (cardHeight * (1f + wobbleAnimation * 0.1f)).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val tierData = this.tierData ?: return
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        // Apply press animation
        val scale = if (isPressed) 0.95f else 1f
        canvas.save()
        canvas.scale(scale, scale, width / 2f, height / 2f)
        
        // Draw shadow
        drawShadow(canvas, width, height)
        
        // Draw background with tier color
        drawBackground(canvas, width, height, tierData.tier)
        
        // Draw content
        drawContent(canvas, width, height, tierData)
        
        canvas.restore()
    }

    private fun drawShadow(canvas: Canvas, width: Float, height: Float) {
        val shadowRect = RectF(
            shadowOffset, shadowOffset,
            width - shadowOffset, height - shadowOffset
        )
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)
    }

    private fun drawBackground(canvas: Canvas, width: Float, height: Float, tier: BudgetTier) {
        // Create gradient background
        val primaryColor = ContextCompat.getColor(context, tier.primaryColor)
        val secondaryColor = ContextCompat.getColor(context, tier.secondaryColor)
        
        val gradient = LinearGradient(
            0f, 0f, width, height,
            intArrayOf(primaryColor, adjustBrightness(primaryColor, 0.8f)),
            null,
            Shader.TileMode.CLAMP
        )
        
        backgroundPaint.shader = gradient
        
        val rect = RectF(0f, 0f, width, height)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)
        
        // Add subtle pattern overlay
        drawPatternOverlay(canvas, width, height)
    }

    private fun drawPatternOverlay(canvas: Canvas, width: Float, height: Float) {
        val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 20
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        
        // Draw subtle geometric pattern
        val spacing = 30f
        for (i in 0 until (width / spacing).toInt()) {
            val x = i * spacing
            canvas.drawLine(x, 0f, x + height * 0.3f, height, patternPaint)
        }
    }

    private fun drawContent(canvas: Canvas, width: Float, height: Float, tierData: TierData) {
        val padding = 20f
        
        // Draw tier icon
        drawTierIcon(canvas, padding + 30f, padding + 30f, tierData.tier)
        
        // Draw tier title and percentage
        drawTierTitle(canvas, padding + 80f, padding + 20f, tierData.tier)
        
        // Draw amounts
        drawAmounts(canvas, padding + 80f, padding + 45f, tierData)
        
        // Draw progress bar
        drawProgressBar(canvas, padding + 80f, height - padding - 20f, width - padding * 2 - 80f, tierData)
        
        // Draw trend indicator
        drawTrendIndicator(canvas, width - padding - 30f, padding + 20f, tierData.trend)
    }

    private fun drawTierIcon(canvas: Canvas, x: Float, y: Float, tier: BudgetTier) {
        // Draw icon background circle
        val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 30
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, y, 25f, iconBgPaint)
        
        // Draw tier icon (using emoji for now)
        iconPaint.textSize = 24f
        val icon = when (tier) {
            BudgetTier.ESSENTIALS -> "🛡️"
            BudgetTier.WANTS -> "❤️"
            BudgetTier.GOALS -> "🚀"
        }
        canvas.drawText(icon, x, y + 8f, iconPaint)
    }

    private fun drawTierTitle(canvas: Canvas, x: Float, y: Float, tier: BudgetTier) {
        textPaint.textSize = 18f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(tier.displayName, x, y, textPaint)
        
        // Draw percentage
        val percentageText = "${(tier.recommendedPercentage * 100).toInt()}%"
        textPaint.textSize = 14f
        textPaint.alpha = 200
        canvas.drawText(percentageText, x + 120f, y, textPaint)
        textPaint.alpha = 255
    }

    private fun drawAmounts(canvas: Canvas, x: Float, y: Float, tierData: TierData) {
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.DEFAULT
        
        val spentText = "₹${String.format("%.0f", tierData.spentAmount)}"
        val budgetText = " / ₹${String.format("%.0f", tierData.allocatedAmount)}"
        
        // Draw spent amount in white
        canvas.drawText(spentText, x, y, textPaint)
        
        // Draw budget amount in lighter color
        val spentWidth = textPaint.measureText(spentText)
        textPaint.alpha = 180
        canvas.drawText(budgetText, x + spentWidth, y, textPaint)
        textPaint.alpha = 255
    }

    private fun drawProgressBar(canvas: Canvas, x: Float, y: Float, width: Float, tierData: TierData) {
        val progressWidth = width - 60f // Leave space for remaining amount
        
        // Draw progress background
        val bgRect = RectF(x, y - progressHeight / 2, x + progressWidth, y + progressHeight / 2)
        canvas.drawRoundRect(bgRect, progressHeight / 2, progressHeight / 2, progressBackgroundPaint)
        
        // Draw progress fill with animation
        val progress = animatedProgress.coerceIn(0f, 1f)
        val fillWidth = progressWidth * progress
        
        if (fillWidth > 0) {
            // Create gradient for progress
            val progressColor = getProgressColor(tierData.spentPercentage)
            val progressGradient = LinearGradient(
                x, y, x + fillWidth, y,
                intArrayOf(progressColor, adjustBrightness(progressColor, 1.2f)),
                null,
                Shader.TileMode.CLAMP
            )
            progressPaint.shader = progressGradient
            
            val fillRect = RectF(x, y - progressHeight / 2, x + fillWidth, y + progressHeight / 2)
            canvas.drawRoundRect(fillRect, progressHeight / 2, progressHeight / 2, progressPaint)
        }
        
        // Draw remaining amount
        val remainingText = "₹${String.format("%.0f", tierData.remainingAmount)}"
        textPaint.textSize = 12f
        textPaint.color = if (tierData.remainingAmount >= 0) Color.WHITE else Color.RED
        canvas.drawText(remainingText, x + progressWidth + 10f, y + 4f, textPaint)
        textPaint.color = Color.WHITE
    }

    private fun drawTrendIndicator(canvas: Canvas, x: Float, y: Float, trend: com.koshpal_android.koshpalapp.ui.budget.model.TrendDirection) {
        val trendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }
        
        val (icon, color) = when (trend) {
            com.koshpal_android.koshpalapp.ui.budget.model.TrendDirection.IMPROVING -> "📈" to Color.GREEN
            com.koshpal_android.koshpalapp.ui.budget.model.TrendDirection.STABLE -> "➡️" to Color.YELLOW
            com.koshpal_android.koshpalapp.ui.budget.model.TrendDirection.DECLINING -> "📉" to Color.RED
        }
        
        trendPaint.color = color
        canvas.drawText(icon, x, y, trendPaint)
    }

    private fun getProgressColor(percentage: Float): Int {
        return when {
            percentage <= 0.6f -> ContextCompat.getColor(context, R.color.urgency_low)
            percentage <= 0.8f -> ContextCompat.getColor(context, R.color.urgency_medium)
            percentage <= 1.0f -> ContextCompat.getColor(context, R.color.urgency_high)
            else -> ContextCompat.getColor(context, R.color.urgency_critical)
        }
    }

    private fun adjustBrightness(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    fun setTierData(data: TierData) {
        this.tierData = data
        animateProgress(data.spentPercentage)
        
        // Start wobble animation if urgency is high
        if (data.spentPercentage > 0.8f) {
            startWobbleAnimation()
        }
        
        invalidate()
    }

    private fun animateProgress(targetProgress: Float) {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(animatedProgress, targetProgress).apply {
            duration = 1000L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                animatedProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startWobbleAnimation() {
        wobbleAnimator?.cancel()
        wobbleAnimator = ValueAnimator.ofFloat(0f, 2f * Math.PI.toFloat()).apply {
            duration = 2000L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                wobbleAnimation = sin(animation.animatedValue as Float) * 0.1f
                requestLayout()
            }
            start()
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        tierData?.let { data ->
            onTierClickListener?.invoke(data.tier)
            animatePress()
        }
        return true
    }

    private fun animatePress() {
        pressAnimator?.cancel()
        pressAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 200L
            addUpdateListener { animation ->
                isPressed = animation.animatedValue as Float > 0.5f
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressAnimator?.cancel()
        wobbleAnimator?.cancel()
        pressAnimator?.cancel()
    }
}
