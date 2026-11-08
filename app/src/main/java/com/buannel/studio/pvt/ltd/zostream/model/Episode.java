package com.buannel.studio.pvt.ltd.zostream.model;

import com.google.gson.annotations.SerializedName;

public class Episode {

    public int num;
    public String id;

    @SerializedName("season_id")
    public String seasonId;

    public boolean isPayPerView;
    public boolean isPremium;

    @SerializedName("episode_number")
    public int episodeNumber;

    public String title;
    public String description;
    public String thumbnail;

    public Integer duration;

    @SerializedName("release_date")
    public String releaseDate;

    @SerializedName("is_active")
    public boolean isActive;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

}