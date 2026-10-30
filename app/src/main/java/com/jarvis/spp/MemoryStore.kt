package com.jarvis.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Random

/**
 * Long-term memory:
 * Saved memories aur pending approvals encrypted SharedPreferences mein rehte hain.
 */
object MemoryStore {

    enum class AddResult {
        SAVED, DUPLICATE, SENSITIVE_PENDING, DISABLED, TOO_SHORT
    }

    class Pending(
        val text: String,
        val kind: String,
        val reason: String,
        val updateId: String? = null
    )

    private const val PREFS = "jarvis_mem2"
    private const val KEY = "entries"
    private const val PENDING_KEY = "pending_entries"
    private const val MAX = 80
    private const val MAX_PENDING = 50

    private var cache: ArrayList<MemoryEntry>? = null
    private var pendingCache: ArrayList<Pending>? = null
    private val rnd = Random()

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun newId(): String =
        java.lang.Long.toString(System.nanoTime(), 36) + rnd.nextInt(1000)

    @Synchronized
    private fun load(ctx: Context): ArrayList<MemoryEntry> {
        cache?.let { return it }

        val out = ArrayList<MemoryEntry>()
        val p = prefs(ctx)
        val stored = p.getString(KEY, null)

        if (stored != null) {
            try {
                val arr = JSONArray(ApiKeyStore.open(stored))
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out.add(
                        MemoryEntry(
                            o.getString("id"),
                            o.getString("t"),
                            o.optString("k", "fact"),
                            o.optLong("c", 0L),
                            o.optBoolean("a", true)
                        )
                    )
                }
            } catch (_: Exception) {
            }
        } else {
            try {
                val legacy = ctx.applicationContext
                    .getSharedPreferences("jarvis_mem", Context.MODE_PRIVATE)
                val s = legacy.getString("facts", null)

                if (s != null) {
                    val arr = JSONArray(s)
                    for (i in 0 until arr.length()) {
                        out.add(
                            MemoryEntry(
                                newId(),
                                arr.getString(i),
                                "fact",
                                System.currentTimeMillis(),
                                true
                            )
                        )
                    }
                    legacy.edit().remove("facts").apply()
                }
            } catch (_: Exception) {
            }
        }

        cache = out

        if (stored == null && out.isNotEmpty()) {
            save(ctx)
        }

        return out
    }

    @Synchronized
    private fun save(ctx: Context) {
        val list = cache ?: return
        val arr = JSONArray()

        for (e in list) {
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("t", e.text)
                    .put("k", e.kind)
                    .put("c", e.createdAt)
                    .put("a", e.auto)
            )
        }

        prefs(ctx).edit()
            .putString(KEY, ApiKeyStore.seal(arr.toString()))
            .apply()
    }

    /**
     * Pending approvals ko encrypted storage se load karta hai.
     */
    @Synchronized
    private fun loadPending(ctx: Context): ArrayList<Pending> {
        pendingCache?.let { return it }

        val out = ArrayList<Pending>()
        val stored = prefs(ctx).getString(PENDING_KEY, null)

        if (stored != null) {
            try {
                val arr = JSONArray(ApiKeyStore.open(stored))

                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)

                    out.add(
                        Pending(
                            o.getString("t"),
                            o.optString("k", "fact"),
                            o.optString("r", ""),
                            if (o.isNull("u")) null else o.optString("u")
                        )
                    )
                }
            } catch (_: Exception) {
                // Corrupt pending data ko memory entries se alag rakha gaya hai.
            }
        }

        pendingCache = out
        return out
    }

    /**
     * Pending approvals ko encrypted form mein save karta hai.
     */
    @Synchronized
    private fun savePending(ctx: Context) {
        val list = pendingCache ?: return
        val arr = JSONArray()

        for (p in list) {
            arr.put(
                JSONObject()
                    .put("t", p.text)
                    .put("k", p.kind)
                    .put("r", p.reason)
                    .put("u", p.updateId)
            )
        }

        prefs(ctx).edit()
            .putString(PENDING_KEY, ApiKeyStore.seal(arr.toString()))
            .apply()
    }

    fun enabled(ctx: Context): Boolean =
        Prefs.memoryEnabled(ctx)

    @Synchronized
    fun list(ctx: Context): List<MemoryEntry> =
        ArrayList(load(ctx))

    @Synchronized
    fun add(
        ctx: Context,
        textIn: String,
        kind: String = "fact",
        auto: Boolean = true,
        force: Boolean = false
    ): AddResult {

        if (!force && !enabled(ctx)) {
            return AddResult.DISABLED
        }

        val t = textIn.trim().trim('"', '\'', '*', '.').take(200)

        if (t.length < 4) {
            return AddResult.TOO_SHORT
        }

        val list = load(ctx)
        val low = t.lowercase()

        if (list.any { it.text.lowercase() == low }) {
            return AddResult.DUPLICATE
        }

        if (!force) {
            val why = SensitiveFilter.reasons(t)

            if (why.isNotEmpty()) {
                val pending = loadPending(ctx)

                if (pending.none {
                        it.text.lowercase() == low && it.updateId == null
                    }) {
                    if (pending.size >= MAX_PENDING) {
                        return AddResult.TOO_SHORT
                    }

                    pending.add(
                        Pending(
                            t,
                            kind,
                            why.joinToString(", ")
                        )
                    )
                    savePending(ctx)
                }

                return AddResult.SENSITIVE_PENDING
            }
        }

        saveEntry(ctx, t, kind, auto)
        return AddResult.SAVED
    }

    /**
     * Normal edit turant save hota hai.
     * Sensitive edit approval ke liye pending mein jata hai.
     */
    @Synchronized
    fun update(
        ctx: Context,
        id: String,
        text: String
    ): AddResult {

        val list = load(ctx)
        val entry = list.firstOrNull { it.id == id }
            ?: return AddResult.TOO_SHORT

        val t = text.trim().take(200)

        if (t.length < 4) {
            return AddResult.TOO_SHORT
        }

        if (list.any {
                it.id != id && it.text.equals(t, ignoreCase = true)
            }) {
            return AddResult.DUPLICATE
        }

        val why = SensitiveFilter.reasons(t)

        if (why.isNotEmpty()) {
            val pending = loadPending(ctx)

            if (pending.none { it.updateId == id }) {
                if (pending.size >= MAX_PENDING) {
                    return AddResult.TOO_SHORT
                }

                pending.add(
                    Pending(
                        t,
                        entry.kind,
                        why.joinToString(", "),
                        updateId = id
                    )
                )
                savePending(ctx)
            }

            return AddResult.SENSITIVE_PENDING
        }

        entry.text = t
        save(ctx)

        return AddResult.SAVED
    }

    private fun saveEntry(
        ctx: Context,
        text: String,
        kind: String,
        auto: Boolean
    ) {
        val list = load(ctx)

        list.add(
            MemoryEntry(
                newId(),
                text,
                kind,
                System.currentTimeMillis(),
                auto
            )
        )

        while (list.size > MAX) {
            val idx = list.indexOfFirst { it.auto }
            list.removeAt(if (idx >= 0) idx else 0)
        }

        save(ctx)
    }

    @Synchronized
    fun remove(ctx: Context, id: String) {
        if (load(ctx).removeAll { it.id == id }) {
            save(ctx)
        }

        val pending = loadPending(ctx)
        if (pending.removeAll { it.updateId == id }) {
            savePending(ctx)
        }
    }

    @Synchronized
    fun clear(ctx: Context) {
        load(ctx).clear()
        loadPending(ctx).clear()

        save(ctx)
        savePending(ctx)
    }

    @Synchronized
    fun pendingList(ctx: Context): List<Pending> =
        ArrayList(loadPending(ctx))

    @Synchronized
    fun approvePending(
        ctx: Context,
        index: Int
    ): AddResult {

        val pending = loadPending(ctx)

        if (index !in pending.indices) {
            return AddResult.TOO_SHORT
        }

        val p = pending[index]

        if (p.updateId != null) {
            val list = load(ctx)
            val existing = list.firstOrNull { it.id == p.updateId }

            if (existing == null) {
                pending.removeAt(index)
                savePending(ctx)
                return AddResult.TOO_SHORT
            }

            if (list.any {
                    it.id != p.updateId &&
                        it.text.equals(p.text, ignoreCase = true)
                }) {
                return AddResult.DUPLICATE
            }

            existing.text = p.text
            save(ctx)

            pending.removeAt(index)
            savePending(ctx)

            return AddResult.SAVED
        }

        val list = load(ctx)

        if (list.any { it.text.equals(p.text, ignoreCase = true) }) {
            pending.removeAt(index)
            savePending(ctx)
            return AddResult.DUPLICATE
        }

        saveEntry(ctx, p.text, p.kind, auto = false)

        pending.removeAt(index)
        savePending(ctx)

        return AddResult.SAVED
    }

    @Synchronized
    fun rejectPending(
        ctx: Context,
        index: Int
    ) {
        val pending = loadPending(ctx)

        if (index in pending.indices) {
            pending.removeAt(index)
            savePending(ctx)
        }
    }

    /**
     * Prompt mein sirf sawaal se milti-julti yaadein jodta hai.
     */
    fun contextLine(ctx: Context, query: String): String {
        if (!enabled(ctx)) return ""

        val related = MemoryRanker.rank(query, list(ctx), 6)

        if (related.isEmpty()) return ""

        val sb = StringBuilder(
            "User ke baare mein saved yaadein (naturally kaam mein lo, har baar mat dohrao): "
        )

        for ((i, e) in related.withIndex()) {
            if (i > 0) sb.append("; ")
            if (e.kind == "project") sb.append("(project) ")
            sb.append(e.text)
        }

        sb.append(". ")
        return sb.toString()
    }

    fun sizeBytes(ctx: Context): Long =
        (prefs(ctx).getString(KEY, "") ?: "").length.toLong()
}
