package com.jarvis.app

import android.content.Context
import android.util.Log

/** Local model ko router ke liye backend banata hai */
class LocalBackend(private val ctx: Context, private val m: LocalModel) : Backend {
    override val id: String get() = "local:" + m.id
    override val label: String get() = "📱 " + m.name.substringBefore(" (") + " (offline)"
    override val isLocal: Boolean get() = true
    override val supportsVision: Boolean get() = false
    override val supportsSearch: Boolean get() = false
    override val capability: Int get() = ModelRank.score(m.id)

    override fun call(req: RouterRequest): Outcome {
        if (!LocalLlm.isLoaded() || LocalLlm.loadedModelId() != m.id) {
            ModelRouter.onStatus?.invoke("Local model load ho raha hai (pehli baar kuch second lagte hain)...")
        }
        val user = req.history.lastOrNull()?.second ?: ""
        if (user.isBlank()) return Outcome(false, "Sawaal khali", ErrKind.BAD_REQUEST)
        // Phone ke aam commands (torch, volume, call, alarm...) chhota model nahi samajhta:
        // rules se seedha ACTION line bana do, model ko load karne ki zaroorat nahi.
        val ruleCmd = OfflineCommands.parse(user)
        if (ruleCmd != null) return Outcome(true, ruleCmd)
        val older = req.history.dropLast(1)
        val hist = ContextTrimmer.trim(older, 1200, 12)
        var r = LocalLlm.generate(ctx, m, req.systemLocal, hist, user, ModelRouter.onPartial)
        if (!r.ok && hist.isNotEmpty() &&
            (r.error.contains("token", true) || r.error.contains("length", true) || r.error.contains("exceed", true))
        ) {
            r = LocalLlm.generate(ctx, m, req.systemLocal, emptyList(), user, ModelRouter.onPartial)
        }
        return if (r.ok) Outcome(true, r.text) else Outcome(false, r.error, ErrKind.OTHER)
    }
}

/** Kaun sa model chalega ye tay karta hai: online/local, fallback, retry, status */
object ModelRouter {

    val health = HealthTracker()
    private val core = RouterCore(health)

    @Volatile
    var lastLabel: String = ""

    /** UI streaming/status ke liye (optional) */
    @Volatile
    var onPartial: ((String) -> Unit)? = null

    @Volatile
    var onStatus: ((String) -> Unit)? = null

    const val LIMIT_MSG =
        "Free limit abhi khatam ho gaya 🥺 Thodi der baad try karo, ya Settings mein doosra provider ya local model jodo."

    fun onlineBackends(ctx: Context): List<Backend> =
        ProviderRegistry.configured(ctx).map { OnlineBackend(ctx, it) }

    fun localBackend(ctx: Context): Backend? =
        LocalLlm.activeModel(ctx)?.let { LocalBackend(ctx, it) }

    fun avoidLocal(ctx: Context): Boolean =
        Prefs.batterySaver(ctx) && ModelFit.shouldAvoidLocal(DeviceInfo.snapshot(ctx))

    fun plan(ctx: Context, task: Task, needsVision: Boolean): List<Backend> =
        RoutePlanner.order(
            task, Prefs.routeMode(ctx), NetworkMonitor.isOnline(ctx),
            onlineBackends(ctx), localBackend(ctx), avoidLocal(ctx), needsVision
        )

    private fun emptyPlanMessage(ctx: Context, needsVision: Boolean): String {
        val online = NetworkMonitor.isOnline(ctx)
        val hasProviders = ProviderRegistry.configured(ctx).isNotEmpty()
        val hasLocal = LocalLlm.activeModel(ctx) != null
        val mode = Prefs.routeMode(ctx)
        return when {
            needsVision -> "Photo/screen samajhne ke liye online vision model (jaise Gemini) aur internet chahiye. Local model abhi photo nahi padhta."
            mode == RouteModes.LOCAL_ONLY && !hasLocal -> "Mode 'Sirf local' hai par koi local model installed nahi. Settings → Local AI models se download karo."
            mode == RouteModes.ONLINE && !online -> "Mode 'Sirf online' hai par internet nahi hai."
            !online && !hasLocal && hasProviders -> "Internet nahi hai aur local model installed nahi hai. Settings → Local AI models se ek model download karo (WiFi par), phir offline bhi chalega."
            !hasProviders && !hasLocal -> "Koi AI model set nahi hai. Settings → Online AI providers mein API key daalo, ya Local AI models se model download karo."
            else -> "Abhi koi model available nahi hai."
        }
    }

    fun complete(ctx: Context, req: RouterRequest, stop: () -> Boolean = { false }): RouteResult {
        val order = plan(ctx, req.task, req.imageB64 != null)
        if (order.isEmpty()) {
            return RouteResult(
                false, "", null, emptyList(), ErrKind.NONE,
                emptyPlanMessage(ctx, req.imageB64 != null), emptyList(), false, "", emptyList()
            )
        }
        val res = core.run(order, req, 2, stop)
        if (res.ok && res.backend != null) {
            lastLabel = res.backend.label
            return res
        }
        val msg = when (res.failKind) {
            ErrKind.RATE_LIMIT, ErrKind.QUOTA -> LIMIT_MSG
            ErrKind.AUTH -> "API key galat ya expire lag rahi hai. Settings → Online AI providers mein 'Check' dabao."
            ErrKind.NETWORK -> "Internet ya API mein dikkat hai. Thodi der baad try karo."
            ErrKind.NONE -> {
                if (res.notes.isNotEmpty()) res.notes.joinToString(". ") + ". Thodi der baad try karo."
                else "Jawab nahi aaya."
            }
            ErrKind.SERVER -> "AI provider ka server abhi dikkat mein hai 😕 Thodi der baad dobara try karo."
            ErrKind.EMPTY -> "Model ne khali jawab diya 😕 Apna sawaal dobara poochho."
            else -> friendlyOther(res.failMsg)
        }
        // Developer ke liye asli wajah (key/token chhupakar), user ko technical text nahi dikhta
        Log.w("JarviRouter", "fail kind=" + res.failKind + " tried=" + res.failedBefore + " msg=" +
            OnlineParsers.redact(res.failMsg).take(200))
        return RouteResult(
            false, "", null, res.failedBefore, res.failKind, msg,
            emptyList(), false, "", res.notes
        )
    }

    /**
     * Local model ke error Hinglish mein aur kaam ke hote hain (RAM, download, load), unhe dikhao.
     * Online provider ka angrezi/technical error user ko mat dikhao, bolkar bhi nahi sunana.
     */
    private fun friendlyOther(raw: String): String {
        val m = raw.trim()
        val localStyle = m.startsWith("Local model") || m.startsWith("Model") ||
            m.startsWith("RAM") || m.startsWith("Memory")
        return if (localStyle && m.isNotEmpty()) m.take(160)
        else "AI se abhi jawab nahi mil paya 😕 Thodi der baad dobara try karo. Baar baar aaye to Online AI providers mein 'Check' dabao."
    }

    /** Header mein dikhane ke liye: abhi kaun sa model chalega */
    fun previewLabel(ctx: Context, task: Task = Task.CHAT): String {
        val first = plan(ctx, task, false).firstOrNull { !health.inCooldown(it.id) }
        return first?.label ?: "Koi model set nahi"
    }

    fun statusLine(ctx: Context): String {
        val online = NetworkMonitor.isOnline(ctx)
        val sb = StringBuilder(if (online) "🟢 Online" else "🔴 Offline")
        sb.append(" · ").append(previewLabel(ctx))
        return sb.toString()
    }
}
