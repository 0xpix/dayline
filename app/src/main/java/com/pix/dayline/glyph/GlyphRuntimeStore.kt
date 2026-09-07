package com.pix.dayline.glyph

import android.content.Context
import com.pix.dayline.model.DaylineGlyphSignal
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class QueuedGlyphSignal(
    val id: String,
    val signal: DaylineGlyphSignal,
    val createdAt: Long,
    val expiresAt: Long
)

/** Small persistent queue so app actions can briefly interrupt the AOD eye loop. */
class GlyphRuntimeStore(context: Context) {
    private val prefs = context.getSharedPreferences("dayline_glyph_runtime", Context.MODE_PRIVATE)

    @Synchronized
    fun enqueue(signal: DaylineGlyphSignal, durationMillis: Long) {
        if (signal.priority <= 0) return
        val now = System.currentTimeMillis()
        val queue = loadRaw(now).toMutableList()
        queue += QueuedGlyphSignal(
            id = UUID.randomUUID().toString(),
            signal = signal,
            createdAt = now,
            expiresAt = now + durationMillis.coerceIn(500L, 30_000L)
        )
        saveRaw(queue.sortedBy { it.createdAt }.takeLast(12))
    }

    @Synchronized
    fun current(now: Long = System.currentTimeMillis()): DaylineGlyphSignal? {
        val queue = loadRaw(now)
        saveRaw(queue)
        return queue
            .sortedWith(compareByDescending<QueuedGlyphSignal> { it.signal.priority }.thenBy { it.createdAt })
            .firstOrNull()
            ?.signal
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_QUEUE).apply()
    }

    private fun loadRaw(now: Long): List<QueuedGlyphSignal> {
        val raw = prefs.getString(KEY_QUEUE, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val json = array.getJSONObject(i)
                    val signal = runCatching {
                        DaylineGlyphSignal.valueOf(json.getString("signal"))
                    }.getOrNull() ?: continue
                    val item = QueuedGlyphSignal(
                        id = json.optString("id"),
                        signal = signal,
                        createdAt = json.optLong("createdAt", 0L),
                        expiresAt = json.optLong("expiresAt", 0L)
                    )
                    if (item.expiresAt > now) add(item)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveRaw(queue: List<QueuedGlyphSignal>) {
        if (queue.isEmpty()) {
            prefs.edit().remove(KEY_QUEUE).apply()
            return
        }
        val array = JSONArray()
        queue.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("signal", it.signal.name)
                    .put("createdAt", it.createdAt)
                    .put("expiresAt", it.expiresAt)
            )
        }
        prefs.edit().putString(KEY_QUEUE, array.toString()).apply()
    }

    companion object {
        private const val KEY_QUEUE = "queue"
    }
}
