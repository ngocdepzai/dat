package hc.manager.datapp.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcapp.ImageCapture;
import com.serenegiant.usb.Size;
import com.serenegiant.widget.AspectRatioSurfaceView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;

import hc.manager.datapp.R;
import hc.manager.datapp.app.ActivityList;
import hc.manager.datapp.app.LogsList;
import hc.manager.datapp.app.UserDataHelper;
import hc.manager.datapp.app.UserItem;
import hc.manager.datapp.app.VehicleItem;
import hc.manager.datapp.camera.CameraHelperSingleton;
import hc.manager.datapp.camera.ShowCamera;
import hc.manager.datapp.models.InOutModel;
import hc.manager.datapp.models.request.CheckUserLoginRequest;
import hc.manager.datapp.models.request.GetUserByIdCardRequest;
import hc.manager.datapp.models.request.StudentLoginRequest;
import hc.manager.datapp.models.request.TeacherLoginRequest;
import hc.manager.datapp.models.response.CheckUserLoginResponse;
import hc.manager.datapp.models.response.GetUserByIdCardResponse;
import hc.manager.datapp.models.response.GetVehicleBySeriCourseIdResponse;
import hc.manager.datapp.models.response.StudentLoginResponse;
import hc.manager.datapp.models.response.TeacherLoginResponse;
import hc.manager.datapp.models.response.UploadAuthResponse;
import hc.manager.datapp.service.ApiService;
import hc.manager.datapp.service.DataService;
import hc.manager.datapp.utils.BitmapExtension;
import hc.manager.datapp.utils.CustomToast;
import hc.manager.datapp.utils.SharedPreferencesUtil;
import hc.manager.datapp.utils.UserTypeContant;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CardActivity2 extends AppCompatActivity {
    private static final boolean DEBUG = true;
    private static final String TAG = CardActivity2.class.getSimpleName();

    File outputFileLogin;
    android.hardware.Camera cameraView;
//    FrameLayout flCameraView;
    ShowCamera showCamera;
    InOutModel inOutModel = new InOutModel(1);
    UserDataHelper hcDatDatabase;
    TextView tvFullname, tvBirthDay, tvGender, tvPhoneNumber, tvIdNo, tvAddress, tvTitle;
    ImageView ivAvatar;
    LinearLayout llNoti, llContent;
    DataService dataService = ApiService.getService(this);
    ProgressDialog progressDialog;
    UserItem userItem;
    List<VehicleItem> listVehicle;
    String imeiDevice = "";
    Dialog dialog;

    private NfcAdapter nfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private PowerManager.WakeLock wakeLock;

    //add Cam roi
    //Cam rời
    private ICameraHelper mCameraHelper;
    private AspectRatioSurfaceView mCameraViewMain;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card2);
        hcDatDatabase = new UserDataHelper(this);
        imeiDevice = hcDatDatabase.getImeiDevice();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // check first app
        InitView();

        ActivityList.getInstance().setMainContext(this);
        ActivityList.getInstance().CreateDir();
        ActivityList.getInstance().LoadConfig();
        ActivityList.getInstance().SetAutoResult();
        hcDatDatabase.LoadAll();
        LogsList.getInstance().Init();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "sc");
        wakeLock.acquire();
        InitReadCard();
    }

//    @Override
//    public void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        processIntent(intent);
//    }

    private File getOutputMediaFile() {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            return null;
        } else {
            File folder_gus = new File(Environment.getExternalStorageDirectory() + "/HC_DAT_IMAGES");
            if (!folder_gus.exists()) {
                folder_gus.mkdirs();
            }
            File outputFile = new File(folder_gus, (System.currentTimeMillis() / 1000) + ".jpg");
            return outputFile;
        }
    }

    @SuppressLint("MissingPermission")
    public void SendDataToServer() {
        if (outputFileLogin == null) return;
        inOutModel.FilePathLocal = outputFileLogin.getPath();
        if (CheckNetworkConnection()) {
            // Kiểm tra xem user có được phép đăng nhập hay không
            Call<CheckUserLoginResponse> callback;
            CheckUserLoginRequest checkUserLoginRequest;
            checkUserLoginRequest = new CheckUserLoginRequest();
            checkUserLoginRequest.setSeri(imeiDevice);
            checkUserLoginRequest.setUserCode(userItem.code);
            callback = dataService.CheckUserLogin(checkUserLoginRequest);
            callback.enqueue(new Callback<CheckUserLoginResponse>() {
                @Override
                public void onResponse(Call<CheckUserLoginResponse> call, Response<CheckUserLoginResponse> response) {
                    if (response.body() != null) {
                        if (response.body().status == 1) {
                            if (response.body().canLogin) {
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
                                                // pushlish lên rabbit
                                                try {
                                                    if (userItem.userType.equals(UserTypeContant.STUDENT)) {
                                                        //Học viên đăng nhập
                                                        dialog.dismiss();
                                                        UserItem us = hcDatDatabase.FindUserById(userItem.id);
                                                        if (null != us && null != us.faceToken) {
                                                            StudentLogin();
                                                        } else {
                                                            new AlertDialog.Builder(CardActivity2.this)
                                                                    .setMessage("Khuôn mặt học viên chưa tồn tại trên thiết bị, cần xác thực khuôn mặt học viên và đồng bộ lên hệ thống trước khi đăng nhập bằng Thẻ!")
                                                                    .setPositiveButton("Đi đến cài đặt KHUÔN MẶT", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                                                            try {
                                                                                Intent intentMenu;
                                                                                clearCameraHelper();
                                                                                if (CameraHelperSingleton.getInstance().getUsbDevice() != null) {
//                                                                                    intentMenu = new Intent(CardActivity2.this, AddFaceActivity2.class);
                                                                                } else {
//                                                                                    intentMenu = new Intent(CardActivity2.this, AddFaceActivity.class);
                                                                                }
//                                                                                startActivity(intentMenu);
                                                                            } finally {
                                                                                finish();
                                                                            }
                                                                        }
                                                                    })
                                                                    .setNegativeButton("Hủy bỏ", null)
                                                                    .show();
                                                        }
                                                    } else {
                                                        dialog.dismiss();
                                                        TeacherLogin();
                                                    }
                                                    if (progressDialog != null && progressDialog.isShowing()) {
                                                        progressDialog.dismiss();
                                                    }
                                                } catch (Exception ee) {
                                                    if (progressDialog != null && progressDialog.isShowing()) {
                                                        progressDialog.dismiss();
                                                    }
                                                    ee.printStackTrace();
                                                }
                                            } else {
                                                Toast.makeText(CardActivity2.this, responseFile.body().message, Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(CardActivity2.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                        }

                                        if (progressDialog != null && progressDialog.isShowing()) {
                                            progressDialog.dismiss();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<UploadAuthResponse> call, Throwable t) {
                                    }
                                });
                            } else {
                                CustomToast.makeText(CardActivity2.this, response.body().message, Toast.LENGTH_SHORT, 3).show();
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                            }
                        } else {
                            CustomToast.makeText(CardActivity2.this, response.body().message, Toast.LENGTH_SHORT, 3).show();
                        }
                    } else {
                        CustomToast.makeText(CardActivity2.this, "Có lỗi xảy ra", Toast.LENGTH_SHORT, 3).show();
                    }
                }

                @Override
                public void onFailure(Call<CheckUserLoginResponse> call, Throwable t) {
                }
            });
        } else {
            CustomToast.makeText(this, "Card: kết nối mạng kém", Toast.LENGTH_LONG, 3).show();
        }
    }

    private void StudentLogin() {
        Call<StudentLoginResponse> callback;
        StudentLoginRequest studentLoginRequest = new StudentLoginRequest();
        studentLoginRequest.setSeri(imeiDevice);
        studentLoginRequest.setCode(userItem.code);
        studentLoginRequest.setLat(inOutModel.getLat());
        studentLoginRequest.setLng(inOutModel.getLng());
        studentLoginRequest.setPath(inOutModel.getFilePath());
        studentLoginRequest.setTime(inOutModel.getTime());
        studentLoginRequest.setLoginType(inOutModel.getLoginType());

        // lấy thông tin teacher
        UserItem teacherItem = SharedPreferencesUtil.getTeacher(CardActivity2.this);
        if (teacherItem != null) {
            studentLoginRequest.setTeacherCode(teacherItem.code);
        }
        callback = dataService.StudentLogin(studentLoginRequest);
        callback.enqueue(new Callback<StudentLoginResponse>() {
            @Override
            public void onResponse(Call<StudentLoginResponse> call, Response<StudentLoginResponse> response) {
                if (response.body() != null) {
                    if (response.body().status == 1) {
                        inOutModel.Sent = 1;
                        if (inOutModel.Type == 1) {
                            if (response.body().getSessionId() != null) {
                                SharedPreferencesUtil.setUser(CardActivity2.this, userItem);
                                SharedPreferencesUtil.setSession(CardActivity2.this, response.body().getSessionId());
                                CustomToast.makeText(CardActivity2.this, "Đăng nhập học viên thành công!", 1000, 1).show();
                            } else {
                                CustomToast.makeText(CardActivity2.this, "Không tạo được phiên học, vui lòng thử lại!",
                                        Toast.LENGTH_SHORT, 3).show();
                                return;
                            }
                        }
                        if (SharedPreferencesUtil.checkGoToMain(CardActivity2.this)) {
                            try {
                                clearCameraHelper();
                                Intent intentLogin;
//                                intentLogin = new Intent(CardActivity2.this, DashbroadActivity2.class);
//                                startActivity(intentLogin);
                                if (dialog != null && dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            } finally {
                                finish();
                            }
                        } else {
                            try {
                                clearCameraHelper();
                                Intent intentLogin = new Intent(CardActivity2.this, LoginActivity.class);
                                startActivity(intentLogin);
                                if (dialog != null && dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            } finally {
                                finish();
                            }
                        }
                        hcDatDatabase.AppendAttendance(inOutModel);
                    } else {
                        inOutModel.Sent = 0;
                        if (response.body().message != null) {
                            CustomToast.makeText(CardActivity2.this, "Thông tin: " + response.body().message,
                                    Toast.LENGTH_SHORT, 3).show();
                        } else {
                            CustomToast.makeText(CardActivity2.this, "Kết nối server lỗi!",
                                    Toast.LENGTH_SHORT, 3).show();
                        }
                    }

                } else {
                    CustomToast.makeText(CardActivity2.this, "Lỗi kết nối", Toast.LENGTH_SHORT, 3).show();
                }
            }

            @Override
            public void onFailure(Call<StudentLoginResponse> call, Throwable t) {
            }
        });
    }

    private void TeacherLogin() {
        Call<TeacherLoginResponse> callback;
        TeacherLoginRequest teacherLoginRequest = new TeacherLoginRequest();
        teacherLoginRequest.setSeri(imeiDevice);
        teacherLoginRequest.setCode(userItem.code);
        teacherLoginRequest.setLat(inOutModel.getLat());
        teacherLoginRequest.setLng(inOutModel.getLng());
        teacherLoginRequest.setPath(inOutModel.getFilePath());
        teacherLoginRequest.setTime(inOutModel.getTime());
        teacherLoginRequest.setLoginType(inOutModel.getLoginType());
        // lấy thông tin session
        String sessionId = SharedPreferencesUtil.getSession(CardActivity2.this);
        if (sessionId != null && sessionId != "") {
            teacherLoginRequest.setSessionId(sessionId);
        }
        callback = dataService.TeacherLogin(teacherLoginRequest);
        callback.enqueue(new Callback<TeacherLoginResponse>() {
            @Override
            public void onResponse(Call<TeacherLoginResponse> call, Response<TeacherLoginResponse> response) {
                if (response.body() != null) {
                    if (response.body().status == 1) {
                        inOutModel.Sent = 1;
                        if (inOutModel.Type == 1) {
                            SharedPreferencesUtil.setUser(CardActivity2.this, userItem);
                            CustomToast.makeText(CardActivity2.this, "Đăng nhập giảng viên thành công!", 1000, 1).show();
                        }
                        if (SharedPreferencesUtil.checkGoToMain(CardActivity2.this)) {
                            try {
                                clearCameraHelper();
                                Intent intentLogin;
//                                intentLogin = new Intent(CardActivity2.this, DashbroadActivity2.class);
//                                startActivity(intentLogin);
                                if (dialog != null && dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            } finally {
                                finish();
                            }
                        } else {
                            try {
                                clearCameraHelper();
                                Intent intentLogin = new Intent(CardActivity2.this, LoginActivity.class);
                                startActivity(intentLogin);
                                if (dialog != null && dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            } finally {
                                finish();
                            }
                        }
                        hcDatDatabase.AppendAttendance(inOutModel);
                    } else {
                        inOutModel.Sent = 0;
                        if (response.body().message != null) {
                            CustomToast.makeText(CardActivity2.this, "Thông tin: " + response.body().message,
                                    Toast.LENGTH_SHORT, 3).show();
                        } else {
                            CustomToast.makeText(CardActivity2.this, "Kết nối server lỗi!",
                                    Toast.LENGTH_SHORT, 3).show();
                        }
                    }

                } else {
                    CustomToast.makeText(CardActivity2.this, "Lỗi kết nối", Toast.LENGTH_SHORT, 3).show();
                }
            }

            @Override
            public void onFailure(Call<TeacherLoginResponse> call, Throwable t) {
            }
        });
    }

    public void InitView() {
        this.tvFullname = (TextView) findViewById(R.id.tvFullname);
        this.tvAddress = (TextView) findViewById(R.id.tvAddress);
//        this.flCameraView = (FrameLayout) findViewById(R.id.flCameraView);
        this.tvBirthDay = (TextView) findViewById(R.id.tvBirthDay);
        this.tvGender = (TextView) findViewById(R.id.tvGender);
        this.tvIdNo = (TextView) findViewById(R.id.tvIdNo);
        this.tvPhoneNumber = (TextView) findViewById(R.id.tvPhoneNumber);
        this.tvTitle = (TextView) findViewById(R.id.tvTitle);
        this.ivAvatar = (ImageView) findViewById(R.id.ivAvatar);
        this.llContent = (LinearLayout) findViewById(R.id.llContent);
        this.llNoti = (LinearLayout) findViewById(R.id.llNoti);

        //Khởi tạo camera
        //Cam rời
        mCameraViewMain = findViewById(R.id.svCameraCard);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            try {
                clearCameraHelper();
                Intent intentLogin = new Intent(CardActivity2.this, LoginActivity.class);
                startActivity(intentLogin);
                return true;
            } finally {
                finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void InitReadCard() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "Device does not support NFC!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "Enable the NFC function in the system settings!",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mFilters = new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)};
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null)
            nfcAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, null);
    }

    private void ShowInfo() {
        dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.show_info_success);
        MediaPlayer mPlayer = MediaPlayer.create(CardActivity2.this, R.raw.start);
        mPlayer.start();
        final TextView tvFullname = (TextView) dialog.findViewById(R.id.tvFullnameInfo);
        final TextView tvAddress = (TextView) dialog.findViewById(R.id.tvAddressInfo);
        final TextView tvBirthDay = (TextView) dialog.findViewById(R.id.tvBirthDayInfo);
        final TextView tvGender = (TextView) dialog.findViewById(R.id.tvGenderInfo);
        final TextView tvIdNo = (TextView) dialog.findViewById(R.id.tvIdNoInfo);
        final TextView tvPhoneNumber = (TextView) dialog.findViewById(R.id.tvPhoneNumberInfo);
        final TextView tvTitle = (TextView) dialog.findViewById(R.id.tvTitleInfo);
        final ImageView ivAvatar = (ImageView) dialog.findViewById(R.id.ivAvatarInfo);
        final Button btSave = (Button) dialog.findViewById(R.id.btSaveInfo);
        final Button btCancel = (Button) dialog.findViewById(R.id.btCancelInfo);
        tvFullname.setText(userItem.name);
        tvPhoneNumber.setText(userItem.phoneNumber);
        tvIdNo.setText(userItem.code);
        tvGender.setText(userItem.gender);
        tvBirthDay.setText(userItem.birthDay);
        tvAddress.setText(userItem.address);
        if (userItem.avatarId != null) {
            String url = getString(R.string.BASE_URL_IMAGE_REAL_RESIZE) + userItem.avatarId;
            new BitmapExtension(ivAvatar).execute(url);
        } else {
            ivAvatar.setImageDrawable(getResources().getDrawable(R.drawable.nonavatar));
        }
        if (userItem.userType.equals(UserTypeContant.STUDENT)) {
            tvTitle.setText("THÔNG TIN HỌC VIÊN");
        } else {
            tvTitle.setText("THÔNG TIN GIẢNG VIÊN");
        }
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90);
        dialog.getWindow().setLayout(width, height);
        if (!dialog.isShowing()) {
            dialog.show();
        }
        try {
            btCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });
            btSave.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onClick(View view) {
                    try {
                        progressDialog = new ProgressDialog(CardActivity2.this);
                        progressDialog.setTitle(R.string.please_wait);
                        progressDialog.setCancelable(true);
                        progressDialog.show();
                        UserItem student = SharedPreferencesUtil.getStudent(CardActivity2.this);
                        UserItem teacher = SharedPreferencesUtil.getTeacher(CardActivity2.this);
                        if (student != null && userItem != null && userItem.userType.equals(UserTypeContant.STUDENT) && student.code.equals(userItem.code)) {
                            CustomToast.makeText(CardActivity2.this, "Vui lòng đăng xuất học viên trước khi đăng nhập!", 1000, 2).show();
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            return;
                        }
                        if (teacher != null && userItem != null && userItem.userType.equals(UserTypeContant.TEACHER) && teacher.code.equals(userItem.code)) {
                            CustomToast.makeText(CardActivity2.this, "Vui lòng đăng xuất giảng viên trước khi đăng nhập!", 1000, 2).show();
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            return;
                        }
                        //Kiem tra tiep xem user co thuoc trung tam va co xe hay khong
                        Call<GetVehicleBySeriCourseIdResponse> callbackVehicle;
                        if (null != userItem.courseId) {
                            callbackVehicle = dataService.GetVehicleBySeriCourseId(imeiDevice, userItem.courseId);
                            callbackVehicle.enqueue(new Callback<GetVehicleBySeriCourseIdResponse>() {
                                @Override
                                public void onResponse(Call<GetVehicleBySeriCourseIdResponse> call, Response<GetVehicleBySeriCourseIdResponse> response) {
                                    if (response.body() != null) {
                                        if (response.body().status == 1) {
                                            listVehicle = response.body().getItems();
                                            if (listVehicle == null || listVehicle.size() == 0) {
                                                CustomToast.makeText(CardActivity2.this, "Bạn chưa được gán đến thiết bị này!", Toast.LENGTH_SHORT, 2).show();
                                                if (progressDialog != null && progressDialog.isShowing()) {
                                                    progressDialog.dismiss();
                                                }
                                            } else {
                                                inOutModel.Seri = imeiDevice;
                                                Intent intent = getIntent();
                                                double lat = intent.getDoubleExtra("Lat", 0);
                                                double lng = intent.getDoubleExtra("Lng", 0);
                                                inOutModel.Lat = lat;
                                                inOutModel.Lng = lng;
                                                inOutModel.LoginType = 3;
                                                inOutModel.UserId = userItem.userid;
                                                inOutModel.UserCode = userItem.code;
                                                inOutModel.Name = userItem.name;
                                                inOutModel.Dis = 0;
                                                // chup anh luc đăng nhập
                                                if (mCameraHelper != null) {
//                                                    cameraView.takePicture(null, null, mPictureCallback);
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
                                                                Toast.makeText(CardActivity2.this, message, Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                                    }
                                                    if (progressDialog != null && progressDialog.isShowing()) {
                                                        progressDialog.dismiss();
                                                    }
                                                } else {
                                                    if (progressDialog != null && progressDialog.isShowing()) {
                                                        progressDialog.dismiss();
                                                    }
                                                    CustomToast.makeText(CardActivity2.this, "Camera lỗi!", 1000, 1).show();
                                                }
                                            }
                                        } else {
                                            CustomToast.makeText(CardActivity2.this, response.body().message, Toast.LENGTH_SHORT, 3).show();
                                            if (progressDialog != null && progressDialog.isShowing()) {
                                                progressDialog.dismiss();
                                            }
                                        }
                                    } else {
                                        CustomToast.makeText(CardActivity2.this, "Có lỗi xảy ra!", Toast.LENGTH_SHORT, 3).show();
                                        if (progressDialog != null && progressDialog.isShowing()) {
                                            progressDialog.dismiss();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<GetVehicleBySeriCourseIdResponse> call, Throwable t) {
                                    CustomToast.makeText(CardActivity2.this, "Có lỗi xảy ra!", Toast.LENGTH_SHORT, 3).show();
                                    if (progressDialog != null && progressDialog.isShowing()) {
                                        progressDialog.dismiss();
                                    }
                                }
                            });
                        } else {
                            callbackVehicle = dataService.GetVehicleBySeriTrainingCenterId(imeiDevice, userItem.trainingCenterId);
                            callbackVehicle.enqueue(new Callback<GetVehicleBySeriCourseIdResponse>() {
                                @Override
                                public void onResponse(Call<GetVehicleBySeriCourseIdResponse> call, Response<GetVehicleBySeriCourseIdResponse> response) {
                                    if (response.body() != null) {
                                        if (response.body().status == 1) {
                                            listVehicle = response.body().getItems();
                                            if (listVehicle == null || listVehicle.size() == 0) {
                                                CustomToast.makeText(CardActivity2.this, "Bạn chưa được gán đến thiết bị này!", Toast.LENGTH_SHORT, 2).show();
                                                if (progressDialog != null && progressDialog.isShowing()) {
                                                    progressDialog.dismiss();
                                                }
                                            } else {
                                                inOutModel.Seri = imeiDevice;
                                                Intent intent = getIntent();
                                                double lat = intent.getDoubleExtra("Lat", 0);
                                                double lng = intent.getDoubleExtra("Lng", 0);
                                                inOutModel.Lat = lat;
                                                inOutModel.Lng = lng;
                                                inOutModel.LoginType = 3;
                                                inOutModel.UserId = userItem.userid;
                                                inOutModel.UserCode = userItem.code;
                                                inOutModel.Name = userItem.name;
                                                inOutModel.Dis = 0;
                                                // chup anh luc đăng nhập
                                                if (mCameraHelper != null) {
//                                                    cameraView.takePicture(null, null, mPictureCallback);
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
                                                                Toast.makeText(CardActivity2.this, message, Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                                    }
                                                } else {
                                                    if (progressDialog != null && progressDialog.isShowing()) {
                                                        progressDialog.dismiss();
                                                    }
                                                }
                                            }
                                        } else {
                                            CustomToast.makeText(CardActivity2.this, response.body().message, Toast.LENGTH_SHORT, 3).show();
                                            if (progressDialog != null && progressDialog.isShowing()) {
                                                progressDialog.dismiss();
                                            }
                                        }
                                    } else {
                                        CustomToast.makeText(CardActivity2.this, "Có lỗi xảy ra!", Toast.LENGTH_SHORT, 3).show();
                                        if (progressDialog != null && progressDialog.isShowing()) {
                                            progressDialog.dismiss();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<GetVehicleBySeriCourseIdResponse> call, Throwable t) {
                                    CustomToast.makeText(CardActivity2.this, "Có lỗi xảy ra!", Toast.LENGTH_SHORT, 3).show();
                                    if (progressDialog != null && progressDialog.isShowing()) {
                                        progressDialog.dismiss();
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {

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

    private String ByteArrayToHexString(byte[] inarray) {
        int i, j, in;
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        String out = "";
        if (null != inarray) {
            for (j = 0; j < inarray.length; ++j) {
                in = (int) inarray[j] & 0xff;
                i = (in >> 4) & 0x0f;
                out += hex[i];
                i = in & 0x0f;
                out += hex[i];
            }
        }
        return out;
    }

    @SuppressLint("MissingPermission")
    private void processIntent(Intent intent) {
        Parcelable[] parselables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (parselables != null && parselables.length > 0) {
            NdefMessage ndefMessage = (NdefMessage) parselables[0];
            NdefRecord[] ndefRecords = ndefMessage.getRecords();
            if (ndefRecords != null && ndefRecords.length > 0) {
                NdefRecord ndefRecord = ndefRecords[0];
                try {
                    byte[] payload = ndefRecord.getPayload();
                    String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
                    int languageSize = payload[0] & 0063;
                    String cardstr = new String(payload, languageSize + 1, payload.length - languageSize - 1, textEncoding);
                    // lấy mã seri của thẻ
                    String hexdump = this.ByteArrayToHexString(getIntent().getByteArrayExtra(NfcAdapter.EXTRA_ID));
                    if (CheckNetworkConnection()) {
                        Call<GetUserByIdCardResponse> callback;
                        GetUserByIdCardRequest getUserByIdCardRequest;
                        getUserByIdCardRequest = new GetUserByIdCardRequest(cardstr, imeiDevice);
                        getUserByIdCardRequest.setSeriCard(hexdump);
                        callback = dataService.GetUserByIdCard(getUserByIdCardRequest);
                        callback.enqueue(new Callback<GetUserByIdCardResponse>() {
                            @Override
                            public void onResponse(Call<GetUserByIdCardResponse> call, Response<GetUserByIdCardResponse> response) {
                                if (response.body() != null) {
                                    if (response.body().status == 1) {
                                        userItem = response.body().getUser();
                                        if (userItem != null) {
                                            ShowInfo();
                                        }
                                    } else {
                                        CustomToast.makeText(CardActivity2.this, response.body().message, Toast.LENGTH_SHORT, 3).show();
                                    }
                                } else {
                                    CustomToast.makeText(CardActivity2.this, "Có lỗi xảy ra", Toast.LENGTH_SHORT, 3).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<GetUserByIdCardResponse> call, Throwable t) {
                            }
                        });
                    } else {
//                      userItem = hcDatDatabase.FindUserItemByCard(cardstr);
//                      if (userItem != null) {
//                          ShowInfo();
//                      }
                        CustomToast.makeText(this, "Card get info: kết nối mạng kém", Toast.LENGTH_LONG, 3).show();
                    }
//
                } catch (UnsupportedEncodingException e) {

                }
            }
        }
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

}