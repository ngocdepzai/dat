package hc.manager.datapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import hc.manager.datapp.R;
import hc.manager.datapp.adapter.AuthPictureAdapter;
import hc.manager.datapp.app.UserItem;
import hc.manager.datapp.models.AuthPictureModel;
import hc.manager.datapp.utils.BitmapExtension;
import hc.manager.datapp.utils.SharedPreferencesUtil;
import hc.manager.datapp.utils.ShowCamera;

public class MainActivity extends AppCompatActivity {
    private static final long INTERVAL = 1000 * 2;
    private static final long FASTEST_INTERVAL = 1000 * 1;
    TextView tvFullname, tvIdNo, tvBirthDay, tvFullnameTeacher, tvIdNoTeacher, tvBirthDayTeacher, tvFullnameTeacherHaveSpeed, tvFullnameHaveSpeed;
    TextView tvDateNow, tvSpeed, tvTotalDistance, tvTotalTime, tvTotalDistanceComplete, tvTotalTimeMissing, tvTotalTimeComplete, tvTotalDistanceMissing;
    ImageView ivAvatarStudent, ivTeacher;
    Camera camera;
    ProgressBar pbSpeed;
    LinearLayout llInfoUser, llInfoUserHaveSpeed;
    FrameLayout flCamera;
    Button btLogOut;
    TextView txtLat;
    FusedLocationProviderClient mFusedLocationClient;
    int PERMISSION_ID = 44;
    float p1, p2, p3, p4;
    ShowCamera showCamera;
    LocationListener li;
    android.text.format.DateFormat df = new android.text.format.DateFormat();
    Float totalDistance = Float.valueOf(0);
    Float totalD = Float.valueOf(200000);
    double totalTime = 0;
    double totalT = 3600 * 4;
    double curLocationLat, curLocationLng;
    private UserItem student;
    private UserItem teacher;
    private RecyclerView lvAuthPicture;
    private AuthPictureAdapter authPictureAdapter;
    ;
    private ArrayList<AuthPictureModel> authPictureModels = new ArrayList<AuthPictureModel>();
    private Date dateNow = new Date();
    String sDateNow = (String) df.format("hh:mm:ss dd-MM-yyyy", dateNow);
    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            p3 = (float) mLastLocation.getLongitude();
            p4 = (float) mLastLocation.getLatitude();
            double dSpeed = mLastLocation.getSpeed();
            double a = 3.6 * (dSpeed);
            int kmhSpeed = (int) (Math.round(a));
//            txtLat.setText("Longitude:" + mLastLocation.getLongitude() + " Latitude:" + mLastLocation.getLatitude()+"  SPEED="+kmhSpeed);
            float[] results = new float[1];
            if (curLocationLng != 0 && curLocationLat != 0) {
                mLastLocation.distanceBetween(
                        mLastLocation.getLatitude(), mLastLocation.getLongitude(),
                        curLocationLat, curLocationLng, results);
                if (results[0] > 10) {
                    totalDistance = totalDistance + results[0];
                }
            }
            curLocationLng = mLastLocation.getLongitude();
            curLocationLat = mLastLocation.getLatitude();
            tvTotalDistance.setText(String.valueOf(Math.ceil(totalDistance / 1000 * 100.0) / 100.0) + " km");
            tvTotalDistanceComplete.setText(String.valueOf(Math.ceil(totalDistance / 1000 * 100.0) / 100.0) + " km");
            tvTotalDistanceMissing.setText(String.valueOf(Math.ceil((totalD - totalDistance) / 1000 * 100.0) / 100.0) + " km");
            if (kmhSpeed > 3) {
                tvSpeed.setText(String.valueOf(kmhSpeed));
                llInfoUser.setVisibility(View.GONE);
                llInfoUserHaveSpeed.setVisibility(View.VISIBLE);
            } else {
                llInfoUser.setVisibility(View.VISIBLE);
                llInfoUserHaveSpeed.setVisibility(View.GONE);
                tvSpeed.setText("00");
            }

        }
    };

    public static double round(double unrounded, int precision, int roundingMode) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, roundingMode);
        return rounded.doubleValue();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
        InitView();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        InitValue();
        ShowCam();
        authPictureAdapter = new AuthPictureAdapter(authPictureModels, MainActivity.this);
        lvAuthPicture.setAdapter(authPictureAdapter);
        lvAuthPicture.scrollToPosition(authPictureModels.size() - 1);
        UpdateAuth();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        UpdateTimeAndSpeed();
//        this.rbStudent.setOnC
//        heckedChangeListener(new RadioButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                doOnGameCharacterChanged(buttonView,isChecked);
//            }
//        });
//
//        // When radio button "Male" checked change.
//        this.radioButtonFemale.setOnCheckedChangeListener(new RadioButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                doOnGameCharacterChanged(buttonView,isChecked);
//            }
//        });
        btLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.putExtra("LogOut", true);
//                finish();
                startActivity(intent);
            }
        });
//        new android.os.Handler(Looper.getMainLooper()).postDelayed(
//                new Runnable() {
//                    public void run() {
//                        llInfoUser.setVisibility(View.GONE);
//                        llInfoUserHaveSpeed.setVisibility(View.VISIBLE);
//                    }
//                },
//                10000);
    }

    private void UpdateAuth() {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Calendar cal = Calendar.getInstance();
                int hours = cal.get(Calendar.HOUR_OF_DAY);
                int minute = cal.get(Calendar.MINUTE);
                int second = cal.get(Calendar.SECOND);
                String timeRevert = String.format("%02d", hours) + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second);
                AuthPictureModel authPictureModel = new AuthPictureModel(timeRevert, "Xác thực thành công");
                authPictureModels.add(authPictureModel);
                authPictureAdapter.notifyDataSetChanged();
                lvAuthPicture.scrollToPosition(authPictureModels.size() - 1);
                handler.postDelayed(this, 60000);// Optional, to repeat the task.
            }
        };
        handler.postDelayed(runnable, 60000);
//        handler.removeCallbacks(runnable);
    }

    private void UpdateTimeAndSpeed() {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
//                CarValue<Integer> speed = CarValue.UNIMPLEMENTED_INTEGER;
////                String v;
////                v = speed.getSpeedDisplayUnit();
//                Integer v = speed.getValue();
//                tvSpeed.setText(v.toString());
//                handler.postDelayed(this, 10000) ;// Optional, to repeat the task.
                getLastLocation();
                totalTime = totalTime + 1;
                tvTotalTime.setText(ConvertHms(totalTime));
                tvTotalTimeMissing.setText(ConvertHms(totalT - totalTime));
                tvTotalTimeComplete.setText(ConvertHms(totalTime));
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    private String ConvertHms(double timeD) {
        int hour = (int) Math.floor(timeD / 3600);
        int minute = (int) Math.floor((timeD - (hour * 3600)) / 60);
        double second = timeD - (hour * 3600) - (minute * 60);
        return padLeftZeros(String.valueOf(hour), 2) + " giờ " + padLeftZeros(String.valueOf(minute), 2) + " phút";
    }

    public String padLeftZeros(String inputString, int length) {
        return String.format("%1$" + length + "s", inputString).replace(' ', '0');
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            p1 = (float) location.getLongitude();
                            p2 = (float) location.getLatitude();
                            double dSpeed = location.getSpeed();
                            double currentSpeed = round(dSpeed, 3, BigDecimal.ROUND_HALF_UP);
                            double kmhSpeed = round(currentSpeed,
                                    3,
                                    BigDecimal.ROUND_HALF_UP);
//                            txtLat.setText("Longitude:" + location.getLongitude() + " Latitude:" + location.getLatitude()+"  SPEED="+kmhSpeed);

                            float[] results = new float[1];
                            if (curLocationLng != 0 && curLocationLat != 0) {
                                location.distanceBetween(
                                        location.getLatitude(), location.getLongitude(),
                                        curLocationLat, curLocationLng, results);
                                if (results[0] > 10) {
                                    totalDistance = totalDistance + results[0];
                                }
                            }
                            pbSpeed.setProgress((int) kmhSpeed);
                            curLocationLng = location.getLongitude();
                            curLocationLat = location.getLatitude();
                            tvTotalDistance.setText(String.valueOf(Math.ceil(totalDistance / 1000 * 100.0) / 100.0) + " km");
                            tvTotalDistanceComplete.setText(String.valueOf(Math.ceil(totalDistance / 1000 * 100.0) / 100.0) + " km");
                            tvTotalDistanceMissing.setText(String.valueOf(Math.ceil((totalD - totalDistance) / 1000 * 100.0) / 100.0) + " km");
                            if (kmhSpeed > 3) {
                                tvSpeed.setText(String.valueOf(kmhSpeed));
                                llInfoUser.setVisibility(View.GONE);
                                llInfoUserHaveSpeed.setVisibility(View.VISIBLE);
                            } else {
                                llInfoUser.setVisibility(View.VISIBLE);
                                llInfoUserHaveSpeed.setVisibility(View.GONE);
                                tvSpeed.setText("00");
                            }
                            requestNewLocationData();

                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
            }
        } else {

            requestPermissions();
        }
    }

    public void ShowCam() {

        camera = Camera.open();
        showCamera = new ShowCamera(this, camera);
        flCamera.addView(showCamera);

    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void InitView() {
        this.tvFullname = (TextView) findViewById(R.id.tvFullname);
        this.tvBirthDay = (TextView) findViewById(R.id.tvBirthDay);
        this.tvIdNo = (TextView) findViewById(R.id.tvIdNo);
        this.ivAvatarStudent = (ImageView) findViewById(R.id.ivAvatarStudent);
        this.llInfoUser = (LinearLayout) findViewById(R.id.llInfoUser);
        this.llInfoUserHaveSpeed = (LinearLayout) findViewById(R.id.llInfoUserHaveSpeed);
        this.pbSpeed = (ProgressBar) findViewById(R.id.progress_bar);
        this.tvFullnameTeacher = (TextView) findViewById(R.id.tvFullnameTeacher);
        this.tvFullnameTeacherHaveSpeed = (TextView) findViewById(R.id.tvFullnameTeacherHaveSpeed);
        this.tvFullnameHaveSpeed = (TextView) findViewById(R.id.tvFullnameHaveSpeed);
        this.tvBirthDayTeacher = (TextView) findViewById(R.id.tvBirthDayTeacher);
        this.tvIdNoTeacher = (TextView) findViewById(R.id.tvIdNoTeacher);
        this.ivTeacher = (ImageView) findViewById(R.id.ivTeacher);
        this.tvSpeed = (TextView) findViewById(R.id.tvSpeedMain);
        this.tvTotalDistance = (TextView) findViewById(R.id.tvTotalDistance);
        this.tvTotalTime = (TextView) findViewById(R.id.tvTotalTime);
        this.tvTotalDistanceComplete = (TextView) findViewById(R.id.tvTotalDistanceComplete);
        this.tvTotalDistanceMissing = (TextView) findViewById(R.id.tvTotalDistanceMissing);
        this.tvTotalTimeComplete = (TextView) findViewById(R.id.tvTotalTimeComplete);
        this.tvTotalTimeMissing = (TextView) findViewById(R.id.tvTotalTimeMissing);
        this.flCamera = (FrameLayout) findViewById(R.id.flCamera);
        tvTotalTime.setText(ConvertHms(totalTime));
        tvTotalTimeMissing.setText(ConvertHms(totalT - totalTime));
        tvTotalTimeComplete.setText(ConvertHms(totalTime));
        this.tvDateNow = (TextView) findViewById(R.id.tvDateNow);
        tvDateNow.setText(sDateNow);
        this.btLogOut = (Button) findViewById(R.id.btLogOut);
        this.lvAuthPicture = (RecyclerView) findViewById(R.id.lvAuthPicture);
//        lvAuthPicture.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager2 = new LinearLayoutManager(this);
        lvAuthPicture.setLayoutManager(layoutManager2);
        lvAuthPicture.setNestedScrollingEnabled(false);
    }

    private void InitValue() {
        this.student = SharedPreferencesUtil.getStudent(MainActivity.this);
        this.teacher = SharedPreferencesUtil.getTeacher(MainActivity.this);
        if (student != null && student.code != null) {
            this.tvFullname.setText(student.name);
            this.tvFullnameHaveSpeed.setText("Học viên: " + student.name);
            this.tvIdNo.setText(student.code);
            this.tvBirthDay.setText(student.birthDay);
            if (student.avatarId != null) {
//                String url = "http://hcsky.vn/api/Resource/get_link_image/" + student.avatarId;
//                String url = "http://apidat-test.blackwind.vn/api/Resource/get_link_image/" + student.avatarId;
                String url = getString(R.string.BASE_URL_IMAGE_REAL) + student.avatarId;
                new BitmapExtension(ivAvatarStudent).execute(url);
            }
        }
        if (teacher != null && teacher.code != null) {
            this.tvFullnameTeacher.setText(teacher.name);
            this.tvFullnameTeacherHaveSpeed.setText("Giảng viên: " + teacher.name);
            this.tvIdNoTeacher.setText(teacher.code);
            this.tvBirthDayTeacher.setText(teacher.birthDay);
            if (teacher.avatarId != null) {
//                String url = "http://hcsky.vn/api/Resource/get_link_image/" + teacher.avatarId;
//                String url = "http://apidat-test.blackwind.vn/api/Resource/get_link_image/" + teacher.avatarId;
                String url = getString(R.string.BASE_URL_IMAGE_REAL) + teacher.avatarId;
                new BitmapExtension(ivTeacher).execute(url);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}