package com.koshpal_android.koshpalapp.sms.rules

import org.json.JSONObject

/**
 * Data models for rules.json structure.
 * Supports parsing the full schema including patterns, data_fields, and date formats.
 */
data class RuleConfig(
    val blacklistRegex: String,
    val minAppVersion: String,
    val rules: List<BankRule>
)

data class BankRule(
    val fullName: String,
    val name: String,
    val senders: List<String>,
    val patterns: List<RulePattern>,
    val senderUid: String? = null,
    val setAccountAsExpense: Boolean? = null
)

data class RulePattern(
    val regex: String,
    val smsType: String,
    val dataFields: Map<String, Any>,
    val accountType: String? = null,
    val patternUid: String? = null,
    val sortUid: String? = null,
    val obsolete: Boolean = false
)

/**
 * Field spec for group_id or value extraction.
 */
data class FieldSpec(
    val groupId: Int? = null,
    val value: Any? = null,
    val setNoPos: Boolean? = null
)

/**
 * Date format spec for parsing.
 */
data class DateFormatSpec(
    val useSmsTime: Boolean,
    val format: String
)

object RuleModelsParser {

    fun parseRuleConfig(json: String): RuleConfig {
        val root = JSONObject(json)
        val blacklistRegex = root.optString("blacklist_regex", "")
        val minAppVersion = root.optString("min_app_version", "0")
        val rulesArray = root.getJSONArray("rules")
        val rules = mutableListOf<BankRule>()

        for (i in 0 until rulesArray.length()) {
            val ruleObj = rulesArray.getJSONObject(i)
            val fullName = ruleObj.optString("full_name", ruleObj.optString("name", ""))
            val name = ruleObj.optString("name", fullName)
            val senderUid = ruleObj.optString("sender_UID", null).takeIf { it.isNotEmpty() }
            val setAccountAsExpense = if (ruleObj.has("set_account_as_expense")) ruleObj.getBoolean("set_account_as_expense") else null

            val sendersList = mutableListOf<String>()
            if (ruleObj.has("senders")) {
                val sendersArr = ruleObj.getJSONArray("senders")
                for (j in 0 until sendersArr.length()) {
                    sendersList.add(sendersArr.getString(j))
                }
            }

            val patternsList = mutableListOf<RulePattern>()
            if (ruleObj.has("patterns")) {
                val patternsArr = ruleObj.getJSONArray("patterns")
                for (j in 0 until patternsArr.length()) {
                    val p = patternsArr.getJSONObject(j)
                    val regex = p.optString("regex", "")
                    if (regex.isEmpty()) continue

                    val smsType = p.optString("sms_type", "transaction")
                    val obsolete = p.optBoolean("obsolete", false)
                    val dataFields = parseDataFields(p.optJSONObject("data_fields"))

                    patternsList.add(
                        RulePattern(
                            regex = regex,
                            smsType = smsType,
                            dataFields = dataFields,
                            accountType = p.optString("account_type", null).takeIf { it.isNotEmpty() },
                            patternUid = p.optString("pattern_UID", null).takeIf { it.isNotEmpty() },
                            sortUid = p.optString("sort_UID", null).takeIf { it.isNotEmpty() },
                            obsolete = obsolete
                        )
                    )
                }
            }

            rules.add(
                BankRule(
                    fullName = fullName,
                    name = name,
                    senders = sendersList,
                    patterns = patternsList,
                    senderUid = senderUid,
                    setAccountAsExpense = setAccountAsExpense
                )
            )
        }

        return RuleConfig(
            blacklistRegex = blacklistRegex,
            minAppVersion = minAppVersion,
            rules = rules
        )
    }

    private fun parseDataFields(obj: JSONObject?): Map<String, Any> {
        if (obj == null) return emptyMap()
        return jsonObjectToMap(obj)
    }

    private fun jsonObjectToMap(obj: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.get(key)
            map[key] = when (value) {
                is JSONObject -> jsonObjectToMap(value)
                is org.json.JSONArray -> jsonArrayToList(value)
                else -> value
            }
        }
        return map
    }

    private fun jsonArrayToList(arr: org.json.JSONArray): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until arr.length()) {
            val v = arr.get(i)
            list.add(when (v) {
                is JSONObject -> jsonObjectToMap(v)
                is org.json.JSONArray -> jsonArrayToList(v)
                else -> v
            })
        }
        return list
    }
}
