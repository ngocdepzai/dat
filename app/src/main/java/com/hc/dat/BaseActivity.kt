package com.hc.dat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    /**
     * List of observer onActivityResult event
     * Add an observer if you want listener event.
     * Remove it if you haven't listen.
     */
    open val onActivityResultObserver =
        mutableListOf<(
                activity: Activity,
                requestCode: Int,
                resultCode: Int,
                data: Intent?
            ) -> Unit>()

    /**
     * List of observer onNewIntent event
     * Add an observer if you want listener event.
     * Remove it if you haven't listen.
     */
    open val onNewIntentObserver =
        mutableListOf<(
                activity: Activity,
                data: Intent?
            ) -> Unit>()

    /**
     * List of observer onRequestPermissionsResult event
     * Add an observer if you want listener event.
     * Remove it if you haven't listen.
     */
    open val onReqPermissionResultObserver =
        mutableListOf<(
                activity: Activity,
                requestCode: Int,
                permissions: Array<out String>,
                grantResults: IntArray
            ) -> Unit>()

    private lateinit var app: BaseApplication

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        app = application as BaseApplication
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        onNewIntentObserver.forEach { callback ->
            callback(this, intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onActivityResultObserver.forEach { callback ->
            callback(this, requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onReqPermissionResultObserver.forEach { callback ->
            callback(this, requestCode, permissions, grantResults)
        }
    }
}
