package com.buannel.studio.pvt.ltd.zostream.payment

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.buannel.studio.pvt.ltd.zostream.ui.components.AmazonIapPaymentScreen
import com.buannel.studio.pvt.ltd.zostream.ui.theme.TvComposeIntroductionTheme

class AmazonIapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TvComposeIntroductionTheme {
                AmazonIapPaymentScreen(
                    onSuccess = {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }
}
