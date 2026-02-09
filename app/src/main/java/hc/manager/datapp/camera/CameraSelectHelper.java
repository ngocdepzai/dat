package hc.manager.datapp.camera;

import android.hardware.usb.UsbDevice;
import android.util.Log;

import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;

public class CameraSelectHelper {
    private static final String TAG = "CameraSelectHelper";
    private static final boolean DEBUG = true;
    private ICameraHelper mCameraHelper;

    private boolean isHasUVCCamera = false;

    public boolean getUVCCamera() {
        return isHasUVCCamera;
    }

    public void setUVCCamera(Boolean isHasUVC) {
        isHasUVCCamera = isHasUVC;
    }

    public CameraSelectHelper() {
        initCameraHelper();
    }

    public void destroyHelper() {
        if (DEBUG) Log.v(TAG, "clearCameraHelper:");
        if (mCameraHelper != null) {
            mCameraHelper.release();
            mCameraHelper = null;
            isHasUVCCamera = false;
        }
    }

    private final ICameraHelper.StateCallback mStateListener = new ICameraHelper.StateCallback() {
        @Override
        public void onAttach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onAttach:");
            setUVCCamera(true);
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

    private void initCameraHelper() {
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
            mCameraHelper.setStateCallback(mStateListener);
        }
    }
}
