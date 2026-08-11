package com.buannel.studio.pvt.ltd.zostream.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buannel.studio.pvt.ltd.zostream.api.Api
import com.buannel.studio.pvt.ltd.zostream.request.QrPaymentRequest
import com.buannel.studio.pvt.ltd.zostream.response.QrLoginResponse
import com.buannel.studio.pvt.ltd.zostream.utils.DeviceUtils
import com.buannel.studio.pvt.ltd.zostream.utils.AuthHeader
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QrViewModel : ViewModel() {

    var qrToken by mutableStateOf<String?>(null) // ✅ ADD THIS
    var timerText by mutableStateOf("")
    var loading by mutableStateOf(false)

    var paymentSuccess by mutableStateOf(false)

    var stopTimer by mutableStateOf(false)
    var showRetry by mutableStateOf(false)

    fun createQr(
        context: Context,
        isPpv: Boolean = false,
        movieId: String? = null,
        amount: Double? = null,
        contentType: String? = null,
        subscriptionId: Int? = null ) {

        loading = true

        val request = if (isPpv) {

            QrPaymentRequest(
                deviceId = SessionManager.getUserDeviceId(context),
                deviceName = SessionManager.getUserDeviceName(context),
                deviceType = "tv",

                userId = SessionManager.getUserId(context),

                type = "payment", // 🔥 important
                appPaymentType = "ppv",

                movieId = movieId,
                amount = amount,
                currency = "INR",

                contentType = contentType,
                subscriptionId = subscriptionId
            )

        } else {

            QrPaymentRequest(
                deviceId = SessionManager.getUserDeviceId(context),
                deviceName = SessionManager.getUserDeviceName(context),
                deviceType = "tv",

                userId = SessionManager.getUserId(context),

                type = "payment",
                appPaymentType = "subscription",
            )
        }

        Api.getApi().createPaymentQr(
            AuthHeader.bearer(SessionManager.getAccessToken(context)),
            SessionManager.getUserDeviceId(context),
            request
        )
            .enqueue(object : Callback<QrLoginResponse> {

                override fun onResponse(
                    call: Call<QrLoginResponse>,
                    response: Response<QrLoginResponse>
                ) {
                    loading = false

                    if (response.isSuccessful) {

                        val token = response.body()?.token ?: return

                        qrToken = token // ✅ ONLY STORE TOKEN

                        listenFirebase(token)
                        startTimer()

                    } else {
                        timerText = "Failed"
                    }
                }

                override fun onFailure(call: Call<QrLoginResponse>, t: Throwable) {
                    loading = false
                    timerText = "Error"
                }
            })
    }

    private fun listenFirebase(token: String) {

        FirebaseDatabase.getInstance()
            .getReference("qr_sessions")
            .child(token)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val status = snapshot.child("status").safeString()

                    when (status) {

                        "pending" -> {
                            stopTimer = true
                            timerText = "QR scanned..."
                        }

                        "payment_started" -> {
                            stopTimer = true
                            timerText = "Processing..."
                        }

                        "payment_completed" -> {
                            stopTimer = true
                            timerText = "Success ✅"
                            paymentSuccess = true
                        }

                        "failed" -> {
                            stopTimer = true
                            timerText = "Failed"
                            showRetry = true
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    stopTimer = true
                    timerText = "Error"
                    showRetry = true
                }
            })
    }

    private fun DataSnapshot.safeString(): String? {
        return when (val rawValue = value) {
            is String -> rawValue
            is Number -> rawValue.toString()
            is Boolean -> rawValue.toString()
            else -> null
        }
    }

    @SuppressLint("DefaultLocale")
    private fun startTimer() {
        viewModelScope.launch {

            stopTimer = false
            showRetry = false

            var time = 120

            while (time >= 0 && !stopTimer) {

                val minutes = time / 60
                val seconds = time % 60

                timerText = String.format("Expires in %02d:%02d", minutes, seconds)

                delay(1000)
                time--
            }

            if (!stopTimer) {
                timerText = "Expired"
                showRetry = true
            }
        }
    }
}
