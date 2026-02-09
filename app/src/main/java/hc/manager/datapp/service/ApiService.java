package hc.manager.datapp.service;

import android.content.Context;

public class ApiService {

    public static DataService getService(Context context) {
        return ApiRetrofitClient.getClient(context).create(DataService.class);
    }
}
