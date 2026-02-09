package com.lws.device

import android.content.Intent


class OnActivityEventDispatch {
    private var callback: OnActivityResultCallback? = null

    fun addCallback(callback: OnActivityResultCallback) {
        this.callback = callback
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callback?.run {
            onActivityResult(requestCode, resultCode, data)
        }
    }
}

interface OnActivityResultCallback {
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
}