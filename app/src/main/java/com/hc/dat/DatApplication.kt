package com.hc.dat

import hc.manager.datapp.BuildConfig
import com.hc.dat.di.component.AppComponent
import com.lws.type.LogRecorder
import com.lws.type.Logger
import io.sentry.Sentry
import android.os.Build
import android.provider.Settings
import com.hc.dat.utils.SentryLogUploader
import io.sentry.android.core.SentryAndroid

class DatApplication : BaseApplication() {

    override fun onCreate() {
        super.onCreate()
        Logger.d("onCreate")
        applicationInjector()
        // start write log
        LogRecorder.startWriteLog()
        LogRecorder.d("", "Mở app")
        initSentry()
    }

    private fun applicationInjector() {
        Logger.d("applicationInjector")
        AppComponent.init(this)
    }

    private fun initSentry() {
        SentryAndroid.init(this) { options ->
            options.dsn = "https://0bd5eefffa4561335366f5cc8fe63b70@o4511182184644608.ingest.us.sentry.io/4511182227767296"
            options.tracesSampleRate = 0.2
            options.isAttachThreads = true

//            // Đính kèm file log của LogRecorder khi có crash
//            options.beforeSend = io.sentry.SentryOptions.BeforeSendCallback { event, _ ->
//                if (BuildConfig.DEBUG) return@BeforeSendCallback null
//                attachLogFileToSentry()
//                event
//            }
        }

        // Gắn thông tin thiết bị DAT
        Sentry.setTag("device_id", Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))
        Sentry.setTag("device_model", Build.MODEL)
        Sentry.setTag("android_version", Build.VERSION.RELEASE)
        Sentry.setTag("app_version", BuildConfig.VERSION_NAME)

        Logger.d("Sentry initialized")
    }

    private fun attachLogFileToSentry() {
        try {
            val logFile = LogRecorder.getLogFile()

            if (logFile != null && logFile.exists()) {
                SentryLogUploader.attachLogToEvent(logFile)
            }
        } catch (e: Exception) {
            Logger.d("attachLogFileToSentry error: ${e.message}")
        }
    }
}
