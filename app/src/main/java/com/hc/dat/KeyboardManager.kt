package com.hc.dat

import android.content.Context
import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import com.lws.type.Logger

/**
 * This class help manage and contact with soft keyboard of TV
 */
object KeyboardManager :
    ViewTreeObserver.OnGlobalLayoutListener {

    /**
     * Input method manager for manage soft keyboard
     */
    private var mInputManager: InputMethodManager? = null

    /**
     * View focused to show soft keyboard
     */
    private var mView: View? = null

    private lateinit var mContext: Context

    /**
     * Check status of keyboard<br></br>
     * @return TRUE if keyboard is curren showing<br></br>
     * FALSE in otherwise
     */
    /**
     * Status of soft keyboard is showed or not
     */
    var isSWKeyBoardVisible = false
    private var mListener: OnSoftKeyBoardListener? = null
    fun setOnkeyboardListener(listener: OnSoftKeyBoardListener?) {
        mListener = listener
    }

    /**
     * Set keyboard status
     * @param isVisible
     * TRUE if show keyboard<br></br>
     * FALSE in otherwise
     */
    fun setKeyboardVisible(isVisible: Boolean) {
        isSWKeyBoardVisible = isVisible
        if (isSWKeyBoardVisible) {
            // Show keyboard
            mView?.isFocusableInTouchMode = true
            mView?.requestFocus()
            //            mInputManager.showSoftInput(mView, 0);
            val imm =
                mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        } else {
            // Hide keyboard
            mView?.isFocusable = false
            mInputManager!!.hideSoftInputFromWindow(mView!!.windowToken, 0)
            val imm =
                mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(mView!!.windowToken, 0)
        }
    }

    /**
     * Toggle keyboard status<br></br>
     * If keyboard is current visible then hide keyboard<br></br>
     * If keyboard is current not visible then show keyboard
     */
    fun toggleSWKeyBoard() {
        mView?.run {
            if (!mView!!.isFocusable) {
                mView?.isFocusable = true
            }
        }

        isSWKeyBoardVisible = !isSWKeyBoardVisible
        setKeyboardVisible(isSWKeyBoardVisible)
    }

    /**
     * Show soft keyboard for user can input text
     */
    fun showKeyboard() {
        if (!mView!!.isFocusable) {
            mView!!.isFocusable = true
        }
        setKeyboardVisible(true)
    }

    override fun onGlobalLayout() {
        if (mView != null) {
            val r = Rect()
            // r will be populated with the coordinates of your view
            // that area still visible.
            mView!!.getWindowVisibleDisplayFrame(r)
            val heightDiff = (
                mView!!.rootView.height -
                    (r.bottom - r.top)
                )
            //            Logger.d( this, "onGlobalLayout", "HeightDiff = " + heightDiff);
            if (heightDiff > 100) { // if more than 100 pixels, its
                // probably a keyboard...
                if (mListener != null) {
                    mListener!!.onShow()
                }
                isSWKeyBoardVisible = true
            } else {
                if (mListener != null) {
                    mListener!!.onHide()
                }
                isSWKeyBoardVisible = false
            }
        }
    }

    /**
     * Set view parent of keyboard<br></br>
     * When focus to this view then keyboard will showing
     * @param mView
     */
    fun setView(mView: View?) {
        if (mView != null) {
            KeyboardManager.mView = mView
            KeyboardManager.mView!!.isFocusable = false
        }
    }

    /**
     * Handle key event of user
     * @param event
     * @return
     */
    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        Logger.d("dispatchKeyEvent press keydown keyCode: ${event.keyCode}")
        return if (mView != null) {
            mView!!.dispatchKeyEvent(event)
        } else false
    }

    interface OnSoftKeyBoardListener {
        fun onShow()
        fun onHide()
    }

    /**
     * Constructor
     * @param mContext
     * @param view
     * View handle keyboard. Usually is EditText
     */
    fun setConfig(
        context: Context,
        view: View
    ) {
        mContext = context
        mInputManager =
            mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mView = view
        if (mView != null) {
            mView!!.viewTreeObserver.addOnGlobalLayoutListener(this)
        }
    }
}
