package com.buannel.studio.pvt.ltd.zostream.ads

import com.google.gson.annotations.SerializedName

data class AdEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val message: String? = null
)

data class ImageAd(
    @SerializedName("campaign_id") val campaignId: Long,
    @SerializedName("creative_id") val creativeId: Long,
    val name: String? = null,
    val type: String = "",
    @SerializedName("media_url") val mediaUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("target_url") val targetUrl: String? = null,
    @SerializedName("ad_url") val adUrl: String? = null,
    val placement: String = "",
    @SerializedName("tracking_token") val trackingToken: String = ""
)

data class AdEventRequest(
    @SerializedName("tracking_token") val trackingToken: String,
    @SerializedName("event_id") val eventId: String,
    val event: String,
    @SerializedName("impression_event_id") val impressionEventId: String? = null,
    @SerializedName("device_id") val deviceId: String? = null
)
