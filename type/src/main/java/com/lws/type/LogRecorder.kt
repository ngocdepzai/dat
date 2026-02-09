package com.lws.type

import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object LogRecorder {

    private var hcLogFolder: File = File(Environment.getExternalStorageDirectory().toString() + "/HC_DAT_LOGS")

    private var logBuffer: StringBuffer = StringBuffer()
    var recordLogFile: File? = null
    private var isWritingLog = false
    fun createLogFile(
        sessionId: String?,
        studentCode: String?,
        versionApp: String,
        startTime: Long?,
        loginStatus: String,
        timeLogin: String? = null,
    ) {

        val timeLoginSuffix = if (timeLogin != null) "_$timeLogin" else ""
        if (!hcLogFolder.exists()) {
            hcLogFolder.mkdirs()
        }
        val dateString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(startTime?:System.currentTimeMillis()))
        val dateFolder = File(hcLogFolder, dateString)
        if (!dateFolder.exists()) {
            dateFolder.mkdirs()
        }
        recordLogFile = File(dateFolder, "${studentCode}_${sessionId}_${versionApp}$timeLoginSuffix$loginStatus.log")
        recordLogFile?.createNewFile()
    }
    fun startWriteLog() {
        logBuffer.delete(0, logBuffer.length)
        Thread {
            kotlin.run {
                isWritingLog = true
                // start write log
                d("START WRITE LOG", "")
                // loop write log 5m
                while (isWritingLog) {
                    Thread.sleep(5*60*1000)
                    saveLog(false)
                }
            }
        }.start()
    }

    fun saveLog(stopWriteLog: Boolean) {
        Thread {
            kotlin.run {
                Thread.sleep(1000)
                if (stopWriteLog) d("FINISH WRITE LOG", "")
                if (isWritingLog) {
                    recordLogFile?.deleteOnExit()
                    recordLogFile?.let {
                        recordLogFile?.appendText(logBuffer.toString())
                        logBuffer.delete(0, logBuffer.length)
                    }
                }
                if (stopWriteLog) isWritingLog = false
            }
        }.start()
    }

    fun i(tag: String, content: String?) {
        logBuffer.append("[${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}]" +
                "[INFO]" +
                "[$tag], $content\n")
    }

    fun e(tag: String, content: String?) {
        logBuffer.append("[${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}]" +
                "[ERROR]" +
                "[$tag], $content\n")
    }

    fun d(tag: String, content: String?) {
        logBuffer.append("[${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}]" +
                "[DO]" +
                "[$tag], $content\n")
    }

    fun w(tag: String, content: String?) {
        logBuffer.append("[${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}]" +
                "[WARNING]" +
                "[$tag], $content\n")
    }

}
