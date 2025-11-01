package com.koshpal_android.koshpalapp.ui.insights

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Premium animation utilities for hyper-polished fintech UI
 * Includes: number roll-up, spring animations, staggered fades, progress bar fills
 */
object PremiumAnimationUtils {

    // ==================== Number Roll-Up Animation ====================
    
    /**
     * Animates a number from 0 to target value with roll-up effect
     * Perfect for counters in insights cards
     */
    fun animateNumberRollUp(
        textView: TextView,
        targetValue: Int,
        duration: Long = 800L,
        prefix: String = "",
        suffix: String = ""
    ) {
        val animator = ValueAnimator.ofInt(0, targetValue)
        animator.duration = duration
        animator.interpolator = DecelerateInterpolator()
        
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            textView.text = "$prefix$value$suffix"
        }
        
        animator.start()
    }
    
    /**
     * Animates a currency amount from 0 to target with roll-up
     */
    fun animateCurrencyRollUp(
        textView: TextView,
        targetAmount: Double,
        duration: Long = 1000L
    ) {
        val animator = ValueAnimator.ofFloat(0f, targetAmount.toFloat())
        animator.duration = duration
        animator.interpolator = DecelerateInterpolator()
        
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            textView.text = "₹${String.format("%.0f", value)}"
        }
        
        animator.start()
    }
    
    // ==================== Spring Animation ====================
    
    /**
     * Spring-based expand/collapse animation for cards
     * Creates bouncy premium feel
     */
    fun springExpand(view: View, duration: Long = 300L) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.scaleY = 0.8f
        
        view.animate()
            .alpha(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
    }
    
    fun springCollapse(view: View, duration: Long = 250L, onEnd: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .scaleY(0.8f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.visibility = View.GONE
                view.scaleY = 1f
                onEnd?.invoke()
            }
            .start()
    }
    
    // ==================== Progress Bar Fill Animation ====================
    
    /**
     * Animates progress bar filling from 0 to target with smooth easing
     */
    fun animateProgressFill(
        progressBar: ProgressBar,
        targetProgress: Int,
        duration: Long = 600L,
        startDelay: Long = 0L
    ) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress)
        animator.duration = duration
        animator.startDelay = startDelay
        animator.interpolator = FastOutSlowInInterpolator()
        animator.start()
    }
    
    /**
     * Staggered progress bar animations for multiple bars
     */
    fun animateProgressBarsStaggered(
        progressBars: List<ProgressBar>,
        targetProgresses: List<Int>,
        staggerDelay: Long = 100L
    ) {
        progressBars.forEachIndexed { index, progressBar ->
            if (index < targetProgresses.size) {
                animateProgressFill(
                    progressBar,
                    targetProgresses[index],
                    duration = 600L,
                    startDelay = index * staggerDelay
                )
            }
        }
    }
    
    // ==================== Fade & Slide Animations ====================
    
    /**
     * Premium fade in with slide up
     * Perfect for cards appearing sequentially
     */
    fun fadeInSlideUp(
        view: View,
        duration: Long = 300L,
        startDelay: Long = 0L,
        distance: Float = 50f
    ) {
        view.alpha = 0f
        view.translationY = distance
        view.visibility = View.VISIBLE
        
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setStartDelay(startDelay)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Fade out with slide down
     */
    fun fadeOutSlideDown(
        view: View,
        duration: Long = 200L,
        distance: Float = 30f,
        onEnd: (() -> Unit)? = null
    ) {
        view.animate()
            .alpha(0f)
            .translationY(distance)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.visibility = View.GONE
                view.translationY = 0f
                onEnd?.invoke()
            }
            .start()
    }
    
    /**
     * Staggered fade-in for list items
     */
    fun staggeredFadeIn(
        views: List<View>,
        staggerDelay: Long = 80L,
        itemDuration: Long = 300L
    ) {
        views.forEachIndexed { index, view ->
            fadeInSlideUp(
                view,
                duration = itemDuration,
                startDelay = index * staggerDelay,
                distance = 30f
            )
        }
    }
    
    // ==================== Badge Pop Animation ====================
    
    /**
     * Makes badges "pop in" with bounce for percentage changes
     */
    fun popInBadge(view: View, duration: Long = 400L) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.visibility = View.VISIBLE
        
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setInterpolator(OvershootInterpolator(2f))
            .start()
    }
    
    // ==================== Rotation Animation ====================
    
    /**
     * Smooth rotation for expand/collapse arrows
     */
    fun rotateArrow(view: View, isExpanded: Boolean, duration: Long = 200L) {
        val targetRotation = if (isExpanded) 180f else 0f
        view.animate()
            .rotation(targetRotation)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
    
    // ==================== Shimmer to Content Transition ====================
    
    /**
     * Premium transition from shimmer to real content
     * Two-phase: shimmer fade out → content fade in
     */
    fun shimmerToContentTransition(
        shimmerView: View,
        contentView: View,
        fadeOutDuration: Long = 200L,
        fadeInDuration: Long = 300L
    ) {
        // Phase 1: Fade out shimmer
        shimmerView.animate()
            .alpha(0f)
            .setDuration(fadeOutDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                shimmerView.visibility = View.GONE
                shimmerView.alpha = 1f
                
                // Phase 2: Fade in content
                contentView.alpha = 0f
                contentView.visibility = View.VISIBLE
                contentView.animate()
                    .alpha(1f)
                    .setDuration(fadeInDuration)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }
    
    // ==================== Pulse Animation ====================
    
    /**
     * Subtle pulse for highlighting important elements
     */
    fun pulse(view: View, scaleFactor: Float = 1.05f, duration: Long = 500L) {
        val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 1f, scaleFactor)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f, scaleFactor)
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", scaleFactor, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", scaleFactor, 1f)
        
        scaleUp.duration = duration / 2
        scaleUpY.duration = duration / 2
        scaleDown.duration = duration / 2
        scaleDownY.duration = duration / 2
        
        scaleUp.interpolator = AccelerateDecelerateInterpolator()
        scaleUpY.interpolator = AccelerateDecelerateInterpolator()
        
        scaleUp.start()
        scaleUpY.start()
        
        scaleUp.doOnEnd {
            scaleDown.start()
            scaleDownY.start()
        }
    }
}
