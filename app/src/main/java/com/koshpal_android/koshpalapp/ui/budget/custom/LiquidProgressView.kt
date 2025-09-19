package com.koshpal_android.koshpalapp.ui.budget.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.koshpal_android.koshpalapp.R
import kotlin.math.*

/**
 * Revolutionary liquid progress bar with wave animation
 * Changes color based on budget health: Green -> Yellow -> Red
 */
class LiquidProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 0f // 0.0 to 1.0
    private var liquidColor = ContextCompat.getColor(context, R.color.liquid_progress_excellent)
    private var backgroundColor = ContextCompat.getColor(context, R.color.universe_orbit_line)
    private var cornerRadius = 24f
    private var animationDuration = 2000L
    private var waveAmplitude = 12f
    
    // Animation properties
    private var waveOffset = 0f
    private var currentProgress = 0f
    private var waveAnimator: ValueAnimator? = null
    private var progressAnimator: ValueAnimator? = null
    
    // Paint objects
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val liquidPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePath = Path()
    private val clipPath = Path()
    
    // Gradient colors for different health levels
    private val excellentColor = ContextCompat.getColor(context, R.color.liquid_progress_excellent)
    private val goodColor = ContextCompat.getColor(context, R.color.liquid_progress_good)
    private val warningColor = ContextCompat.getColor(context, R.color.liquid_progress_warning)
    private val dangerColor = ContextCompat.getColor(context, R.color.liquid_progress_danger)
    private val criticalColor = ContextCompat.getColor(context, R.color.liquid_progress_critical)

    init {
        setupPaints()
        startWaveAnimation()
    }

    private fun setupPaints() {
        backgroundPaint.color = backgroundColor
        backgroundPaint.style = Paint.Style.FILL
        
        liquidPaint.style = Paint.Style.FILL
        updateLiquidColor()
    }

    private fun updateLiquidColor() {
        liquidColor = when {
            progress <= 0.5f -> excellentColor
            progress <= 0.65f -> goodColor
            progress <= 0.8f -> warningColor
            progress <= 0.95f -> dangerColor
            else -> criticalColor
        }
        
        // Create gradient effect
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            intArrayOf(liquidColor, adjustBrightness(liquidColor, 1.2f)),
            null,
            Shader.TileMode.CLAMP
        )
        liquidPaint.shader = gradient
    }

    private fun adjustBrightness(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        // Create rounded rectangle clip path
        clipPath.reset()
        clipPath.addRoundRect(0f, 0f, width, height, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(clipPath)
        
        // Draw background
        canvas.drawRoundRect(0f, 0f, width, height, cornerRadius, cornerRadius, backgroundPaint)
        
        // Calculate liquid level
        val liquidLevel = height - (height * currentProgress)
        
        if (currentProgress > 0f) {
            // Create wave path
            createWavePath(width, height, liquidLevel)
            
            // Draw liquid with wave effect
            canvas.drawPath(wavePath, liquidPaint)
            
            // Add shimmer effect for visual appeal
            drawShimmerEffect(canvas, width, height, liquidLevel)
        }
    }

    private fun createWavePath(width: Float, height: Float, liquidLevel: Float) {
        wavePath.reset()
        
        val waveLength = width / 2f
        val waveCount = 2
        
        // Start from bottom left
        wavePath.moveTo(0f, height)
        
        // Draw bottom edge
        wavePath.lineTo(0f, liquidLevel + waveAmplitude)
        
        // Create wave effect
        for (i in 0..waveCount) {
            val x = i * waveLength
            val nextX = (i + 1) * waveLength
            
            val controlX1 = x + waveLength * 0.25f
            val controlY1 = liquidLevel + sin((waveOffset + x * 0.02f)) * waveAmplitude
            
            val controlX2 = x + waveLength * 0.75f
            val controlY2 = liquidLevel - sin((waveOffset + nextX * 0.02f)) * waveAmplitude
            
            val endX = nextX.coerceAtMost(width)
            val endY = liquidLevel + sin((waveOffset + endX * 0.02f)) * waveAmplitude
            
            wavePath.cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
        }
        
        // Complete the path
        wavePath.lineTo(width, height)
        wavePath.lineTo(0f, height)
        wavePath.close()
    }

    private fun drawShimmerEffect(canvas: Canvas, width: Float, height: Float, liquidLevel: Float) {
        if (currentProgress > 0.1f) {
            val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            shimmerPaint.color = Color.WHITE
            shimmerPaint.alpha = (50 * sin(waveOffset * 0.5f)).toInt().coerceIn(20, 80)
            
            val shimmerPath = Path()
            shimmerPath.moveTo(0f, liquidLevel - 4f)
            
            for (x in 0..width.toInt() step 2) {
                val y = liquidLevel - 4f + sin((waveOffset * 1.5f + x * 0.01f)) * 2f
                shimmerPath.lineTo(x.toFloat(), y)
            }
            
            shimmerPaint.strokeWidth = 2f
            shimmerPaint.style = Paint.Style.STROKE
            canvas.drawPath(shimmerPath, shimmerPaint)
        }
    }

    private fun startWaveAnimation() {
        waveAnimator?.cancel()
        waveAnimator = ValueAnimator.ofFloat(0f, 2 * PI.toFloat()).apply {
            duration = 3000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                waveOffset = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setProgress(newProgress: Float, animate: Boolean = true) {
        val clampedProgress = newProgress.coerceIn(0f, 1f)
        
        if (animate) {
            progressAnimator?.cancel()
            progressAnimator = ValueAnimator.ofFloat(currentProgress, clampedProgress).apply {
                duration = animationDuration
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    currentProgress = animation.animatedValue as Float
                    updateLiquidColor()
                    invalidate()
                }
                start()
            }
        } else {
            currentProgress = clampedProgress
            updateLiquidColor()
            invalidate()
        }
        
        progress = clampedProgress
    }

    fun setLiquidColor(color: Int) {
        this.liquidColor = color
        updateLiquidColor()
        invalidate()
    }

    override fun setBackgroundColor(color: Int) {
        this.backgroundColor = color
        backgroundPaint.color = color
        invalidate()
    }

    fun setCornerRadius(radius: Float) {
        this.cornerRadius = radius
        invalidate()
    }

    fun setWaveAmplitude(amplitude: Float) {
        this.waveAmplitude = amplitude
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator?.cancel()
        progressAnimator?.cancel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateLiquidColor()
    }
}
