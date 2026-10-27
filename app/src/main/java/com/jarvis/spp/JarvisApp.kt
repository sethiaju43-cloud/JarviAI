package com.jarvis.app

import android.app.Application

class JarvisApp : Application() {
    /** Phone ki memory kam ho to local model hata do (crash se bachne ke liye) */
    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        LocalLlm.onTrim(level)
    }
}
