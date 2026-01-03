package com.koshpal_android.koshpalapp.ui.home.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.model.BankSpending
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.ui.goals.GoalsViewModel
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Stateless HomeScreen Composable
 * 
 * @param greetingText Greeting text (e.g., "Good Evening")
 * @param userName User's name to display
 * @param currentMonthIncome Current month income amount
 * @param currentMonthExpenses Current month expenses amount
 * @param incomeChangePercentage Percentage change for income (e.g., "+12%")
 * @param expenseChangePercentage Percentage change for expenses (e.g., "-8%")
 * @param bankCards List of bank cards to display horizontally
 * @param recentTransactions List of recent transactions (max 4-5 items)
 * @param onProfileClick Callback when profile icon is clicked
 * @param onNotificationClick Callback when notification bell is clicked
 * @param onViewDetailsClick Callback when "View Details" is clicked
 * @param onBankCardClick Callback when a bank card is clicked (bank name as parameter)
 * @param onAddCashClick Callback when add cash button is clicked on Cash card
 * @param onAddPaymentClick Callback when "Add Payment" button is clicked
 * @param onTransactionClick Callback when a transaction item is clicked
 * @param onViewAllTransactionsClick Callback when "View All" is clicked
 */
@Composable
fun HomeScreen(
    greetingText: String,
    userName: String,
    currentMonthIncome: Double,
    currentMonthExpenses: Double,
    incomeChangePercentage: String? = null,
    expenseChangePercentage: String? = null,
    bankCards: List<BankSpending>,
    recentTransactions: List<Transaction>,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onViewDetailsClick: () -> Unit,
    onBankCardClick: (String) -> Unit,
    onAddCashClick: () -> Unit,
    onAddPaymentClick: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onViewAllTransactionsClick: () -> Unit,
    goalsViewModel: GoalsViewModel? = null,
    onAddGoalClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.backgroundstrucure2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp) // Bottom padding for navigation bar
        ) {
            // Top App Bar
            item {
                TopAppBarSection(
                    greetingText = greetingText,
                    userName = userName,
                    onProfileClick = onProfileClick,
                    onNotificationClick = onNotificationClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // This Month Overview Card
            item {
                ThisMonthOverviewCard(
                    income = currentMonthIncome,
                    expenses = currentMonthExpenses,
                    incomeChangePercentage = incomeChangePercentage,
                    expenseChangePercentage = expenseChangePercentage,
                    onViewDetailsClick = onViewDetailsClick,
                    currencyFormatter = currencyFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Your Banks & Cards Section
            item {
                BanksAndCardsSection(
                    bankCards = bankCards,
                    onBankCardClick = onBankCardClick,
                    onAddCashClick = onAddCashClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Action Buttons Row
            item {
                ActionButtonsRow(
                    onAddPaymentClick = onAddPaymentClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Recent Transactions Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: Icon + Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_rup),
                            contentDescription = "Transactions",
                            tint = AppColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Recent Transactions",
                            color = AppColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // View All Button
                    TextButton(
                        onClick = onViewAllTransactionsClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = AppColors.AccentBlue
                        )
                    ) {
                        Text(
                            text = "View All",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Recent Transactions Items
            if (recentTransactions.isNotEmpty()) {
                items(recentTransactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) },
                        currencyFormatter = currencyFormatter
                    )
                }
            } else {
                item {
                    // Empty state for transactions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent transactions",
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Financial Goals Section (moved below recent transactions)
            goalsViewModel?.let { vm ->
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    FinancialGoalsSection(
                        goalsViewModel = vm,
                        onAddGoalClick = onAddGoalClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Top App Bar - Clean design with profile icon and "Hi [USERNAME]" text
 */
@Composable
private fun TopAppBarSection(
    greetingText: String,
    userName: String,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Icon with blue background and glow effect
        Box(
            modifier = Modifier
                .size(48.dp)
                .drawBehind {
                    // Blue glow effect around the circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AppColors.AccentBlue.copy(alpha = 0.3f),
                                AppColors.AccentBlue.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width / 2f + 4.dp.toPx()
                        ),
                        radius = size.width / 2f + 4.dp.toPx()
                    )
                }
                .clip(CircleShape)
                .background(AppColors.AccentBlue)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            // Blue user icon
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // "Hi [USERNAME]" text - matching reference style (only one "Hi" in normal weight)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Hi",
                color = AppColors.TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = userName.uppercase(),
                color = AppColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * This Month Overview Card - Dark gray card with Income and Expense
 * Styled to match reference image with proper glow effects and layout
 */
@Composable
private fun ThisMonthOverviewCard(
    income: Double,
    expenses: Double,
    incomeChangePercentage: String?,
    expenseChangePercentage: String?,
    onViewDetailsClick: () -> Unit,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    // Get current month name
    val currentMonthName = remember {
        val calendar = Calendar.getInstance()
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        monthNames[calendar.get(Calendar.MONTH)]
    }
    
    // Use blue color for month and navigation (replacing green)
    val vibrantBlue = AppColors.AccentBlue
    
    // Dark charcoal gray base colors for sophisticated gradient - more visible
    val darkCharcoalBase = Color(0xFF0F0F0F) // Very dark charcoal (not pure black)
    val darkCharcoalLighter = Color(0xFF1F1F1F) // Lighter for gradient (more visible)
    val darkCharcoalMid = Color(0xFF151515) // Mid tone
    val darkCharcoalDarker = Color(0xFF080808) // Darker for depth
    val blueTint = Color(0xFF1E2A3A) // More visible blue-gray tint
    val blueTintBright = Color(0xFF2A3A4F) // Brighter blue for VFX effect
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = AppColors.AccentBlue.copy(alpha = 0.3f),
                ambientColor = AppColors.AccentBlue.copy(alpha = 0.15f)
            )
            .drawBehind {
                // Base dark charcoal background
                drawRect(color = darkCharcoalBase)
                
                // Premium organic gradient effect - centered at bottom, with prominent blue VFX
                // Main gradient from center-bottom (organic, not geometric)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            darkCharcoalLighter, // Center - lighter (fully opaque for visibility)
                            darkCharcoalMid, // Mid
                            darkCharcoalBase, // Base
                            darkCharcoalDarker // Outer - darker
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.85f), // Center-bottom
                        radius = size.width * 1.3f // Large radius for organic spread
                    )
                )
                
                // Prominent blue VFX gradient overlay (very visible)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            blueTintBright.copy(alpha = 0.8f), // Bright blue center - very visible
                            blueTint.copy(alpha = 0.6f), // Medium blue
                            blueTint.copy(alpha = 0.4f), // Fading blue
                            blueTint.copy(alpha = 0.2f), // Subtle blue
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.85f), // Center-bottom
                        radius = size.width * 1.1f // Large radius for organic spread
                    )
                )
                
                // Secondary blue VFX gradient (offset for natural variation)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            blueTintBright.copy(alpha = 0.5f), // Bright blue
                            blueTint.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.48f, size.height * 0.88f), // Slightly offset
                        radius = size.width * 0.9f
                    )
                )
                
                // Tertiary blue VFX gradient (for more depth and randomization)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            blueTint.copy(alpha = 0.4f),
                            blueTint.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.52f, size.height * 0.82f), // Another offset
                        radius = size.width * 0.7f
                    )
                )
                
                // Additional blue variation gradient for more VFX effect
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            blueTintBright.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.45f, size.height * 0.9f), // Another variation
                        radius = size.width * 0.6f
                    )
                )
                
                // Enhanced glow effects for premium look
                val glowColor = Color.White.copy(alpha = 0.15f)
                
                // Top edge glow (stronger)
                drawLine(
                    color = glowColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 3f
                )
                
                // Left edge glow (stronger)
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 2.5f
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent) // Transparent to show gradient
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Navigation Bar - "Money Manager" with bar chart icon, green "November", and green arrow button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Bar chart icon + "Money Manager" + green arrow + "November"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bar chart icon
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chart),
                        contentDescription = "Money Manager",
                        tint = AppColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    
                    Text(
                        text = "Money Manager",
                        color = AppColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Blue chevron separator
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = vibrantBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    
                    // Blue month name
                    Text(
                        text = currentMonthName,
                        color = vibrantBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Right side: Blue circular arrow button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AppColors.DarkCard)
                        .clickable(onClick = onViewDetailsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = "View Details",
                        tint = vibrantBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Income and Expense Row - Two column layout with perfectly centered content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp), // Equal padding from outside edges
                horizontalArrangement = Arrangement.SpaceBetween // Equal space on both sides
            ) {
                // Spends Column (Left) - Note: In reference, "Spends" is on left
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Arrow icon with blue glow effect
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .drawBehind {
                                    // Blue glow effect around the icon
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                AppColors.AccentBlue.copy(alpha = 0.4f),
                                                AppColors.AccentBlue.copy(alpha = 0.1f),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.width / 2f
                                        ),
                                        radius = size.width / 2f
                                    )
                                    // Subtle outline box
                                    drawRect(
                                        color = AppColors.AccentBlue.copy(alpha = 0.3f),
                                        style = Stroke(width = 0.5.dp.toPx())
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Arrow icon pointing up (for Spends/Expense) with blue tint
                            Icon(
                                painter = painterResource(id = R.drawable.arrowup),
                                contentDescription = "Spends",
                                tint = AppColors.AccentBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        
                        Text(
                            text = "Spends",
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = currencyFormatter.format(expenses).replace(".00", ""),
                        color = AppColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                
                // Income Column (Right)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Arrow icon with blue glow effect
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .drawBehind {
                                    // Blue glow effect around the icon
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                AppColors.AccentBlue.copy(alpha = 0.4f),
                                                AppColors.AccentBlue.copy(alpha = 0.1f),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.width / 2f
                                        ),
                                        radius = size.width / 2f
                                    )
                                    // Subtle outline box
                                    drawRect(
                                        color = AppColors.AccentBlue.copy(alpha = 0.3f),
                                        style = Stroke(width = 0.5.dp.toPx())
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Arrow icon pointing down (for Income) with blue tint
                            Icon(
                                painter = painterResource(id = R.drawable.arrowdown),
                                contentDescription = "Income",
                                tint = AppColors.AccentBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        
                        Text(
                            text = "Income",
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = currencyFormatter.format(income).replace(".00", ""),
                        color = AppColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Banks & Cards Section with horizontal scrollable list
 */
@Composable
private fun BanksAndCardsSection(
    bankCards: List<BankSpending>,
    onBankCardClick: (String) -> Unit,
    onAddCashClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_credit_card),
                contentDescription = "Banks",
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Your Banks & Cards",
                color = AppColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Horizontal Scrollable Bank Cards
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(bankCards) { bankCard ->
                BankCardItem(
                    bankSpending = bankCard,
                    onCardClick = { onBankCardClick(bankCard.bankName) },
                    onAddCashClick = onAddCashClick
                )
            }
        }
    }
}

/**
 * Get bank-specific card colors (light backgrounds for dark theme contrast)
 * Left side: Light pastel tint of bank's brand color (matching logo)
 * Right side: Always white/light gray
 */
private fun getBankCardColors(bankName: String): Pair<Color, Color> {
    val normalizedName = bankName.uppercase()
    
    // Get light pastel color for left side (matching bank's brand color)
    val leftColor = when {
        normalizedName.contains("IPPB") || normalizedName.contains("INDIA POST") -> {
            // India Post: Light orange tint (matching orange envelope icon)
            Color(0xFFFFE0B2) // Light orange pastel
        }
        normalizedName.contains("SBI") || normalizedName.contains("STATE BANK") -> {
            // SBI: Light blue tint (matching SBI blue brand)
            Color(0xFFB3E5FC) // Light blue pastel
        }
        normalizedName.contains("HDFC") -> {
            // HDFC: Light red tint
            Color(0xFFFFCDD2) // Light red pastel
        }
        normalizedName.contains("ICICI") -> {
            // ICICI: Light orange tint
            Color(0xFFFFE0B2) // Light orange pastel
        }
        normalizedName.contains("AXIS") -> {
            // Axis: Light burgundy/pink tint
            Color(0xFFF8BBD0) // Light pink/burgundy pastel
        }
        normalizedName.contains("KOTAK") -> {
            Color(0xFFFFCDD2) // Light red pastel
        }
        normalizedName.contains("PNB") || normalizedName.contains("PUNJAB NATIONAL") -> {
            Color(0xFFBBDEFB) // Light blue pastel
        }
        normalizedName.contains("BOB") || normalizedName.contains("BANK OF BARODA") -> {
            Color(0xFFFFE0B2) // Light orange pastel
        }
        normalizedName.contains("CANARA") -> {
            Color(0xFFFFCDD2) // Light red pastel
        }
        normalizedName.contains("UNION BANK") -> {
            Color(0xFFFFCDD2) // Light red pastel
        }
        normalizedName.contains("PAYTM") -> {
            Color(0xFFB3E5FC) // Light blue pastel
        }
        normalizedName.contains("PHONEPE") -> {
            Color(0xFFE1BEE7) // Light purple pastel
        }
        normalizedName.contains("GOOGLE PAY") || normalizedName.contains("GPAY") -> {
            Color(0xFFBBDEFB) // Light blue pastel
        }
        else -> {
            // Default: Light gray
            Color(0xFFE0E0E0) // Light gray
        }
    }
    
    // Right side: Always white/light gray
    val rightColor = Color(0xFFF5F5F5) // Off-white
    
    return Pair(leftColor, rightColor)
}

/**
 * Individual Bank Card Item - Styled exactly like reference images
 */
@Composable
private fun BankCardItem(
    bankSpending: BankSpending,
    onCardClick: () -> Unit,
    onAddCashClick: () -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormatter = remember { 
        SimpleDateFormat("hh:mm a • d, MMM", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }
    
    // Use real account number or fallback
    val accountNumber = remember(bankSpending.accountNumber, bankSpending.bankName) {
        bankSpending.accountNumber ?: run {
            val hash = bankSpending.bankName.hashCode()
            val last4 = Math.abs(hash) % 10000
            String.format("%04d", last4)
        }
    }
    
    // Get bank-specific colors
    val (leftColor, rightColor) = getBankCardColors(bankSpending.bankName)
    
    // Format last updated timestamp
    val lastUpdatedText = remember(bankSpending.lastUpdated) {
        bankSpending.lastUpdated?.let { dateFormatter.format(Date(it)) } ?: ""
    }
    
    // Always show spending instead of balance
    val displaySpending = bankSpending.totalSpending
    
    // Check if India Post for special styling
    val isIndiaPost = bankSpending.bankName.contains("IPPB", ignoreCase = true) || 
                      bankSpending.bankName.contains("India Post", ignoreCase = true)
    
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(128.dp) // Reduced by 20% (from 160dp to 128dp)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent // Transparent to show two-tone background
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Two-tone background - matching reference exactly
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Left section (45% width) - Bank-specific color
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(126.dp) // ~45% of 280dp
                        .background(leftColor)
                )
                
                // Right section (55% width) - Light gray/off-white
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(rightColor)
                )
            }
            
            // Content overlay - matching reference layout exactly
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Left: Bank Icon
                Box(
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    if (bankSpending.isCash) {
                        // Cash card - orange envelope icon
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_envelope),
                                contentDescription = "Cash",
                                tint = Color(0xFFFF9800), // Orange
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else if (isIndiaPost) {
                        // India Post - orange envelope icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_envelope),
                            contentDescription = "India Post",
                            tint = Color(0xFFFF9800), // Orange
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        // Other banks - use bank initials or icon
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E88E5)), // SBI blue
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = bankSpending.bankName.take(2).uppercase(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Top Right: Recent transaction amount with arrow (only for India Post or if there's recent activity)
                if (isIndiaPost && bankSpending.totalSpending > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currencyFormatter.format(bankSpending.totalSpending).replace(".00", ""),
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // Arrow icon in circular background
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0E0E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_up_right),
                                    contentDescription = null,
                                    tint = Color(0xFF666666),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
                
                // Bottom Left: Bank Name and Account Number
                Column(
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = if (isIndiaPost) "India Post" else bankSpending.bankName,
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "xx$accountNumber",
                        color = Color(0xFF808080), // Light gray
                        fontSize = 12.sp
                    )
                }
                
                // Bottom Right: Timestamp, Balance, and Refresh Icon
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalAlignment = Alignment.End
                ) {
                    if (lastUpdatedText.isNotEmpty()) {
                        Text(
                            text = lastUpdatedText,
                            color = Color(0xFF808080), // Light gray
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = currencyFormatter.format(displaySpending).replace(".00", ""),
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Action Buttons Row - Add Payment and Reminders
 */
@Composable
private fun ActionButtonsRow(
    onAddPaymentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Add Payment Button (Full Width)
    Button(
        onClick = onAddPaymentClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.DarkButtonBg
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_expense),
                contentDescription = "Add",
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Payment",
                color = AppColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Recent Transactions Card - Only list, title is outside
 */
@Composable
private fun RecentTransactionsCard(
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.DarkCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Transactions List (no header - title is outside)
            if (transactions.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent transactions",
                        color = AppColors.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    transactions.forEach { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction) },
                            currencyFormatter = currencyFormatter
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Transaction Item - Exact match to reference image
 * Each item is in its own rounded card with category icon
 */
@Composable
private fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    currencyFormatter: NumberFormat
) {
    // Date format: "Dec 31, 22:09" (Month Day, HH:mm)
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(transaction.date) {
        dateFormat.format(Date(transaction.date))
    }
    
    val isExpense = transaction.type == TransactionType.DEBIT
    val isIncome = transaction.type == TransactionType.CREDIT
    val isTransfer = transaction.type == TransactionType.TRANSFER
    
    // Get category info
    val category = remember(transaction.categoryId) {
        com.koshpal_android.koshpalapp.model.TransactionCategory.getDefaultCategories()
            .find { it.id == transaction.categoryId }
    }
    
    // Category icon and color
    val categoryIcon = category?.icon ?: R.drawable.ic_category_default
    val categoryColor = remember(category?.color) {
        try {
            category?.color?.let { 
                val androidColor = AndroidColor.parseColor(it)
                Color(androidColor)
            } ?: Color(0xFF757575) // Default grey if no category
        } catch (e: Exception) {
            Color(0xFF757575) // Default grey if parsing fails
        }
    }
    
    // For transfers, use purple; otherwise use category color
    val indicatorBgColor = if (isTransfer) Color(0xFF9C27B0) else categoryColor
    
    // Amount color: Green for income, White for expense
    val amountColor = if (isIncome) Color(0xFF4CAF50) else AppColors.TextPrimary
    val amountPrefix = if (isIncome) "+" else "-"
    
    // Transaction type label
    val typeLabel = if (isIncome) "Income" else "Expense"
    
    // Compact transaction item card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.DarkCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Category Icon in colored circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(indicatorBgColor),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isTransfer -> {
                        // White swap/transfer icon for transfers
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_swap_horizontal),
                            contentDescription = "Transfer",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    else -> {
                        // Category icon (white tinted)
                        Icon(
                            painter = painterResource(id = categoryIcon),
                            contentDescription = category?.name ?: "Category",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Middle: Name, Date, and Type - properly aligned
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = transaction.merchant.ifEmpty { transaction.description },
                    color = AppColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = "•",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = typeLabel,
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            
            // Right: Amount with prefix and arrow - properly aligned
            Row(
                modifier = Modifier.wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$amountPrefix${currencyFormatter.format(transaction.amount)}",
                    color = amountColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                // Action icon (arrow pointing up-right)
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_up_right),
                    contentDescription = "View Details",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

