package com.buannel.studio.pvt.ltd.zostream.ads

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface AdApi {
    @Headers("X-Client-Platform: android-tv", "X-Device-Type: tv")
    @GET("api/v4/ads/serve")
    suspend fun serve(
        @Query("placement") placement: String,
        @Query("platform") platform: String = "android-tv"
    ): AdServeResponse

    @Headers("X-Client-Platform: android-tv", "X-Device-Type: tv")
    @POST("api/v4/ads/events")
    suspend fun record(@Body request: AdEventRequest): AdEnvelope<Map<String, Any>>
}
