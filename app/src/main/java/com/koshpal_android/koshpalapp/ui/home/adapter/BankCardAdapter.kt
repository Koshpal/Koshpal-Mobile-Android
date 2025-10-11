package com.koshpal_android.koshpalapp.ui.home.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ItemBankCardBinding
import com.koshpal_android.koshpalapp.model.BankSpending
import com.koshpal_android.koshpalapp.utils.BankThemeProvider

class BankCardAdapter(
    private val onAddCashClick: () -> Unit
) : ListAdapter<BankSpending, BankCardAdapter.BankCardViewHolder>(BankSpendingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankCardViewHolder {
        val binding = ItemBankCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BankCardViewHolder(binding, onAddCashClick)
    }

    override fun onBindViewHolder(holder: BankCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BankCardViewHolder(
        private val binding: ItemBankCardBinding,
        private val onAddCashClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bankSpending: BankSpending) {
            binding.apply {
                val context = binding.root.context

                // Show add button only for Cash card
                if (bankSpending.isCash) {
                    btnAddCash.visibility = View.VISIBLE
                    btnAddCash.setOnClickListener { onAddCashClick() }
                    
                    // Set cash card gradient
                    cardContent.setBackgroundResource(R.drawable.gradient_cash_card)
                    
                    // Ensure financial texture is visible for cash card
                    binding.ivFinancialTexture.visibility = View.VISIBLE
                    
                    // Cash card UI
                    tvBankName.text = "Cash"
                    // Show text initials for cash
                    binding.ivBankIcon.visibility = View.GONE
                    binding.tvBankInitials.visibility = View.VISIBLE
                    binding.tvBankInitials.text = "CA"
                    tvSpending.text = "₹${String.format("%,.0f", bankSpending.totalSpending)}"
                    tvTransactionCount.text = "${bankSpending.transactionCount} transactions"
                    
                } else {
                    btnAddCash.visibility = View.GONE
                    
                    // Get theme for bank/payment app
                    val theme = BankThemeProvider.getThemeForBankConsistent(bankSpending.bankName)
                    
                    // Apply bank/payment app branding
                    tvBankName.text = theme.displayName
                    
                    // Use actual drawable icon if available, otherwise fallback to text
                    if (theme.iconDrawable != null) {
                        // Show real icon
                        binding.ivBankIcon.setImageResource(theme.iconDrawable)
                        binding.ivBankIcon.visibility = View.VISIBLE
                        binding.tvBankInitials.visibility = View.GONE
                    } else {
                        // Show text initials
                        binding.ivBankIcon.visibility = View.GONE
                        binding.tvBankInitials.visibility = View.VISIBLE
                        binding.tvBankInitials.text = theme.iconInitials
                        binding.tvBankInitials.textSize = if (theme.iconInitials.length == 1) 18f else 14f
                    }
                    
                    tvSpending.text = "₹${String.format("%,.0f", bankSpending.totalSpending)}"
                    tvTransactionCount.text = "${bankSpending.transactionCount} transactions"
                    
                    // Apply gradient background
                    val gradient = createBrandedGradient(theme)
                    cardContent.background = gradient
                    
                    // Ensure financial texture is visible
                    binding.ivFinancialTexture.visibility = View.VISIBLE
                    
                    // Style icon based on icon style
                    styleIcon(theme)
                    
                    // Log for debugging
                    android.util.Log.d("BankCard", "🏦 ${bankSpending.bankName} → ${theme.displayName} (${theme.iconInitials})")
                }
            }
        }

        private fun createBrandedGradient(theme: BankThemeProvider.BankTheme): GradientDrawable {
            val context = binding.root.context
            
            // Map gradient angle to orientation
            val orientation = when (theme.gradientAngle) {
                BankThemeProvider.GradientAngle.DIAGONAL -> GradientDrawable.Orientation.TL_BR
                BankThemeProvider.GradientAngle.VERTICAL -> GradientDrawable.Orientation.TOP_BOTTOM
                BankThemeProvider.GradientAngle.HORIZONTAL -> GradientDrawable.Orientation.LEFT_RIGHT
                BankThemeProvider.GradientAngle.RADIAL -> GradientDrawable.Orientation.TL_BR // Default to diagonal for radial
            }
            
            // Create color array (2 or 3 colors)
            val colors = if (theme.accentColor != null) {
                intArrayOf(theme.primaryColor, theme.secondaryColor, theme.accentColor)
            } else {
                intArrayOf(theme.primaryColor, theme.secondaryColor)
            }
            
            return GradientDrawable(orientation, colors).apply {
                cornerRadius = 16 * context.resources.displayMetrics.density
                
                // For radial gradients, set gradient type
                if (theme.gradientAngle == BankThemeProvider.GradientAngle.RADIAL) {
                    gradientType = GradientDrawable.RADIAL_GRADIENT
                    gradientRadius = 300f
                }
            }
        }
        
        private fun styleIcon(theme: BankThemeProvider.BankTheme) {
            val context = binding.root.context
            
            binding.cardBankIcon.apply {
                when (theme.iconStyle) {
                    BankThemeProvider.IconStyle.GRADIENT -> {
                        // Gradient icon background
                        val iconGradient = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                                theme.primaryColor,
                                theme.secondaryColor
                )
            ).apply {
                            cornerRadius = 20 * context.resources.displayMetrics.density
                        }
                        setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                        background = iconGradient
                    }
                    BankThemeProvider.IconStyle.CIRCLE_SOLID -> {
                        // Solid color circle
                        setCardBackgroundColor(adjustAlpha(theme.primaryColor, 0.3f))
                    }
                    BankThemeProvider.IconStyle.ROUNDED -> {
                        // Rounded with semi-transparent background
                        setCardBackgroundColor(android.graphics.Color.parseColor("#40FFFFFF"))
                    }
                    BankThemeProvider.IconStyle.TEXT -> {
                        // Simple transparent background
                        setCardBackgroundColor(android.graphics.Color.parseColor("#30FFFFFF"))
                    }
                }
            }
        }
        
        private fun adjustAlpha(color: Int, factor: Float): Int {
            val alpha = Math.round(android.graphics.Color.alpha(color) * factor)
            val red = android.graphics.Color.red(color)
            val green = android.graphics.Color.green(color)
            val blue = android.graphics.Color.blue(color)
            return android.graphics.Color.argb(alpha, red, green, blue)
        }
    }

    class BankSpendingDiffCallback : DiffUtil.ItemCallback<BankSpending>() {
        override fun areItemsTheSame(oldItem: BankSpending, newItem: BankSpending): Boolean {
            return oldItem.bankName == newItem.bankName
        }

        override fun areContentsTheSame(oldItem: BankSpending, newItem: BankSpending): Boolean {
            return oldItem == newItem
        }
    }
}
