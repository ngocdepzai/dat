package com.hc.dat.utils

import com.lws.type.Logger
import io.sentry.Attachment
import io.sentry.protocol.Message
import io.sentry.Scope
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import java.io.File

object SentryLogUploader {

    fun captureLogFileResult(
            success: Boolean,
            file: File?,
            imei: String? = "",
            tag: String,
            sessionId: String? = null,
            studentCode: String? = null,
            teacherCode: String? = null,
            isWifiEnabled: Boolean = false,
            isBatterySaverEnabled: Boolean = false,
            totalGPSUpload: Int = 0,
            totalGPSUploadSuccess: Int = 0,
            message: String? = null
    ) {
        try {
            val sentryMessage = buildString {
                append(if (success) "Upload log success" else "Upload log fail")
                append(" | sessionId=")
                append(sessionId ?: "")
                append(" | studentCode=")
                append(studentCode ?: "")
                append(" | teacherCode=")
                append(teacherCode ?: "")
                append(" | file=")
                append(file?.name ?: "")
                if (!message.isNullOrBlank()) {
                    append(" | message=")
                    append(message)
                }
            }

            val event = SentryEvent().apply {
                level = if (success) SentryLevel.INFO else SentryLevel.ERROR

                this.message = Message().apply {
                    formatted = sentryMessage
                }

                setTag("event_type", tag)
                setTag("upload_status", if (success) "success" else "fail")

                setExtra("imei", imei ?: "")
                setExtra("session_id", sessionId ?: "")
                setExtra("student_code", studentCode ?: "")
                setExtra("teacher_code", teacherCode ?: "")
                setExtra("wifi_enabled", isWifiEnabled)
                setExtra("battery_saver", isBatterySaverEnabled)
                setExtra("gps_upload_count", totalGPSUpload)
                setExtra("gps_upload_success", totalGPSUploadSuccess)
                setExtra("log_file_name", file?.name ?: "")
                setExtra("log_file_path", file?.absolutePath ?: "")
                setExtra("log_file_exists", file?.exists() ?: false)
                setExtra("log_file_size_kb", (file?.length() ?: 0L) / 1024)
                setExtra("custom_message", message ?: "")
            }

            Sentry.withScope { scope: Scope ->
                attachFileToScope(scope, file)
                Sentry.captureEvent(event)
            }

            Sentry.flush(5000)
        } catch (e: Exception) {
            Logger.d("captureLogFileResult error: ${e.message}")
            Logger.d("captureLogFileResult stacktrace: ${e.stackTraceToString()}")
        }
    }

    fun attachLogToEvent(logFile: File) {
        try {
            if (!logFile.exists() || logFile.length() <= 0) return

            Sentry.configureScope { scope ->
                scope.clearAttachments()
                scope.addAttachment(
                        Attachment(
                                logFile.absolutePath,
                                logFile.name,
                                "text/plain"
                        )
                )
            }
        } catch (e: Exception) {
            Logger.d("attachLogToEvent error: ${e.message}")
        }
    }

    fun captureException(
            throwable: Throwable,
            tag: String? = null,
            extras: Map<String, String>? = null,
            file: File? = null
    ) {
        try {
            Sentry.withScope { scope ->

                tag?.let {
                    scope.setTag("error_tag", it)
                }

                extras?.forEach { (key, value) ->
                    scope.setExtra(key, value)
                }

                attachFileToScope(scope, file)

                Sentry.captureException(throwable)
            }

            Sentry.flush(5000)
        } catch (e: Exception) {
            Logger.d("captureException error: ${e.message}")
        }
    }

    fun captureInfo(
            tag: String,
            message: String,
            extras: Map<String, Any>? = null,
            file: File? = null
    ) {
        try {
            val event = SentryEvent().apply {
                level = SentryLevel.INFO

                this.message = Message().apply {
                    formatted = message
                }

                setTag("event_type", tag)

                extras?.forEach { (key, value) ->
                    setExtra(key, value)
                }
            }

            Sentry.withScope { scope ->
                attachFileToScope(scope, file)
                Sentry.captureEvent(event)
            }

            Sentry.flush(5000)
        } catch (e: Exception) {
            Logger.d("captureInfo error: ${e.message}")
        }
    }

    fun clearAttachments() {
        try {
            Sentry.configureScope { scope ->
                scope.clearAttachments()
            }
        } catch (e: Exception) {
            Logger.d("clearAttachments error: ${e.message}")
        }
    }

    private fun attachFileToScope(scope: Scope, file: File?) {
        try {
            scope.clearAttachments()

            file?.takeIf { it.exists() && it.length() > 0 }?.let {
                scope.addAttachment(
                        Attachment(
                                it.absolutePath,
                                it.name,
                                "text/plain"
                        )
                )
            }
        } catch (e: Exception) {
            Logger.d("attachFileToScope error: ${e.message}")
        }
    }
}