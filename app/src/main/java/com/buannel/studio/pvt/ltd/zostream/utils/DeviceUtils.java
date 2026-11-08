package com.buannel.studio.pvt.ltd.zostream.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import java.util.UUID;

public class DeviceUtils {
    public static String generateUuid() {
        return UUID.randomUUID().toString(); // e.g. "3f0c9b3e-7b6d-4a4f-9f9b-2d1c3e9a7b8c"
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    @SuppressLint("HardwareIds")
    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    // Combine both
    public static String getDeviceInfo(Context context) {
        return "Name: " + getDeviceName() + ", ID: " + getDeviceId(context);
    }
}
