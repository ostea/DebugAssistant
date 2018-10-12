package com.mm.debugassistant.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by elegant.wang on 2017/8/5.
 */

public class ServiceGenerator {
    private static Retrofit.Builder builder = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create());
    private static OkHttpClient.Builder httpClient =
            new OkHttpClient.Builder();


    private static HttpLoggingInterceptor logging =
            new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY);

    public static <S> S createService(Class<S> serviceClass, String basUrl) {
        Retrofit.Builder builder =
                new Retrofit.Builder()
                        .baseUrl(basUrl)
                        .addConverterFactory(GsonConverterFactory.create());
        if (!httpClient.interceptors().contains(logging)) {
            httpClient.addInterceptor(logging);
            builder.client(httpClient.build());
        }
        Retrofit retrofit = builder.build();
        return retrofit.create(serviceClass);
    }
}
