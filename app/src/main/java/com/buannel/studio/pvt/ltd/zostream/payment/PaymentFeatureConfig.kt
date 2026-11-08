package com.buannel.studio.pvt.ltd.zostream.payment

import com.buannel.studio.pvt.ltd.zostream.BuildConfig
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PaymentFeatureConfig {
    private const val FIREBASE_PATH = "payment_features/amazon_tv/iap_enabled"

    private val _amazonIapEnabled = MutableStateFlow(false)
    val amazonIapEnabled: StateFlow<Boolean> = _amazonIapEnabled.asStateFlow()

    @Volatile
    private var started = false

    fun start() {
        if (started || !BuildConfig.AMAZON_IAP_ENABLED) return
        started = true

        FirebaseDatabase.getInstance()
            .getReference(FIREBASE_PATH)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    _amazonIapEnabled.value =
                        BuildConfig.AMAZON_IAP_ENABLED &&
                            (snapshot.getValue(Boolean::class.java) ?: false)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    _amazonIapEnabled.value = false
                }
            })
    }

    @JvmStatic
    fun isAmazonIapEnabled(): Boolean =
        BuildConfig.AMAZON_IAP_ENABLED && _amazonIapEnabled.value
}
