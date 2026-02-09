package hc.manager.datapp.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hc.manager.datapp.R;
import hc.manager.datapp.app.UserDataHelper;
import hc.manager.datapp.camera.CameraHelperSingleton;

public class MenuActivity extends Activity {

    UserDataHelper hcDatDatabase;
    ProgressDialog progressDialog;
    private ListView listView;
    private List<Map<String, Object>> mData;

    public static String getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return formatSize(totalBlocks * blockSize);
    }

    public static String getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long availableSpace = -1L;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)
                availableSpace = (long) stat.getBlockSizeLong() * (long) stat.getAvailableBlocksLong();
            else
                availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
            return formatSize(availableSpace);
        } else {
            return "";
        }
    }

    public static String getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            return formatSize(totalBlocks * blockSize);
        } else {
            return "";
        }
    }

    public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    public static String formatSize(long size) {
        String suffix = null;
        if (size >= 1024) {
            suffix = "KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = "MB";
                size /= 1024;
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        hcDatDatabase = new UserDataHelper(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        listView = (ListView) findViewById(R.id.listView1);
        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.listview_menuitem,
                new String[]{"title", "info", "img"},
                new int[]{R.id.title, R.id.info, R.id.img});
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                switch (pos) {
                    case 0: {
                        deleteData();
                        break;
                    }
                    case 1: {
                        try {
                            Intent intent;
                            if (CameraHelperSingleton.getInstance().getUsbDevice() != null) {
//                                intent = new Intent(MenuActivity.this, AddFaceActivity2.class);
                            } else {
//                                intent = new Intent(MenuActivity.this, AddFaceActivity.class);
                            }
                            overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
//                            startActivity(intent);
                        } finally {
                            finish();
                        }
                        break;
                    }
                    case 2: {
                        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        break;
                    }
                    case 3: {
                        try {
                            Intent intent = new Intent(MenuActivity.this, InfoDeviceActivity.class);
                            overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                            startActivity(intent);
                        } finally {
                            finish();
                        }
                        break;
                    }
//                    case 4: {
//                        try {
//                            Intent intent = new Intent(MenuActivity.this, ListUserActivity.class);
//                            overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
//                            startActivity(intent);
//                        } finally {
//                            finish();
//                        }
//                        break;
//                    }
                    case 4: {
                        try {
                            Intent intent = new Intent(MenuActivity.this, LoginActivity.class);
                            overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                            startActivity(intent);
                        } finally {
                            finish();
                        }
                        break;
                    }
                }
            }
        });
    }

    private void deleteData() {
        progressDialog = new ProgressDialog(MenuActivity.this);
        progressDialog.setTitle("THIẾT BỊ ĐANG XÓA DỮ LIỆU!");
        progressDialog.setCancelable(false);
        progressDialog.show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                progressDialog.dismiss();
                SimpleAdapter adapter = new SimpleAdapter(MenuActivity.this, getData(), R.layout.listview_menuitem,
                        new String[]{"title", "info", "img"},
                        new int[]{R.id.title, R.id.info, R.id.img});
                listView.setAdapter(adapter);
//                try {
//                    Intent intent = new Intent(MenuActivity.this, LoginActivity.class);
//                    overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
//                    startActivity(intent);
//                } finally {
//                    finish();
//                }
            }
        }, 3000);
        File filepath = Environment.getExternalStorageDirectory();
        File dirImage = new File(filepath.getAbsolutePath()
                + "/HC_DAT_IMAGE");
        File dirImages = new File(filepath.getAbsolutePath()
                + "/HC_DAT_IMAGES");
        deleteRecursive(dirImage);
        deleteRecursive(dirImages);
//        if (progressDialog != null && progressDialog.isShowing()) {
//            progressDialog.dismiss();
//        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        String totalSize = getTotalInternalMemorySize();
        String totalSize2 = getAvailableExternalMemorySize();
        Map<String, Object> map = new HashMap<String, Object>();
        map = new HashMap<String, Object>();
        map.put("title", "Xóa bộ nhớ");
        map.put("info", totalSize2 + " / " + totalSize);
        map.put("img", R.drawable.menu_users);
        list.add(map);

        map = new HashMap<String, Object>();
        map.put("title", "Cài đặt hình ảnh");
        map.put("info", "Nhận diện khuôn mặt bằng hình ảnh");
        map.put("img", R.drawable.menu_option);
        list.add(map);

        map = new HashMap<String, Object>();
        map.put("title", "Cài đặt mạng");
        map.put("info", "Cài đặt mạng");
        map.put("img", R.drawable.menu_network);
        list.add(map);

//        map = new HashMap<String, Object>();
//        map.put("title", getString(R.string.txt_title_05));
//        map.put("info", getString(R.string.txt_info_05));
//        map.put("img", R.drawable.menu_updown);
//        list.add(map);

        map = new HashMap<String, Object>();
        map.put("title", "Thông tin");
        map.put("info", "Thông tin");
        map.put("img", R.drawable.menu_about);
        list.add(map);

//        map = new HashMap<String, Object>();
//        map.put("title", "Danh sách người dùng");
//        map.put("info", "Danh sách người dùng");
//        map.put("img", R.drawable.menu_about);
//        list.add(map);

        map = new HashMap<String, Object>();
        map.put("title", "Thoát");
        map.put("info", "Thoát");
        map.put("img", R.drawable.menu_exit);
        list.add(map);

        mData = list;
        return list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.system, menu);
        return true;
    }

    private void ReturnMain() {
/*		ActivityList.getInstance().setNavigationBarState(true);
		ActivityList.getInstance().setStatusBarDisable(true);*/
        this.setResult(1);
        this.finish();
        overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            try {
                Intent intentLogin = new Intent(MenuActivity.this, LoginActivity.class);
                startActivity(intentLogin);
            } finally {
                finish();
            }

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                ReturnMain();
                return true;
            case R.id.action_settings:
                //Intent intent = new Intent(this, SettingsActivity.class);
                //startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
