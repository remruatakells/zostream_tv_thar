package com.buannel.studio.pvt.ltd.zostream.api;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.buannel.studio.pvt.ltd.zostream.utils.TokenAuthenticator;
import com.buannel.studio.pvt.ltd.zostream.utils.TokenInterceptor;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Api {

    private static Retrofit retrofit;
    private static ApiInterface api;

    private Api() {}

    public static void init(Context context) {

        if (retrofit != null) return;

        Context appContext = context.getApplicationContext();

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new TokenInterceptor(appContext))   // attach token
                .authenticator(new TokenAuthenticator(appContext)); // refresh token

        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            clientBuilder.addInterceptor(logging);
        }

        OkHttpClient client = clientBuilder.build();

        retrofit = new Retrofit.Builder()
                .baseUrl("https://apis.zostream.in/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        api = retrofit.create(ApiInterface.class);
    }

    public static ApiInterface getApi() {
        return api;
    }
}
