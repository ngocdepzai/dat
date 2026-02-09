package com.hc.dat.view.viewholder

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.hc.dat.model.database.entity.RiderSessionEntity
import com.hc.dat.model.result.SessionHistory
import com.hc.dat.view.adapter.ItemButtonClickListener
import hc.manager.datapp.databinding.ItemSessionBinding
import hc.manager.datapp.utils.DateUtil

data class SessionsHistoryViewHolder constructor(
    val viewBinding: ItemSessionBinding,
    var listener: ItemButtonClickListener?
) :
    RecyclerView.ViewHolder(viewBinding.root) {
    private lateinit var localSessionHistory: RiderSessionEntity
    fun build(
        sessionHistory: SessionHistory,
        position: Int,
        localListSessionHistory: List<RiderSessionEntity>
    ) {
        viewBinding.tvNo.text = position.toString()
        if(!sessionHistory.isSessionValid){
            viewBinding.tvNo.setTextColor(Color.RED)
        }
        viewBinding.tvStudentName.text = sessionHistory.studentName
        viewBinding.tvLoginDate.text = sessionHistory.loginDateParse
        viewBinding.tvLogoutDate.text = sessionHistory.logoutDateParse
        viewBinding.tvTotalTime.text = DateUtil.ConvertHms(sessionHistory.totalTime)
        viewBinding.tvTotalDis.text = sessionHistory.totalDis.toString()
        viewBinding.tvStatus.text = sessionHistory.sentGeneral.toString()
        viewBinding.tvId.text = sessionHistory.id
        if (!sessionHistory.sentGeneral) {
            viewBinding.tvStatus.text = "Chưa truyền Tc"
        } else {
            viewBinding.tvStatus.text = "Đã truyền Tc"
            viewBinding.tvStatus.setTextColor(Color.GREEN)
        }
        if(sessionHistory.logoutDateParse == null){
            viewBinding.btnUploadLog.visibility = View.VISIBLE
        }
        run {
            localListSessionHistory.forEach {
                if (it.sessionId == sessionHistory.id && it.logoutTime != null) {
                    localSessionHistory = it
                    viewBinding.btnExportSessionReport.visibility = View.VISIBLE
                    viewBinding.btnCheckDataMissing.visibility = View.VISIBLE

                    return@run
                } else {
                    viewBinding.btnExportSessionReport.visibility = View.GONE
                    viewBinding.btnCheckDataMissing.visibility = View.GONE
                }
            }
        }
        viewBinding.btnExportSessionReport.setOnClickListener {
            listener?.onExportSessionReport(riderSessionEntity = localSessionHistory)
        }
        viewBinding.btnUploadLog.setOnClickListener{
            listener?.onUploadLog()
        }
        viewBinding.tvNo.setOnClickListener {
            listener?.onShowDialog(messages = sessionHistory.invalidReasons)
        }
        viewBinding.btnCheckDataMissing.setOnClickListener {
            listener?.onCheckDataMissing(sessionId = sessionHistory.id)
        }
    }
}
