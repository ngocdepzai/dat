package com.hc.dat

import android.app.Application
import com.lws.type.LogRecorder
import com.lws.type.Logger
import io.sentry.Sentry

abstract class BaseApplication : Application() {
    override fun onTerminate() {
        // Upload log lần cuối trước khi app tắt
        try {
            LogRecorder.saveLog(true)
            Logger.d("onTerminate - log saved")
        } catch (e: Exception) {
            Sentry.captureException(e)
        }

        super.onTerminate()
        Logger.d("onTerminate")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Cảnh báo memory thấp lên Sentry
        Sentry.addBreadcrumb("System: onLowMemory triggered")
        Logger.d("onLowMemory")
    }
}
