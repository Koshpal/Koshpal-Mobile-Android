package com.koshpal_android.koshpalapp.sms.rules

/**
 * Result of a successful rule match.
 * Contains all extracted fields from rules.json data_fields.
 */
data class RuleMatchResult(
    val bankName: String,
    val smsType: String,
    val transactionType: String?,
    val statementType: String?,
    val transactionCategory: String?,
    val amount: Double,
    val date: Long,
    val pan: String?,
    val accountBalance: Double?,
    val pos: String?,
    val networkReferenceId: String?,
    val matchedPatternUid: String? = null
) {
    /** Whether this match represents a transaction (vs statement or balance-only). */
    val isTransaction: Boolean
        get() = smsType == "transaction" && transactionType != "balance"

    /** Whether we should create a Transaction record. */
    val shouldCreateTransaction: Boolean
        get() = isTransaction && amount > 0 && (pos?.isNotBlank() == true || transactionType == "credit")
}
