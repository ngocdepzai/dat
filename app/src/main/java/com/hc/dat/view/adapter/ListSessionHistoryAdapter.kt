package com.hc.dat.view.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hc.dat.model.database.entity.RiderSessionEntity
import com.hc.dat.model.result.SessionHistory
import com.hc.dat.view.viewholder.SessionsHistoryViewHolder
import hc.manager.datapp.databinding.ItemSessionBinding

class ListSessionHistoryAdapter(
    var listener: ItemButtonClickListener?

) : RecyclerView.Adapter<SessionsHistoryViewHolder>() {

    private var listSessionHistory: List<SessionHistory> = listOf()
    private var localListSessionHistory: List<RiderSessionEntity> = listOf()

    @SuppressLint("NotifyDataSetChanged")
    fun updateListSessionHistory(listSessionHistory: List<SessionHistory>){
        this.listSessionHistory = listSessionHistory
        notifyDataSetChanged()
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateLocalListSessionHistory(localListSessionHistory: List<RiderSessionEntity>){
        this.localListSessionHistory = localListSessionHistory
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionsHistoryViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        return SessionsHistoryViewHolder(
            ItemSessionBinding.inflate(inflate, parent, false),
            listener
        )
    }

    override fun getItemCount(): Int {
        if(listSessionHistory.isNullOrEmpty()){
            return 0
        }
        return listSessionHistory.size
    }

    override fun onBindViewHolder(holder: SessionsHistoryViewHolder, position: Int) {
        holder.build(listSessionHistory[position], position, localListSessionHistory)
    }
}
interface ItemButtonClickListener {
    fun onExportSessionReport(riderSessionEntity: RiderSessionEntity)
    fun onUploadLog()
    fun onShowDialog(messages: List<String>)
    fun onCheckDataMissing(sessionId: String)
    fun onItemResentClickListener(position: Int)
}
