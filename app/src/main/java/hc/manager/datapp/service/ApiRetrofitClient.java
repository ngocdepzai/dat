package hc.manager.datapp.service;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import hc.manager.datapp.utils.SharedPreferencesUtil;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiRetrofitClient {
    private static Retrofit retrofit;

    public static Retrofit getClient(final Context context) {
//        DeviceModel deviceModel = SharedPreferencesUtil.getDevice(context);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .protocols(Arrays.asList(Protocol.HTTP_1_1))
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request request;
//                        String test = SharedPreferencesUtil.getValue(context,SharedPreferencesUtil.TOKEN);
                        if (SharedPreferencesUtil.getValue(context, SharedPreferencesUtil.TOKEN) == "") {
                            request = original.newBuilder()
                                    .header("Authorization", "HC-DAT")
                                    .method(original.method(), original.body())
                                    .build();
                        } else {
                            request = original.newBuilder()
                                    .header("Authorization", "Bearer " + SharedPreferencesUtil.getValue(context, SharedPreferencesUtil.TOKEN))
                                    .method(original.method(), original.body())
                                    .build();
                        }
//                        Request request = original.newBuilder()
//                                .header("Authorization", "Bearer "+ SharedPreferencesUtil.getValue(context,SharedPreferencesUtil.TOKEN))
//                                .method(original.method(), original.body())
//                                .build();
                        return chain.proceed(request);
                    }
                })
                .build();

        Gson gson = new GsonBuilder().setLenient().create();

        retrofit = new Retrofit.Builder()
//                .baseUrl("http://192.168.1.71:19674/")
                .baseUrl("http://api.hcsky.vn/")
//                .baseUrl("http://apidat-test.blackwind.vn/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit;
    }
}
