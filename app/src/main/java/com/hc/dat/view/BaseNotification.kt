package com.hc.dat.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.lws.type.Logger
import hc.manager.datapp.R
import java.util.Deque
import java.util.LinkedList

enum class Priority {
    HIGH,
    LOW
}
@SuppressLint("StaticFieldLeak")
internal object BaseNotification {
    private var textToSpeech: TextToSpeech? = null
    private var context: Context? = null
    private val priorityQueues: Deque<String> = LinkedList()
    private val speechCallbacks: MutableMap<String, () -> Unit> = mutableMapOf()
    private val speechDoneListeners: MutableList<() -> Unit> = mutableListOf()
    private var isTtsReady = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingSpeakActions: MutableList<() -> Unit> = mutableListOf()

    fun init(context: Context) {
        if (textToSpeech != null) return
        this.context = context
        textToSpeech = TextToSpeech(
            context
        ) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                    }

                    override fun onDone(utteranceId: String?) {
                        fireSpeechCallbacks(utteranceId)
                        autoSpeaking()
                    }

                    override fun onError(utteranceId: String?) {
                        fireSpeechCallbacks(utteranceId)
                    }
                })
                isTtsReady = true
            }
            val pending = pendingSpeakActions.toList()
            pendingSpeakActions.clear()
            pending.forEach { it.invoke() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Logger.i("List locales: ${context.resources.configuration.locales}")
            }
            val locale = context.resources.configuration.locale
            Logger.i("Current locale: $locale")
        }
        textToSpeech?.setSpeechRate(0.8F)
    }

    fun showMessage(
        message: String,
        duration: Int = Toast.LENGTH_LONG,
        muteSpeak: Boolean = false,
        showToast: Boolean = true,
        priority: Priority = Priority.LOW
    ) {
        // speak message
        if (!muteSpeak) {
            handleNotifyQueue(message = message, priority = priority)
        }
        if (showToast) {
            showStyledToast(message, R.drawable.success_shape, duration)
        }
    }

    fun showWarning(
        message: String,
        duration: Int = Toast.LENGTH_LONG,
        muteSpeak: Boolean = false,
        showToast: Boolean = true,
        priority: Priority = Priority.LOW
    ) {
        // speak message
        if (!muteSpeak) {
            handleNotifyQueue(message = message, priority = priority)
        }

        if (showToast) {
            showStyledToast(message, R.drawable.warning_shape, duration)
        }
    }

    fun showError(
        message: String,
        duration: Int = Toast.LENGTH_LONG,
        muteSpeak: Boolean = false,
        showToast: Boolean = true,
        priority: Priority = Priority.LOW
    ) {
        // speak message
        if (!muteSpeak) {
            handleNotifyQueue(message = message, priority = priority)
        }

        if (showToast) {
            showStyledToast(message, R.drawable.error_shape, duration)
        }
    }

    // Mỗi thông báo phải có Toast và view riêng: dùng lại một Toast duy nhất khiến thông báo
    // sau ghi đè text của thông báo trước trong cùng cửa sổ hiển thị, nên có message chỉ được
    // TTS đọc mà không bao giờ hiện chữ. Toast riêng thì hệ thống xếp hàng hiển thị lần lượt.
    private fun showStyledToast(message: String, backgroundRes: Int, duration: Int) {
        val currentContext = context ?: return
        // Toast lấy Handler theo thread gọi, mà nhiều nơi báo lỗi từ thread nền, nên luôn
        // dựng Toast trên main thread.
        mainHandler.post {
            val layout = LayoutInflater.from(currentContext)
                .inflate(R.layout.custom_toast, null, false)
            (layout.findViewById<View>(R.id.toast_text) as TextView).text = message
            layout.findViewById<View>(R.id.toast_type).setBackgroundResource(backgroundRes)
            Toast(currentContext).apply {
                this.duration = duration
                this.view = layout
            }.show()
        }
    }

    fun waitForSpeechDone(onDone: () -> Unit) {
        if (textToSpeech?.isSpeaking == true) {
            speechDoneListeners.add(onDone)
        } else {
            onDone()
        }
    }

    fun speakWithCallback(message: String, onDone: () -> Unit) {
        if (message.isBlank()) {
            onDone()
            return
        }
        if (!isTtsReady) {
            pendingSpeakActions.add { speakWithCallback(message, onDone) }
            return
        }
        val tts = textToSpeech
        if (tts == null) {
            onDone()
            return
        }
        priorityQueues.clear()
        val utteranceId = "cb_${System.currentTimeMillis()}"
        speechCallbacks[utteranceId] = onDone
        val params = hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to utteranceId)
        val result = tts.speak(message, TextToSpeech.QUEUE_FLUSH, params)
        if (result != TextToSpeech.SUCCESS) {
            speechCallbacks.remove(utteranceId)?.invoke()
        }
    }

    private fun fireSpeechCallbacks(utteranceId: String?) {
        utteranceId?.let { id -> speechCallbacks.remove(id)?.invoke() }
        val listeners = speechDoneListeners.toList()
        speechDoneListeners.clear()
        listeners.forEach { it.invoke() }
    }

    private fun speechMessage(message: String) {
        val params = hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to "utteranceId")

        if (textToSpeech != null) {
            if (message.isNotBlank()) {
                textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, params)
            }
        } else {
            Logger.e("Text to Speech didn't work!")
        }
    }
    private fun handleNotifyQueue(message: String, priority: Priority) {

        // add notify message in queue if 'isSpeaking == true'
        if (textToSpeech?.isSpeaking == true) {

            // if notify message in queue over 4 size then last message will deleted
            if (priorityQueues.size > 4) {
                priorityQueues.pollLast()
            }
            if (priority == Priority.HIGH) {
                priorityQueues.offerFirst(message)
            } else {
                priorityQueues.offerLast(message)
            }

        } else {
            speechMessage(message)
        }
    }

    private fun autoSpeaking() {
        if (priorityQueues.isNotEmpty()) {
            val nextSpeech = priorityQueues.poll()
            speechMessage(nextSpeech!!)
        }
    }

}
