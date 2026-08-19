package com.hc.dat.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
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
    private var toastMessage: Toast? = null
    private var messageText: TextView? = null
    private var messageView: LinearLayout? = null
    private val priorityQueues: Deque<String> = LinkedList()
    private val speechCallbacks: MutableMap<String, () -> Unit> = mutableMapOf()
    private val speechDoneListeners: MutableList<() -> Unit> = mutableListOf()

    fun init(context: Context) {
        this.context = context
        textToSpeech = TextToSpeech(
            context
        ) { status ->
            if (status == TextToSpeech.ERROR) {
            } else if (status == TextToSpeech.SUCCESS) {

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                    }

                    override fun onDone(utteranceId: String?) {
                        utteranceId?.let { id -> speechCallbacks.remove(id)?.invoke() }
                        val listeners = speechDoneListeners.toList()
                        speechDoneListeners.clear()
                        listeners.forEach { it.invoke() }
                        autoSpeaking()
                    }

                    override fun onError(utteranceId: String?) {
                    }
                })
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Logger.i("List locales: ${context.resources.configuration.locales}")
            }
            val locale = context.resources.configuration.locale
            Logger.i("Current locale: $locale")
        }
        textToSpeech?.setSpeechRate(0.8F)

        toastMessage = Toast(BaseNotification.context)
        val layout = LayoutInflater.from(BaseNotification.context)
            .inflate(R.layout.custom_toast, null, false)
        messageText = layout.findViewById<View>(R.id.toast_text) as TextView
        messageView = layout.findViewById<View>(R.id.toast_type) as LinearLayout
        messageText!!.text = ""
        messageView!!.setBackgroundResource(R.drawable.success_shape)
        toastMessage!!.duration = Toast.LENGTH_LONG
        toastMessage!!.view = layout
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
            messageText!!.text = message
            messageView!!.setBackgroundResource(R.drawable.success_shape)
            toastMessage!!.duration = duration
            toastMessage!!.show()
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
            messageText!!.text = message
            messageView!!.setBackgroundResource(R.drawable.warning_shape)
            toastMessage!!.duration = duration
            toastMessage!!.show()
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
            messageText!!.text = message
            messageView!!.setBackgroundResource(R.drawable.error_shape)
            toastMessage!!.duration = duration
            toastMessage!!.show()
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
        val utteranceId = "cb_${System.currentTimeMillis()}"
        speechCallbacks[utteranceId] = onDone
        val params = hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to utteranceId)
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, params)
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
