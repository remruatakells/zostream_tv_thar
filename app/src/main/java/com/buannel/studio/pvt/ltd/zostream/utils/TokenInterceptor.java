package com.buannel.studio.pvt.ltd.zostream.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

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

        Request.Builder requestBuilder = chain.request().newBuilder()
                .header("X-Client-Platform", "android-tv")
                .header("X-Client-Version", appVersion())
                .header("X-Device-Type", "tv");

        if (token != null) {
            requestBuilder.header("Authorization", AuthHeader.bearer(token));
        }

        Response response = chain.proceed(requestBuilder.build());
        ResponseBody body = response.body();

        if (body == null || body.contentType() == null
                || !"application".equals(body.contentType().type())
                || !"json".equals(body.contentType().subtype())) {
            return response;
        }

        String json = body.string();
        try {
            JsonElement parsed = new JsonParser().parse(json);
            if (parsed.isJsonObject()) {
                JsonObject object = parsed.getAsJsonObject();
                if (object.has("success") && object.has("data") && object.has("error")
                        && object.get("success").getAsBoolean()) {
                    json = object.get("data").toString();
                }
            }
        } catch (RuntimeException ignored) {
            // Preserve non-standard JSON so Retrofit can report it normally.
        }

        return response.newBuilder()
                .body(ResponseBody.create(body.contentType(), json))
                .build();
    }

    private String appVersion() {
        try {
            String version = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionName;
            return version == null ? "android-tv" : version;
        } catch (Exception ignored) {
            return "android-tv";
        }
    }
}
