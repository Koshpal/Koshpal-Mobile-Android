package com.koshpal_android.koshpalapp.ui.custom

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.koshpal_android.koshpalapp.R

class NotchedBottomNavDrawable : Drawable() {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    
    // Colors
    private val backgroundColor = Color.WHITE
    private val shadowColor = Color.parseColor("#30000000")
    
    // Dimensions
    private val cornerRadius = 32f
    private val shadowOffset = 6f
    private val notchDepth = 24f
    private val notchWidth = 80f
    
    init {
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        
        shadowPaint.color = shadowColor
        shadowPaint.style = Paint.Style.FILL
        shadowPaint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }
    
    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        val centerX = width / 2
        
        // Draw shadow
        drawShadow(canvas, width, height, centerX)
        
        // Draw main background with notch
        drawNotchedBackground(canvas, width, height, centerX)
    }
    
    private fun drawShadow(canvas: Canvas, width: Float, height: Float, centerX: Float) {
        val shadowPath = Path()
        
        // Create shadow path with notch
        shadowPath.moveTo(0f, shadowOffset)
        shadowPath.lineTo(0f, height)
        shadowPath.lineTo(width, height)
        shadowPath.lineTo(width, shadowOffset)
        
        // Add notch curve for FAB
        shadowPath.quadTo(centerX, -notchDepth, 0f, shadowOffset)
        
        shadowPath.close()
        canvas.drawPath(shadowPath, shadowPaint)
    }
    
    private fun drawNotchedBackground(canvas: Canvas, width: Float, height: Float, centerX: Float) {
        path.reset()
        
        // Start from top-left corner
        path.moveTo(0f, cornerRadius)
        
        // Top-left rounded corner
        path.quadTo(0f, 0f, cornerRadius, 0f)
        
        // Top edge with notch for FAB
        path.lineTo(centerX - notchWidth/2, 0f)
        path.quadTo(centerX, -notchDepth, centerX + notchWidth/2, 0f)
        
        // Continue to top-right
        path.lineTo(width - cornerRadius, 0f)
        
        // Top-right rounded corner
        path.quadTo(width, 0f, width, cornerRadius)
        
        // Right edge
        path.lineTo(width, height)
        
        // Bottom edge
        path.lineTo(0f, height)
        
        // Left edge
        path.close()
        
        canvas.drawPath(path, paint)
    }
    
    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        shadowPaint.alpha = alpha
    }
    
    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        shadowPaint.colorFilter = colorFilter
    }
    
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
