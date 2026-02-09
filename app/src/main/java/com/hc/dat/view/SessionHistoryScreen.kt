package com.hc.dat.view

import android.R
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hc.dat.model.database.entity.RiderSessionEntity
import com.hc.dat.model.result.SessionHistory
import com.hc.dat.service.model.DateMissing
import com.hc.dat.service.model.SessionHistoryRequest
import com.hc.dat.service.model.SessionHistoryResponse
import com.hc.dat.utils.Utils
import com.hc.dat.view.adapter.DialogButtonClickListener
import com.hc.dat.view.adapter.ItemButtonClickListener
import com.hc.dat.view.adapter.ListSessionHistoryAdapter
import com.hc.dat.viewmodel.AppAction
import com.hc.dat.viewmodel.RiderSessionAction
import com.hc.dat.viewmodel.RiderSessionViewModel
import com.lws.type.LogRecorder
import com.lws.type.Logger
import hc.manager.datapp.databinding.ActivityCompareSessionBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar

class SessionHistoryScreen : DatBaseScreen() {
    private val statusData = arrayOf("Tất cả", "Chưa truyền TC", "Đã truyền TC")


    private lateinit var viewBinding: ActivityCompareSessionBinding
    private lateinit var riderSessionViewModel: RiderSessionViewModel

    lateinit var listSessionHistoryAdapter: ListSessionHistoryAdapter
    private lateinit var listSessionHistory: List<SessionHistory>
    private var serial: String = ""
    private var startTime: String? = null
    private var endTime: String? = null
    private var sendGeneral: Boolean? = null
    private var page: Int = 1
    private var limit: Int = 12
    private var totalSession = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        viewBinding = ActivityCompareSessionBinding.inflate(inflater, container, false)
        riderSessionViewModel =
            ViewModelProviders.of(
                requireActivity(),
                viewModelFactory
            )[RiderSessionViewModel::class.java]
        initView()
        return viewBinding.root
    }

    private fun initView() {
        viewBinding.rvSession.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        listSessionHistoryAdapter = ListSessionHistoryAdapter(listener)
        viewBinding.rvSession.adapter = listSessionHistoryAdapter

        viewBinding.etStartDate.inputType = InputType.TYPE_NULL
        viewBinding.etEndDate.inputType = InputType.TYPE_NULL
        serial = riderSessionViewModel.getImeiDevice(requireActivity())


        viewBinding.spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                when (i) {
                    StatusSendTC.ALL.code -> sendGeneral = null
                    StatusSendTC.NOT_SENT_TC.code -> sendGeneral = false
                    StatusSendTC.SENT_TC.code -> sendGeneral = true
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
        val spinnerAdapter = ArrayAdapter<String>(
            requireActivity(),
            R.layout.simple_spinner_item, statusData
        )
        spinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        viewBinding.spStatus.adapter = spinnerAdapter

        getListSessionHistory()
        CoroutineScope(Dispatchers.IO).launch() {
            val localListRiderSessionEntity = riderSessionViewModel.getLocalListRiderSessionEntity()
            listSessionHistoryAdapter.updateLocalListSessionHistory(localListSessionHistory = localListRiderSessionEntity)
        }

        viewBinding.etStartDate.setOnClickListener {
            showDateDialog(true)
        }

        viewBinding.etEndDate.setOnClickListener(View.OnClickListener {
            showDateDialog(
                false
            )
        })

        viewBinding.btNext.setOnClickListener {
            if (checkCanNext()) {
                page += 1
                getListSessionHistory()
            }
        }
        viewBinding.btPrew.setOnClickListener {
            if (checkCanPrew()) {
                page -= 1
                getListSessionHistory()
            }
        }
        viewBinding.btSearch.setOnClickListener {
            page = 1
            getListSessionHistory()
        }
    }

    private fun getListSessionHistory(){
        showProgressDialog()
        riderSessionViewModel.getListSessionHistory(
            sessionHistoryRequest = SessionHistoryRequest(
                seri = serial,
                startTime = startTime,
                endTime = endTime,
                limit = limit,
                page = page,
                sendGeneral = sendGeneral
            ),
            callback = riderSessionCallback
        )
    }

    val listener: ItemButtonClickListener = object : ItemButtonClickListener {

        override fun onExportSessionReport(riderSessionEntity: RiderSessionEntity) {
                showProgressDialog()
            CoroutineScope(Dispatchers.Default).launch() {
                val channel = Channel<Any>()
                riderSessionViewModel.exportSessionReport(riderSessionEntity = riderSessionEntity, channel = channel)
                channel.receive()

                val reportFile =
                    riderSessionEntity.sessionId?.let {
                        appViewModel.getFileBySessionId(
                            sessionId = it,
                            localSessionId = riderSessionEntity.id,
                            studentCode = riderSessionEntity.studentCode,
                        )
                    }
                reportFile?.let {
                    appViewModel.pushFile(files = listOf(reportFile), callback = appCallback)
                } ?: dismissProgress()
            }
        }

        override fun onUploadLog() {
            showProgressDialog()
            LogRecorder.saveLog(false)
            riderSessionViewModel.pushLogFile(callback = appCallback)
        }

        override fun onShowDialog(messages: List<String>) {
            val message = messages.joinToString(separator = "\n")
            showDialog(
                title = getString(hc.manager.datapp.R.string.title_notification),
                message = message,
                buttonList = listOf(getString(hc.manager.datapp.R.string.ok)),
                listener = object : DialogButtonClickListener {
                    override fun onDialogButtonClick(position: Int) {
                        dismissDialog()
                    }
                }
            )
        }

        override fun onCheckDataMissing(sessionId: String) {
            riderSessionViewModel.checkMissingDataSession(sessionId = sessionId, callback = riderSessionCallback)
        }
    }
    private val appCallback: (action: AppAction, data: Any?)
    -> Unit = { action: AppAction, data: Any? ->
        Logger.d("Callback action: $action | data: $data")
        dismissProgress()
        when (action) {
            AppAction.UPLOAD_FILE_SUCCESS -> {
                val message: String = data.toString()
                BaseNotification.showError(message = message, muteSpeak = true)
            }
            AppAction.UPLOAD_FILE_FAIL -> {
                val message: String = data.toString()
                BaseNotification.showError(message = message, muteSpeak = true)
            }
            else -> {}
        }
    }

    private fun checkCanNext(): Boolean {
        return if (page * limit < totalSession) {
            viewBinding.btNext.setBackgroundColor(Color.GREEN)
            true
        } else {
            viewBinding.btNext.setBackgroundColor(Color.GRAY)
            false
        }
    }

    private fun checkCanPrew(): Boolean {
        return if (page > 1) {
            viewBinding.btPrew.setBackgroundColor(Color.GREEN)
            true
        } else {
            viewBinding.btPrew.setBackgroundColor(Color.GRAY)
            false
        }
    }

    private fun showDateDialog(isStart: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH]
        val day = calendar[Calendar.DAY_OF_MONTH]
        val datePickerDialog = DatePickerDialog(
            requireActivity(), { _, i, i1, i2 ->
                calendar.set(i, i1, i2)
                val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
                val formatCallApi = SimpleDateFormat("yyyy-MM-dd")
                if (isStart) {
                    startTime = formatCallApi.format(calendar.time)
                    viewBinding.etStartDate.text = Editable.Factory.getInstance()
                        .newEditable(simpleDateFormat.format(calendar.time))
                } else {
                    endTime = formatCallApi.format(calendar.time)
                    viewBinding.etEndDate.text = Editable.Factory.getInstance()
                        .newEditable(simpleDateFormat.format(calendar.time))
                }
            }, year, month, day
        )
        datePickerDialog.show()
    }


    override fun onBackPressed(): Boolean {
        requireActivity().finish()
        return true
    }
    private fun pushDataMissing(dataMissing: Triple<String, List<DateMissing>, List<DateMissing>>) {
        showProgressDialog()
        riderSessionViewModel.pushDataMissing(
            dataMissing = dataMissing,
            riderSessionCallback = riderSessionCallback
        )
    }

    private val riderSessionCallback: (action: RiderSessionAction, data: Any?)
    -> Unit = { action: RiderSessionAction, data: Any? ->
        Logger.d("callback action: $action | data: $data")
        dismissProgress()
        when (action) {
            RiderSessionAction.GET_SESSION_HISTORY_SUCCESS -> {
                val sessionHistoryResponse = data as SessionHistoryResponse
                listSessionHistory = sessionHistoryResponse.listSessionHistory
                listSessionHistoryAdapter.updateListSessionHistory(listSessionHistory = listSessionHistory)
                totalSession = sessionHistoryResponse.total
            }

            RiderSessionAction.GET_SESSION_HISTORY_FAIL -> {
                val message = data as String
                showDialog(
                    title = getString(hc.manager.datapp.R.string.title_notification),
                    message = message,
                    buttonList = listOf(getString(hc.manager.datapp.R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    })
            }

            RiderSessionAction.CHECK_MISSING_DATA_SUCCESS -> {
                dismissProgress()
                if (data == null) {
                    showDialog(
                        title = getString(hc.manager.datapp.R.string.title_notification),
                        message = "Không có dữ liệu nào thiếu",
                        buttonList = listOf(getString(hc.manager.datapp.R.string.confirm_title)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )

                } else {
                    val dataMissing = data as Triple<String, List<DateMissing>, List<DateMissing>>
                    pushDataMissing(dataMissing = dataMissing)
                }
            }
            RiderSessionAction.CHECK_MISSING_DATA_FAIL -> {
                dismissProgress()
                showDialog(
                    title = getString(hc.manager.datapp.R.string.error_title_dialog),
                    message = getString(hc.manager.datapp.R.string.check_missing_data_fail),
                    buttonList = listOf(getString(hc.manager.datapp.R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
            RiderSessionAction.PUSH_DATA_MISSING_SUCESS -> {
                dismissProgress()
                if(data != null){
                    val message = data as String
                    BaseNotification.showMessage(message = message, muteSpeak = true)
                } else {
                    BaseNotification.showError(message = "Không tìm được dữ liệu phù hợp", muteSpeak = true)
                }

            }
            RiderSessionAction.PUSH_DATA_MISSING_FAIL -> {
                dismissProgress()
                BaseNotification.showError(message = "Đẩy dữ liệu thất bại", muteSpeak = true)
            }
            else -> {}
        }
        checkCanPrew()
        checkCanNext()
    }

}
enum class StatusSendTC(val code: Int) {
    ALL(0),
    NOT_SENT_TC(1),
    SENT_TC(2)
}
