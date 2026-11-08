package com.buannel.studio.pvt.ltd.zostream.model;

import com.google.gson.annotations.SerializedName;

public class Device {

    public int id;

    @SerializedName("subscription_id")
    public int subscriptionId;

    @SerializedName("user_id")
    public String userId;

    @SerializedName("device_name")
    public String deviceName;

    @SerializedName("device_type")
    public String deviceType;

    @SerializedName("device_token")
    public String deviceToken;

    @SerializedName("is_owner_device")
    public boolean isOwnerDevice;

    @SerializedName("last_activity")
    public String lastActivity;

    public String status;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

}