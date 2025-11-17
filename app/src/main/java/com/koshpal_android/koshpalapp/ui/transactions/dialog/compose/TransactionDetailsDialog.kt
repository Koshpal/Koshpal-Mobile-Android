package com.koshpal_android.koshpalapp.ui.transactions.dialog.compose

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import com.koshpal_android.koshpalapp.ui.theme.KoshpalTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Format timestamp to a readable date
 */
private fun formatDate(timestamp: Long?): String? {
    if (timestamp == null) return null
    
    val date = Date(timestamp)
    val format = SimpleDateFormat("EEE, d MMM, h:mm a", Locale.getDefault())
    return format.format(date)
}

@Composable
fun TransactionDetailsDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    // Dialog state
    val context = LocalContext.current
    
    // Editable state
    var merchantName by remember { mutableStateOf(TextFieldValue(transaction.merchant)) }
    var amount by remember { mutableStateOf(TextFieldValue(transaction.amount.toString())) }
    var notes by remember { mutableStateOf(TextFieldValue(transaction.notes ?: "")) }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var includedInCashFlow by remember { mutableStateOf(transaction.includedInCashFlow) }
    var isStarred by remember { mutableStateOf(transaction.isStarred) }
    
    // Photo state
    var photoUri by remember { 
        mutableStateOf<Uri?>(
            if (!transaction.attachmentPath.isNullOrEmpty()) {
                Uri.parse(transaction.attachmentPath)
            } else {
                null
            }
        )
    }
    
    // Category list (mock data for now, should be injected)
    val categories = remember { TransactionCategory.getDefaultCategories() }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        KoshpalTheme {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.PureBlack
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.PureBlack)
                            .padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Close button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.DarkCard)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Close",
                                    tint = AppColors.TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Title
                            Text(
                                text = "Transaction Details",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary,
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                            )
                            
                            // Star button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.DarkCard)
                                    .clickable { isStarred = !isStarred },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (isStarred) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                                    ),
                                    contentDescription = "Star",
                                    tint = if (isStarred) Color(0xFFFFD700) else AppColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Menu button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.DarkCard)
                                    .padding(start = 8.dp)
                                    .clickable { /* Open menu */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more_vert),
                                    contentDescription = "More Options",
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    // Transaction Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.AccentBlue
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            // Merchant Name field
                            OutlinedTextField(
                                value = merchantName,
                                onValueChange = { merchantName = it },
                                placeholder = { Text("Merchant", color = Color.White.copy(alpha = 0.7f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = Color.White,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                textStyle = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Amount field
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "₹",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                
                                OutlinedTextField(
                                    value = amount,
                                    onValueChange = { amount = it },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = Color.White,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    textStyle = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 4.dp)
                                )
                            }
                            
                            // Category Selection
                            val category = categories.find { it.id == selectedCategoryId }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(0.dp),
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .wrapContentWidth()
                                    .clickable { /* Open category selection */ }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    // Category icon
                                    val categoryColor = category?.color?.let { Color(android.graphics.Color.parseColor(it)) }
                                        ?: Color.Gray
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = category?.icon ?: R.drawable.ic_category_default
                                            ),
                                            contentDescription = "Category Icon",
                                            tint = categoryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    
                                    // Category name
                                    Text(
                                        text = category?.name ?: "Uncategorized",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(start = 10.dp, end = 4.dp)
                                    )
                                    
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                                        contentDescription = "Select Category",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            // Date
                            Text(
                                text = formatDate(transaction.timestamp) ?: "Unknown date",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                    
                    // Notes Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.DarkCard
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_note),
                                    contentDescription = "Notes",
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                
                                Text(
                                    text = "Notes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                placeholder = { Text("Add notes...", color = AppColors.TextTertiary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppColors.DarkCard,
                                    unfocusedContainerColor = AppColors.DarkCard,
                                    disabledContainerColor = AppColors.DarkCard,
                                    focusedBorderColor = Color(0xFF3D3D3D),
                                    unfocusedBorderColor = Color(0xFF3D3D3D),
                                    cursorColor = AppColors.AccentBlue,
                                    focusedTextColor = AppColors.TextPrimary,
                                    unfocusedTextColor = AppColors.TextPrimary
                                ),
                                minLines = 2,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Attach Photo Button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 12.dp)
                            .clickable { /* Photo attachment logic */ },
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.DarkCard
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_image),
                                contentDescription = "Attach Photo",
                                tint = AppColors.AccentBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Text(
                                text = "Attach Photo",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppColors.TextPrimary,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                    
                    // Photo Preview (conditionally shown)
                    if (photoUri != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = AppColors.DarkCard
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppColors.DarkButtonBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_image),
                                        contentDescription = "Receipt Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                ) {
                                    Text(
                                        text = "Receipt Photo",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TextPrimary
                                    )
                                    
                                    Text(
                                        text = "2.3 MB", // This should be calculated from actual photo
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary
                                    )
                                }
                                
                                IconButton(
                                    onClick = { photoUri = null }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_close),
                                        contentDescription = "Remove Photo",
                                        tint = Color(0xFFF44336),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Other Info Section (UPI Ref and SMS)
                    // Check for either SMS body or UPI reference number (using description which might contain UPI refs)
                    if (!transaction.description.isNullOrEmpty() || !transaction.smsBody.isNullOrEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = AppColors.DarkCard
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                // Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_info),
                                        contentDescription = "Other Info",
                                        tint = AppColors.TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    
                                    Text(
                                        text = "Other info",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TextPrimary,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                
                                // UPI Ref No
                                // Check if description contains potential UPI reference
                                if (!transaction.description.isNullOrEmpty() && transaction.description.contains("ref", ignoreCase = true)) {
                                    Column(
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Text(
                                            text = "UPI Ref No",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppColors.TextSecondary
                                        )
                                        
                                        Text(
                                            text = transaction.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AppColors.TextPrimary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                                
                                // SMS Body
                                if (!transaction.smsBody.isNullOrEmpty()) {
                                    Column {
                                        Text(
                                            text = "SMS",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppColors.TextSecondary
                                        )
                                        
                                        Text(
                                            text = transaction.smsBody,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AppColors.TextPrimary,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Cash Flow Toggle
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.DarkCard
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_trending_up),
                                    contentDescription = "Cash Flow",
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Column(
                                    modifier = Modifier.padding(start = 12.dp)
                                ) {
                                    Text(
                                        text = "Cash Flow",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TextPrimary
                                    )
                                    
                                    Text(
                                        text = "Include in cash flow analysis",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.TextSecondary
                                    )
                                }
                            }
                            
                            Switch(
                                checked = includedInCashFlow,
                                onCheckedChange = { includedInCashFlow = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AppColors.AccentBlue,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = AppColors.DarkButtonBg,
                                )
                            )
                        }
                    }
                    
                    // Save Button
                    Button(
                        onClick = {
                            // Create updated transaction
                            val updatedTransaction = transaction.copy(
                                merchant = merchantName.text,
                                amount = amount.text.toDoubleOrNull() ?: transaction.amount,
                                notes = notes.text.ifEmpty { null },
                                categoryId = selectedCategoryId,
                                includedInCashFlow = includedInCashFlow,
                                isStarred = isStarred,
                                attachmentPath = photoUri?.toString()
                            )
                            onSave(updatedTransaction)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.AccentBlue
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp, top = 12.dp)
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Save Changes",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
