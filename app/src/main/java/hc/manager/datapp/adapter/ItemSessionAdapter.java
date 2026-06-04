package hc.manager.datapp.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hc.manager.datapp.R;
import hc.manager.datapp.models.SessionModel;
import hc.manager.datapp.utils.DateUtil;

public class ItemSessionAdapter extends RecyclerView.Adapter<ItemSessionAdapter.RecyclerViewHolder> {

    private Context context;
    private List<SessionModel> listData;
    private ItemButtonClickListener mItemButtonClickListener;

    public ItemSessionAdapter(List<SessionModel> list, Context context) {
        this.listData = list;
        this.context = context;
    }

    public void setOnItemButtonClickListener(ItemButtonClickListener listener) {
        mItemButtonClickListener = listener;
    }

    @Override
    public ItemSessionAdapter.RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_session, parent, false);

        return new ItemSessionAdapter.RecyclerViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ItemSessionAdapter.RecyclerViewHolder holder, int position) {
        final SessionModel item = listData.get(position);
        try {
            if (item.id != null && !item.id.isEmpty()) {
                holder.tvNo.setText(String.valueOf(position + 1));
                holder.tvStudentName.setText(item.studentName);
                holder.tvId.setText(item.id);
                holder.tvTotalTime.setText(DateUtil.ConvertHms(item.totalTime));
                holder.tvTotalDis.setText(String.valueOf(item.totalDis));
                holder.tvLoginDate.setText(item.loginDateParse);
                holder.tvLogoutDate.setText(item.logoutDateParse);

                if (!item.sentGeneral) {
                    holder.tvStatus.setText("Chưa truyền Tc");
                    holder.tvStatus.setTextColor(Color.RED);
                    holder.ivResentTC.setVisibility(View.VISIBLE);
                    holder.ivResentTC.setImageResource(R.drawable.resenttc);
//                    Bitmap bImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.resenttc);
//                    holder.ivResentTC.setImageBitmap(bImage);
                } else {
                    holder.tvStatus.setText("Đã truyền Tc");
                    holder.tvStatus.setTextColor(Color.GREEN);
                    holder.ivResentTC.setVisibility(View.INVISIBLE);
                }
                holder.ivResentTC.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mItemButtonClickListener != null) {
                            mItemButtonClickListener.onItemResentClickListener(position);
                        }
                    }
                });
            } else {

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public int getItemCount() {
        return listData.size();
    }

    public void clear() {
        listData.clear();
    }

    public interface ItemButtonClickListener {

        void onItemResentClickListener(int position);

    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNo, tvStudentName, tvId, tvTotalTime, tvTotalDis, tvStatus, tvLoginDate, tvLogoutDate;
        private ImageView ivResentTC;

        public RecyclerViewHolder(View itemView) {
            super(itemView);
            tvNo = (TextView) itemView.findViewById(R.id.tvNo);
            tvStudentName = (TextView) itemView.findViewById(R.id.tvStudentName);
            tvId = (TextView) itemView.findViewById(R.id.tvId);
            tvTotalTime = (TextView) itemView.findViewById(R.id.tvTotalTime);
            tvTotalDis = (TextView) itemView.findViewById(R.id.tvTotalDis);
            tvStatus = (TextView) itemView.findViewById(R.id.tvStatus);
            tvLoginDate = (TextView) itemView.findViewById(R.id.tvLoginDate);
            tvLogoutDate = (TextView) itemView.findViewById(R.id.tvLogoutDate);
            ivResentTC = (ImageView) itemView.findViewById(R.id.ivResentTC);
        }
    }
}
