package com.hc.dat.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hc.dat.view.viewholder.DialogButtonViewHolder
import hc.manager.datapp.databinding.DialogButtonItemBinding

class DialogButtonAdapter(
    private val list: List<String>,
    var listener: DialogButtonClickListener?
) :
    RecyclerView.Adapter<DialogButtonViewHolder>() {

    override fun onBindViewHolder(holder: DialogButtonViewHolder, position: Int) {
        val item = list[position]
        holder.build(item, position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DialogButtonViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        return DialogButtonViewHolder(
            DialogButtonItemBinding.inflate(inflate, parent, false),
            listener
        )
    }

    override fun getItemCount(): Int = list.size
}

interface DialogButtonClickListener {
    fun onDialogButtonClick(position: Int)
}
