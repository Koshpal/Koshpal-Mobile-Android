package com.koshpal_android.koshpalapp.ui.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.koshpal_android.koshpalapp.R

class FabCradleBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {
    
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // FAB Cradle Properties
    private val fabCradleMargin = 24f
    private val fabCradleRadius = 56f
    private val cornerRadius = 36f
    private val shadowOffset = 8f
    
    init {
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        
        shadowPaint.color = Color.parseColor("#40000000")
        shadowPaint.style = Paint.Style.FILL
        shadowPaint.isAntiAlias = true
        shadowPaint.maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        
        setBackgroundColor(Color.TRANSPARENT)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        createFabCradlePath(w.toFloat(), h.toFloat())
    }
    
    private fun createFabCradlePath(width: Float, height: Float) {
        path.reset()
        val centerX = width / 2
        
        // Start from bottom-left
        path.moveTo(0f, height)
        
        // Left edge
        path.lineTo(0f, cornerRadius)
        
        // Top-left rounded corner
        path.quadTo(0f, 0f, cornerRadius, 0f)
        
        // Top edge until FAB cradle start
        path.lineTo(centerX - fabCradleRadius - fabCradleMargin, 0f)
        
        // FAB Cradle - Left curve
        path.quadTo(
            centerX - fabCradleMargin, 
            -fabCradleRadius * 0.8f, 
            centerX - fabCradleMargin / 2, 
            -fabCradleRadius * 0.6f
        )
        
        // FAB Cradle - Center curve
        path.quadTo(
            centerX, 
            -fabCradleRadius * 0.7f, 
            centerX + fabCradleMargin / 2, 
            -fabCradleRadius * 0.6f
        )
        
        // FAB Cradle - Right curve
        path.quadTo(
            centerX + fabCradleMargin, 
            -fabCradleRadius * 0.8f, 
            centerX + fabCradleRadius + fabCradleMargin, 
            0f
        )
        
        // Top edge from FAB cradle to top-right
        path.lineTo(width - cornerRadius, 0f)
        
        // Top-right rounded corner
        path.quadTo(width, 0f, width, cornerRadius)
        
        // Right edge
        path.lineTo(width, height)
        
        // Bottom edge
        path.lineTo(0f, height)
        
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
