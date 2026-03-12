package com.koshpal_android.koshpalapp.sms.rules

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

/**
 * Rule-based SMS classification engine.
 * Singleton that loads rules.json once, precompiles regexes and builds sender index.
 */
object RuleEngine {

    private const val TAG = "RuleEngine"

    private val initialized = AtomicBoolean(false)
    private lateinit var config: RuleConfig
    private var blacklistPattern: Pattern? = null

    /**
     * Precompiled rule with its owning bank metadata.
     */
    private data class CompiledRule(
        val bankRule: BankRule,
        val pattern: RulePattern,
        val regex: Pattern
    )

    // Sender (normalized) -> patterns
    private val senderIndex: MutableMap<String, MutableList<CompiledRule>> = mutableMapOf()
    
    // Fallback patterns when sender not found (rules without explicit senders)
    private val globalPatterns: MutableList<CompiledRule> = mutableListOf()

    // All compiled patterns, used as a safety-net fallback when sender mapping fails.
    // This is slightly heavier but ensures we still classify SMS on devices
    // where sender IDs don't exactly match the rules.json sender list.
    private val allPatterns: MutableList<CompiledRule> = mutableListOf()

    private val DATE_FORMATS = mapOf(
        "dd-MMM-yy" to SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH),
        "dd-MMM-yyyy" to SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
        "dd-MMM" to SimpleDateFormat("dd-MMM", Locale.ENGLISH),
        "dd MMM, yy" to SimpleDateFormat("dd MMM, yy", Locale.ENGLISH),
        "dd MMM, yyyy" to SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH),
        "dd/MM/yyyy" to SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        "dd/MM/yy" to SimpleDateFormat("dd/MM/yy", Locale.ENGLISH),
        "dd MM yyyy" to SimpleDateFormat("dd MM yyyy", Locale.ENGLISH),
        "dd MMM yy at HH:mm" to SimpleDateFormat("dd MMM yy 'at' HH:mm", Locale.ENGLISH),
        "dd MMM yyyy at HH:mm" to SimpleDateFormat("dd MMM yyyy 'at' HH:mm", Locale.ENGLISH)
    )

    /**
     * Initialize the rule engine once during app startup.
     * Safe to call multiple times; subsequent calls are no-ops.
     */
    fun initialize(context: Context) {
        if (initialized.get()) return
        synchronized(this) {
            if (initialized.get()) return

            config = RuleParser.loadRules(context)
            blacklistPattern = config.blacklistRegex
                .takeIf { it.isNotBlank() }
                ?.let { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }

            buildIndexes()

            initialized.set(true)
            Log.d(TAG, "✅ RuleEngine initialized with ${config.rules.size} bank rules")
        }
    }

    /**
     * Match SMS against precompiled rules.
     * @return RuleMatchResult if a rule matches, null otherwise.
     */
    fun match(sender: String, smsBody: String, smsTimestamp: Long): RuleMatchResult? {
        if (!initialized.get()) {
            Log.w(TAG, "RuleEngine.match called before initialize()")
            return null
        }

        if (blacklistPattern?.matcher(smsBody)?.matches() == true) return null

        val senderNorm = normalizeSender(sender)

        // Prefer sender-specific patterns; if not found, fall back to:
        // 1) globalPatterns (rules with no explicit senders)
        // 2) allPatterns (last-resort: scan all compiled rules)
        val candidates: List<CompiledRule> = when {
            senderIndex[senderNorm]?.isNotEmpty() == true -> senderIndex[senderNorm]!!
            globalPatterns.isNotEmpty() -> globalPatterns
            else -> allPatterns
        }
        if (candidates.isEmpty()) return null

        var transactionMatch: RuleMatchResult? = null
        var anyMatch: RuleMatchResult? = null

        for (compiled in candidates) {
            val matcher = compiled.regex.matcher(smsBody)
            if (!matcher.find()) continue

            val result = extractFields(
                compiled.pattern,
                matcher,
                smsTimestamp,
                compiled.bankRule.fullName
            ) ?: continue

            if (result.shouldCreateTransaction && transactionMatch == null) {
                transactionMatch = result
            }
            if (anyMatch == null) {
                anyMatch = result
            }
        }

        return transactionMatch ?: anyMatch
    }

    private fun buildIndexes() {
        senderIndex.clear()
        globalPatterns.clear()
        allPatterns.clear()

        for (rule in config.rules) {
            for (pattern in rule.patterns) {
                if (pattern.obsolete) continue

                val compiledRegex = try {
                    Pattern.compile(pattern.regex)
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid regex in pattern ${pattern.patternUid}: ${e.message}")
                    continue
                }

                val compiledRule = CompiledRule(rule, pattern, compiledRegex)

                // Track every compiled rule for all-patterns fallback.
                allPatterns.add(compiledRule)

                // If rule has senders, index by normalized sender; otherwise add to global patterns
                if (rule.senders.isNotEmpty()) {
                    for (sender in rule.senders) {
                        val key = normalizeSender(sender)
                        val list = senderIndex.getOrPut(key) { mutableListOf() }
                        list.add(compiledRule)
                    }
                } else {
                    globalPatterns.add(compiledRule)
                }
            }
        }

        Log.d(
            TAG,
            "Built sender index for ${senderIndex.size} senders, " +
                "${globalPatterns.size} global fallback patterns, " +
                "${allPatterns.size} total compiled patterns"
        )
    }

    private fun normalizeSender(raw: String): String {
        // Strip SMSC prefixes like VM-, VK-, JD-, etc. and uppercase
        val trimmed = raw.trim()
        val parts = trimmed.split("-")
        val lastPart = parts.lastOrNull() ?: trimmed
        return lastPart.uppercase(Locale.ENGLISH)
    }

    private fun extractFields(
        pattern: RulePattern,
        matcher: java.util.regex.Matcher,
        smsTimestamp: Long,
        bankName: String
    ): RuleMatchResult? {
        val df = pattern.dataFields

        // transaction_type / statement_type (constants)
        val transactionType = getStringField(df, "transaction_type")
        val statementType = getStringField(df, "statement_type")
        val transactionCategory = getStringField(df, "transaction_category")

        // amount - from group or create_txn
        var amount = extractAmount(df, matcher)
        if (amount <= 0 && df.containsKey("create_txn")) {
            val createTxn = df["create_txn"]
            if (createTxn is Map<*, *>) {
                amount = extractAmountFromSpec(createTxn as Map<String, Any>, matcher)
            }
        }

        // date
        val date = extractDate(df, matcher, smsTimestamp) ?: smsTimestamp

        // pan
        val pan = extractStringFromSpec(df["pan"], matcher)

        // account_balance
        val accountBalance = extractDoubleFromSpec(df["account_balance"], matcher)

        // pos (merchant)
        val pos = extractPos(df["pos"], matcher)

        // network_reference_id
        val networkRefId = extractStringFromSpec(df["network_reference_id"], matcher)

        return RuleMatchResult(
            bankName = bankName,
            smsType = pattern.smsType,
            transactionType = transactionType,
            statementType = statementType,
            transactionCategory = transactionCategory,
            amount = amount,
            date = date,
            pan = pan?.takeIf { it.isNotBlank() },
            accountBalance = accountBalance,
            pos = pos?.takeIf { it.isNotBlank() } ?: "Unknown",
            networkReferenceId = networkRefId?.takeIf { it.isNotBlank() },
            matchedPatternUid = pattern.patternUid
        )
    }

    private fun getStringField(df: Map<String, Any>, key: String): String? {
        val v = df[key] ?: return null
        return when (v) {
            is String -> v
            else -> null
        }
    }

    private fun extractAmount(df: Map<String, Any>, matcher: java.util.regex.Matcher): Double {
        return extractDoubleFromSpec(df["amount"], matcher)
    }

    private fun extractAmountFromSpec(spec: Map<String, Any>, matcher: java.util.regex.Matcher): Double {
        return extractDoubleFromSpec(spec, matcher)
    }

    private fun extractDoubleFromSpec(spec: Any?, matcher: java.util.regex.Matcher): Double {
        if (spec == null) return 0.0
        when (spec) {
            is Number -> return spec.toDouble()
            is Map<*, *> -> {
                val m = spec as Map<String, Any>
                val value = m["value"]
                if (value != null && value is Number) return value.toDouble()
                val gid = (m["group_id"] as? Number)?.toInt() ?: return 0.0
                if (gid < 0) return 0.0
                val group = matcher.group(gid) ?: return 0.0
                return parseAmount(group)
            }
        }
        return 0.0
    }

    private fun extractStringFromSpec(spec: Any?, matcher: java.util.regex.Matcher): String? {
        if (spec == null) return null
        when (spec) {
            is String -> return spec
            is Map<*, *> -> {
                val m = spec as Map<String, Any>
                val value = m["value"]
                if (value != null) return value.toString()
                val gid = (m["group_id"] as? Number)?.toInt() ?: return null
                if (gid < 0) return null
                return matcher.group(gid)?.trim()
            }
        }
        return null
    }

    private fun extractPos(spec: Any?, matcher: java.util.regex.Matcher): String? {
        if (spec == null) return null
        when (spec) {
            is String -> return spec
            is Map<*, *> -> {
                val m = spec as Map<String, Any>
                val value = m["value"]
                if (value != null) return value.toString()
                val gid = (m["group_id"] as? Number)?.toInt()
                if (gid != null && gid >= 0) {
                    return matcher.group(gid)?.trim()
                }
            }
        }
        return null
    }

    private fun extractDate(df: Map<String, Any>, matcher: java.util.regex.Matcher, smsTimestamp: Long): Long? {
        val dateSpec = df["date"] ?: return null
        if (dateSpec is Map<*, *>) {
            val m = dateSpec as Map<String, Any>
            val formats = m["formats"]
            if (formats is List<*>) {
                for (f in formats) {
                    if (f !is Map<*, *>) continue
                    val fm = f as Map<String, Any>
                    val useSmsTime = fm["use_sms_time"] == true
                    if (useSmsTime) return smsTimestamp
                    val formatStr = (fm["format"] as? String)?.takeIf { it.isNotBlank() && it != "'immediate'" }
                        ?: continue
                    val gid = (m["group_id"] as? Number)?.toInt()
                    if (gid != null && gid >= 0) {
                        val group = matcher.group(gid) ?: continue
                        val parsed = parseDate(group, formatStr)
                        if (parsed != null) return parsed
                    }
                }
            }
            // date: { use_sms_time: true } without formats
            if (m["use_sms_time"] == true) return smsTimestamp
        }
        return null
    }

    private fun parseAmount(s: String): Double {
        val cleaned = s.replace(",", "").replace(" ", "").trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun parseDate(dateStr: String, formatKey: String): Long? {
        val fmt = DATE_FORMATS[formatKey] ?: try {
            SimpleDateFormat(formatKey, Locale.ENGLISH)
        } catch (_: Exception) {
            return null
        }
        return try {
            fmt.parse(dateStr)?.time
        } catch (_: Exception) {
            null
        }
    }
}
