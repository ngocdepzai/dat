package com.lws.type

import android.os.Environment
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Logger {

    private var DEBUG = true
//    private var hcLogFolder: File = File(Environment.getExternalStorageDirectory().toString() + "/HC_DAT_LOGS")

    private const val TRACE_METHOD = "trace"

    private const val START_LOG_METHOD = "startLogMethod"
    private const val END_LOG_METHOD = "endLogMethod"

    private const val CLASS_NAME_INDEX = 0

    private const val METHOD_NAME_INDEX = 1

    /**
     * set Debug Mode for all module.
     *
     * @param debugMode Boolean input value of debugMode.
     */
    fun setDebugMode(debugMode: Boolean) {
        DEBUG = debugMode
    }

    private var userId: String = "unknown"
//    private var logBuffer: StringBuffer = StringBuffer()
//    private var fileLog: File? = null
//    private var isWritingLog = false
//    fun startWriteLog(userId: String) {
//        this.userId = userId
//        logBuffer.delete(0, logBuffer.length)
//        Thread {
//            kotlin.run {
//                if (!hcLogFolder.exists()) {
//                    hcLogFolder.mkdirs()
//                }
//                val studentFolder = File(hcLogFolder, userId)
//                if (!studentFolder.exists()) {
//                    studentFolder.mkdirs()
//                }
//                val dateString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
//                fileLog = File(studentFolder, "${userId}_$dateString.log")
//                if (fileLog?.exists() == true) {
//                    // Read file
//                    val bufferedReader: BufferedReader = File(fileLog!!.path).bufferedReader()
//                    val inputString = bufferedReader.use { it.readText() }
//                    logBuffer.insert(0, inputString)
//                } else {
//                    fileLog?.createNewFile()
//                }
//                isWritingLog = true
//                // loop write log 10m
//                while (isWritingLog) {
//                    Thread.sleep(10*60*1000)
//                    saveLog(false)
//                }
//            }
//        }.start()
//    }
//
//    fun saveLog(stopWriteLog: Boolean) {
//        Thread {
//            kotlin.run {
//                if (isWritingLog) {
//                    fileLog?.deleteOnExit()
//                    fileLog?.writeText(logBuffer.toString())
//                }
//                if (stopWriteLog) isWritingLog = false
//            }
//        }.start()
//    }

    /**
     * Send an information log message.
     *
     * @param content The message you would like logged.
     */
    fun i(content: String) {
        if (DEBUG) {
            val msg = trace()
            if (msg != null) {
                i(
                    msg[CLASS_NAME_INDEX],
                    msg[METHOD_NAME_INDEX] + content
                )
            }
        }
    }

    private fun i(tag: String, content: String) {
//        logBuffer.append("[${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}]" +
//                "[$tag], $content\n")
        if (DEBUG) {
            Log.i(tag, content)
        }
    }

    /**
     * Send an error log message.
     *
     * @param content The message you would like logged.
     */
    fun e(content: String) {
        if (DEBUG) {
            val msg = trace()
            if (msg != null) {
                e(
                    msg[CLASS_NAME_INDEX],
                    msg[METHOD_NAME_INDEX] + content
                )
            }
        }
    }

    private fun e(tag: String, content: String) {
//        logBuffer.append("[${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}]" +
//                "[$tag], $content\n")
        if (DEBUG) {
            Log.e(tag, content)
        }
    }

    /**
     * Send an debug log message.
     *
     * @param content The message you would like logged.
     */
    fun d(content: String) {
        if (DEBUG) {
            val msg = trace()
            if (msg != null) {
                d(
                    msg[CLASS_NAME_INDEX],
                    "[ " + msg[METHOD_NAME_INDEX] + " ] " + content
                )
            }
        }
    }

    /**
     * Send an warning log message.
     *
     * @param content The message you would like logged.
     */
    fun w(content: String) {
        if (DEBUG) {
            val msg = trace()
            if (msg != null) {
                w(
                    msg[CLASS_NAME_INDEX],
                    "[ " + msg[METHOD_NAME_INDEX] + " ] " + content
                )
            }
        }
    }

    private fun d(tag: String, content: String) {
//        logBuffer.append("[${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}]" +
//                "[$tag], $content\n")
        if (DEBUG) {
            Log.d(tag, content)
        }
    }

    private fun w(tag: String, content: String) {
//        logBuffer.append("[${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}]" +
//                "[$tag], $content\n")
        if (DEBUG) {
            Log.w(tag, content)
        }
    }

    private fun trace(): Array<String>? {
        var index = 0
        val stackTraceElements = Thread.currentThread().stackTrace ?: return null
        for (i in stackTraceElements.indices) {
            val ste = stackTraceElements[i]
            if ((ste.className == Logger::class.java.name) && (ste.methodName.contains(
                    TRACE_METHOD
                ))) {
                index = i + 2 // index for startEndMethodLog method
                if (index < stackTraceElements.size && stackTraceElements[index].methodName.contains(
                        START_LOG_METHOD
                    ) || index < stackTraceElements.size && stackTraceElements[index].methodName.contains(
                        END_LOG_METHOD
                    )
                ) {
                    break
                }
                index = i + 1 // index for d method
                break
            }
        }

        index++ // index for method call d or startEndMethodLog method

        if ((stackTraceElements.size >= index) && (stackTraceElements[index] != null)) {
            return arrayOf(
                stackTraceElements[index].fileName,
                stackTraceElements[index].methodName + "[" + stackTraceElements[index].lineNumber + "] "
            )
        }
        return null
    }
}
