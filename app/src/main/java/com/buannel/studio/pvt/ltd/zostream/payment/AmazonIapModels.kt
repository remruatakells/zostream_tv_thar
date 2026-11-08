package com.buannel.studio.pvt.ltd.zostream.payment

import com.google.gson.annotations.SerializedName

data class BillingPlansResponse(
    val status: String? = null,
    val data: List<BillingPlan> = emptyList()
)

data class BillingPlan(
    @SerializedName("plan_id") val planId: Int,
    @SerializedName("plan") val name: String,
    @SerializedName("duration_days") val durationDays: Int
)

data class AmazonIapVerifyRequest(
    @SerializedName("receipt_id") val receiptId: String,
    @SerializedName("amazon_user_id") val amazonUserId: String,
    @SerializedName("parent_sku") val parentSku: String,
    @SerializedName("term_sku") val termSku: String,
    @SerializedName("plan_id") val planId: Int,
    @SerializedName("device_id") val deviceId: String
)

data class AmazonPurchaseOption(
    val sku: String,
    val title: String,
    val description: String,
    val price: String,
    val context: AmazonPurchaseContext
)

data class AmazonPurchaseContext(
    val planId: Int
)

data class AmazonIapUiState(
    val loading: Boolean = false,
    val purchasing: Boolean = false,
    val products: List<AmazonPurchaseOption> = emptyList(),
    val message: String? = null,
    val verificationRetryAvailable: Boolean = false,
    val completed: Boolean = false
)
