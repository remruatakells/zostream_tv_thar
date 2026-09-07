package com.buannel.studio.pvt.ltd.zostream.ads

import com.google.gson.annotations.SerializedName

data class AdEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val message: String? = null
)

/**
 * Ad serving is deployed in both V4-envelope and older direct-payload forms.
 * Keep the TV client compatible while every API worker is rolled forward.
 */
data class AdServeResponse(
    val success: Boolean? = null,
    val data: ImageAd? = null,
    @SerializedName("campaign_id") val campaignId: Long = 0,
    @SerializedName("creative_id") val creativeId: Long = 0,
    val name: String? = null,
    val type: String = "",
    @SerializedName("media_url") val mediaUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("proxy_media_url") val proxyMediaUrl: String? = null,
    @SerializedName("target_url") val targetUrl: String? = null,
    @SerializedName("ad_url") val adUrl: String? = null,
    val placement: String = "",
    @SerializedName("tracking_token") val trackingToken: String = ""
) {
    fun servedAd(): ImageAd? {
        data?.let { return it }
        if (creativeId <= 0 || type.isBlank()) return null

        return ImageAd(
            campaignId = campaignId,
            creativeId = creativeId,
            name = name,
            type = type,
            mediaUrl = mediaUrl,
            thumbnailUrl = thumbnailUrl,
            proxyMediaUrl = proxyMediaUrl,
            targetUrl = targetUrl,
            adUrl = adUrl,
            placement = placement,
            trackingToken = trackingToken
        )
    }
}

data class ImageAd(
    @SerializedName("campaign_id") val campaignId: Long,
    @SerializedName("creative_id") val creativeId: Long,
    val name: String? = null,
    val type: String = "",
    @SerializedName("media_url") val mediaUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("proxy_media_url") val proxyMediaUrl: String? = null,
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
