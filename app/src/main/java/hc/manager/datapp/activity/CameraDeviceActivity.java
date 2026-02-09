package hc.manager.datapp.activity;

import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;

import hc.manager.datapp.camera.CameraSelectHelper;

public class CameraDeviceActivity extends AppCompatActivity {

    private static final String TAG = CameraDeviceActivity.class.getSimpleName();
    private ICameraHelper mCameraHelper;
    private static final boolean DEBUG = true;

    private boolean isHasUVCCamera = false;
    CameraSelectHelper v;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       v = new CameraSelectHelper();

    }

    @Override
    protected void onStart() {
        super.onStart();
        initCameraHelper();
    }

    private final ICameraHelper.StateCallback mStateListener = new ICameraHelper.StateCallback() {
        @Override
        public void onAttach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onAttach:");
            isHasUVCCamera = true;
        }

        @Override
        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
            if (DEBUG) Log.v(TAG, "onDeviceOpen:");
        }

        @Override
        public void onCameraOpen(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCameraOpen:");
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCameraClose:");
        }

        @Override
        public void onDeviceClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDeviceClose:");
        }

        @Override
        public void onDetach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDetach:");
        }

        @Override
        public void onCancel(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCancel:");
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearCameraHelper();
        v.destroyHelper();
    }

    private void initCameraHelper() {
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
            mCameraHelper.setStateCallback(mStateListener);
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    if (DEBUG) Log.v(TAG, "onAttach: isHasUVCCamera " + isHasUVCCamera);
                    if (isHasUVCCamera) {
                        try {
//                            Intent intent = new Intent(CameraDeviceActivity.this, FaceActivity2.class);
//                            startActivity(intent);
                        } finally {
                            finish();
                        }
                    } else {
                        try {
//                            Intent intent = new Intent(CameraDeviceActivity.this, FaceActivity.class);
//                            startActivity(intent);
                        } finally {
                            finish();
                        }
                    }
                }
            }, 200);
        }
    }

    private void clearCameraHelper() {
        if (DEBUG) Log.v(TAG, "clearCameraHelper:");
        if (mCameraHelper != null) {
            mCameraHelper.release();
            mCameraHelper = null;
        }
    }

}