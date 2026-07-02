package com.buannel.studio.pvt.ltd.zostream.request;

public class OtpRequest {
    private String user_id;
    private String phone_number;

    public OtpRequest(String user_id, String phone_number) {
        this.user_id = user_id;
        this.phone_number = phone_number;
    }
}
