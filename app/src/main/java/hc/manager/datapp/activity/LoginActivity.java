package hc.manager.datapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import hc.manager.datapp.R;
import hc.manager.datapp.app.ReceiveAvatarList;
import hc.manager.datapp.app.UserDataHelper;
import hc.manager.datapp.app.UserItem;
import hc.manager.datapp.camera.CameraHelperSingleton;
import hc.manager.datapp.models.AuthModel;
import hc.manager.datapp.models.DeviceModel;
import hc.manager.datapp.models.GpsModel;
import hc.manager.datapp.models.InOutModel;
import hc.manager.datapp.models.ResetDeviceModel;
import hc.manager.datapp.models.TrainingCenterModel;
import hc.manager.datapp.models.request.CreateUpdateUserDeviceRequest;
import hc.manager.datapp.models.request.GetDeviceBySeriRequest;
import hc.manager.datapp.models.request.GetResetDeviceRequest;
import hc.manager.datapp.models.request.GetUpdateUserBySeriRequest;
import hc.manager.datapp.models.response.CreateUpdateUserDeviceResponse;
import hc.manager.datapp.models.response.GetDeviceBySeriResponse;
import hc.manager.datapp.models.response.GetResetDeviceResponse;
import hc.manager.datapp.models.response.GetUpdateUserBySeriResponse;
import hc.manager.datapp.models.response.UploadAuthResponse;
import hc.manager.datapp.service.ApiService;
import hc.manager.datapp.service.DataService;
import hc.manager.datapp.service.ScreenOnOffBroadCastReciever;
import hc.manager.datapp.service.Sender;
import hc.manager.datapp.service.SenderAuth;
import hc.manager.datapp.service.SenderInOut;
import hc.manager.datapp.utils.CustomToast;
import hc.manager.datapp.utils.MathUtil;
import hc.manager.datapp.utils.SharedPreferencesUtil;
import hc.manager.datapp.utils.UpdateUserType;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    protected Context context;
    UserItem student, teacher;
    Location myLocation;
    TextView tvManager, tvTitle, tvMain;
    DataService dataService = ApiService.getService(this);
    UserDataHelper hcDatDatabase;
    ProgressDialog progressDialog;
    boolean isResentData = true;
    String imeiDevice = "";
    private Button btCard, btCamera, btLogOutStudent, btLogOutTeacher, btCompare;
    DeviceModel deviceInfo;
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
//                    Toast.makeText(this, "Cám ơn!", Toast.LENGTH_SHORT).show();
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
                        if (speedTemp >= 3.8) {
                            Log.d("LoginActivity", "sspeed>3" + speedTemp);
                            myLocation = location;
                        } else {
                            if (null != myLocation) {
                                myLocation.setSpeed(0);
                            } else { //lan dau tien chay
                                myLocation = location;
                                myLocation.setSpeed(0);
                            }
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
        setContentView(R.layout.activity_login);
        hcDatDatabase = new UserDataHelper(this);
        imeiDevice = hcDatDatabase.getImeiDevice();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        CheckFolderExisr();

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

        InitView();
        CheckShowLogout();
        ReceiveAvatarList.getInstance().LoadAll();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Intent intent = getIntent();
        Boolean isLogOut = intent.getBooleanExtra("LogOut", false);
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver mReceiver = new ScreenOnOffBroadCastReciever();
        registerReceiver(mReceiver, intentFilter);
        if (!isLogOut) {
            tvTitle.setText("Đăng nhập");
        } else {
            tvTitle.setText("Đăng xuất");
        }
        //kiem tra thong tin thiet bi
        this.deviceInfo = SharedPreferencesUtil.getDevice(LoginActivity.this);
        if (null == deviceInfo) {
            if (CheckNetworkConnection()) {
                initDevice();
                getResetDevice();
            } else {
                CustomToast.makeText(LoginActivity.this, "Kiểm tra kết nối mạng!", 5000, 2).show();
                clearDialog();
            }
        }

        btCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check vận tốc
                if (CheckCanLogin()) {
                    try {
                        Intent intent;
                        if (CameraHelperSingleton.getInstance().getUsbDevice() != null) {
//                            intent = new Intent(LoginActivity.this, FaceActivity2.class);
                        } else {
//                            intent = new Intent(LoginActivity.this, FaceActivity.class);
                        }
                        if (myLocation != null) {
//                            intent.putExtra("Lat", myLocation.getLatitude());
//                            intent.putExtra("Lng", myLocation.getLongitude());
                        }
//                        startActivity(intent);
                    } finally {
                        finish();
                    }
                }
            }
        });

        btCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CheckCanLogin()) {
                    try {
                        Intent intent;
                        if (CameraHelperSingleton.getInstance().getUsbDevice() != null) {
                            intent = new Intent(LoginActivity.this, CardActivity2.class);
                        } else {
                            intent = new Intent(LoginActivity.this, CardActivity.class);
                        }
                        if (myLocation != null) {
                            intent.putExtra("Lat", myLocation.getLatitude());
                            intent.putExtra("Lng", myLocation.getLongitude());
                        }
                        startActivity(intent);
                    } finally {
                        finish();
                    }
                }
            }
        });

        btCompare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(LoginActivity.this, CompareSession.class);
                    startActivity(intent);
                } finally {
                    finish();
                }
            }
        });
        tvManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent(LoginActivity.this, MenuActivity.class);
                    startActivity(intent);
                } finally {
                    finish();
                }
            }
        });
        tvMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GoToMain();
            }
        });
        ResentData();
        //bo sung canh bao xoa bo nho
        checkMemoryOverload();
    }

    public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    public void checkMemoryOverload() {
        if (externalMemoryAvailable()) {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long totalSize = totalBlocks * blockSize;
            File pathUsed = Environment.getExternalStorageDirectory();
            StatFs statUsed = new StatFs(pathUsed.getPath());
            long availableSpace = (long) statUsed.getBlockSizeLong() * (long) statUsed.getAvailableBlocksLong();
            if (availableSpace <= totalSize/1.5) {
                //canh bao xoa bot du lieu
                new AlertDialog.Builder(LoginActivity.this)
                        .setMessage("Bộ nhớ ứng dụng gần đầy, vui lòng xoá bớt để đảm bảo ứng dụng hoạt động hiệu quả!")
                        .setPositiveButton("Đi đến Menu", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                try {
                                    Intent intentMenu = new Intent(LoginActivity.this, MenuActivity.class);
                                    startActivity(intentMenu);
                                } finally {
                                    finish();
                                }
                            }
                        })
                        .setNegativeButton("Hủy bỏ", null)
                        .show();
            }
        }
    }

    public void ResentData() {
        try {
            if (CheckNetworkConnection()) {
                ArrayList<AuthModel> resentList = hcDatDatabase.LoadDataResentAuth();
                if (resentList != null && resentList.size() > 0) {
                    for (int i = 0; i < resentList.size(); i++) {
                        AuthModel authModel = resentList.get(i);
                        if (CheckNetworkConnection() && authModel.getFilePathLocal() != null) {
                            File file = new File(authModel.getFilePathLocal());
                            Call<UploadAuthResponse> callback;
                            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), RequestBody.create(MediaType.parse("image/*"), file));
                            RequestBody userCodeBody = RequestBody.create(MediaType.parse("text/plain"),
                                    authModel.UserCode);
                            @SuppressLint("MissingPermission") RequestBody seriBody = RequestBody.create(MediaType.parse("text/plain"),
                                    imeiDevice);
                            callback = dataService.UploadAuth(filePart, userCodeBody, seriBody);

                            callback.enqueue(new Callback<UploadAuthResponse>() {
                                @Override
                                public void onResponse(Call<UploadAuthResponse> call, Response<UploadAuthResponse> response) {
                                    if (response.body() != null) {
                                        if (response.body().status == 1) {
                                            authModel.FilePath = response.body().getFilePath();
                                            try {
                                                if (CheckNetworkConnection()) {
                                                    Boolean rabbitMq = new SenderAuth(authModel).execute().get();
                                                    if (rabbitMq) {
                                                        hcDatDatabase.UpdateResentAuth(authModel.Time);
                                                    }
                                                }
                                            } catch (ExecutionException e) {
                                                e.printStackTrace();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            Toast.makeText(LoginActivity.this, response.body().message, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<UploadAuthResponse> call, Throwable t) {
                                }
                            });
                        } else {
                            hcDatDatabase.AppendAuth(authModel);
                        }
                    }
                }
                ArrayList<GpsModel> gpsResent = hcDatDatabase.LoadDataResentGps();
                if (gpsResent.size() > 0) {
                    for (int i = 0; i < gpsResent.size(); i++) {
                        GpsModel gpsModel = gpsResent.get(i);
                        Boolean rabbitMq = new Sender(gpsModel).execute().get();
                        if (rabbitMq) {
                            hcDatDatabase.UpdateResentGps(gpsModel.Time);
                        }
                    }
                }
                ArrayList<InOutModel> attendanceResent = hcDatDatabase.LoadResentDataAttendance();
                if (attendanceResent.size() > 0) {
                    for (int i = 0; i < attendanceResent.size(); i++) {
                        if (CheckNetworkConnection()) {
                            InOutModel inOutModel = attendanceResent.get(i);
                            File outputFileLogin = new File((inOutModel.FilePathLocal));
                            Call<UploadAuthResponse> callback;
                            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", outputFileLogin.getName(), RequestBody.create(MediaType.parse("image/*"), outputFileLogin));
                            RequestBody userCodeBody = RequestBody.create(MediaType.parse("text/plain"),
                                    inOutModel.UserCode);
                            @SuppressLint("MissingPermission") RequestBody seriBody = RequestBody.create(MediaType.parse("text/plain"),
                                    imeiDevice);
                            callback = dataService.UploadAuth(filePart, userCodeBody, seriBody);
                            callback.enqueue(new Callback<UploadAuthResponse>() {
                                @Override
                                public void onResponse(Call<UploadAuthResponse> call, Response<UploadAuthResponse> responseFile) {
                                    if (responseFile.body() != null) {
                                        if (responseFile.body().status == 1) {
                                            inOutModel.FilePath = responseFile.body().getFilePath();
                                            // pushlish lên rabbit
                                            try {
                                                Boolean rabbitMq = new SenderInOut(inOutModel).execute().get();
                                                if (rabbitMq) {
                                                    hcDatDatabase.UpdateResentAttendance(inOutModel.Time);
                                                }
                                            } catch (Exception ee) {
                                                ee.printStackTrace();
                                            }
                                        }
                                    }
                                }
                                @Override
                                public void onFailure(Call<UploadAuthResponse> call, Throwable t) {
                                }
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("RabbitMQ ", e.getMessage());
        }
        isResentData = false;
    }

    private boolean CheckCanLogin() {
        if (myLocation != null && myLocation.getSpeed() > 3) {
            CustomToast.makeText(LoginActivity.this, "Không thể đăng nhập khi xe đang chạy!", 5000, 1).show();
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    protected void startLocationUpdates() {
        if (null != fusedClient) {
            fusedClient.requestLocationUpdates(mRequest, mCallback, null);
        }
    }

    @Override
    protected void onPause() {
        if (null != fusedClient) {
            fusedClient.removeLocationUpdates(mCallback);
        }
        super.onPause();
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

    private void CheckFolderExisr() {
        File f = new File(Environment.getExternalStorageDirectory() + "/HC_DAT");
        if (!f.exists()) {
            f.mkdir();
        }
        File f2 = new File(Environment.getExternalStorageDirectory() + "/HC_DAT_BACKUP");
        if (!f2.exists()) {
            f2.mkdir();
        }
        File f3 = new File(Environment.getExternalStorageDirectory() + "/HC_DAT_IMAGES");
        if (!f3.exists()) {
            f3.mkdir();
        }
    }

    private void GoToMain() {
        try {
            UsbDevice usb = CameraHelperSingleton.getInstance().getUsbDevice();
            Intent intent;
            if (usb != null) {
//                intent = new Intent(LoginActivity.this, DashbroadActivity2.class);
            } else {
//                intent = new Intent(LoginActivity.this, DashbroadActivity.class);
            }
//            startActivity(intent);
        } finally {
            finish();
        }
    }

    public void CheckShowLogout() {
        student = SharedPreferencesUtil.getStudent(LoginActivity.this);
        teacher = SharedPreferencesUtil.getTeacher(LoginActivity.this);
        if (student == null) {
            this.btLogOutStudent.setVisibility(View.GONE);
        }
        if (teacher == null) {
            this.btLogOutTeacher.setVisibility(View.GONE);
        }
    }

    private void InitView() {
        btCamera = (Button) findViewById(R.id.btLogin_face);
        this.tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvManager = (TextView) findViewById(R.id.tvManager);
        tvMain = (TextView) findViewById(R.id.tvMain);
        btCard = (Button) findViewById(R.id.btLogin_rfid);
        btCompare = (Button) findViewById(R.id.btCompare);
        this.btLogOutStudent = (Button) findViewById(R.id.btLogOutStudent);
        this.btLogOutTeacher = (Button) findViewById(R.id.btLogOutTeacher);
        this.btLogOutStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogoutUser(1);
            }
        });
        this.btLogOutTeacher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogoutUser(2);
            }
        });
    }

    private void LogoutUser(int type) {
        try {
            Intent intent;
            if (CameraHelperSingleton.getInstance().getUsbDevice() != null) {
                intent = new Intent(LoginActivity.this, LogoutActivity2.class);
            } else {
                intent = new Intent(LoginActivity.this, LogoutActivity.class);
            }
            intent.putExtra("TypeLogout", type);
            startActivity(intent);
        } finally {
            finish();
        }
    }

    private void getResetDevice() {
        // kiểm tra xem thiết bị có nhận lệnh reset lại csdl nào hay k.
        // nếu không có thì update finger cho những user nào mới được cập nhật
        Call<GetResetDeviceResponse> callback;
        GetResetDeviceRequest getResetDeviceRequest;
        getResetDeviceRequest = new GetResetDeviceRequest(imeiDevice);
        callback = dataService.GetResetDevice(getResetDeviceRequest);
        callback.enqueue(new Callback<GetResetDeviceResponse>() {
            @Override
            public void onResponse(Call<GetResetDeviceResponse> call, Response<GetResetDeviceResponse> response) {
                if (response.body() != null) {
                    if (response.body().status == 1) {
                        ResetDeviceModel resetDeviceModel = response.body().getResetDevice();
                        if (resetDeviceModel != null && resetDeviceModel.resetType == 4) {
                            try {
//                                Intent intent = new Intent(LoginActivity.this, ChangeTrainingCenterActivity.class);
//                                startActivity(intent);
                            } finally {
                                finish();
                            }
                        } else {
                            initFinger();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, response.body().message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, R.string.err, Toast.LENGTH_SHORT).show();
                    try {
                        Intent intent = new Intent(LoginActivity.this, InfoDeviceActivity.class);
                        startActivity(intent);
                    } finally {
                        finish();
                    }
                    clearDialog();
                }
            }

            @Override
            public void onFailure(Call<GetResetDeviceResponse> call, Throwable t) {
                clearDialog();
            }
        });
    }

    private void initDevice() {
        // lấy thông tin của thiết bị bao gồm seri, vị trí lắp đặt, thuộc trung tâm nào
        Call<GetDeviceBySeriResponse> callback;
        GetDeviceBySeriRequest getDeviceBySeriRequest;
        getDeviceBySeriRequest = new GetDeviceBySeriRequest(imeiDevice);
        callback = dataService.GetDeviceBySeri(getDeviceBySeriRequest);
        callback.enqueue(new Callback<GetDeviceBySeriResponse>() {
            @Override
            public void onResponse(Call<GetDeviceBySeriResponse> call, Response<GetDeviceBySeriResponse> response) {
                if (response.body() != null) {
                    if (response.body().status == 1) {
                        DeviceModel device = response.body().getDevice();
                        if (device == null) {
                            try {
                                Intent intent = new Intent(LoginActivity.this, InfoDeviceActivity.class);
                                startActivity(intent);
                            } finally {
                                finish();
                            }
                        }
                        TrainingCenterModel trainingCenter = response.body().getTrainingCenter();
                        SharedPreferencesUtil.setDevice(LoginActivity.this, device);
                        SharedPreferencesUtil.setTrainingCenter(LoginActivity.this, trainingCenter);
                        SharedPreferencesUtil.setVehicle(LoginActivity.this, response.body().getVehicle());
                    } else {
                        Toast.makeText(LoginActivity.this, response.body().message, Toast.LENGTH_SHORT).show();
                        try {
                            Intent intent = new Intent(LoginActivity.this, InfoDeviceActivity.class);
                            startActivity(intent);
                        } finally {
                            finish();
                        }
                    }
                } else {
                    Toast.makeText(LoginActivity.this, R.string.err, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GetDeviceBySeriResponse> call, Throwable t) {
            }
        });
    }

    private void initFinger() {
        // lấy danh sách user được update avatar của seri ứng với trung tâm
        Call<GetUpdateUserBySeriResponse> callback;
        GetUpdateUserBySeriRequest getUpdateUserBySeriRequest;
        getUpdateUserBySeriRequest = new GetUpdateUserBySeriRequest(imeiDevice, UpdateUserType.CHANGE_FINGER_PRINT);
        callback = dataService.GetUpdateUserBySeri(getUpdateUserBySeriRequest);
        callback.enqueue(new Callback<GetUpdateUserBySeriResponse>() {
            @Override
            public void onResponse(Call<GetUpdateUserBySeriResponse> call, Response<GetUpdateUserBySeriResponse> response) {
                if (response.body() != null) {
                    if (response.body().status == 1) {
                        List<UserItem> users = response.body().getUsers();
                        if (users != null && users.size() > 0) {
                            List<String> deleteAvatarList = new ArrayList<String>();
                            for (int i = 0; i < users.size(); i++) {
                                UserItem item = users.get(i);
                                if (hcDatDatabase.UserExist(item.id)) {
                                    hcDatDatabase.UpdateUser(item);
                                } else {
                                    hcDatDatabase.AppendUser(item);
                                }
                                if (item.updateUserId != null && item.fingerPrintId1 != null && !item.fingerPrintId1.equals("")) {
                                    deleteAvatarList.add(item.updateUserId);
                                }
                            }
                            clearDialog();
                            if (deleteAvatarList.size() > 0) {
                                Call<CreateUpdateUserDeviceResponse> callback;
                                CreateUpdateUserDeviceRequest createUpdateUserDeviceRequest;
                                createUpdateUserDeviceRequest = new CreateUpdateUserDeviceRequest(imeiDevice, deleteAvatarList);
                                callback = dataService.CreateUpdateUserDevice(createUpdateUserDeviceRequest);
                                callback.enqueue(new Callback<CreateUpdateUserDeviceResponse>() {
                                    @Override
                                    public void onResponse(Call<CreateUpdateUserDeviceResponse> call, Response<CreateUpdateUserDeviceResponse> response) {
                                        if (response.body() != null) {

                                        } else {
                                            Toast.makeText(LoginActivity.this, response.body().message, Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<CreateUpdateUserDeviceResponse> call, Throwable t) {
                                    }
                                });
                            }
                        } else {
                            clearDialog();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, response.body().message, Toast.LENGTH_SHORT).show();
                        clearDialog();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, R.string.err, Toast.LENGTH_SHORT).show();
                    clearDialog();
                }
            }

            @Override
            public void onFailure(Call<GetUpdateUserBySeriResponse> call, Throwable t) {
                clearDialog();
            }
        });
    }

    //Camera roi
    protected void onStart() {
        super.onStart();
    }

    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void clearDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}