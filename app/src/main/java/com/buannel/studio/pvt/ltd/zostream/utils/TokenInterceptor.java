package com.buannel.studio.pvt.ltd.zostream.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TokenInterceptor implements Interceptor {

    private Context context;

    public TokenInterceptor(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {

        SharedPreferences prefs = context.getSharedPreferences("ZoStreamPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("access_token", null);

        Request request = chain.request();

        if (token != null) {

            Log.d("ZO_TOKEN", "Using Access Token: " + token);

            request = request.newBuilder()
                    .header("Authorization", AuthHeader.bearer(token))
                    .build();
        } else {
            Log.d("ZO_TOKEN", "No access token found");
        }

        Response response = chain.proceed(request);

        Log.d("ZO_TOKEN", "API Response Code: " + response.code());

        return response;
    }
}
