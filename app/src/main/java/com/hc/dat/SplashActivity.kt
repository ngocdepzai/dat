package com.hc.dat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import hc.manager.datapp.R
import hc.manager.datapp.databinding.DatActivitySplashBinding

class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewBinding = DatActivitySplashBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
    }

    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, DatMainActivity::class.java))
            overridePendingTransition(R.anim.fadein, R.anim.fadeout)
            finish()
        }, 1000)
    }
}
