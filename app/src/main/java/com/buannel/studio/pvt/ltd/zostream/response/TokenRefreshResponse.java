package com.buannel.studio.pvt.ltd.zostream.response;

import com.google.gson.annotations.SerializedName;

public class TokenRefreshResponse {
    private String status;
    private String message;

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("access_expires_at")
    private String accessExpiresAt;

    @SerializedName("token_type")
    private String tokenType;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getAccessToken() { return accessToken; }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAccessExpiresAt() { return accessExpiresAt; }
    public String getTokenType() { return tokenType; }
}
