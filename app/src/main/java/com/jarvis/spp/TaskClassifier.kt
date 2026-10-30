package com.jarvis.app

/**
 * Jarvi Advanced Task Classifier
 *
 * User ke message ko samajhkar decide karta hai
 * ki use kis tarah ka AI processing chahiye.
 */
object TaskClassifier {

    private val splitter =
        Regex("[^\\p{L}\\p{M}\\p{Nd}+#]+")

    fun tokens(text: String): Set<String> =
        text.lowercase()
            .split(splitter)
            .filter { it.isNotBlank() }
            .toSet()

    // Latest information aur search ke liye
    private val searchTokens = setOf(
        "search", "सर्च", "news", "न्यूज",
        "समाचार", "samachar", "mausam", "मौसम",
        "weather", "score", "स्कोर",
        "ढूंढ", "ढूँढ", "dhundo", "dhoondo",
        "latest", "price", "daam", "दाम",
        "current", "currently", "headlines",
        "today", "आज", "अभी", "live",
        "forecast", "rate", "रिजल्ट",
        "result", "नतीजा", "ताजा", "ताज़ा"
    )

    private val searchPhrases = listOf(
        "aaj ka",
        "आज का",
        "aaj ki",
        "आज की",
        "abhi ka",
        "अभी का",
        "stock price",
        "share price",
        "live score",
        "kaun jeeta",
        "who won",
        "who is the current",
        "kitne ka hai",
        "आज मौसम",
        "आज की खबर",
        "latest news",
        "current price",
        "near me"
    )

    // Logical thinking aur reasoning
    private val complexTokens = setOf(
        "kyun", "kyu", "kyon",
        "kaise", "why", "how",
        "explain", "samjhao", "समझाओ",
        "compare", "tulna", "difference",
        "antar", "solve", "calculate",
        "hisab", "derive", "prove",
        "plan", "strategy", "step",
        "logic", "reason", "analyse",
        "analyze", "optimize", "algorithm",
        "pros", "cons", "behtar",
        "better", "decide", "equation",
        "puzzle", "riddle", "probability",
        "calculate", "calculation",
        "reasoning", "analysis",
        "विश्लेषण", "तुलना", "समाधान",
        "क्यों", "कैसे", "समझाइए",
        "फायदा", "नुकसान", "तरीका",
        "कारण", "विकल्प", "निर्णय",
        "साबित", "गणना", "हल"
    )

    // Programming aur coding
    private val codeTokens = setOf(
        "code", "program", "function",
        "python", "java", "kotlin",
        "javascript", "js", "html",
        "css", "sql", "bug", "error",
        "exception", "compile", "script",
        "api", "regex", "class",
        "array", "loop", "c++",
        "debug", "gradle", "android",
        "stacktrace", "programming",
        "developer", "github",
        "repository", "apk",
        "कोड", "प्रोग्राम", "ऐप",
        "एरर", "बिल्ड"
    )

    // Summary, translation aur rewriting
    private val summarizeTokens = setOf(
        "summarize", "summarise",
        "summary", "saransh",
        "सारांश", "rewrite",
        "rephrase", "paraphrase",
        "translate", "anuvad",
        "अनुवाद", "shorten",
        "simplify", "सरल"
    )

    private val summarizePhrases = listOf(
        "short mein",
        "dobara likho",
        "mail likh",
        "email likh",
        "simple words",
        "aasan bhasha"
    )

    // Phone control
    private val phoneTokens = setOf(
        "kholo", "khol", "open",
        "call", "sms", "whatsapp",
        "alarm", "timer", "torch",
        "flashlight", "volume",
        "battery", "photo", "selfie",
        "scroll", "click", "tap",
        "screenshot", "lock",
        "brightness", "wifi",
        "bluetooth", "hotspot",
        "navigate", "maps",
        "gaana", "music", "dabao",
        "chalao", "back", "home",
        "recents", "notification",
        "notifications", "flash",
        "camera", "phone",
        "कॉल", "खोलो", "खोल",
        "चलाओ", "दबाओ", "अलार्म",
        "टॉर्च", "बैटरी",
        "फोटो", "कैमरा",
        "स्क्रीनशॉट", "आवाज",
        "वॉल्यूम", "स्क्रॉल",
        "लॉक", "वाइफाई",
        "ब्लूटूथ"
    )

    // General questions
    private val questionTokens = setOf(
        "who", "what", "when",
        "where", "which",
        "kaun", "kya", "kab",
        "kahan", "kitna", "kitne",
        "kaunsa", "capital",
        "president", "ceo",
        "population", "year",
        "कौन", "क्या", "कब",
        "कहाँ", "कितना", "कितने"
    )

    private fun hasPhrase(
        text: String,
        list: List<String>
    ): Boolean {
        val low = text.lowercase()
        return list.any { low.contains(it) }
    }

    // Search ki zaroorat hai?
    fun needsSearch(text: String): Boolean {
        val tk = tokens(text)

        if (tk.any { it in searchTokens }) {
            return true
        }

        return hasPhrase(text, searchPhrases)
    }

    // Phone command hai?
    fun isPhone(text: String): Boolean {
        if (text.length > 160) return false

        return tokens(text).any {
            it in phoneTokens
        }
    }

    // Coding request hai?
    fun isCode(text: String): Boolean {
        if (text.contains("```")) return true

        val tk = tokens(text)

        if (tk.any { it in codeTokens }) {
            return true
        }

        return text.contains("{") &&
                text.contains(";") &&
                text.contains("(")
    }

    // Summary ya translation request
    fun isSummarize(text: String): Boolean {
        val tk = tokens(text)

        if (tk.any { it in summarizeTokens }) {
            return true
        }

        return hasPhrase(text, summarizePhrases)
    }

    // Mathematical expressions
    private val mathRegex = Regex(
        "\\d+\\s*[+\\-*/x×÷^]\\s*\\d+"
    )

    // Complex question detection
    fun isComplex(text: String): Boolean {

        val clean = text.trim()

        val len = clean.length

        if (len >= 220) return true

        if (mathRegex.containsMatchIn(clean)) {
            return true
        }

        val tk = tokens(clean)

        if (tk.any { it in complexTokens }) {
            return true
        }

        if (clean.count { it == '?' || it == '؟' } >= 2) {
            return true
        }

        if (len >= 100 && tk.size >= 15) {
            return true
        }

        return false
    }

    // Factual question detection
    fun looksFactual(text: String): Boolean {

        val tk = tokens(text)

        if (tk.none { it in questionTokens }) {
            return false
        }

        return text.trim().endsWith("?") ||
                text.trim().endsWith("।") ||
                text.trim().length >= 15
    }

    /**
     * Final decision:
     * User ke message ke hisaab se task select karo.
     */
    fun classify(
        text: String,
        hasImage: Boolean = false
    ): Task {

        if (hasImage) {
            return Task.VISION
        }

        if (isPhone(text)) {
            return Task.PHONE
        }

        if (needsSearch(text)) {
            return Task.SEARCH
        }

        if (isCode(text)) {
            return Task.CODE
        }

        if (isSummarize(text)) {
            return Task.SUMMARIZE
        }

        if (isComplex(text)) {
            return Task.REASONING
        }

        return Task.CHAT
    }
}
