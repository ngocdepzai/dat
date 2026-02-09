package hc.manager.datapp.adapter;


import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import hc.manager.datapp.R;
import hc.manager.datapp.models.AuthPictureModel;

public class AuthPictureAdapter extends RecyclerView.Adapter<AuthPictureAdapter.RecyclerViewHolder> {

    private Context context;
    private ArrayList<AuthPictureModel> listData;


    public AuthPictureAdapter(ArrayList<AuthPictureModel> list, Context context) {
        this.listData = list;
        this.context = context;
    }

    @Override
    public AuthPictureAdapter.RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_auth_picture, parent, false);

        return new AuthPictureAdapter.RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final AuthPictureAdapter.RecyclerViewHolder holder, final int position) {
        final AuthPictureModel goodsReceiptMapping = listData.get(position);
        try {
            if (goodsReceiptMapping.getType() == 1) {
                //    holder.tvTime.setTextColor(Color.parseColor("#119E34"));
                holder.tvStatus.setTextColor(Color.parseColor("#119E34"));
                holder.tvStatus.setText("Xác thực thành công");
            } else {

                //    holder.tvTime.setTextColor(Color.parseColor("#D13056"));
                holder.tvStatus.setTextColor(Color.parseColor("#D13056"));
                holder.tvStatus.setText("Xác thực thất bại");
            }
            // holder.tvTime.setText(goodsReceiptMapping.getTime());
            //  holder.tvStatus.setText(goodsReceiptMapping.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTime, tvStatus;

        public RecyclerViewHolder(View itemView) {
            super(itemView);
            // tvTime = (TextView) itemView.findViewById(R.id.tvTime);
            tvStatus = (TextView) itemView.findViewById(R.id.tvStatus);
        }
    }
}
