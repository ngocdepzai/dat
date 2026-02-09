package hc.manager.datapp.camera;

import android.hardware.usb.UsbDevice;
import android.util.Log;

import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;

public class CameraHelperSingleton {
    private static final String TAG = "CameraHelperSingleton";
    private static final boolean DEBUG = true;
    private static CameraHelperSingleton INSTANCE = null;

    private ICameraHelper mCameraHelper;
    private UsbDevice mUsbDevice;

    private final ICameraHelper.StateCallback mStateCallback = new MyCameraHelperCallback();

    public UsbDevice getUsbDevice() {
        return mUsbDevice;
    }

    public void setUsbDevice(UsbDevice usbDevice) {
        mUsbDevice = usbDevice;
    }

    private CameraHelperSingleton() {
        if (DEBUG) Log.v(TAG, "onCancel:device=");
        checkCameraHelper();
    };

    public static synchronized CameraHelperSingleton getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CameraHelperSingleton();
        }
        return(INSTANCE);
    }

    private void checkCameraHelper() {
        if (mUsbDevice != null) {
            clearCameraHelper();
        }
        initCameraHelper();
    }

    private void initCameraHelper() {
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
            mCameraHelper.setStateCallback(mStateCallback);
        }
    }

    private void clearCameraHelper() {
        if (DEBUG) Log.v(TAG, "clearCameraHelper:");
        if (mCameraHelper != null) {
            mCameraHelper.release();
            mCameraHelper = null;
        }
    }

    private class MyCameraHelperCallback implements ICameraHelper.StateCallback {
        @Override
        public void onAttach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onAttach:device=" + device.getDeviceName());
            if (mUsbDevice == null) {
                setUsbDevice(device);
            }
        }

        /**
         * After obtaining USB device permissions, connect the USB camera
         */
        @Override
        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
            if (DEBUG) Log.v(TAG, "onDeviceOpen:device=" + device.getDeviceName());
        }

        @Override
        public void onCameraOpen(UsbDevice device) {}

        @Override
        public void onCameraClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCameraClose:device=" + device.getDeviceName());
        }

        @Override
        public void onDeviceClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDeviceClose:device=" + device.getDeviceName());
        }

        @Override
        public void onDetach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDetach:device=" + device.getDeviceName());
            if (device.equals(mUsbDevice)) {
                setUsbDevice(null);
            }
        }

        @Override
        public void onCancel(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCancel:device=" + device.getDeviceName());
            if (device.equals(mUsbDevice)) {
                setUsbDevice(null);
            }
        }
    }
}
