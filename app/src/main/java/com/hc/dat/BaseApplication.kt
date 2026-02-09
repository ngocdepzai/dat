package com.hc.dat

import android.app.Application
import com.lws.type.LogRecorder
import com.lws.type.Logger

abstract class BaseApplication : Application() {
    override fun onTerminate() {
        LogRecorder.saveLog(true)
        super.onTerminate()
        Logger.d("onTerminate")
    }
}
