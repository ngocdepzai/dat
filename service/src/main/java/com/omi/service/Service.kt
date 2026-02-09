package com.omi.service

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object Service {
    inline fun <reified T> buildService(apiService: Class<T>, hostUrl: String, timeouts: Long = 30): T {
        return Retrofit.Builder()
            .baseUrl(hostUrl)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(timeouts, TimeUnit.SECONDS)
                    .readTimeout(timeouts, TimeUnit.SECONDS)
                    .callTimeout(timeouts, TimeUnit.SECONDS)
                    .writeTimeout(timeouts, TimeUnit.SECONDS)
                    .addInterceptor(Interceptor { chain ->
                        val newRequest: Request = chain.request().newBuilder()
                            .addHeader("Authorization", "HC-DAT")
                            .build()
                        chain.proceed(newRequest)
                    })
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
            .build()
            .create(T::class.java)
    }
}
