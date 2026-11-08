package com.buannel.studio.pvt.ltd.zostream.request

data class QrLoginRequest(
    val device_id: String,
    val device_name: String,
    val device_type: String,
    val type: String
)
