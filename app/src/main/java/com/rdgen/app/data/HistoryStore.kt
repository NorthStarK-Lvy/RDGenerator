package com.rdgen.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 一条生成记录。 */
data class HistoryEntry(
    val timestamp: Long,
    val min: Long,
    val max: Long,
    val count: Int,
    val distinct: Boolean,
    val ascending: Boolean = false,
    val values: List<Long>,
)

/**
 * 历史记录落盘：SharedPreferences 里存一小段 JSON。
 * 只保留最近 [MAX_ENTRIES] 条，读坏了就当空列表处理，不 crash。
 */
class HistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): List<HistoryEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val values = obj.optJSONArray("values") ?: continue
                    add(
                        HistoryEntry(
                            timestamp = obj.optLong("time"),
                            min = obj.optLong("min"),
                            max = obj.optLong("max"),
                            count = obj.optInt("count"),
                            distinct = obj.optBoolean("distinct"),
                            ascending = obj.optBoolean("ascending"),
                            values = buildList {
                                for (j in 0 until values.length()) add(values.optLong(j))
                            },
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun add(entry: HistoryEntry) {
        val next = buildList {
            add(entry)
            addAll(load())
        }.take(MAX_ENTRIES)
        prefs.edit().putString(KEY_ENTRIES, encode(next)).apply()
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_ENTRIES).apply()
    }

    private fun encode(entries: List<HistoryEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("time", entry.timestamp)
                    put("min", entry.min)
                    put("max", entry.max)
                    put("count", entry.count)
                    put("distinct", entry.distinct)
                    put("ascending", entry.ascending)
                    put("values", JSONArray().apply { entry.values.forEach { put(it) } })
                }
            )
        }
        return array.toString()
    }

    companion object {
        private const val PREFS_NAME = "rdgenerator_history"
        private const val KEY_ENTRIES = "entries"
        const val MAX_ENTRIES = 30
    }
}
