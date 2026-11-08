package com.buannel.studio.pvt.ltd.zostream.response

import com.buannel.studio.pvt.ltd.zostream.model.Movie
import com.buannel.studio.pvt.ltd.zostream.model.Episode
import com.google.gson.annotations.SerializedName

data class WatchContinueResponse(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("data")
    val data: List<WatchHistoryItem>? = null,

    @SerializedName("watch_history")
    val watchHistory: List<WatchHistoryItem>? = null,

    @SerializedName("pagination")
    val pagination: Pagination? = null
)

data class WatchHistoryItem(
    @SerializedName("id")
    val id: Int? = null,

    // ✅ FIX: use String (handles both int & string)
    @SerializedName("movie_id")
    val movieId: String? = null,

    @SerializedName("episode_id")
    val episodeId: String? = null,

    @SerializedName("movie_type")
    val movieType: String? = null,

    @SerializedName("position")
    val position: Long? = null,

    @SerializedName("duration")
    val duration: Long? = null,

    @SerializedName("watched_at")
    val watchedAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("movie")
    val movie: Movie? = null,

    @SerializedName("episode")
    val episode: Episode? = null
)

data class Pagination(
    @SerializedName("current_page")
    val currentPage: Int? = null,

    @SerializedName("per_page")
    val perPage: Int? = null,

    @SerializedName("total")
    val total: Int? = null,

    @SerializedName("last_page")
    val lastPage: Int? = null,

    @SerializedName("from")
    val from: Int? = null,

    @SerializedName("to")
    val to: Int? = null,

    @SerializedName("has_more_pages")
    val hasMorePages: Boolean? = null,

    @SerializedName("next_page_url")
    val nextPageUrl: String? = null,

    @SerializedName("prev_page_url")
    val prevPageUrl: String? = null
)