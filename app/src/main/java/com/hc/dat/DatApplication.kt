package com.hc.dat

import com.hc.dat.di.component.AppComponent
import com.lws.type.LogRecorder
import com.lws.type.Logger

class DatApplication : BaseApplication() {

    override fun onCreate() {
        super.onCreate()
        Logger.d("onCreate")
        applicationInjector()
        // start write log
        LogRecorder.startWriteLog()
        LogRecorder.d("", "Mở app")
    }

    private fun applicationInjector() {
        Logger.d("applicationInjector")
        AppComponent.init(this)
    }
}
