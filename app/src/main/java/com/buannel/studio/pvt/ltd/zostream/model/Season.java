package com.buannel.studio.pvt.ltd.zostream.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Season {

    public int num;
    public String id;

    @SerializedName("movie_id")
    public int movieId;

    @SerializedName("season_number")
    public int seasonNumber;

    public String title;
    public String description;
    public String poster;

    @SerializedName("release_year")
    public Integer releaseYear;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

    public List<Episode> episodes;

}
