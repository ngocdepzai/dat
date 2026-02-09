package hc.manager.datapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import hc.manager.datapp.R;
import hc.manager.datapp.app.UserDataHelper;
import hc.manager.datapp.app.UserItem;
import hc.manager.datapp.camera.ShowCamera;
import hc.manager.datapp.models.InOutModel;
import hc.manager.datapp.models.ResetDeviceModel;
import hc.manager.datapp.models.request.CheckUserLoginRequest;
import hc.manager.datapp.models.request.DeleteResetDeviceRequest;
import hc.manager.datapp.models.request.GetUserByIdCardRequest;
import hc.manager.datapp.models.request.StudentLoginRequest;
import hc.manager.datapp.models.request.TeacherLoginRequest;
import hc.manager.datapp.models.response.CheckUserLoginResponse;
import hc.manager.datapp.models.response.DeleteResetDeviceResponse;
import hc.manager.datapp.models.response.GetUserByIdCardResponse;
import hc.manager.datapp.models.response.StudentLoginResponse;
import hc.manager.datapp.models.response.TeacherLoginResponse;
import hc.manager.datapp.models.response.UploadAuthResponse;
import hc.manager.datapp.service.ApiService;
import hc.manager.datapp.service.DataService;
import hc.manager.datapp.utils.BitmapExtension;
import hc.manager.datapp.utils.CustomToast;
import hc.manager.datapp.utils.ResetType;
import hc.manager.datapp.utils.SharedPreferencesUtil;
import hc.manager.datapp.utils.UserTypeContant;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginSupportActivity extends AppCompatActivity {
    File outputFileLogin;
    android.hardware.Camera cameraView;
    FrameLayout flCameraView;
    ShowCamera showCamera;
    InOutModel inOutModel = new InOutModel(1);
    UserDataHelper hcDatDatabase;
    TextView tvFullname, tvBirthDay, tvGender, tvPhoneNumber, tvIdNo, tvAddress, tvTitle;
    ImageView ivAvatar;
    LinearLayout llNoti, llContent;
    DataService dataService = ApiService.getService(this);
    ProgressDialog progressDialog;
    Button btSave;
    String sessionGuid = UUID.randomUUID().toString();
    UserItem userItem;
    String imeiDevice = "";
    ResetDeviceModel resetDeviceModel;

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            outputFileLogin = getOutputMediaFile();
            if (outputFileLogin == null) {
                return;
            } else {
                try {
                    FileOutputStream fos = new FileOutputStream(outputFileLogin);
                    fos.write(data);
                    fos.close();
                    camera.startPreview();
                    SendDataToServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    private NfcAdapter nfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private Bitmap fpImage;
    private PowerManager.WakeLock wakeLock;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_support);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        resetDeviceModel = SharedPreferencesUtil.getLoginSuport(this);
        hcDatDatabase = new UserDataHelper(this);
        imeiDevice = hcDatDatabase.getImeiDevice();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // check first app
        InitView();
        try {
            cameraView = android.hardware.Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            showCamera = new ShowCamera(LoginSupportActivity.this, cameraView);
            flCameraView.addView(showCamera);
        } catch (Exception ex) {
            cameraView = android.hardware.Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            showCamera = new ShowCamera(LoginSupportActivity.this, cameraView);
            flCameraView.addView(showCamera);
        }
        //hcDatDatabase.LoadAllAttendance();
        Log.d("VERSION", String.valueOf(Build.VERSION.SDK_INT));
        hcDatDatabase.LoadAll();
        getInfoUser();
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
                                                        // học viên đăng nhập
                                                        StudentLogin();
                                                    } else {
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
                                                Toast.makeText(LoginSupportActivity.this, responseFile.body().message, Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(LoginSupportActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
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
                                CustomToast.makeText(LoginSupportActivity.this, response.body().message, Toast.LENGTH_SHORT, 3).show();
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                            }
                        } else {
                            CustomToast.makeText(LoginSupportActivity.this, response.body().message, Toast.LENGTH_SHORT, 3).show();
                        }
                    } else {
                        CustomToast.makeText(LoginSupportActivity.this, "Có lỗi xảy ra", Toast.LENGTH_SHORT, 3).show();
                    }
                }

                @Override
                public void onFailure(Call<CheckUserLoginResponse> call, Throwable t) {
                }
            });
        } else {
//            inOutModel.Sent = 0;
//            SharedPreferencesUtil.setUser(CardActivity.this, userItem);
//            CustomToast.makeText(CardActivity.this, "Đăng nhập thành công!", 1000, 1).show();
//            SharedPreferencesUtil.setTimeDriver(CardActivity.this, 0);
//            SharedPreferencesUtil.setDistantDriver(CardActivity.this, 0);
//            hcDatDatabase.AppendAttendance(inOutModel);
//
//            if (progressDialog != null && progressDialog.isShowing()) {
//                progressDialog.dismiss();
//            }
//            Intent intentLogin = new Intent(CardActivity.this, DashbroadActivity.class);
//            finish();
//            startActivity(intentLogin);
            // tắt kết nối mạng
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
        UserItem teacherItem = SharedPreferencesUtil.getTeacher(LoginSupportActivity.this);
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
                                SharedPreferencesUtil.setUser(LoginSupportActivity.this, userItem);
                                SharedPreferencesUtil.setSession(LoginSupportActivity.this, response.body().getSessionId());
                                CustomToast.makeText(LoginSupportActivity.this, "Đăng nhập học viên thành công!", 1000, 1).show();
                            } else {
                                CustomToast.makeText(LoginSupportActivity.this, "Không tạo được phiên học, vui lòng thử lại!",
                                        Toast.LENGTH_SHORT, 3).show();
                                return;
                            }
                        }
                        DeleteResetDevice(ResetType.USER_LOGIN);
                        if (SharedPreferencesUtil.checkGoToMain(LoginSupportActivity.this)) {
                            try {
//                                Intent intentLogin = new Intent(LoginSupportActivity.this, DashbroadActivity.class);
//                                startActivity(intentLogin);
                            } finally {
                                finish();
                            }
                        } else {
                            try {
                                Intent intentLogin = new Intent(LoginSupportActivity.this, LoginActivity.class);
                                startActivity(intentLogin);
                            } finally {
                                finish();
                            }
                        }
                        hcDatDatabase.AppendAttendance(inOutModel);
                    } else {
                        inOutModel.Sent = 0;
                        CustomToast.makeText(LoginSupportActivity.this, "Kết nối server lỗi!",
                                Toast.LENGTH_SHORT, 3).show();
                    }

                } else {
                    CustomToast.makeText(LoginSupportActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT, 3).show();
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
        String sessionId = SharedPreferencesUtil.getSession(LoginSupportActivity.this);
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
                            SharedPreferencesUtil.setUser(LoginSupportActivity.this, userItem);
                            CustomToast.makeText(LoginSupportActivity.this, "Đăng nhập giảng viên thành công!", 1000, 1).show();
                        }
                        if (SharedPreferencesUtil.checkGoToMain(LoginSupportActivity.this)) {
                            try {
//                                Intent intentLogin = new Intent(LoginSupportActivity.this, DashbroadActivity.class);
//                                startActivity(intentLogin);
                            } finally {
                                finish();
                            }
                        } else {
                            try {
                                Intent intentLogin = new Intent(LoginSupportActivity.this, LoginActivity.class);
                                startActivity(intentLogin);
                            } finally {
                                finish();
                            }
                        }
                        DeleteResetDevice(ResetType.USER_LOGIN);
                        hcDatDatabase.AppendAttendance(inOutModel);
                    } else {
                        inOutModel.Sent = 0;
                        CustomToast.makeText(LoginSupportActivity.this, "Kết nối server lỗi!",
                                Toast.LENGTH_SHORT, 3).show();
                    }

                } else {
                    CustomToast.makeText(LoginSupportActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT, 3).show();
                }
            }

            @Override
            public void onFailure(Call<TeacherLoginResponse> call, Throwable t) {
            }
        });
    }

    private void DeleteResetDevice(int resetType) {
        Call<DeleteResetDeviceResponse> callback;
        DeleteResetDeviceRequest deleteResetDeviceRequest;
        deleteResetDeviceRequest = new DeleteResetDeviceRequest(imeiDevice, resetType);
        callback = dataService.DeleteResetDevice(deleteResetDeviceRequest);
        callback.enqueue(new Callback<DeleteResetDeviceResponse>() {
            @Override
            public void onResponse(Call<DeleteResetDeviceResponse> call, Response<DeleteResetDeviceResponse> response) {
                if (response.body() != null) {
                    if (response.body().status == 1) {
                        Log.d("Delete reset device ", response.body().message);
                    } else {
                        Log.d("Delete reset device ", response.body().message);
//                                    finish();
                    }
                } else {
//                                finish();
                }
            }

            @Override
            public void onFailure(Call<DeleteResetDeviceResponse> call, Throwable t) {
            }
        });
    }

    public void InitView() {
        this.tvFullname = (TextView) findViewById(R.id.tvFullname);
        this.tvAddress = (TextView) findViewById(R.id.tvAddress);
        this.flCameraView = (FrameLayout) findViewById(R.id.flCameraView);
        this.tvBirthDay = (TextView) findViewById(R.id.tvBirthDay);
        this.tvGender = (TextView) findViewById(R.id.tvGender);
        this.tvIdNo = (TextView) findViewById(R.id.tvIdNo);
        this.tvPhoneNumber = (TextView) findViewById(R.id.tvPhoneNumber);
        this.tvTitle = (TextView) findViewById(R.id.tvTitle);
        this.ivAvatar = (ImageView) findViewById(R.id.ivAvatar);
        this.llContent = (LinearLayout) findViewById(R.id.llContent);
        this.llNoti = (LinearLayout) findViewById(R.id.llNoti);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            try {
                Intent intentLogin = new Intent(LoginSupportActivity.this, LoginActivity.class);
                startActivity(intentLogin);
            } finally {
                finish();
            }

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null)
            nfcAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, null);
    }

    private void ShowInfo() {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.show_info_success);
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//
//        View view = LayoutInflater.from(this).inflate(R.layout.show_info_success, null);
        MediaPlayer mPlayer = MediaPlayer.create(LoginSupportActivity.this, R.raw.start);
        mPlayer.start();
        final TextView tvFullname = (TextView) dialog.findViewById(R.id.tvFullnameInfo);
        final TextView tvAddress = (TextView) dialog.findViewById(R.id.tvAddressInfo);
        final TextView tvBirthDay = (TextView) dialog.findViewById(R.id.tvBirthDayInfo);
        final TextView tvGender = (TextView) dialog.findViewById(R.id.tvGenderInfo);
        final TextView tvIdNo = (TextView) dialog.findViewById(R.id.tvIdNoInfo);
        final TextView tvPhoneNumber = (TextView) dialog.findViewById(R.id.tvPhoneNumberInfo);
        final TextView tvTitle = (TextView) dialog.findViewById(R.id.tvTitleInfo);
        final ImageView ivAvatar = (ImageView) dialog.findViewById(R.id.ivAvatarInfo);
//        builder.setView(view);
        final Button btSave = (Button) dialog.findViewById(R.id.btSaveInfo);
        final Button btCancel = (Button) dialog.findViewById(R.id.btCancelInfo);
        tvFullname.setText(userItem.name);
        tvPhoneNumber.setText(userItem.phoneNumber);
        tvIdNo.setText(userItem.code);
        tvGender.setText(userItem.gender);
        tvBirthDay.setText(userItem.birthDay);
        tvAddress.setText(userItem.address);
        if (userItem.avatarId != null) {
//            String url = "http://hcsky.vn/api/Resource/get_link_image_resize/" + userItem.avatarId;
//            String url = "http://apidat-test.blackwind.vn/api/Resource/get_link_image_resize/" + userItem.avatarId;
            String url = getString(R.string.BASE_URL_IMAGE_REAL_RESIZE) + userItem.avatarId;
            new BitmapExtension(ivAvatar).execute(url);
        }
        if (userItem.userType.equals(UserTypeContant.STUDENT)) {
            tvTitle.setText("THÔNG TIN HỌC VIÊN");
        } else {
            tvTitle.setText("THÔNG TIN GIẢNG VIÊN");
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
                        progressDialog = new ProgressDialog(LoginSupportActivity.this);
                        progressDialog.setTitle(R.string.please_wait);
                        progressDialog.setCancelable(true);
                        progressDialog.show();
                        UserItem student = SharedPreferencesUtil.getStudent(LoginSupportActivity.this);
                        UserItem teacher = SharedPreferencesUtil.getTeacher(LoginSupportActivity.this);
                        if (student != null && userItem != null && userItem.userType.equals(UserTypeContant.STUDENT) && student.code.equals(userItem.code)) {
                            CustomToast.makeText(LoginSupportActivity.this, "Vui lòng đăng xuất học viên trước khi đăng nhập!", 1000, 2).show();
                            return;
                        }
                        if (teacher != null && userItem != null && userItem.userType.equals(UserTypeContant.TEACHER) && teacher.code.equals(userItem.code)) {
                            CustomToast.makeText(LoginSupportActivity.this, "Vui lòng đăng xuất giảng viên trước khi đăng nhập!", 1000, 2).show();
                            return;
                        }
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
                        if (cameraView != null) {
                            cameraView.takePicture(null, null, mPictureCallback);
                        } else {
                            return;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception ex) {
        }
//        mSyncGroupDialog = builder.create();

//        WindowManager m = getWindowManager();
//        Display d = m.getDefaultDisplay();  //为获取屏幕宽、高
//
//        WindowManager.LayoutParams attributes = mSyncGroupDialog.getWindow().getAttributes();
//        attributes.height = d.getHeight();
//        attributes.width = d.getWidth();
//        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        mSyncGroupDialog.getWindow().setAttributes(attributes);
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90);
        dialog.getWindow().setLayout(width, height);
        dialog.show();
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

    private void getInfoUser() {
        if (CheckNetworkConnection()) {
            Call<GetUserByIdCardResponse> callback;
            GetUserByIdCardRequest getUserByIdCardRequest;
            getUserByIdCardRequest = new GetUserByIdCardRequest(resetDeviceModel.getUserCode(), imeiDevice, resetDeviceModel.getUserCode());
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
                            CustomToast.makeText(LoginSupportActivity.this, response.body().message, Toast.LENGTH_SHORT, 3).show();
                        }
                    } else {
                        CustomToast.makeText(LoginSupportActivity.this, "Có lỗi xảy ra", Toast.LENGTH_SHORT, 3).show();
                    }
                }

                @Override
                public void onFailure(Call<GetUserByIdCardResponse> call, Throwable t) {
                }
            });
        } else {
            CustomToast.makeText(this, "Card get info: kết nối mạng kém", Toast.LENGTH_LONG, 3).show();
        }
    }
}