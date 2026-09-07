package com.buannel.studio.pvt.ltd.zostream.api

import com.buannel.studio.pvt.ltd.zostream.model.Movie
import com.buannel.studio.pvt.ltd.zostream.payment.AmazonIapVerifyRequest
import com.buannel.studio.pvt.ltd.zostream.payment.BillingPlansResponse
import com.buannel.studio.pvt.ltd.zostream.request.OTPVerifyRequest
import com.buannel.studio.pvt.ltd.zostream.request.OtpRequest
import com.buannel.studio.pvt.ltd.zostream.request.QrPaymentRequest
import com.buannel.studio.pvt.ltd.zostream.response.ApiResponse
import com.buannel.studio.pvt.ltd.zostream.response.BannerResponse
import com.buannel.studio.pvt.ltd.zostream.response.CheckPpvRentalResponse
import com.buannel.studio.pvt.ltd.zostream.response.HomeRecommendationResponse
import com.buannel.studio.pvt.ltd.zostream.response.MovieDetailsResponse
import com.buannel.studio.pvt.ltd.zostream.response.MovieFilterResponse
import com.buannel.studio.pvt.ltd.zostream.response.QrLoginResponse
import com.buannel.studio.pvt.ltd.zostream.response.SeasonResponse
import com.buannel.studio.pvt.ltd.zostream.response.TokenRefreshResponse
import com.buannel.studio.pvt.ltd.zostream.response.UserResponse
import com.buannel.studio.pvt.ltd.zostream.response.WatchContinueResponse
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface ApiInterface {

    @POST("api/v4/auth/otp/request")
    fun requestOtp(
        @Body request: OtpRequest?
    ): Call<ApiResponse?>?

    @POST("api/v4/auth/otp/verify")
    fun verifyOtp(
        @Body request: OTPVerifyRequest?
    ): Call<ApiResponse?>?

    @FormUrlEncoded
    @POST("api/v4/auth/tokens/refresh")
    fun refreshToken(
        @Field("refresh_token") refreshToken: String?,
    ): Call<TokenRefreshResponse>

    @FormUrlEncoded
    @POST("api/v4/auth/logout")
    fun revokeToken(
        @Header("Authorization") bearerToken: String,
        @Field("access_token") refreshToken: String?,
    ): Call<ApiResponse>

    // ➕ Start Stream
    @POST("api/v4/playback/sessions")
    fun startStream(
        @Header("Authorization") bearerToken: String,
        @Header("Device-Token") deviceToken: String,
        @Body body: JsonObject
    ): Call<JsonObject>

    @POST("api/v4/playback/sessions/stop")
    fun stopStream(
        @Header("Authorization") bearerToken: String,
        @Header("Device-Token") deviceToken: String,
        @Body body: JsonObject
    ): Call<JsonObject>

    @GET("api/v4/catalog/home")
    fun homeSection(
        @Header("Authorization") bearerToken: String,
        @Header("X-Mode") mode: String,
        @Query("age_restriction") ageRestriction: Boolean,
        @Query("user_id") uid: String,
    ): Call<Map<String, List<Movie>>>

    @GET("api/v4/recommendations/home")
    fun homeRecommendations(
        @Header("Authorization") bearerToken: String,
        @Header("X-Mode") mode: String,
        @Query("age_restriction") ageRestriction: Boolean,
        @Query("section") section: String?,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Call<HomeRecommendationResponse>

    @GET("api/v4/catalog/items/{contentId}/details")
    fun getDetails(
        @Header("Authorization") bearerToken: String,
        @Path("contentId") contentId: String,
        @Query("user_id") userId: String,
        @Query("device_id") deviceId: String,
        @Query("device_type") deviceType: String,
        @Query("type") type: String,
        @Query("movie_id") movieId: String,
    ): Call<MovieDetailsResponse>

    @GET("api/v4/catalog/items/{num}/seasons")
    fun getSeasons(
        @Header("Authorization") bearerToken: String,
        @Path("num") num: Int,
    ): Call<SeasonResponse>

    @GET("api/v4/account")
    fun findUser(
        @Header("Authorization") bearerToken: String,
        @Query("uid") uid: String
    ): Call<UserResponse>

    @GET("api/v4/catalog/items/search")
    fun searchMovies(
        @Header("X-User-Id") uid: String?,
        @Query("q") query: String?,
        @Query("age_restriction") isAgeRestrict: Boolean
    ): Call<List<Movie?>?>?


    @POST("api/v4/qr-sessions")
    fun createQr(
        @Body request: QrPaymentRequest
    ): Call<QrLoginResponse>

    @POST("api/v4/qr-sessions/payment")
    fun createPaymentQr(
        @Header("Authorization") bearerToken: String,
        @Header("Device-Token") deviceToken: String,
        @Body request: QrPaymentRequest
    ): Call<QrLoginResponse>

    @GET("/api/v4/catalog/items/{contentId}/recommendations")
    fun getAlsoLike(
        @Header("Authorization") bearerToken: String,
        @Path("contentId") contentId: String,
        @Header("X-User-Id") uid: String?,
        @Query("movie_title") title: String?,
        @Query("age_restriction") age: Boolean?
    ): Call<List<Movie?>?>?

    @GET("/api/v4/catalog/items/{contentId}/ppv-status")
    fun checkPayPerViewRental(
        @Header("Authorization") bearerToken: String?,
        @Path("contentId") pathContentId: String?,
        @Query("type") type: String?,
        @Query("content_id") contentId: String?,
        @Query("season_id") seasonId: String?,
        @Query("user_id") userId: String?,
        @Query("device_type") deviceType: String?
    ): Call<CheckPpvRentalResponse?>?

    @GET("/api/v4/library/history")
    fun getWatchContinue(
        @Header("Authorization") bearerToken: String,
        @Query("userId") userId: String,
        @Query("parental_mode") parentalMode: String,
        @Query("isAgeRestricted") isAgeRestricted: Boolean,
    ): Call<WatchContinueResponse>

    @GET("api/v4/catalog/items/filter")
    fun getMovie(
        @Header("Authorization") bearerToken: String,
        @Query("age_restriction") ageRestriction: Boolean,
        @Query("isChildMode") isChildMode: Boolean,
        @Query("user_id") uid: String,
        @Query("category") category: String?,
        @Query("genre") genre: String?,
        @Query("page") page: Int?,
    ): Call<MovieFilterResponse>

    @GET("/api/v4/banners")
    fun getBanner(
    ): Call<BannerResponse>

    @GET("api/v4/billing/plans/device/tv")
    fun getTvBillingPlans(): Call<BillingPlansResponse>

    @POST("api/v4/billing/payments/amazon/verify")
    fun verifyAmazonIap(
        @Header("Authorization") bearerToken: String,
        @Header("Device-Token") deviceToken: String,
        @Body request: AmazonIapVerifyRequest
    ): Call<JsonObject>

}
