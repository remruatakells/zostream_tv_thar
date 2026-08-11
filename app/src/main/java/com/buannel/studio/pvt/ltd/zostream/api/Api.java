package com.buannel.studio.pvt.ltd.zostream.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import com.buannel.studio.pvt.ltd.zostream.utils.TokenAuthenticator;
import com.buannel.studio.pvt.ltd.zostream.utils.TokenInterceptor;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Api {

    private static final String DEBUG_EMULATOR_BASE_URL = "https://test-api.zostream.in/";
    private static final String OFFICIAL_PREFS = "official_client_cache";
    private static final String KEY_ACCEPTED = "accepted";
    private static final String KEY_API_BASE_URL = "api_base_url";
    private static Retrofit retrofit;
    private static ApiInterface api;
    private static String baseUrl = "";

    private Api() {}

    public static void init(Context context) {

        if (retrofit != null) return;

        if (baseUrl.trim().isEmpty()) {
            SharedPreferences prefs = context.getSharedPreferences(OFFICIAL_PREFS, Context.MODE_PRIVATE);
            if (prefs.getBoolean(KEY_ACCEPTED, false)) {
                baseUrl = normalizeRootBaseUrl(prefs.getString(KEY_API_BASE_URL, ""));
            }
        }

        if (baseUrl.trim().isEmpty() && isDebuggable(context) && isProbablyEmulator()) {
            baseUrl = normalizeRootBaseUrl(DEBUG_EMULATOR_BASE_URL);
        }

        if (baseUrl.trim().isEmpty()) return;

        rebuild(context, baseUrl);
    }

    public static synchronized void setBaseUrl(Context context, String url) {
        String normalized = normalizeRootBaseUrl(url);
        if (normalized.isEmpty() || (normalized.equals(baseUrl) && retrofit != null)) return;

        baseUrl = normalized;
        rebuild(context, baseUrl);
    }

    private static void rebuild(Context context, String retrofitBaseUrl) {

        Context appContext = context.getApplicationContext();

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new TokenInterceptor(appContext))   // attach token
                .authenticator(new TokenAuthenticator(appContext)); // refresh token

        if (isDebuggable(context)) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            clientBuilder.addInterceptor(logging);
        }

        OkHttpClient client = clientBuilder.build();

        retrofit = new Retrofit.Builder()
                .baseUrl(retrofitBaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        api = retrofit.create(ApiInterface.class);
    }

    private static String normalizeRootBaseUrl(String url) {
        if (url == null) return "";

        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.endsWith("/api/v4")) {
            normalized = normalized.substring(0, normalized.length() - "/api/v4".length());
        }

        return normalized + "/";
    }

    private static boolean isDebuggable(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private static boolean isProbablyEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.toLowerCase().contains("google_sdk")
                || Build.MODEL.toLowerCase().contains("emulator")
                || Build.MODEL.toLowerCase().contains("android sdk built for")
                || Build.MANUFACTURER.toLowerCase().contains("genymotion")
                || Build.BRAND.toLowerCase().startsWith("generic")
                || Build.DEVICE.toLowerCase().startsWith("generic")
                || Build.PRODUCT.toLowerCase().contains("sdk");
    }

    public static ApiInterface getApi() {
        if (api == null) {
            throw new IllegalStateException("Zo Stream API is unavailable until official verification returns api_base_url.");
        }
        return api;
    }
}
