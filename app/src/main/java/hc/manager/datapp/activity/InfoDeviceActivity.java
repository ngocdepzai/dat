package hc.manager.datapp.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import hc.manager.datapp.R;
import hc.manager.datapp.app.UserDataHelper;

public class InfoDeviceActivity extends AppCompatActivity {
    UserDataHelper hcDatDatabase;
    private Button btUpdateApp;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_device);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        TextView tvSeries = (TextView) findViewById(R.id.tvSeri);
        btUpdateApp = (Button) findViewById(R.id.btUpdateApp);
        btUpdateApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(InfoDeviceActivity.this, InAppUpdateActivity.class);
                    startActivity(intent);
                } finally {
                    finish();
                }
            }
        });
        hcDatDatabase = new UserDataHelper(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            String series = hcDatDatabase.getImeiDevice();
            tvSeries.setText(series);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}