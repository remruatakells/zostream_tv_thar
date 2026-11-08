package com.buannel.studio.pvt.ltd.zostream.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Movie implements Serializable {

    public int num;
    public String id;
    public String title;
    public String description;
    public String director;
    public String duration;
    public String genre;
    public String poster;

    @SerializedName("cover_img")
    public String coverImg;

    @SerializedName("title_img")
    public String titleImg;

    @SerializedName("release_on")
    public String releaseOn;

    public int views;
    public String status;

    @SerializedName("isPremium")
    public boolean isPremium;

    @SerializedName("isPayPerView")
    public boolean isPayPerView;

    @SerializedName("isChildMode")
    public boolean isChildMode;

    @SerializedName("isAgeRestricted")
    public boolean isAgeRestricted;

    @SerializedName("isCompleted")
    public boolean isCompleted;

    @SerializedName("isSeason")
    public boolean isSeason;

    @SerializedName("create_date")
    public String createDate;

    public String trailer;

    @SerializedName("ppv_amount")
    public String ppvAmount;

    @SerializedName("watch_position")
    public int watchPosition;

    @SerializedName("watch_duration")
    public int watchDuration;

    // ✅ NEW FIELD (nullable)
    @SerializedName("ppv_details")
    public PpvDetails ppvDetails;

    // ✅ INNER CLASS
    public static class PpvDetails implements Serializable {

        @SerializedName("isRented")
        public Boolean isRented;

        @SerializedName("rentalPurchased")
        public String rentalPurchased;

        @SerializedName("rentalExpiry")
        public String rentalExpiry;

        @SerializedName("daysLeft")
        public String daysLeft;
    }
}
