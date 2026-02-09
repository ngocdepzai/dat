package hc.manager.datapp.activity;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import hc.manager.datapp.camera.CameraHelperSingleton;

public class BootActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        CameraHelperSingleton.getInstance();
        try {
//            startActivity(new Intent(BootActivity.this, SyncDataActivity.class));
        } finally {
            finish();
        }
    }
}