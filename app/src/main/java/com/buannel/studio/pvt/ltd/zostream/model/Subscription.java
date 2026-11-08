package com.buannel.studio.pvt.ltd.zostream.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Subscription {

    public int id;

    @SerializedName("user_id")
    public String userId;

    @SerializedName("plan_id")
    public int planId;

    @SerializedName("start_at")
    public String startAt;

    @SerializedName("end_at")
    public String endAt;

    @SerializedName("is_active")
    public boolean isActive;

    @SerializedName("renewed_by")
    public String renewedBy;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

    public Plan plan;
    public List<Device> devices;

    @SerializedName("current_date")
    public String currentDate;

    @SerializedName("isAdsFree")
    public boolean isAdsFree;

}