package com.buannel.studio.pvt.ltd.zostream.ads;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Video-ad API contract for Android TV. */
public interface VideoAdApi {
    @Headers({"X-Client-Platform: android-tv", "X-Device-Type: tv"})
    @GET("api/v4/ads/serve")
    Call<AdEnvelope<ImageAd>> serve(
            @Query("placement") String placement,
            @Query("platform") String platform
    );

    @Headers({"X-Client-Platform: android-tv", "X-Device-Type: tv"})
    @POST("api/v4/ads/events")
    Call<AdEnvelope<Map<String, Object>>> record(@Body Map<String, Object> event);
}
