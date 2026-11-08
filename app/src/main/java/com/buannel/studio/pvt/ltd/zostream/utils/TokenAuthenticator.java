package com.buannel.studio.pvt.ltd.zostream.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.buannel.studio.pvt.ltd.zostream.R;
import com.buannel.studio.pvt.ltd.zostream.api.Api;
import com.buannel.studio.pvt.ltd.zostream.response.TokenRefreshResponse;
import com.buannel.studio.pvt.ltd.zostream.ui.screens.LoginActivity;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

public class TokenAuthenticator implements Authenticator {

    private Context context;
    private static final Object LOCK = new Object();

    public TokenAuthenticator(Context context) {
        this.context = context;
    }

    @Override
    public Request authenticate(Route route, Response response) {

        synchronized (LOCK) {

            Log.d("ZO_TOKEN", "Access token expired. Refreshing token...");

            SharedPreferences prefs = context.getSharedPreferences("ZoStreamPrefs", Context.MODE_PRIVATE);

            String refreshToken = prefs.getString("refresh_token", null);
            String currentToken = prefs.getString("access_token", null);

            String requestToken = response.request().header("Authorization");

            if (requestToken != null && !requestToken.equals(AuthHeader.bearer(currentToken))) {

                Log.d("ZO_TOKEN", "Token already refreshed by another request");

                return response.request().newBuilder()
                        .header("Authorization", AuthHeader.bearer(currentToken))
                        .build();
            }

            try {

                Log.d("ZO_TOKEN", "Calling refresh API...");

                Call<TokenRefreshResponse> call =
                        Api.getApi().refreshToken(refreshToken);

                retrofit2.Response<TokenRefreshResponse> res = call.execute();

                Log.d("ZO_TOKEN", "Refresh API HTTP Code: " + res.code());

                if (res.isSuccessful() && res.body() != null && res.body().getAccessToken() != null) {

                    String newAccessToken = res.body().getAccessToken();
                    String newRefreshToken = res.body().getRefreshToken();

                    prefs.edit()
                            .putString("access_token", newAccessToken)
                            .putString("refresh_token", newRefreshToken)
                            .apply();

                    return response.request().newBuilder()
                            .header("Authorization", AuthHeader.bearer(newAccessToken))
                            .build();
                } else {
                    Log.d("ZO_TOKEN", "Refresh token expired. Logging out...");

                    prefs.edit().clear().apply();

                    new Handler(Looper.getMainLooper()).post(() -> AppDialog.show(
                            context,
                            R.drawable.warning,
                            "Session Expired",
                            "Please login again",
                            false,
                            true,
                            () -> {   // OK button action

                                Intent intent = new Intent(context, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                context.startActivity(intent);

                            },
                            null
                    ));

                    return null;
                }

            } catch (Exception e) {
                Log.e("ZO_TOKEN", "Refresh error: " + e.getMessage());
            }

            return null;
        }
    }
}
