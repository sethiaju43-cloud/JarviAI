package com.jarvis.app

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog

/** Online AI providers (Gemini zaroori nahi), API keys, model chunav, availability check, search tools */
class ProvidersActivity : ScreenActivity() {

    override fun screenTitle() = "Online AI Providers"

    override fun build() {
        val c = this
        val online = NetworkMonitor.isOnline(c)
        container.addView(UiKit.text(c, if (online) "🟢 Online" else "🔴 Offline: provider check/search abhi nahi chalenge", 13f, dim = true))
        container.addView(UiKit.text(c,
            "Koi bhi ek provider kaafi hai. Key phone ke secure storage mein encrypted rehti hai, kabhi code ya logs mein nahi. Free limit provider ke rules par hai, unlimited ki guarantee nahi.",
            12f, dim = true))
        for (p in ProviderRegistry.presets) container.addView(providerCard(p, online))

        container.addView(UiKit.section(c, "🔎 Internet Search tools"))
        container.addView(UiKit.text(c,
            "Gemini ka search built-in hai. Doosre providers ke liye yahan koi search tool chuno. Tavily/Brave ke liye apni key chahiye (free plan mil sakta hai, provider ke rules par). Wikipedia bina key chalta hai par sirf encyclopedia jaankari deta hai, news/live nahi.",
            12f, dim = true))
        val names = listOf("auto" to "Auto", "tavily" to "Tavily", "brave" to "Brave", "wikipedia" to "Wikipedia (bina key)", "off" to "Band")
        val cur = names.firstOrNull { it.first == Prefs.searchProvider(c) }?.second ?: "Auto"
        container.addView(UiKit.button(c, "Search tool: $cur") {
            AlertDialog.Builder(c).setTitle("Search tool")
                .setItems(names.map { it.second }.toTypedArray()) { _, i ->
                    Prefs.setStr(c, "search_provider", names[i].first); rebuild()
                }.show()
        })
        for ((id, label, url) in listOf(
            Triple("tavily", "Tavily API key", "https://app.tavily.com"),
            Triple("brave", "Brave Search API key", "https://brave.com/search/api/")
        )) {
            val has = ApiKeyStore.hasSecret(c, "search_$id")
            container.addView(UiKit.text(c, label + (if (has) "  ✅ saved" else "  (nahi hai)"), 13f))
            val e = UiKit.input(c, if (has) "naya key daalo to badal jayegi" else "key", password = true)
            container.addView(e)
            val r = UiKit.row(c)
            r.addView(UiKit.rowButton(c, "Save") {
                if (e.text.isNotBlank()) { ApiKeyStore.setSecret(c, "search_$id", e.text.toString()); UiKit.toast(c, "Save ho gaya ✅"); rebuild() }
            })
            r.addView(UiKit.rowButton(c, "Hatao") { ApiKeyStore.setSecret(c, "search_$id", ""); rebuild() })
            r.addView(UiKit.rowButton(c, "Key kahan?") { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) })
            container.addView(r)
        }
    }

    private fun providerCard(p: ProviderPreset, online: Boolean): android.view.View {
        val c = this
        val card = UiKit.card(c)
        card.addView(UiKit.text(c, p.name, 16f, true))
        card.addView(UiKit.text(c, p.freeNote, 12f, dim = true))

        card.addView(UiKit.switchRow(c, "Use karo", ProviderRegistry.enabled(c, p.id)) { ProviderRegistry.setEnabled(c, p.id, it) })

        var baseEdit: android.widget.EditText? = null
        if (p.custom) {
            card.addView(UiKit.text(c, "Base URL (https://... /v1 tak)", 12f, dim = true))
            baseEdit = UiKit.input(c, "https://mera-server.com/v1", ProviderRegistry.baseUrl(c, p.id))
            card.addView(baseEdit)
        }
        val hasKey = ProviderRegistry.key(c, p.id).isNotBlank()
        card.addView(UiKit.text(c, "API key: " + (if (hasKey) "✅ saved" else if (p.custom) "optional (backend ke paas ho to)" else "❌ nahi hai"), 13f))
        val keyEdit = UiKit.input(c, if (hasKey) "naya key daalo to badal jayegi" else "API key", password = true)
        card.addView(keyEdit)
        card.addView(UiKit.text(c, "Model", 12f, dim = true))
        val modelEdit = UiKit.input(c, "model ka naam", ProviderRegistry.model(c, p.id))
        card.addView(modelEdit)
        if (p.kind != ProviderKind.GEMINI) {
            card.addView(UiKit.switchRow(c, "Is model mein photo samajhne (vision) ki shakti hai", ProviderRegistry.vision(c, p.id)) {
                ProviderRegistry.setVision(c, p.id, it)
            })
        }

        val status = ProviderRegistry.lastCheck[p.id] ?: ""
        val cool = ModelRouter.health.remainingMs(p.id)
        val sb = StringBuilder()
        sb.append(if (ProviderRegistry.isConfigured(c, p.id)) "✅ Router ke liye available" else "⛔ Available nahi (key/model/URL poora karo)")
        if (status.isNotBlank()) sb.append("\n").append(status)
        if (cool > 0) sb.append("\n⏳ Abhi rest par: ").append(ModelRouter.health.reasonOf(p.id)).append(" (").append(cool / 60000 + 1).append(" min)")
        card.addView(UiKit.text(c, sb.toString(), 12f))

        fun saveAll() {
            if (keyEdit.text.isNotBlank()) ProviderRegistry.setKey(c, p.id, keyEdit.text.toString())
            if (modelEdit.text.isNotBlank()) ProviderRegistry.setModel(c, p.id, modelEdit.text.toString())
            val be = baseEdit
            if (be != null) {
                val u = be.text.toString().trim()
                if (u.isNotEmpty() && !Net.isHttps(u)) UiKit.toast(c, "Sirf https:// URL allowed hai") else ProviderRegistry.setBaseUrl(c, p.id, u)
            }
        }

        val r = UiKit.row(c)
        r.addView(UiKit.rowButton(c, "💾 Save") { saveAll(); UiKit.toast(c, "Save ho gaya ✅"); rebuild() })
        r.addView(UiKit.rowButton(c, if (online) "🔍 Check + models" else "Offline") {
            if (!online) { UiKit.toast(c, "Internet nahi hai"); return@rowButton }
            saveAll()
            UiKit.toast(c, "Check ho raha hai...")
            Thread {
                val res = ProviderRegistry.check(c, p.id)
                runOnUiThread {
                    if (res.ok && res.models.isNotEmpty()) pickModel(p, res.models) else {
                        AlertDialog.Builder(c).setTitle(p.name).setMessage((if (res.ok) "Key theek hai par models ki list khali aayi." else "❌ ") + res.message)
                            .setPositiveButton("Theek hai", null).show()
                        rebuild()
                    }
                }
            }.start()
        })
        card.addView(r)
        if (hasKey) card.addView(UiKit.button(c, "🗑 Key hatao") { ProviderRegistry.setKey(c, p.id, ""); rebuild() })
        if (p.keyUrl.isNotBlank()) card.addView(UiKit.button(c, "🔑 Key kahan milegi") { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.keyUrl))) })
        return card
    }

    private fun pickModel(p: ProviderPreset, models: List<String>) {
        val c = this
        val list = models.take(200)
        AlertDialog.Builder(c).setTitle(p.name + ": " + models.size + " models mile (chuno)")
            .setItems(list.toTypedArray()) { _, i ->
                ProviderRegistry.setModel(c, p.id, list[i]); rebuild()
            }.setNegativeButton("Band", null).setOnDismissListener { rebuild() }.show()
    }
}
