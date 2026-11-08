package com.buannel.studio.pvt.ltd.zostream.payment

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserDataResponse
import com.buannel.studio.pvt.ltd.zostream.api.Api
import com.buannel.studio.pvt.ltd.zostream.utils.AuthHeader
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.LinkedHashMap

object AmazonIapManager : PurchasingListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val contexts = LinkedHashMap<String, AmazonPurchaseContext>()
    private val _state = MutableStateFlow(AmazonIapUiState())
    val state: StateFlow<AmazonIapUiState> = _state.asStateFlow()

    private lateinit var application: Application
    private var initialized = false
    private var pendingReceipt: Receipt? = null
    private var pendingAmazonUserId: String? = null

    @Synchronized
    fun initialize(app: Application) {
        if (initialized) return
        application = app
        PurchasingService.enablePendingPurchases()
        PurchasingService.registerListener(app, this)
        PurchasingService.getUserData()
        initialized = true
    }

    fun prepareSubscription() {
        if (!PaymentFeatureConfig.isAmazonIapEnabled()) return
        _state.value = AmazonIapUiState(loading = true)

        Api.getApi().getTvBillingPlans().enqueue(object : Callback<BillingPlansResponse> {
            override fun onResponse(
                call: Call<BillingPlansResponse>,
                response: Response<BillingPlansResponse>
            ) {
                val plans = response.body()?.data.orEmpty()
                if (!response.isSuccessful || plans.isEmpty()) {
                    fail("No Amazon subscription plan is available.")
                    return
                }

                contexts.clear()
                plans.forEach { plan ->
                    AmazonSku.termFor(plan)?.let { termSku ->
                        contexts[termSku] = AmazonPurchaseContext(planId = plan.planId)
                    }
                }
                requestProductData()
            }

            override fun onFailure(call: Call<BillingPlansResponse>, error: Throwable) {
                fail(error.localizedMessage ?: "Unable to load subscription plans.")
            }
        })
    }

    fun purchase(option: AmazonPurchaseOption) {
        if (!PaymentFeatureConfig.isAmazonIapEnabled() || !contexts.containsKey(option.sku)) {
            fail("Amazon purchasing is not enabled.")
            return
        }

        _state.value = _state.value.copy(
            purchasing = true,
            message = "Waiting for Amazon...",
            completed = false
        )
        PurchasingService.purchase(option.sku)
    }

    fun reset() {
        _state.value = AmazonIapUiState()
        contexts.clear()
        pendingReceipt = null
        pendingAmazonUserId = null
    }

    fun retryVerification() {
        val receipt = pendingReceipt
        val amazonUserId = pendingAmazonUserId
        if (receipt == null || amazonUserId.isNullOrBlank()) {
            fail("There is no Amazon receipt to retry.")
            return
        }
        verifyReceipt(receipt, amazonUserId)
    }

    private fun requestProductData() {
        if (contexts.isEmpty()) {
            fail("No Amazon product is configured.")
            return
        }
        PurchasingService.getProductData(contexts.keys + AmazonSku.PARENT)
    }

    override fun onUserDataResponse(response: UserDataResponse) = Unit

    override fun onProductDataResponse(response: ProductDataResponse) {
        mainHandler.post {
            if (response.requestStatus != ProductDataResponse.RequestStatus.SUCCESSFUL) {
                fail("Amazon product information is unavailable.")
                return@post
            }

            val options = contexts.mapNotNull { (sku, context) ->
                response.productData[sku]?.let { product ->
                    AmazonPurchaseOption(
                        sku = sku,
                        title = product.title,
                        description = product.description,
                        price = product.price,
                        context = context
                    )
                }
            }

            if (options.isEmpty()) {
                val unavailable = response.unavailableSkus.joinToString()
                fail(
                    if (unavailable.isBlank()) "No Amazon product is available."
                    else "Amazon product not configured: $unavailable"
                )
            } else {
                _state.value = AmazonIapUiState(products = options)
            }
        }
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        mainHandler.post {
            when (response.requestStatus) {
                PurchaseResponse.RequestStatus.SUCCESSFUL -> {
                    val receipt = response.receipt
                    val amazonUserId = response.userData?.userId
                    if (receipt == null || amazonUserId.isNullOrBlank()) {
                        fail("Amazon returned an incomplete purchase receipt.")
                    } else {
                        verifyReceipt(receipt, amazonUserId)
                    }
                }

                PurchaseResponse.RequestStatus.PENDING -> {
                    _state.value = _state.value.copy(
                        purchasing = false,
                        message = "Amazon payment is pending approval."
                    )
                }

                PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                    _state.value = _state.value.copy(message = "Restoring your Amazon purchase...")
                    PurchasingService.getPurchaseUpdates(true)
                }

                PurchaseResponse.RequestStatus.INVALID_SKU -> fail("Amazon product is invalid.")
                PurchaseResponse.RequestStatus.NOT_SUPPORTED -> fail("Amazon IAP is not supported on this device.")
                else -> fail("Amazon payment was cancelled or failed.")
            }
        }
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        mainHandler.post {
            if (response.requestStatus != PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) {
                fail("Unable to restore Amazon purchases.")
                return@post
            }

            val receipt = response.receipts.firstOrNull { receipt ->
                receipt.sku == AmazonSku.PARENT && contexts.containsKey(receipt.termSku)
            }
            val amazonUserId = response.userData?.userId

            if (receipt != null && !amazonUserId.isNullOrBlank()) {
                verifyReceipt(receipt, amazonUserId)
            } else if (response.hasMore()) {
                PurchasingService.getPurchaseUpdates(false)
            } else {
                fail("No matching Amazon purchase was found.")
            }
        }
    }

    private fun verifyReceipt(receipt: Receipt, amazonUserId: String) {
        val termSku = receipt.termSku
        val context = contexts[termSku]
        if (receipt.sku != AmazonSku.PARENT || termSku.isNullOrBlank() || context == null) {
            PurchasingService.notifyFulfillment(receipt.receiptId, FulfillmentResult.UNAVAILABLE)
            fail("Amazon product does not match ZoStream content.")
            return
        }

        pendingReceipt = receipt
        pendingAmazonUserId = amazonUserId
        _state.value = _state.value.copy(purchasing = true, message = "Verifying purchase...")
        val request = AmazonIapVerifyRequest(
            receiptId = receipt.receiptId,
            amazonUserId = amazonUserId,
            parentSku = receipt.sku,
            termSku = termSku,
            planId = context.planId,
            deviceId = SessionManager.getUserDeviceId(application)
        )

        Api.getApi().verifyAmazonIap(
            AuthHeader.bearer(SessionManager.getAccessToken(application)),
            SessionManager.getUserDeviceId(application),
            request
        ).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                val success = response.isSuccessful &&
                    response.body()?.get("status")?.asString.equals("success", ignoreCase = true)
                if (success) {
                    PurchasingService.notifyFulfillment(receipt.receiptId, FulfillmentResult.FULFILLED)
                    pendingReceipt = null
                    pendingAmazonUserId = null
                    _state.value = _state.value.copy(
                        loading = false,
                        purchasing = false,
                        message = "Payment successful",
                        verificationRetryAvailable = false,
                        completed = true
                    )
                } else {
                    val message = errorMessage(response) ?: "Amazon receipt verification failed."
                    verificationFailed(message)
                }
            }

            override fun onFailure(call: Call<JsonObject>, error: Throwable) {
                verificationFailed(error.localizedMessage ?: "Amazon receipt verification failed.")
            }
        })
    }

    private fun errorMessage(response: Response<JsonObject>): String? = try {
        response.errorBody()?.string()?.let { body ->
            JsonParser().parse(body).asJsonObject.get("message")?.asString
        }
    } catch (_: Exception) {
        null
    }

    private fun fail(message: String) {
        pendingReceipt = null
        pendingAmazonUserId = null
        _state.value = _state.value.copy(
            loading = false,
            purchasing = false,
            message = message,
            verificationRetryAvailable = false,
            completed = false
        )
    }

    private fun verificationFailed(message: String) {
        _state.value = _state.value.copy(
            loading = false,
            purchasing = false,
            products = emptyList(),
            message = message,
            verificationRetryAvailable = true,
            completed = false
        )
    }
}
