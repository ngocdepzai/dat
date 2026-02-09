package hc.manager.datapp.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.dcastalia.localappupdate.DownloadApk;

import hc.manager.datapp.R;

public class InAppUpdateActivity extends AppCompatActivity {
    public static int UPDATE_CODE = 5;
    private static String TAG = "UPDATE_APP";
    private static String upadte_url = "";
    private static String apk_fileName = "hc_dat.apk";
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_update);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        inAppUpdate();
    }

    private void inAppUpdate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cập nhật ứng dụng");
        builder.setMessage("Đã có phiên bản HC-DAT mới. Bạn có muốn cập nhật ứng dụng không?");
        builder.setCancelable(false);
        builder.setPositiveButton("ĐỒNG Ý", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (checkPer()) {
                    DownloadApk downloadApk = new DownloadApk(InAppUpdateActivity.this);
                    downloadApk.startDownloadingApk("http://datversion.hcsky.vn/version/dat_update.apk", "hc_dat_app.apk");
                } else {
                    requestPer();
                }
            }
        });
        builder.setNegativeButton("LÚC KHÁC", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog = builder.create();
        dialog.show();
    }

    boolean checkPer() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    void requestPer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 200);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}