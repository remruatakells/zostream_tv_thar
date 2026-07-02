package com.buannel.studio.pvt.ltd.zostream.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.buannel.studio.pvt.ltd.zostream.ui.screens.LoginActivity;

public class SessionManager {

    private static final String PREF_NAME = "ZoStreamPrefs";
    public static final String MODE_ADULT = "adult";
    public static final String MODE_KIDS = "kids";

    public static boolean checkUserSession(Activity activity) {

        SharedPreferences prefs =
                activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (!isLoggedIn) {

            Intent intent = new Intent(activity, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);

            activity.startActivity(intent);
            activity.finish();
            return false;
        }

        return true;
    }

    public static String getAccessToken(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return prefs.getString("access_token", "");
    }

    public static String getUserId(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return prefs.getString("uid", "");
    }

    public static String getUserDeviceId(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return prefs.getString("device_id", "");
    }

    public static String getUserDeviceName(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return prefs.getString("device_name", "");
    }

    public static boolean getAgeRestriction(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return prefs.getBoolean("ageRestriction", false);
    }

    public static void setAgeRestriction(Context context, boolean enabled) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("ageRestriction", enabled);
        editor.apply();
    }

    public static void setParentalMode(Context context, String mode) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("parental_mode", mode);
        editor.apply();
    }

    public static String getParentalMode(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return prefs.getString("parental_mode", MODE_ADULT);
    }

    public static boolean getIsDeviceOwner(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return prefs.getBoolean("isDeviceOwner", false);
    }

    // ✅ Logout Function
    public static void logout(Activity activity) {

        SharedPreferences prefs =
                activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // remove all saved session data
        editor.apply();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);

        activity.startActivity(intent);
        activity.finish();
    }
}
