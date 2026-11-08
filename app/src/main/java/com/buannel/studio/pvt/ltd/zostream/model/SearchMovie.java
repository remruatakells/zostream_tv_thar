package com.buannel.studio.pvt.ltd.zostream.model;

import com.google.gson.annotations.SerializedName;

public class SearchMovie {

    @SerializedName("cover_img")
    public String coverImg;

    @SerializedName("title_img")
    public String titleImg;

    @SerializedName("create_date")
    public String createDate;

    @SerializedName("description")
    public String description;

    @SerializedName("director")
    public String director;

    @SerializedName("duration")
    public String duration;

    @SerializedName("genre")
    public String genre;

    @SerializedName("id")
    public String id;

    @SerializedName("isProtected")
    public boolean isProtected;

    @SerializedName("isBollywood")
    public boolean isBollywood;

    @SerializedName("isCompleted")
    public boolean isCompleted;

    @SerializedName("isDocumentary")
    public boolean isDocumentary;

    @SerializedName("isAgeRestricted")
    public boolean isAgeRestricted;

    @SerializedName("isDubbed")
    public boolean isDubbed;

    @SerializedName("isEnable")
    public boolean isEnable;

    @SerializedName("isHollywood")
    public boolean isHollywood;

    @SerializedName("isKorean")
    public boolean isKorean;

    @SerializedName("isMizo")
    public boolean isMizo;

    @SerializedName("isPayPerView")
    public boolean isPayPerView;

    @SerializedName("isPremium")
    public boolean isPremium;

    @SerializedName("isSeason")
    public boolean isSeason;

    @SerializedName("isSubtitle")
    public boolean isSubtitle;

    @SerializedName("subtitle")
    public String subtitle;

    @SerializedName("num")
    public int num;

    @SerializedName("poster")
    public String poster;

    @SerializedName("ppv_amount")
    public String ppvAmount;

    @SerializedName("release_on")
    public String releaseOn;

    @SerializedName("title")
    public String title;

    @SerializedName("url")
    public String url;

    @SerializedName("dash_url")
    public String dashUrl;

    @SerializedName("hls_url")
    public String hlsUrl;

    @SerializedName("trailer")
    public String trailer;

    @SerializedName("views")
    public int views;

    @SerializedName("token")
    public String token;

    @SerializedName("status")
    public String status;

    @SerializedName("isChildMode")
    public boolean isChildMode;

    @SerializedName("relevance")
    public double relevance;
}