package com.buannel.studio.pvt.ltd.zostream.request;


public class OTPVerifyRequest {
    private String user_id;
    private String otp;

    private String device_name;

    private String device_id;
    private String device_type;

    public OTPVerifyRequest(String user_id, String otp, String device_name, String device_id, String device_type) {
        this.user_id = user_id;
        this.otp = otp;
        this.device_name = device_name;
        this.device_id = device_id;
        this.device_type = device_type;

    }
}
