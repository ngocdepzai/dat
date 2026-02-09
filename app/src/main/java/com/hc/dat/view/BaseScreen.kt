package com.hc.dat.view

import android.os.Bundle
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.hc.dat.view.adapter.DialogButtonClickListener
import com.lws.type.Logger

abstract class BaseScreen : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d("onCreate")

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            // If screen fragment override back key press event (set return true)
            // -> don't disable dispatcher callback handler
            if (!onBackPressed()) {
                isEnabled = false
                requireActivity().onBackPressed()
//                parentFragmentManager.popBackStack()
            }
        }
    }

    open fun onBackPressed(): Boolean {
        return false
    }

    /**
     * REQUIRE call method after context is arrived
     */
    fun showDialog(
        title: String? = null,
        message: String? = null,
        buttonList: List<String>? = null,
        cancelable: Boolean = true,
        listener: DialogButtonClickListener? = null
    ) {
        BaseDialog.showDialog(
            context = requireContext(),
            title = title,
            message = message,
            buttonList = buttonList,
            cancelable = cancelable,
            listener = listener
        )
    }

    /**
     * REQUIRE call method after context is arrived
     */
    fun showProgressDialog(message: String? = null) {
        BaseDialog.showProgressDialog(
            context = requireContext(),
            message = message
        )
    }

    fun dismissProgress() {
        BaseDialog.dismissProgress()
    }

    fun dismissDialog() {
        BaseDialog.dismiss()
    }
}
