package hc.manager.datapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hc.manager.datapp.R;
import hc.manager.datapp.adapter.ItemUserAdapter;
import hc.manager.datapp.app.UserDataHelper;
import hc.manager.datapp.app.UserItem;
import hc.manager.datapp.utils.CustomToast;

public class ListUserActivity extends AppCompatActivity {
    UserDataHelper hcDatDatabase;
    private ItemUserAdapter itemUserAdapter;
    private List<UserItem> listUsers = new ArrayList<>();
    private RecyclerView lvUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_user);
        hcDatDatabase = new UserDataHelper(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hcDatDatabase.LoadAll();
        if (listUsers.size() > 0) {
            listUsers = hcDatDatabase.usersList;
            this.lvUser = (RecyclerView) findViewById(R.id.lvUser);
            lvUser.setHasFixedSize(true);
            RecyclerView.LayoutManager layoutManager2 = new LinearLayoutManager(this);
            lvUser.setLayoutManager(layoutManager2);
            lvUser.setNestedScrollingEnabled(false);
            itemUserAdapter = new ItemUserAdapter(listUsers, ListUserActivity.this);
            lvUser.setAdapter(itemUserAdapter);
            lvUser.scrollToPosition(listUsers.size() - 1);
        } else {
            CustomToast.makeText(ListUserActivity.this, "Không có dữ liệu!", Toast.LENGTH_SHORT, 1).show();
        }
    }

    @Override
    public void onBackPressed() {
        try {
            Intent intent = new Intent(ListUserActivity.this, MenuActivity.class);
//            overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
            startActivity(intent);
        } finally {
            finish();
        }
    }
}