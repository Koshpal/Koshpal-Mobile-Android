package com.koshpal_android.koshpalapp.ui.custom

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.koshpal_android.koshpalapp.R

class WaveBackgroundDrawable : Drawable() {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    init {
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        
        shadowPaint.color = Color.parseColor("#30000000")
        shadowPaint.style = Paint.Style.FILL
        shadowPaint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }
    
    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        
        // Draw shadow first
        drawShadow(canvas, width, height)
        
        // Draw main background with wave
        drawWaveBackground(canvas, width, height)
    }
    
    private fun drawShadow(canvas: Canvas, width: Float, height: Float) {
        val shadowPath = Path()
        shadowPath.moveTo(0f, 8f)
        shadowPath.lineTo(0f, height)
        shadowPath.lineTo(width, height)
        shadowPath.lineTo(width, 8f)
        shadowPath.quadTo(width / 2, 0f, 0f, 8f)
        shadowPath.close()
        
        canvas.drawPath(shadowPath, shadowPaint)
    }
    
    private fun drawWaveBackground(canvas: Canvas, width: Float, height: Float) {
        path.reset()
        
        // Start from top-left corner
        path.moveTo(0f, 32f)
        
        // Top edge with slight curve
        path.quadTo(width * 0.1f, 0f, width * 0.2f, 0f)
        path.lineTo(width * 0.8f, 0f)
        path.quadTo(width * 0.9f, 0f, width, 32f)
        
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
