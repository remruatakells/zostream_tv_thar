package com.buannel.studio.pvt.ltd.zostream.request

import com.google.gson.annotations.SerializedName

data class QrPaymentRequest(

    @SerializedName("device_id")
    val deviceId: String? = null,

    @SerializedName("movie_id")
    val movieId: String? = null,

    @SerializedName("device_name")
    val deviceName: String? = null,

    @SerializedName("device_type")
    val deviceType: String? = null,

    @SerializedName("amount")
    val amount: Double? = null,

    @SerializedName("currency")
    val currency: String? = null,

    @SerializedName("plan_id")
    val planId: Int? = null,

    @SerializedName("app_payment_type")
    val appPaymentType: String? = null,

    @SerializedName("payment_method")
    val paymentMethod: String? = null,

    @SerializedName("payment_gateway")
    val paymentGateway: String? = null,

    @SerializedName("transaction_id")
    val transactionId: String? = null,

    @SerializedName("note")
    val note: String? = null,

    @SerializedName("type")
    val type: String? = "login", // default from backend

    @SerializedName("status")
    val status: String? = "initialized",

    @SerializedName("user_id")
    val userId: String? = null,

    @SerializedName("expires_at")
    val expiresAt: Long? = null, // usually backend handles

    @SerializedName("content_type")
    val contentType: String? = null,

    @SerializedName("subscription_id")
    val subscriptionId: Int? = null

)