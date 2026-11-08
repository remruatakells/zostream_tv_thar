package com.buannel.studio.pvt.ltd.zostream.response;

import com.google.gson.annotations.SerializedName;

public class ApiResponse {
    private String status;
    private String user_id;
    private String message;
    private Data data; // 👈 nested object

    public String getStatus() {
        return status;
    }
    public String getUserId() {
        return user_id;
    }

    public String getMessage() {
        return message;
    }

    public Data getData() {
        return data;
    }

    // ✅ Inner class to map the "data" object
    public static class Data {
        private String uid;

        @SerializedName("is_owner_device")
        private boolean isOwnerDevice;

        @SerializedName("access_token")
        private String accessToken;

        @SerializedName("access_expires_at")
        private String accessExpiresAt;

        @SerializedName("refresh_token")
        private String refreshToken;

        @SerializedName("refresh_expires_at")
        private String refreshExpiresAt;

        @SerializedName("token_type")
        private String tokenType;

        @SerializedName("device_name")
        private String deviceName;

        @SerializedName("device_id")
        private String deviceId;

        public String getUid() { return uid; }

        public boolean getIsOwnerDevice() { return isOwnerDevice; }
        public String getAccessToken() { return accessToken; }
        public String getAccessExpiresAt() { return accessExpiresAt; }
        public String getRefreshToken() { return refreshToken; }
        public String getRefreshExpiresAt() { return refreshExpiresAt; }
        public String getTokenType() { return tokenType; }
        public String getDeviceName() { return deviceName; }
        public String getDeviceId() { return deviceId; }

    }
}