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

        // Xử lý nút Back an toàn
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (!onBackPressed()) {
                isEnabled = false
                requireActivity().onBackPressed()
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
        // KIỂM TRA QUAN TRỌNG: Nếu Fragment chưa được gắn vào Activity hoặc đang bị hủy thì không làm gì cả
        val safeContext = context ?: return
        if (!isAdded) return

        try {
            BaseDialog.showDialog(
                    context = safeContext,
                    title = title,
                    message = message,
                    buttonList = buttonList,
                    cancelable = cancelable,
                    listener = listener
            )
        } catch (e: Exception) {
            Logger.e("BaseScreen: showDialog error: ${e.message}")
        }
    }

    /**
     * REQUIRE call method after context is arrived
     */
    fun showProgressDialog(message: String? = null) {
        val safeContext = context ?: return
        if (!isAdded) return

        try {
            BaseDialog.showProgressDialog(
                    context = safeContext,
                    message = message
            )
        } catch (e: Exception) {
            Logger.e("BaseScreen: showProgressDialog error: ${e.message}")
        }
    }

    fun dismissProgress() {
        // Kiểm tra isAdded để đảm bảo không đụng vào UI khi Fragment đã thoát
        if (isAdded) {
            BaseDialog.dismissProgress()
        }
    }

    fun dismissDialog() {
        if (isAdded) {
            BaseDialog.dismiss()
        }
    }

    // Hỗ trợ kiểm tra nhanh trạng thái màn hình trước khi thực hiện các tác vụ UI phức tạp
    fun isScreenActive(): Boolean {
        return isAdded && !isDetached && activity != null
    }
}
