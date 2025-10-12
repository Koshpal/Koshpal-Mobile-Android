package com.koshpal_android.koshpalapp.ui.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.koshpal_android.koshpalapp.R

class CurvedBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {
    
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Dimensions
    private val cornerRadius = 40f
    private val notchRadius = 80f
    private val shadowOffset = 6f
    private val fabCradleMargin = 20f
    private val fabCradleRadius = 50f
    
    init {
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        
        shadowPaint.color = Color.parseColor("#30000000")
        shadowPaint.style = Paint.Style.FILL
        shadowPaint.isAntiAlias = true
        shadowPaint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        
        setBackgroundColor(Color.TRANSPARENT)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        createPath(w.toFloat(), h.toFloat())
    }
    
    private fun createPath(width: Float, height: Float) {
        path.reset()
        val centerX = width / 2
        
        // Create the curved path with FAB cradle
        path.moveTo(0f, cornerRadius)
        
        // Top-left rounded corner
        path.quadTo(0f, 0f, cornerRadius, 0f)
        
        // Top edge with FAB cradle notch
        path.lineTo(centerX - fabCradleRadius - fabCradleMargin, 0f)
        
        // FAB cradle curve with proper margin and radius
        path.quadTo(
            centerX - fabCradleMargin, 
            -fabCradleRadius * 0.7f, 
            centerX, 
            -fabCradleRadius * 0.5f
        )
        path.quadTo(
            centerX + fabCradleMargin, 
            -fabCradleRadius * 0.7f, 
            centerX + fabCradleRadius + fabCradleMargin, 
            0f
        )
        
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
    }
    
    override fun onDraw(canvas: Canvas) {
        // Draw shadow first
        canvas.save()
        canvas.translate(0f, shadowOffset)
        canvas.drawPath(path, shadowPaint)
        canvas.restore()
        
        // Draw main background
        canvas.drawPath(path, paint)
        
        // Draw the navigation items
        super.onDraw(canvas)
    }
}