package com.buannel.studio.pvt.ltd.zostream.response

import com.google.gson.annotations.SerializedName

data class QrLoginResponse(

    @SerializedName("status")
    val status: String,

    @SerializedName("token")
    val token: String,

    @SerializedName("qr_url")
    val qrUrl: String,

    @SerializedName("expires_in")
    val expiresIn: Int
)