package com.koshpal_android.koshpalapp.ui.budget.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.ui.budget.model.BudgetCategory
import com.koshpal_android.koshpalapp.ui.budget.model.BudgetPlanet
import com.koshpal_android.koshpalapp.ui.budget.model.FinancialUniverseData
import kotlin.math.*
import kotlin.random.Random

/**
 * Revolutionary Financial Universe Visualization
 * Each budget category is a planet orbiting around the user's spending center
 */
class FinancialUniverseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Universe data
    private var universeData = FinancialUniverseData()
    private var centerX = 0f
    private var centerY = 0f
    private var maxRadius = 0f
    
    // Animation
    private var orbitAnimator: ValueAnimator? = null
    private var expansionAnimator: ValueAnimator? = null
    private var currentTime = 0f
    
    // Touch handling
    private val gestureDetector = GestureDetector(context, UniverseGestureListener())
    private var selectedPlanet: BudgetPlanet? = null
    
    // Paint objects
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val planetPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Colors
    private val universeBackground = ContextCompat.getColor(context, R.color.universe_background)
    private val orbitLineColor = ContextCompat.getColor(context, R.color.universe_orbit_line)
    private val centerGlowColor = ContextCompat.getColor(context, R.color.universe_center_glow)
    private val planetGlowColor = ContextCompat.getColor(context, R.color.planet_glow_effect)
    
    // Interaction callback
    var onPlanetClickListener: ((BudgetCategory) -> Unit)? = null
    var onPlanetLongClickListener: ((BudgetCategory) -> Unit)? = null

    init {
        setupPaints()
        startOrbitAnimation()
    }

    private fun setupPaints() {
        backgroundPaint.color = universeBackground
        
        orbitPaint.apply {
            color = orbitLineColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
            alpha = 100
            pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
        }
        
        planetPaint.apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        glowPaint.apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        textPaint.apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        
        centerGlowPaint.apply {
            color = centerGlowColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        maxRadius = min(w, h) / 2f - 100f
        
        // Setup center glow gradient
        val centerGradient = RadialGradient(
            centerX, centerY, 60f,
            intArrayOf(centerGlowColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        centerGlowPaint.shader = centerGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw universe background
        canvas.drawColor(universeBackground)
        
        // Draw center glow effect
        drawCenterGlow(canvas)
        
        // Draw orbit lines
        drawOrbitLines(canvas)
        
        // Draw planets
        drawPlanets(canvas)
        
        // Draw center spending indicator
        drawCenterIndicator(canvas)
        
        // Draw expanded planet details if any
        selectedPlanet?.let { planet ->
            if (planet.isExpanded) {
                drawExpandedPlanetDetails(canvas, planet)
            }
        }
    }

    private fun drawCenterGlow(canvas: Canvas) {
        // Pulsing center glow
        val pulseScale = 1f + 0.1f * sin(currentTime * 2f)
        canvas.save()
        canvas.scale(pulseScale, pulseScale, centerX, centerY)
        canvas.drawCircle(centerX, centerY, 60f, centerGlowPaint)
        canvas.restore()
    }

    private fun drawOrbitLines(canvas: Canvas) {
        universeData.planets.forEach { planet ->
            canvas.drawCircle(centerX, centerY, planet.orbitRadius, orbitPaint)
        }
    }

    private fun drawPlanets(canvas: Canvas) {
        universeData.planets.forEach { planet ->
            drawPlanet(canvas, planet)
        }
    }

    private fun drawPlanet(canvas: Canvas, planet: BudgetPlanet) {
        val angle = planet.currentAngle + currentTime * planet.orbitSpeed
        val x = centerX + cos(angle) * planet.orbitRadius
        val y = centerY + sin(angle) * planet.orbitRadius
        
        val baseSize = planet.size * (if (planet.isExpanded) 2f else 1f)
        val pulseSize = baseSize * (1f + planet.pulseIntensity * sin(currentTime * 4f))
        
        // Draw planet glow
        if (planet.pulseIntensity > 0f || planet.isExpanded) {
            val glowRadius = pulseSize * 1.5f
            val glowGradient = RadialGradient(
                x, y, glowRadius,
                intArrayOf(planetGlowColor, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            glowPaint.shader = glowGradient
            glowPaint.alpha = (100 * (planet.pulseIntensity + if (planet.isExpanded) 0.5f else 0f)).toInt()
            canvas.drawCircle(x, y, glowRadius, glowPaint)
        }
        
        // Draw planet
        val planetColor = getPlanetColor(planet.category)
        planetPaint.color = planetColor
        canvas.drawCircle(x, y, pulseSize, planetPaint)
        
        // Draw planet ring for expanded state
        if (planet.isExpanded) {
            val ringPaint = Paint(planetPaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = Color.WHITE
                alpha = 150
            }
            canvas.drawCircle(x, y, pulseSize + 8f, ringPaint)
        }
        
        // Draw category icon
        drawPlanetIcon(canvas, planet, x, y, pulseSize)
        
        // Draw category name if expanded or large enough
        if (planet.isExpanded || pulseSize > 40f) {
            drawPlanetLabel(canvas, planet, x, y + pulseSize + 30f)
        }
    }

    private fun drawPlanetIcon(canvas: Canvas, planet: BudgetPlanet, x: Float, y: Float, size: Float) {
        // For now, draw a simple icon based on category
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = size * 0.6f
            textAlign = Paint.Align.CENTER
        }
        
        val icon = getCategoryIcon(planet.category.name)
        val textBounds = Rect()
        iconPaint.getTextBounds(icon, 0, icon.length, textBounds)
        canvas.drawText(icon, x, y + textBounds.height() / 2f, iconPaint)
    }

    private fun drawPlanetLabel(canvas: Canvas, planet: BudgetPlanet, x: Float, y: Float) {
        val labelPaint = Paint(textPaint).apply {
            textSize = 20f
            alpha = if (planet.isExpanded) 255 else 180
        }
        canvas.drawText(planet.category.name, x, y, labelPaint)
        
        if (planet.isExpanded) {
            val amountText = "₹${String.format("%.0f", planet.category.spentAmount)}"
            labelPaint.textSize = 16f
            labelPaint.alpha = 200
            canvas.drawText(amountText, x, y + 25f, labelPaint)
        }
    }

    private fun drawCenterIndicator(canvas: Canvas) {
        // Draw total spending in center
        val totalSpent = universeData.planets.sumOf { it.category.spentAmount }
        val centerText = "₹${String.format("%.0f", totalSpent)}"
        
        val centerTextPaint = Paint(textPaint).apply {
            textSize = 28f
            color = Color.WHITE
        }
        
        canvas.drawText(centerText, centerX, centerY + 10f, centerTextPaint)
        
        val labelText = "Total Spent"
        centerTextPaint.textSize = 14f
        centerTextPaint.alpha = 180
        canvas.drawText(labelText, centerX, centerY - 20f, centerTextPaint)
    }

    private fun drawExpandedPlanetDetails(canvas: Canvas, planet: BudgetPlanet) {
        // Draw detailed information panel for expanded planet
        val panelWidth = 200f
        val panelHeight = 120f
        val panelX = width - panelWidth - 20f
        val panelY = 20f
        
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = 200
        }
        
        val panelRect = RectF(panelX, panelY, panelX + panelWidth, panelY + panelHeight)
        canvas.drawRoundRect(panelRect, 16f, 16f, panelPaint)
        
        // Draw panel content
        val contentPaint = Paint(textPaint).apply {
            textSize = 16f
            textAlign = Paint.Align.LEFT
        }
        
        val padding = 16f
        var currentY = panelY + padding + 20f
        
        canvas.drawText(planet.category.name, panelX + padding, currentY, contentPaint)
        currentY += 25f
        
        canvas.drawText("Spent: ₹${String.format("%.0f", planet.category.spentAmount)}", panelX + padding, currentY, contentPaint)
        currentY += 20f
        
        canvas.drawText("Budget: ₹${String.format("%.0f", planet.category.allocatedAmount)}", panelX + padding, currentY, contentPaint)
        currentY += 20f
        
        val remaining = planet.category.remainingAmount
        val remainingColor = if (remaining >= 0) Color.GREEN else Color.RED
        contentPaint.color = remainingColor
        canvas.drawText("Remaining: ₹${String.format("%.0f", remaining)}", panelX + padding, currentY, contentPaint)
    }

    private fun getPlanetColor(category: BudgetCategory): Int {
        return when (category.tier.name) {
            "ESSENTIALS" -> ContextCompat.getColor(context, R.color.tier_essentials_primary)
            "WANTS" -> ContextCompat.getColor(context, R.color.tier_wants_primary)
            "GOALS" -> ContextCompat.getColor(context, R.color.tier_goals_primary)
            else -> Color.GRAY
        }
    }

    private fun getCategoryIcon(categoryName: String): String {
        return when (categoryName.lowercase()) {
            "food", "dining", "restaurant" -> "🍽️"
            "transport", "uber", "taxi" -> "🚗"
            "shopping", "amazon", "flipkart" -> "🛍️"
            "entertainment", "netflix", "spotify" -> "🎬"
            "groceries", "supermarket" -> "🛒"
            "bills", "electricity", "water" -> "💡"
            "rent", "home" -> "🏠"
            "savings", "investment" -> "💰"
            "health", "medical" -> "🏥"
            "education", "books" -> "📚"
            else -> "💳"
        }
    }

    private fun startOrbitAnimation() {
        orbitAnimator?.cancel()
        orbitAnimator = ValueAnimator.ofFloat(0f, Float.MAX_VALUE).apply {
            duration = Long.MAX_VALUE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                currentTime = (animation.animatedValue as Float) * 0.01f
                invalidate()
            }
            start()
        }
    }

    fun setUniverseData(data: FinancialUniverseData) {
        this.universeData = data
        invalidate()
    }

    fun addPlanet(category: BudgetCategory) {
        val planet = createPlanetFromCategory(category)
        val newPlanets = universeData.planets.toMutableList()
        newPlanets.add(planet)
        universeData = universeData.copy(planets = newPlanets)
        invalidate()
    }

    private fun createPlanetFromCategory(category: BudgetCategory): BudgetPlanet {
        val orbitIndex = universeData.planets.size
        val orbitRadius = 80f + (orbitIndex * 60f).coerceAtMost(maxRadius - 50f)
        val size = (20f + (category.allocatedAmount / 1000f).toFloat()).coerceIn(15f, 50f)
        
        return BudgetPlanet(
            category = category,
            size = size,
            orbitRadius = orbitRadius,
            orbitSpeed = 0.5f + Random.nextFloat() * 0.5f,
            currentAngle = Random.nextFloat() * 2f * PI.toFloat(),
            pulseIntensity = category.urgencyLevel.animationIntensity
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private inner class UniverseGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val touchedPlanet = findPlanetAtPosition(e.x, e.y)
            touchedPlanet?.let { planet ->
                onPlanetClickListener?.invoke(planet.category)
                togglePlanetExpansion(planet)
                return true
            }
            return false
        }

        override fun onLongPress(e: MotionEvent) {
            val touchedPlanet = findPlanetAtPosition(e.x, e.y)
            touchedPlanet?.let { planet ->
                onPlanetLongClickListener?.invoke(planet.category)
            }
        }
    }

    private fun findPlanetAtPosition(x: Float, y: Float): BudgetPlanet? {
        return universeData.planets.find { planet ->
            val angle = planet.currentAngle + currentTime * planet.orbitSpeed
            val planetX = centerX + cos(angle) * planet.orbitRadius
            val planetY = centerY + sin(angle) * planet.orbitRadius
            val distance = sqrt((x - planetX).pow(2) + (y - planetY).pow(2))
            distance <= planet.size * (if (planet.isExpanded) 2f else 1f)
        }
    }

    private fun togglePlanetExpansion(planet: BudgetPlanet) {
        // Collapse other planets first
        val updatedPlanets = universeData.planets.map { p ->
            if (p == planet) {
                p.copy(isExpanded = !p.isExpanded)
            } else {
                p.copy(isExpanded = false)
            }
        }
        
        universeData = universeData.copy(planets = updatedPlanets)
        selectedPlanet = if (planet.isExpanded) null else planet.copy(isExpanded = true)
        
        // Animate expansion
        animateExpansion()
    }

    private fun animateExpansion() {
        expansionAnimator?.cancel()
        expansionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        orbitAnimator?.cancel()
        expansionAnimator?.cancel()
    }
}
