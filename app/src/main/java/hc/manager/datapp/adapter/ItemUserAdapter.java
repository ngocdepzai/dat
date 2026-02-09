package hc.manager.datapp.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hc.manager.datapp.R;
import hc.manager.datapp.app.UserItem;
import hc.manager.datapp.utils.BitmapExtension;

public class ItemUserAdapter extends RecyclerView.Adapter<ItemUserAdapter.RecyclerViewHolder> {

    private Context context;
    private List<UserItem> listData;


    public ItemUserAdapter(List<UserItem> list, Context context) {
        this.listData = list;
        this.context = context;
    }

    @Override
    public ItemUserAdapter.RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_user, parent, false);

        return new ItemUserAdapter.RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        final UserItem item = listData.get(position);
        try {
            holder.tvName.setText(item.name);
            holder.tvCode.setText(item.code);
            holder.tvFacetoken.setText(item.faceToken);
            holder.tvIdNo.setText(item.idNo);
            holder.tvUserType.setText(item.userType);
            holder.tvName.setText(item.name);
            if (item.avatarId != null) {
//                String url = "http://hcsky.vn/api/Resource/get_link_image_resize/" + item.avatarId;
//                String url = "http://apidat-test.blackwind.vn/api/Resource/get_link_image_resize/" + item.avatarId;
                String url = context.getResources().getString(R.string.BASE_URL_IMAGE_REAL_RESIZE) + item.avatarId;
                new BitmapExtension(holder.ivAvatarInfo).execute(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public int getItemCount() {
        return listData.size();
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvUserType, tvCode, tvFacetoken, tvIdNo;
        private ImageView ivAvatarInfo;

        public RecyclerViewHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvUserType = (TextView) itemView.findViewById(R.id.tvUserType);
            tvCode = (TextView) itemView.findViewById(R.id.tvCode);
            tvFacetoken = (TextView) itemView.findViewById(R.id.tvFacetoken);
            tvIdNo = (TextView) itemView.findViewById(R.id.tvIdNo);
            ivAvatarInfo = (ImageView) itemView.findViewById(R.id.ivAvatarInfo);
        }
    }
}