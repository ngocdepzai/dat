package com.hc.dat.view.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.hc.dat.view.adapter.DialogButtonClickListener
import com.lws.type.Logger
import hc.manager.datapp.databinding.DialogButtonItemBinding

data class DialogButtonViewHolder constructor(
    val viewBinding: DialogButtonItemBinding,
    var listener: DialogButtonClickListener?
) :
    RecyclerView.ViewHolder(viewBinding.root) {

    fun build(name: String, position: Int) {
        viewBinding.dialogButton.text = name

        itemView.setOnClickListener {
            Logger.d("Click on: ${viewBinding.dialogButton.text}")
            listener?.onDialogButtonClick(position)
        }
    }
}
