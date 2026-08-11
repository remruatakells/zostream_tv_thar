package com.buannel.studio.pvt.ltd.zostream.request;

import com.google.gson.annotations.SerializedName;

public class OtpRequest {
    private String user_id;
    private String phone_number;

    @SerializedName("country_code")
    private String countryCode;

    public OtpRequest(String user_id, String phone_number, String countryCode) {
        this.user_id = user_id;
        this.phone_number = phone_number;
        this.countryCode = countryCode;
    }
}
