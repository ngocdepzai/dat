package hc.manager.datapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcapp.ImageCapture;
import com.serenegiant.usb.Size;
import com.serenegiant.widget.AspectRatioSurfaceView;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

import hc.manager.datapp.R;
import hc.manager.datapp.app.UserDataHelper;
import hc.manager.datapp.app.UserItem;
import hc.manager.datapp.camera.CameraHelperSingleton;
import hc.manager.datapp.camera.ShowCamera;
import hc.manager.datapp.models.InOutModel;
import hc.manager.datapp.models.request.StudentLogoutRequest;
import hc.manager.datapp.models.request.TeacherLogoutRequest;
import hc.manager.datapp.models.response.StudentLogoutResponse;
import hc.manager.datapp.models.response.TeacherLogoutResponse;
import hc.manager.datapp.models.response.UploadAuthResponse;
import hc.manager.datapp.service.ApiService;
import hc.manager.datapp.service.DataService;
import hc.manager.datapp.utils.CustomToast;
import hc.manager.datapp.utils.MathUtil;
import hc.manager.datapp.utils.SharedPreferencesUtil;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogoutActivity2 extends AppCompatActivity {
    private static final boolean DEBUG = true;
    private static final String TAG = LogoutActivity2.class.getSimpleName();

    TextView tvFullname, tvBirthDay, tvGender, tvPhoneNumber, tvIdNo, tvAddress, tvTitle;
    Button btSave, btCancel;
    File outputFileLogin;
    DataService dataService = ApiService.getService(this);
    ProgressDialog progressDialog;
    UserDataHelper hcDatDatabase;
    Location myLocation;
    InOutModel inOutModel = new InOutModel(2);
    UserItem userItem;
    int typeLogout = 1;
    String imeiDevice = "";

    //Cam rời
    private ICameraHelper mCameraHelper;
    private AspectRatioSurfaceView mCameraViewMain;

    //Change GPS function
    FusedLocationProviderClient fusedClient;
    private LocationRequest mRequest;
    private LocationCallback mCallback;

    private void showRationale() {
        AlertDialog dialog = new AlertDialog.Builder(this).setMessage("Ứng dụng cần bạn cho phép quyền truy cập vị trí!").setPositiveButton("Đồng ý", (dialogInterface, i) ->
                {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
                    dialogInterface.dismiss();
                })
                .create();
        dialog.show();
    }

    private void requireOpenGPS() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationWizardry();
                } else {
                    Toast.makeText(this, "Ứng dụng cần bạn cho phép truy cập vị trí!", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                break;
        }
    }


    @SuppressLint("MissingPermission")
    private void locationWizardry() {
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        //Initially, get last known location. We can refine this estimate later
        fusedClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
//                    String loc = location.getProvider() + ":Accu:(" + location.getAccuracy() + "). Lat:" + location.getLatitude() + ",Lon:" + location.getLongitude();
                }
            }
        });
        mRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(500)
                .setMaxUpdateDelayMillis(1000)
                .build();

        mCallback = new LocationCallback() {
            //This callback is where we get "streaming" location updates. We can check things like accuracy to determine whether
            //this latest update should replace our previous estimate.
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d("LoginActivity", "locationResult null");
                    myLocation = null;
                    return;
                }
                List<Location> locationList = locationResult.getLocations();
                if (locationList.size() > 0) {
                    Location location = locationList.get(locationList.size() - 1);
                    if (location.hasSpeed()) {
                        double speedTemp = location.getSpeed() * 3.6; //ms => km/h
                        double reSpeed = MathUtil.round(speedTemp, 1, BigDecimal.ROUND_HALF_UP);
                        BigDecimal parseSpeed = new BigDecimal(reSpeed);
                        speedTemp = parseSpeed.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        if (speedTemp >= 3) {
                            Log.d("LoginActivity", "sspeed>3" + speedTemp);
                            myLocation = location;
                        } else {
                            if (null == myLocation) { //lan dau tien chay
                                myLocation = location;
                            }
                            myLocation.setSpeed(0);
                        }
                    } else {
                        if (null != myLocation) {
                            myLocation.setSpeed(0);
                        }
                    }
                } else {
                    if (null != myLocation) {
                        myLocation.setSpeed(0);
                    } else {
                        myLocation = null;
                    }
                }
            }
            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                if (!locationAvailability.isLocationAvailable()) {
                    requireOpenGPS();
                }
                super.onLocationAvailability(locationAvailability);
            }
        };
        fusedClient.requestLocationUpdates(mRequest, mCallback, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout2);
        hcDatDatabase = new UserDataHelper(this);
        imeiDevice = hcDatDatabase.getImeiDevice();
        //permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                showRationale();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE}, 2);
            }
        } else {
            locationWizardry();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = getIntent();
        typeLogout = intent.getIntExtra("TypeLogout", 1);
        InitView();
        if (typeLogout == 1) {
            tvTitle.setText("ĐĂNG XUẤT HỌC VIÊN");
            userItem = SharedPreferencesUtil.getStudent(LogoutActivity2.this);
        } else {
            tvTitle.setText("ĐĂNG XUẤT GIẢNG VIÊN");
            userItem = SharedPreferencesUtil.getTeacher(LogoutActivity2.this);
        }
        ShowInfo();
    }

    public void InitView() {
        this.tvFullname = (TextView) findViewById(R.id.tvFullnameInfo);
        this.tvAddress = (TextView) findViewById(R.id.tvAddressInfo);
        this.tvBirthDay = (TextView) findViewById(R.id.tvBirthDayInfo);
        this.tvGender = (TextView) findViewById(R.id.tvGenderInfo);
        this.tvIdNo = (TextView) findViewById(R.id.tvIdNoInfo);
        this.tvPhoneNumber = (TextView) findViewById(R.id.tvPhoneNumberInfo);
        this.tvTitle = (TextView) findViewById(R.id.tvTitle);
        this.btCancel = (Button) findViewById(R.id.btCancelInfo);
        this.btSave = (Button) findViewById(R.id.btSaveInfo);
        this.btCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if (CameraHelperSingleton.getInstance().getUsbDevice() != null) {
//                    intent = new Intent(LogoutActivity2.this, DashbroadActivity2.class);
                } else {
//                    intent = new Intent(LogoutActivity2.this, DashbroadActivity.class);
                }
                finish();
//                startActivity(intent);
            }
        });

        this.btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    MediaPlayer mPlayer = MediaPlayer.create(LogoutActivity2.this, R.raw.start);
                    mPlayer.start();
                    progressDialog = new ProgressDialog(LogoutActivity2.this);
                    progressDialog.setTitle(R.string.please_wait);
                    progressDialog.setCancelable(true);
                    progressDialog.show();
                    // chup anh luc đăng xuat
                    if (mCameraHelper != null) {
                        String state = Environment.getExternalStorageState();
                        if (state.equals(Environment.MEDIA_MOUNTED)) {
                            outputFileLogin = getOutputMediaFile();
                            if (outputFileLogin == null) {
                                return;
                            }
                            ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(outputFileLogin).build();
                            mCameraHelper.takePicture(options, new ImageCapture.OnImageCaptureCallback() {
                                @Override
                                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                                    SendDataToServer();
                                }
                                @Override
                                public void onError(int imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                                    Toast.makeText(LogoutActivity2.this, message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        CustomToast.makeText(LogoutActivity2.this, "Camera lỗi!", 1000, 1).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        //Khởi tạo camera
        //Cam rời
        mCameraViewMain = findViewById(R.id.svCameraLogout);
        mCameraViewMain.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (mCameraHelper != null) {
                    mCameraHelper.addSurface(holder.getSurface(), false);
                }
            }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (mCameraHelper != null) {
                    mCameraHelper.removeSurface(holder.getSurface());
                }
            }
        });

    }

    private void ShowInfo() {
        tvFullname.setText(userItem.name);
        tvPhoneNumber.setText(userItem.phoneNumber);
        tvIdNo.setText(userItem.code);
        tvGender.setText(userItem.gender);
        tvBirthDay.setText(userItem.birthDay);
        tvAddress.setText(userItem.address);
    }

    public void SendDataToServer() {
        if (outputFileLogin == null) return;
        inOutModel.FilePathLocal = outputFileLogin.getPath();
        inOutModel.Seri = imeiDevice;
        Intent intent = getIntent();
        if (myLocation != null) {
            inOutModel.Lat = myLocation.getLatitude();
            inOutModel.Lng = myLocation.getLongitude();
        } else {
            double lat = intent.getDoubleExtra("Lat", 0);
            double lng = intent.getDoubleExtra("Lng", 0);
            inOutModel.Lat = lat;
            inOutModel.Lng = lng;
        }
        inOutModel.UserId = userItem.userid;
        inOutModel.UserCode = userItem.code;
        inOutModel.Name = userItem.name;
        inOutModel.Sent = 0;
        inOutModel.Dis = 0;
        if (typeLogout == 1) {
            inOutModel.UserType = 1;
        } else {
            inOutModel.UserType = 2;
        }
        if (CheckNetworkConnection()) {
            hcDatDatabase = new UserDataHelper(this);
            Call<UploadAuthResponse> callback;
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", outputFileLogin.getName(), RequestBody.create(MediaType.parse("image/*"), outputFileLogin));
            RequestBody userCodeBody = RequestBody.create(MediaType.parse("text/plain"),
                    userItem.code);
            RequestBody seriBody = RequestBody.create(MediaType.parse("text/plain"),
                    imeiDevice);
            callback = dataService.UploadAuth(filePart, userCodeBody, seriBody);
            callback.enqueue(new Callback<UploadAuthResponse>() {
                @Override
                public void onResponse(Call<UploadAuthResponse> call, Response<UploadAuthResponse> responseFile) {
                    if (responseFile.body() != null) {
                        if (responseFile.body().status == 1) {
                            inOutModel.FilePath = responseFile.body().getFilePath();
                            if (typeLogout == 1) {
                                StudentLogout();
                            } else {
                                TeacherLogout();
                            }
                        } else {
                            Toast.makeText(LogoutActivity2.this, responseFile.body().message, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LogoutActivity2.this, "Lỗi kết nối server!", Toast.LENGTH_SHORT).show();
                    }
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }

                @Override
                public void onFailure(Call<UploadAuthResponse> call, Throwable t) {
                    CustomToast.makeText(LogoutActivity2.this, "Kết nối mạng kém 2!", Toast.LENGTH_LONG, 1).show();
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }
            });
        } else {
            CustomToast.makeText(LogoutActivity2.this, "Vui lòng tạo kiểm tra kết nối mạng!", Toast.LENGTH_LONG, 2).show();
            MediaPlayer mPlayer = MediaPlayer.create(LogoutActivity2.this, R.raw.warning);
            mPlayer.start();
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }

    }

    private void StudentLogout() {
        Call<StudentLogoutResponse> callback;
        StudentLogoutRequest studentLogoutRequest = new StudentLogoutRequest();
        studentLogoutRequest.setSeri(imeiDevice);
        studentLogoutRequest.setCode(userItem.code);
        studentLogoutRequest.setLat(inOutModel.getLat());
        studentLogoutRequest.setLng(inOutModel.getLng());
        studentLogoutRequest.setPath(inOutModel.getFilePath());
        studentLogoutRequest.setTime(inOutModel.getTime());
        String sessionId = SharedPreferencesUtil.getSession(LogoutActivity2.this);
        if (sessionId != null && sessionId != "") {
            studentLogoutRequest.setSessionId(sessionId);
        } else {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            SharedPreferencesUtil.removeStudent(LogoutActivity2.this);
            SharedPreferencesUtil.removeSession(LogoutActivity2.this);
            CustomToast.makeText(LogoutActivity2.this, "Đăng xuất học viên thành công!", 1000, 1).show();
            GoToLogin();
            return;
        }
        callback = dataService.StudentLogout(studentLogoutRequest);
        callback.enqueue(new Callback<StudentLogoutResponse>() {
            @Override
            public void onResponse(Call<StudentLogoutResponse> call, Response<StudentLogoutResponse> response) {
                if (response.body() != null) {
                    if (response.body().status == 1) {
                        SharedPreferencesUtil.removeStudent(LogoutActivity2.this);
                        SharedPreferencesUtil.removeSession(LogoutActivity2.this);
                        CustomToast.makeText(LogoutActivity2.this, "Đăng xuất học viên thành công!", 1000, 1).show();
                        GoToLogin();
                        return;
                    } else {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        if (response.body().getMessage() != null) {
                            if (response.body().message.equals("Không tồn tại học viên trên hệ thống")) {
                                SharedPreferencesUtil.removeStudent(LogoutActivity2.this);
                                SharedPreferencesUtil.removeSession(LogoutActivity2.this);
                                CustomToast.makeText(LogoutActivity2.this, "Đăng xuất học viên thành công!", 1000, 1).show();
                                GoToLogin();
                                return;
                            } else {
                                CustomToast.makeText(LogoutActivity2.this, response.body().getMessage(),
                                        Toast.LENGTH_SHORT, 3).show();
                            }
                        } else {
                            CustomToast.makeText(LogoutActivity2.this, "Kết nối server lỗi!",
                                    Toast.LENGTH_SHORT, 3).show();
                        }
                    }
                } else {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    CustomToast.makeText(LogoutActivity2.this, "Kết nối server lỗi!", Toast.LENGTH_SHORT, 3).show();
                }
            }

            @Override
            public void onFailure(Call<StudentLogoutResponse> call, Throwable t) {
                CustomToast.makeText(LogoutActivity2.this, "Kết nối server lỗi!",
                        Toast.LENGTH_SHORT, 3).show();
            }
        });
    }

    private void TeacherLogout() {
        Call<TeacherLogoutResponse> callback;
        TeacherLogoutRequest teacherLogoutRequest = new TeacherLogoutRequest();
        teacherLogoutRequest.setSeri(imeiDevice);
        teacherLogoutRequest.setCode(userItem.code);
        teacherLogoutRequest.setLat(inOutModel.getLat());
        teacherLogoutRequest.setLng(inOutModel.getLng());
        teacherLogoutRequest.setPath(inOutModel.getFilePath());
        teacherLogoutRequest.setTime(inOutModel.getTime());
        callback = dataService.TeacherLogout(teacherLogoutRequest);
        callback.enqueue(new Callback<TeacherLogoutResponse>() {
            @Override
            public void onResponse(Call<TeacherLogoutResponse> call, Response<TeacherLogoutResponse> response) {
                if (response.body() != null) {
                    if (response.body().status == 1) {
                        CustomToast.makeText(LogoutActivity2.this, "Đăng xuất giảng viên thành công!", 1000, 1).show();
                        SharedPreferencesUtil.removeTeacher(LogoutActivity2.this);
                        GoToLogin();
                    } else {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        if (response.body().getMessage() != null) {
                            if (response.body().message.equals("Giảng viên không tồn tại trên hệ thống")) {
                                CustomToast.makeText(LogoutActivity2.this, "Đăng xuất giảng viên thành công!", 1000, 1).show();
                                SharedPreferencesUtil.removeTeacher(LogoutActivity2.this);
                                GoToLogin();
                            } else {
                                CustomToast.makeText(LogoutActivity2.this, response.body().getMessage(),
                                        Toast.LENGTH_SHORT, 3).show();
                            }
                        } else {
                            CustomToast.makeText(LogoutActivity2.this, "Kết nối server lỗi!",
                                    Toast.LENGTH_SHORT, 3).show();
                        }
                    }

                } else {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    CustomToast.makeText(LogoutActivity2.this, "Lỗi kết nối", Toast.LENGTH_SHORT, 3).show();
                }
            }

            @Override
            public void onFailure(Call<TeacherLogoutResponse> call, Throwable t) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        });
    }

    private void GoToLogin() {
        try {
            Intent intent = new Intent(LogoutActivity2.this, LoginActivity.class);
            startActivity(intent);
        } finally {
            finish();
        }
    }

    private File getOutputMediaFile() {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            return null;
        } else {
            File folder_gus = new File(Environment.getExternalStorageDirectory() + "/HC_DAT_IMAGES");
            if (!folder_gus.exists()) {
                folder_gus.mkdirs();
            }
            File outputFile = new File(folder_gus, (System.currentTimeMillis() / 1000) + ".png");
            return outputFile;
        }
    }

    private boolean CheckNetworkConnection() {
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            connected = true;
        } else
            connected = false;
        return connected;
    }

    //Camera roi
    protected void onStart() {
        super.onStart();
        //UVC CAMERA
        initCameraHelper();
    }

    public void initCameraHelper() {
        Log.i(TAG, "initCameraHelper");
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
            mCameraHelper.setStateCallback(mStateListener);
        }
    }

    private void selectDevice(final UsbDevice device) {
        if (DEBUG) Log.v(TAG, "selectDevice:device=" + device.getDeviceName());
        mCameraHelper.selectDevice(device);
    }

    private final ICameraHelper.StateCallback mStateListener = new ICameraHelper.StateCallback() {
        @Override
        public void onAttach(UsbDevice device) {
            if (DEBUG) Log.i(TAG, "onAttach");
            selectDevice(device);
        }

        @Override
        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
            if (DEBUG) Log.v(TAG, "onDeviceOpen:"); //cho nay bi loi
            mCameraHelper.openCamera();
        }

        @Override
        public void onCameraOpen(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCameraOpen:");

            mCameraHelper.startPreview();

            Size size = mCameraHelper.getPreviewSize();

            if (size != null) {
                int width = size.width;
                int height = size.height;
                //auto aspect ratio
                mCameraViewMain.setAspectRatio(width, height);
            }
            mCameraHelper.addSurface(mCameraViewMain.getHolder().getSurface(), false);
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCameraClose:");
            if (mCameraHelper != null) {
                mCameraHelper.removeSurface(mCameraViewMain.getHolder().getSurface());
            }
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
    protected void onStop() {
        super.onStop();
        //Camera rời
        clearCameraHelper();
    }

    //Camera rời
    private void clearCameraHelper() {
        if (DEBUG) Log.d(TAG, "clearCameraHelper:");
        if (mCameraHelper != null) {
            mCameraHelper.release();
            mCameraHelper = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("MissingPermission")
    protected void startLocationUpdates() {
        if (null != fusedClient) {
            fusedClient.requestLocationUpdates(mRequest, mCallback, null);
        }
    }

    @Override
    protected void onPause() {
        Log.d("LoginActivity", "removeLocationUpdates is ");
        if (null != fusedClient) {
            fusedClient.removeLocationUpdates(mCallback);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

}