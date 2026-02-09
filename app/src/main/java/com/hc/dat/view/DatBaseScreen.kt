package com.hc.dat.view

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.hc.dat.DatMainActivity
import com.hc.dat.di.component.AppComponent
import com.hc.dat.view.adapter.DialogButtonClickListener
import com.hc.dat.view.comm.NavButton
import com.hc.dat.viewmodel.ApplicationViewModel
import com.hc.dat.viewmodel.FaceRecognitionViewModel
import com.hc.dat.viewmodel.TriggerFaceRecognitionEvent
import com.hc.dat.viewmodel.TriggerLogout
import com.lws.type.Logger
import hc.manager.datapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class DatBaseScreen : BaseScreen(), NavigationEventHandler {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    lateinit var appViewModel: ApplicationViewModel

    companion object {
        const val TITLE_SCREEN = "title_screen"
        const val TOOLBAR_BUTTON_LEFT = "toolbar_button_left"
        const val TOOLBAR_TEXT_RIGHT = "toolbar_text_right"
        const val TOOLBAR_DESCRIPTION_RIGHT = "toolbar_description_right"
        const val BOTTOM_NAV_BUTTON_LEFT = "bottom_nav_button_left"
        const val BOTTOM_NAV_BUTTON_CENTER = "bottom_nav_button_center"
        const val BOTTOM_NAV_BUTTON_RIGHT = "bottom_nav_button_right"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppComponent.getInstance().inject(this)
        appViewModel =
            ViewModelProviders.of(
                requireActivity(),
                viewModelFactory
            )[ApplicationViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val faceRecognitionViewModel = ViewModelProviders.of(
            requireActivity(),
            viewModelFactory
        )[FaceRecognitionViewModel::class.java]
        faceRecognitionViewModel.triggerFaceRecognitionEvent.observe(
            viewLifecycleOwner,
            Observer {
                Logger.d("Trigger event throw from Face Recognition progress: $it")
                when (it) {
                    TriggerFaceRecognitionEvent.CERTIFICATION_VERIFY_FAIL -> {
                        dismissProgress()
                        showDialog(
                            title = getString(R.string.error_title_dialog),
                            message = getString(R.string.active_security_fail),
                            cancelable = false,
                            buttonList = listOf(getString(R.string.exit_button)),
                            listener = object : DialogButtonClickListener {
                                override fun onDialogButtonClick(position: Int) {
                                    dismissDialog()
                                    requireActivity().finish()
                                }
                            }
                        )
                    }
                    TriggerFaceRecognitionEvent.CREATE_FACE_PASS_GROUP_FAIL -> {
                        showDialog(
                            title = getString(R.string.error_title_dialog),
                            message = getString(R.string.create_face_group_error),
                            cancelable = false,
                            buttonList = listOf(getString(R.string.exit_button)),
                            listener = object : DialogButtonClickListener {
                                override fun onDialogButtonClick(position: Int) {
                                    dismissDialog()
                                    restartApplication()
                                }
                            }
                        )
//                        BaseNotification.showError(
//                            getString(R.string.create_face_group_error,),
//                        )
                    }
                    TriggerFaceRecognitionEvent.DOWNLOAD_LICENSE_START -> {
                        showProgressDialog(message = "Đang tải license...")
                    }
                    TriggerFaceRecognitionEvent.CERTIFICATION_VERIFY_TIMEOUT -> {
                        dismissProgress()
                        showDialog(
                            title = getString(R.string.error_title_dialog),
                            message = getString(R.string.certification_verify_timeout_message),
                            buttonList = listOf(getString(R.string.exit_button)),
                            listener = object : DialogButtonClickListener {
                                override fun onDialogButtonClick(position: Int) {
                                    dismissDialog()
                                }
                            }
                        )
                    }
                    else -> {
                        dismissProgress()
                        Logger.w("Value triggerFaceRecognitionEvent not handle!")
                    }
                }
//            if (it == TriggerFaceRecognitionEvent.VERIFY_CER_NEED_INTERNET_CONNECTION) {
//                // Though to AppViewModel handle
// //                appViewModel.endLoginSession()
//            }
            }
        )

        appViewModel.triggerLogout.observe(
            viewLifecycleOwner,
            Observer {
                Logger.d("Trigger time up or MDM request force logout!")
                when (it) {
                    TriggerLogout.TIMEOUTS -> {
//                    showDialog(
//                        title = getString(R.string.verification),
//                        message = getString(R.string.time_out),
//                        buttonList = listOf(getString(R.string.ok)),
//                        listener = object : DialogButtonClickListener {
//                            override fun onDialogButtonClick(position: Int) {
//                                view.findNavController().navigate(R.id.action_to_loginFrag)
//                                dismissDialog()
//                            }
//
//                        }
//                    )
                    }
                    TriggerLogout.FORCE_LOGOUT -> {
//                    view.findNavController().navigate(R.id.action_to_loginFrag)
                    }
                    TriggerLogout.SERVER_DECLINE -> {
//                    showDialog(
//                        title = getString(R.string.title_notification),
//                        message = getString(R.string.message_er0105),
//                        buttonList = listOf(getString(R.string.ok)),
//                        listener = object : DialogButtonClickListener{
//                            override fun onDialogButtonClick(position: Int) {
//                                view.findNavController().navigate(R.id.action_to_loginFrag)
//                                dismissDialog()
//                            }
//                        }
//                    )
                    }
                }
            }
        )
    }

    private fun restartApplication() {
        val intent = Intent(context, DatMainActivity::class.java)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        requireActivity().startActivity(intent)
        requireActivity().finish()

        Runtime.getRuntime().exit(0)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Logger.d("onActivityCreated")

        // Handle set action bar title
        val titleName: String? = arguments?.getString(TITLE_SCREEN)
        val topButtonLeft: String? = arguments?.getString(TOOLBAR_BUTTON_LEFT)
        val topTextRight: String? = arguments?.getString(TOOLBAR_TEXT_RIGHT)
        val topDescriptionRight: String? = arguments?.getString(TOOLBAR_DESCRIPTION_RIGHT)
        val bottomButtonLeft: String? = arguments?.getString(BOTTOM_NAV_BUTTON_LEFT)
        val bottomButtonCenter: String? = arguments?.getString(BOTTOM_NAV_BUTTON_CENTER)
        val bottomButtonRight: String? = arguments?.getString(BOTTOM_NAV_BUTTON_RIGHT)
        if (activity is CommonAppUI) {
            (activity as CommonAppUI).setAppHeader(
                btLeft = topButtonLeft,
                title = titleName,
                textRight = topTextRight,
                descriptionRight = topDescriptionRight
            )
            (activity as CommonAppUI).setAppBottom(
                btLeft = bottomButtonLeft,
                btCenter = bottomButtonCenter,
                btRight = bottomButtonRight
            )
            (activity as CommonAppUI).setNavigationCallback(this)
        }
        super.onActivityCreated(savedInstanceState)
    }

    override fun onNavigationButtonPressed(navButton: NavButton) {
        Logger.d("onNavigationButtonPressed navButton: $navButton")
    }

    fun setAppHeaderState(
        isEnableBtLeft: Boolean? = null,
        isVisibleTitle: Boolean? = null,
        isVisibleElementRight: Boolean? = null
    ) {
        (activity as CommonAppUI).setAppHeaderState(
            isEnableBtLeft,
            isVisibleTitle,
            isVisibleElementRight
        )
    }

    fun setAppBottomState(
        isEnableBtLeft: Boolean? = null,
        isEnableBtCenter: Boolean? = null,
        isEnableBtRight: Boolean? = null
    ) {
        (activity as CommonAppUI).setAppBottomState(
            isEnableBtLeft,
            isEnableBtCenter,
            isEnableBtRight
        )
    }

    fun setAppBottom(
        btLeft: String? = null,
        btCenter: String? = null,
        btRight: String? = null
    ) {
        (activity as CommonAppUI).setAppBottom(btLeft, btCenter, btRight)
    }
}

interface CommonAppUI {
    fun setAppHeader(
        btLeft: String?,
        title: String?,
        textRight: String?,
        descriptionRight: String?
    )

    fun setAppBottom(
        btLeft: String?,
        btCenter: String?,
        btRight: String?
    )

    fun setAppHeaderState(
        isEnableBtLeft: Boolean? = null,
        isVisibleTitle: Boolean? = null,
        isVisibleElementRight: Boolean? = null
    )

    fun setAppBottomState(
        isEnableBtLeft: Boolean? = null,
        isEnableBtCenter: Boolean? = null,
        isEnableBtRight: Boolean? = null
    )

    fun setNavigationCallback(callback: NavigationEventHandler)
}

interface NavigationEventHandler {
    fun onNavigationButtonPressed(navButton: NavButton)
}
