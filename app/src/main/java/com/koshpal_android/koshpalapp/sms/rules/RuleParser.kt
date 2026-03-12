package com.koshpal_android.koshpalapp.sms.rules

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.InputStreamReader

/**
 * Loads and parses rules.json from assets.
 * Caches the parsed config in memory.
 */
object RuleParser {

    private const val TAG = "RuleParser"
    private const val RULES_ASSET = "rules.json"

    @Volatile
    private var cachedConfig: RuleConfig? = null

    /**
     * Load rules.json from assets and parse into RuleConfig.
     * Cached after first load.
     */
    fun loadRules(context: Context): RuleConfig {
        return cachedConfig ?: synchronized(this) {
            cachedConfig ?: run {
                val config = parseFromAssets(context)
                cachedConfig = config
                Log.d(TAG, "Loaded ${config.rules.size} bank rules from $RULES_ASSET")
                config
            }
        }
    }

    /**
     * Clear cache (e.g. for testing or hot-reload).
     */
    fun clearCache() {
        synchronized(this) {
            cachedConfig = null
        }
    }

    private fun parseFromAssets(context: Context): RuleConfig {
        val inputStream = context.assets.open(RULES_ASSET)
        val json = InputStreamReader(inputStream).use { it.readText() }
        return RuleModelsParser.parseRuleConfig(json)
    }
}
