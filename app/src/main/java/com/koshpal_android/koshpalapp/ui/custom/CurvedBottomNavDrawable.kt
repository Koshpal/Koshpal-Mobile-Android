package com.koshpal_android.koshpalapp.ui.custom

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.koshpal_android.koshpalapp.R

class CurvedBottomNavDrawable : Drawable() {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    
    // Colors
    private val backgroundColor = Color.WHITE
    private val shadowColor = Color.parseColor("#20000000")
    
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
        
        // Draw shadow first
        drawShadow(canvas, width, height)
        
        // Draw main curved background
        drawCurvedBackground(canvas, width, height)
    }
    
    private fun drawShadow(canvas: Canvas, width: Float, height: Float) {
        val shadowPath = Path()
        
        // Create shadow path with curve
        shadowPath.moveTo(0f, 8f)
        shadowPath.lineTo(0f, height)
        shadowPath.lineTo(width, height)
        shadowPath.lineTo(width, 8f)
        
        // Add curve for center button area
        val centerX = width / 2
        val curveHeight = 20f
        shadowPath.quadTo(centerX, -curveHeight, 0f, 8f)
        
        shadowPath.close()
        canvas.drawPath(shadowPath, shadowPaint)
    }
    
    private fun drawCurvedBackground(canvas: Canvas, width: Float, height: Float) {
        path.reset()
        
        val centerX = width / 2
        val curveHeight = 16f
        val cornerRadius = 32f
        
        // Start from top-left corner
        path.moveTo(0f, cornerRadius)
        
        // Top-left rounded corner
        path.quadTo(0f, 0f, cornerRadius, 0f)
        
        // Top edge with slight curve
        path.lineTo(centerX - 40f, 0f)
        
        // Center curve for FAB
        path.quadTo(centerX, -curveHeight, centerX + 40f, 0f)
        
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
