package com.koshpal_android.koshpalapp.ui.transactions

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import kotlin.math.abs

class SwipeToDeleteCallback(
    private val context: Context,
    private val onSwipeToDelete: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)
    private val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0
    private val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0
    private val background = ColorDrawable()
    private val backgroundColor = ContextCompat.getColor(context, R.color.error)
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
    
    private var hasVibrated = false
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(0, ItemTouchHelper.LEFT)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        onSwipeToDelete(position)
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(canvas, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Calculate swipe progress (0.0 to 1.0)
        val swipeProgress = abs(dX) / itemView.width.toFloat()
        val clampedProgress = swipeProgress.coerceAtMost(1.0f)
        
        // Trigger haptic feedback when threshold is reached
        if (clampedProgress >= 0.3f && !hasVibrated) {
            triggerHapticFeedback()
            hasVibrated = true
        } else if (clampedProgress < 0.3f) {
            hasVibrated = false
        }

        // Draw the red delete background with gradient effect
        background.color = backgroundColor
        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        background.draw(canvas)

        // Calculate position of delete icon with animation
        val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
        val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
        
        // Animate icon position based on swipe progress
        val maxIconOffset = 100f
        val iconOffset = (maxIconOffset * (1 - clampedProgress)).toInt()
        val deleteIconLeft = itemView.right - deleteIconMargin - intrinsicWidth - iconOffset
        val deleteIconRight = itemView.right - deleteIconMargin - iconOffset
        val deleteIconBottom = deleteIconTop + intrinsicHeight

        // Draw the delete icon with scaling animation
        val iconScale = 0.5f + (0.5f * clampedProgress) // Scale from 0.5 to 1.0
        val scaledWidth = (intrinsicWidth * iconScale).toInt()
        val scaledHeight = (intrinsicHeight * iconScale).toInt()
        val scaledLeft = deleteIconLeft + (intrinsicWidth - scaledWidth) / 2
        val scaledTop = deleteIconTop + (intrinsicHeight - scaledHeight) / 2
        val scaledRight = scaledLeft + scaledWidth
        val scaledBottom = scaledTop + scaledHeight

        deleteIcon?.setBounds(scaledLeft, scaledTop, scaledRight, scaledBottom)
        deleteIcon?.setTint(Color.WHITE)
        
        // Add alpha to icon based on swipe progress
        val iconAlpha = (255 * clampedProgress).toInt().coerceAtLeast(50)
        deleteIcon?.alpha = iconAlpha
        deleteIcon?.draw(canvas)

        // Add smooth fade and scale effect to the item
        val itemAlpha = 1.0f - (clampedProgress * 0.7f) // Fade to 30% opacity
        val itemScale = 1.0f - (clampedProgress * 0.1f) // Scale down to 90%
        
        itemView.alpha = itemAlpha.coerceAtLeast(0.3f)
        itemView.scaleY = itemScale.coerceAtLeast(0.9f)
        
        // Add slight rotation for more dynamic effect
        val rotation = clampedProgress * 2f // Max 2 degrees
        itemView.rotation = if (dX < 0) rotation else -rotation

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(canvas: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        canvas?.drawRect(left, top, right, bottom, clearPaint)
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.3f // Require 30% swipe to trigger delete
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        // Reset all transformations
        viewHolder.itemView.apply {
            alpha = 1.0f
            scaleY = 1.0f
            rotation = 0f
        }
        // Reset icon alpha
        deleteIcon?.alpha = 255
        // Reset vibration flag
        hasVibrated = false
    }
    
    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }
}
