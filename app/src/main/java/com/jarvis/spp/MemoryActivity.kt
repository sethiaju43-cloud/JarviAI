package com.jarvis.app

import androidx.appcompat.app.AlertDialog

/** Jarvi ne kya yaad rakha hai: dekho, badlo, hatao. Sensitive baatein yahan 'Allow' maangti hain. */
class MemoryActivity : ScreenActivity() {

    override fun screenTitle() = "Memory"

    override fun build() {
        val c = this
        container.addView(UiKit.switchRow(c, "Long-term memory chalu", Prefs.memoryEnabled(c)) { Prefs.setBool(c, "memory_enabled", it) })
        container.addView(UiKit.text(c,
            "Ye encrypted hokar sirf aapke phone mein rehti hai. Password, OTP, card, phone number jaisi nijji baatein bina aapki 'Allow' ke kabhi save nahi hotin.",
            12f, dim = true))

        val pend = MemoryStore.pendingList()
        if (pend.isNotEmpty()) {
            container.addView(UiKit.section(c, "🔒 Aapki permission chahiye"))
            for ((i, p) in pend.withIndex()) {
                val card = UiKit.card(c)
                card.addView(UiKit.text(c, p.text, 14f))
                card.addView(UiKit.text(c, "Sensitive lagta hai: " + p.reason, 12f, dim = true))
                val r = UiKit.row(c)
                r.addView(UiKit.rowButton(c, "✅ Allow") {
                    when (MemoryStore.approvePending(c, i)) {
                        MemoryStore.AddResult.SAVED -> UiKit.toast(c, "Permission mil gayi; yaad save/update ho gayi 📝")
                        MemoryStore.AddResult.DUPLICATE -> UiKit.toast(c, "Aisi yaad pehle se maujood hai")
                        else -> UiKit.toast(c, "Yaad save nahi ho paayi")
                    }
                    rebuild()
                })
                r.addView(UiKit.rowButton(c, "❌ Mat rakho") { MemoryStore.rejectPending(i); rebuild() })
                card.addView(r)
                container.addView(card)
            }
        }

        container.addView(UiKit.section(c, "Saved yaadein"))
        val list = MemoryStore.list(c).sortedByDescending { it.createdAt }
        if (list.isEmpty()) container.addView(UiKit.text(c, "Abhi kuch save nahi hai. Bolo: 'yaad rakho ki ...'", 13f, dim = true))
        for (e in list) {
            val card = UiKit.card(c)
            val tag = when (e.kind) { "pref" -> "⭐ Pasand"; "project" -> "📁 Project"; "summary" -> "🧠 Saar"; else -> "📝 Baat" }
            card.addView(UiKit.text(c, tag + (if (e.auto) " · auto" else ""), 11f, dim = true))
            card.addView(UiKit.text(c, e.text, 14f))
            val r = UiKit.row(c)
            r.addView(UiKit.rowButton(c, "✏️ Badlo") { editDialog(e) })
            r.addView(UiKit.rowButton(c, "🗑 Hatao") { MemoryStore.remove(c, e.id); rebuild() })
            card.addView(r)
            container.addView(card)
        }
        container.addView(UiKit.button(c, "➕ Nayi yaad jodo") { addDialog() })
        if (list.isNotEmpty()) {
            container.addView(UiKit.button(c, "🗑 Sab yaadein hatao") {
                AlertDialog.Builder(c).setTitle("Sab hatana hai?").setPositiveButton("Haan") { _, _ ->
                    MemoryStore.clear(c); rebuild()
                }.setNegativeButton("Nahi", null).show()
            })
        }
    }

    private fun editDialog(e: MemoryEntry) {
        val c = this
        val et = android.widget.EditText(c)
        et.setText(e.text)
        AlertDialog.Builder(c).setTitle("Badlo").setView(et)
            .setPositiveButton("Save") { _, _ ->
                when (MemoryStore.update(c, e.id, et.text.toString())) {
                    MemoryStore.AddResult.SAVED -> UiKit.toast(c, "Yaad update ho gayi 📝")
                    MemoryStore.AddResult.SENSITIVE_PENDING -> UiKit.toast(c, "Ye sensitive lagti hai. Allow karne ke baad hi update hogi 🔒")
                    MemoryStore.AddResult.DUPLICATE -> UiKit.toast(c, "Aisi yaad pehle se maujood hai")
                    MemoryStore.AddResult.TOO_SHORT -> UiKit.toast(c, "Thoda aur likho")
                    MemoryStore.AddResult.DISABLED -> UiKit.toast(c, "Memory band hai")
                }
                rebuild()
            }
            .setNegativeButton("Nahi", null).show()
    }

    private fun addDialog() {
        val c = this
        val et = android.widget.EditText(c)
        et.hint = "Jaise: Mujhe chhote jawab pasand hain"
        AlertDialog.Builder(c).setTitle("Nayi yaad").setView(et)
            .setPositiveButton("Save") { _, _ ->
                // Nayi yaad par sensitive filter chalega; zarurat par Allow maanga jayega.
                val t = et.text.toString()
                when (MemoryStore.add(c, t, "fact", auto = false)) {
                    MemoryStore.AddResult.SAVED -> UiKit.toast(c, "Yaad save ho gayi 📝")
                    MemoryStore.AddResult.SENSITIVE_PENDING -> UiKit.toast(c, "Sensitive lagti hai. Allow karne ke baad hi save hogi 🔒")
                    MemoryStore.AddResult.DUPLICATE -> UiKit.toast(c, "Ye yaad pehle se saved hai")
                    MemoryStore.AddResult.TOO_SHORT -> UiKit.toast(c, "Thoda aur likho")
                    MemoryStore.AddResult.DISABLED -> UiKit.toast(c, "Memory band hai")
                }
                rebuild()
            }.setNegativeButton("Nahi", null).show()
    }
}
