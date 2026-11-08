package com.buannel.studio.pvt.ltd.zostream.model;

import com.google.gson.annotations.SerializedName;

public class Plan {

    public int id;
    public String name;

    @SerializedName("device_type")
    public String deviceType;

    @SerializedName("device_limit")
    public int deviceLimit;

    public String price;

    @SerializedName("duration_days")
    public int durationDays;

    public String quality;

    @SerializedName("is_active")
    public boolean isActive;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

}