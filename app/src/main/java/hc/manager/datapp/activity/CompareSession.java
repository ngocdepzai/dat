package hc.manager.datapp.activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import hc.manager.datapp.R;
import hc.manager.datapp.adapter.ItemSessionAdapter;
import hc.manager.datapp.app.UserDataHelper;
import hc.manager.datapp.models.SessionModel;
import hc.manager.datapp.models.TrainingCenterModel;
import hc.manager.datapp.models.request.GetListSessionRequest;
import hc.manager.datapp.models.request.ResentSessionRequest;
import hc.manager.datapp.models.response.GetListSessionResponse;
import hc.manager.datapp.models.response.ResentSessionResponse;
import hc.manager.datapp.service.ApiService;
import hc.manager.datapp.service.DataService;
import hc.manager.datapp.utils.CustomToast;
import hc.manager.datapp.utils.SharedPreferencesUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompareSession extends AppCompatActivity {
    private static final String[] statusData = {"Tất cả", "Chưa truyền TC", "Đã truyền TC"};
    EditText etStartDate, etEndDate, etStudentCode;
    ItemSessionAdapter itemSessionAdapter;
    Button btNext, btSearch, btPrew;
    ProgressDialog progressDialog;
    String imeiDevice = "";
    Spinner spStatus;
    UserDataHelper hcDatDatabase;
    DataService dataService = ApiService.getService(this);
    GetListSessionRequest getListSessionRequest = new GetListSessionRequest();
    private List<SessionModel> sessions = new ArrayList<>();
    private int totalSession = 0;
    private RecyclerView lvSession;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_session);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initView();
        hcDatDatabase = new UserDataHelper(this);
        imeiDevice = hcDatDatabase.getImeiDevice();
        lvSession.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager2 = new LinearLayoutManager(this);
        lvSession.setLayoutManager(layoutManager2);
        lvSession.setNestedScrollingEnabled(false);
        itemSessionAdapter = new ItemSessionAdapter(sessions, CompareSession.this);
        lvSession.setAdapter(itemSessionAdapter);
        lvSession.scrollToPosition(sessions.size() - 1);
        etStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDateDialog(etStartDate, true);
            }
        });
        etEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDateDialog(etEndDate, false);
            }
        });
        getSessions();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(CompareSession.this,
                android.R.layout.simple_spinner_item, statusData);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(adapter);
        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    getListSessionRequest.setSendGeneral(null);
                }
                if (i == 1) {
                    getListSessionRequest.setSendGeneral(false);
                }
                if (i == 2) {
                    getListSessionRequest.setSendGeneral(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        itemSessionAdapter.setOnItemButtonClickListener(new ItemSessionAdapter.ItemButtonClickListener() {
            @Override
            public void onItemResentClickListener(int position) {
                SessionModel sessionModel = sessions.get(position);
                resentSessionToTC(sessionModel);
            }
        });
//        lvSession.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(@NonNull @NotNull RecyclerView recyclerView, int newState) {
//                super.onScrollStateChanged(recyclerView, newState);
//                if (!recyclerView.canScrollVertically(1)) {
//                    Toast.makeText(CompareSession.this, "Last", Toast.LENGTH_LONG).show();
//
//                }
//            }
//        });
        btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CheckCanNext()) {
                    getListSessionRequest.setPage(getListSessionRequest.page + 1);
                    getSessions();
                }
            }
        });
        btPrew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CheckCanPrew()) {
                    getListSessionRequest.setPage(getListSessionRequest.page - 1);
                    getSessions();
                }
            }
        });
        btSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getListSessionRequest.setPage(1);
                getSessions();
            }
        });
    }

    private void initView() {
        etStartDate = (EditText) findViewById(R.id.etStartDate);
        lvSession = (RecyclerView) findViewById(R.id.rvSession);
        etEndDate = (EditText) findViewById(R.id.etEndDate);
        etStudentCode = (EditText) findViewById(R.id.etStudentCode);
        btNext = (Button) findViewById(R.id.btNext);
        btPrew = (Button) findViewById(R.id.btPrew);
        btSearch = (Button) findViewById(R.id.btSearch);
        spStatus = (Spinner) findViewById(R.id.spStatus);
        etStartDate.setInputType(InputType.TYPE_NULL);
        etEndDate.setInputType(InputType.TYPE_NULL);
    }

    private boolean CheckCanNext() {
        if (getListSessionRequest.page * getListSessionRequest.limit < totalSession) {
            btNext.setBackgroundColor(Color.GREEN);
            return true;
        } else {
            btNext.setBackgroundColor(Color.GRAY);
            return false;
        }
    }

    private boolean CheckCanPrew() {
        if (getListSessionRequest.page > 1) {
            btPrew.setBackgroundColor(Color.GREEN);
            return true;
        } else {
            btPrew.setBackgroundColor(Color.GRAY);
            return false;
        }
    }

    private void showDateDialog(EditText dateChange, boolean isStart) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                calendar.set(i, i1, i2);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                dateChange.setText(simpleDateFormat.format(calendar.getTime()));
                SimpleDateFormat formatCallApi = new SimpleDateFormat("yyyy-MM-dd");
                if (isStart) {
                    getListSessionRequest.setStartTime(formatCallApi.format(calendar.getTime()));
                } else {
                    getListSessionRequest.setEndTime(formatCallApi.format(calendar.getTime()));
                }
            }
        }, year, month, day);
        datePickerDialog.show();
    }

    private void getSessions() {
        progressDialog = new ProgressDialog(CompareSession.this);
        progressDialog.setTitle("Vui lòng đợi");
        progressDialog.setCancelable(true);
        progressDialog.show();
        Call<GetListSessionResponse> callback;
        getListSessionRequest.setSeri(imeiDevice);
        callback = dataService.GetListSession(getListSessionRequest);
        callback.enqueue(new Callback<GetListSessionResponse>() {
            @Override
            public void onResponse(Call<GetListSessionResponse> call, Response<GetListSessionResponse> response) {
                if (response.body() != null) {
                    if (response.body().status == 1) {
                        if (response.body().sessions.size() >= 0) {
                            sessions.clear();
                            itemSessionAdapter.clear();
                            sessions.addAll(response.body().sessions);
                            totalSession = response.body().total;
                            itemSessionAdapter.notifyDataSetChanged();
                            CheckCanNext();
                            CheckCanPrew();
//                            lvSession.scrollToPosition(authPictureModels.size() - 1);
                        }
                    } else {
                        CustomToast.makeText(CompareSession.this, "Kết nối server lỗi!",
                                Toast.LENGTH_SHORT, 3).show();
                    }

                } else {
                    CustomToast.makeText(CompareSession.this, "Lỗi kết nối", Toast.LENGTH_SHORT, 3).show();
                }
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onFailure(Call<GetListSessionResponse> call, Throwable t) {
            }
        });
    }

    private void resentSessionToTC(SessionModel sessionModel) {
        // check quyền
        TrainingCenterModel trainingCenterModel = SharedPreferencesUtil.getTrainingCenter(CompareSession.this);
        if (trainingCenterModel != null && trainingCenterModel.isTeacherSendTc()) {
            progressDialog = new ProgressDialog(CompareSession.this);
            progressDialog.setTitle("Vui lòng đợi");
            progressDialog.setCancelable(true);
            progressDialog.show();
            Call<ResentSessionResponse> callback;
            ResentSessionRequest resentSessionRequest = new ResentSessionRequest();
            resentSessionRequest.setSessionId(sessionModel.id);
            callback = dataService.ResentSession(resentSessionRequest);
            callback.enqueue(new Callback<ResentSessionResponse>() {
                @Override
                public void onResponse(Call<ResentSessionResponse> call, Response<ResentSessionResponse> response) {
                    if (response.body() != null) {
                        if (response.body().status == 1) {
                            CustomToast.makeText(CompareSession.this, response.body().getMessage(),
                                    Toast.LENGTH_SHORT, 1).show();
                            getSessions();
                        } else {
                            CustomToast.makeText(CompareSession.this, response.body().getMessage(),
                                    Toast.LENGTH_SHORT, 3).show();
                        }

                    } else {
                        CustomToast.makeText(CompareSession.this, "Lỗi kết nối", Toast.LENGTH_SHORT, 3).show();
                    }
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }

                @Override
                public void onFailure(Call<ResentSessionResponse> call, Throwable t) {
                }
            });
        } else {
            new AlertDialog.Builder(CompareSession.this)
                    .setTitle("THÔNG BÁO")
                    .setMessage("KHÔNG CÓ QUYỀN, VUI LÒNG LIÊN HỆ VỚI TRUNG TÂM!")
                    .setPositiveButton("Tắt", null)
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
//        try {
//            Intent intentLogin = new Intent(CompareSession.this, LoginActivity.class);
//            startActivity(intentLogin);
//        } finally {
//            finish();
//        }
    }
}