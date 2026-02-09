package hc.manager.datapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import hc.manager.datapp.app.UserItem;
import hc.manager.datapp.models.DeviceModel;
import hc.manager.datapp.models.ResetDeviceModel;
import hc.manager.datapp.models.TrainingCenterModel;
import hc.manager.datapp.models.VehicleModel;

public class SharedPreferencesUtil {
    public static final String HCDAT = "HCDAT";
    public static final String TOKEN = "TOKEN";
    public static final String SERI = "SERI";
    public static final String USER_ID = "USER_ID";
    public static final String USER_NAME = "USER_NAME";
    public static final String USER_FULLNAME = "USER_FULLNAME";
    public static final String USER_CODE = "USER_CODE";
    public static final String STUDENT = "STUDENT";
    public static final String TEACHER = "TEACHER";
    public static final String STYLE_DASHBOARD = "HC_STYLE_DASHBOARD";
    public static final String DEVICE = "DEVICE";
    public static final String TRAINING_CENTER = "TRAINING_CENTER";
    public static final String VEHICLE = "VEHICLE";
    public static final String HAVE_GROUP_NAME = "HAVE_GROUP_NAME";
    public static final String SESSION = "SESSION";
    public static final String LOGIN_SUPPORT = "LOGIN_SUPPORT";

    public static void setSession(Context context, String value) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SESSION, value);
        editor.commit();
    }

    public static void removeSession(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(SESSION);
        editor.commit();
    }

    public static String getSession(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        return preferences.getString(SESSION, "");
    }

    public static void setValue(Context context, String key, String value) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String getValue(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        return preferences.getString(key, "");
    }

    // 0 là tối
    // 1 là sáng
    public static void setStyleDashboard(Context context, int value) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(STYLE_DASHBOARD, value);
        editor.commit();
    }

    public static int getStyleDashboard(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        return preferences.getInt(STYLE_DASHBOARD, 0);
    }

    public static int getHaveGroupName(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        return preferences.getInt(HAVE_GROUP_NAME, 0);
    }

    public static void setHaveGroupName(Context context, float value) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(HAVE_GROUP_NAME, value);
        editor.commit();
    }

    public static void setDevice(Context context, DeviceModel obj) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(obj);
        editor.putString(DEVICE, json);
        editor.commit();
    }

    public static DeviceModel getDevice(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString(DEVICE, "");
        return gson.fromJson(json, DeviceModel.class);
    }

    public static void setLoginSuport(Context context, ResetDeviceModel obj) {
        SharedPreferences preferences = context.getSharedPreferences(LOGIN_SUPPORT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(obj);
        editor.putString(LOGIN_SUPPORT, json);
        editor.commit();
    }

    public static ResetDeviceModel getLoginSuport(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(LOGIN_SUPPORT, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString(LOGIN_SUPPORT, "");
        return gson.fromJson(json, ResetDeviceModel.class);
    }

    public static void setTrainingCenter(Context context, TrainingCenterModel obj) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(obj);
        editor.putString(TRAINING_CENTER, json);
        editor.commit();
    }

    public static TrainingCenterModel getTrainingCenter(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString(TRAINING_CENTER, "");
        return gson.fromJson(json, TrainingCenterModel.class);
    }

    public static void setVehicle(Context context, VehicleModel obj) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(obj);
        editor.putString(VEHICLE, json);
        editor.commit();
    }

    public static VehicleModel getVehicle(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString(VEHICLE, "");
        return gson.fromJson(json, VehicleModel.class);
    }

    public static void setUser(Context context, UserItem obj) {
        obj.loginTime = System.currentTimeMillis() / 1000;
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(obj);
        if (obj.userType.equals(UserTypeContant.STUDENT)) {
            editor.putString(STUDENT, json);
        } else {
            editor.putString(TEACHER, json);
        }
        editor.commit();
    }

    public static UserItem getStudent(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString(STUDENT, "");
        return gson.fromJson(json, UserItem.class);
    }

    public static boolean checkGoToMain(Context context) {
        UserItem student = getStudent(context);
        UserItem teacher = getTeacher(context);
        if (student != null && teacher != null) {
            return true;
        }
        return false;
    }

    public static void setTeacher(Context context, UserItem obj) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(obj);
        editor.putString(STUDENT, json);
        editor.commit();
    }

    public static UserItem getTeacher(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString(TEACHER, "");
        return gson.fromJson(json, UserItem.class);
    }

    public static void removeTeacher(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(TEACHER);
        editor.commit();
    }

    public static void removeStudent(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(STUDENT);
        editor.commit();
    }

    public static Boolean checkExistUser(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String jsonStudent = preferences.getString(STUDENT, null);
        UserItem student = gson.fromJson(jsonStudent, UserItem.class);
        String jsonTeacher = preferences.getString(TEACHER, null);
        UserItem teacher = gson.fromJson(jsonTeacher, UserItem.class);
        if ((student != null && student.code != null) || (teacher != null && teacher.code != null)) {
            return true;
        }
        return false;
    }

    public static Boolean checkExistStudent(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String jsonStudent = preferences.getString(STUDENT, null);
        UserItem student = gson.fromJson(jsonStudent, UserItem.class);
        if ((student != null && student.code != null)) {
            return true;
        }
        return false;
    }

    public static Boolean checkExisTeacher(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(HCDAT, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String jsonTeacher = preferences.getString(TEACHER, null);
        UserItem teacher = gson.fromJson(jsonTeacher, UserItem.class);
        if ((teacher != null && teacher.code != null)) {
            return true;
        }
        return false;
    }
}
