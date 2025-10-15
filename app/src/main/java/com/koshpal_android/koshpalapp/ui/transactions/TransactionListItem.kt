package com.koshpal_android.koshpalapp.ui.transactions

import com.koshpal_android.koshpalapp.model.Transaction

/**
 * Sealed class to represent items in the transaction list
 * Supports both actual data and loading placeholders for progressive loading
 */
sealed class TransactionListItem {
    /**
     * Represents a fully loaded transaction with actual data
     */
    data class Data(val transaction: Transaction) : TransactionListItem() {
        // Use transaction ID for unique identification
        val id: String get() = transaction.id
    }
    
    /**
     * Represents a loading placeholder with shimmer effect
     * Each placeholder has a unique ID to enable proper DiffUtil comparison
     */
    data class Loading(val id: String = java.util.UUID.randomUUID().toString()) : TransactionListItem()
}
