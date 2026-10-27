package com.jarvis.app

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.app.databinding.ActivityScreenBinding

/** Settings jaisi sab screens ka common dhaancha: upar title + back, neeche scroll hone wali list */
abstract class ScreenActivity : AppCompatActivity() {

    protected lateinit var container: LinearLayout

    protected abstract fun screenTitle(): String
    protected abstract fun build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityScreenBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.tvTitle.text = screenTitle()
        b.btnBack.setOnClickListener { finish() }
        container = b.container
        build()
    }

    protected fun rebuild() {
        container.removeAllViews()
        build()
    }

    override fun onResume() {
        super.onResume()
        if (::container.isInitialized && container.childCount == 0) build()
    }
}
