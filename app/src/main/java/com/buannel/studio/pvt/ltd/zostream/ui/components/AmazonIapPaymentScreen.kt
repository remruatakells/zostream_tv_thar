package com.buannel.studio.pvt.ltd.zostream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.buannel.studio.pvt.ltd.zostream.payment.AmazonIapManager
import com.buannel.studio.pvt.ltd.zostream.payment.AmazonPurchaseOption
import com.buannel.studio.pvt.ltd.zostream.ui.screens.TvButton

@Composable
fun AmazonIapPaymentScreen(
    onSuccess: () -> Unit
) {
    val state by AmazonIapManager.state.collectAsState()

    LaunchedEffect(Unit) {
        AmazonIapManager.prepareSubscription()
    }

    LaunchedEffect(state.completed) {
        if (state.completed) onSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F18)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .fillMaxHeight(0.72f)
                .background(Color(0xFF111827), RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Subscribe with Amazon",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Your purchase is securely processed by your Amazon account.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 17.sp
                    )
                    state.message?.let {
                        Spacer(Modifier.height(20.dp))
                        Text(it, color = Color(0xFFFBBF24), fontSize = 16.sp)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    when {
                        state.loading -> Text("Loading Amazon products...", color = Color.White)
                        state.purchasing -> Text("Completing your Amazon purchase...", color = Color.White)
                        state.products.isNotEmpty() -> LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.products, key = { it.sku }) { option ->
                                AmazonProductRow(option)
                            }
                        }
                        else -> TvButton(
                            text = if (state.verificationRetryAvailable) "Retry verification" else "Retry",
                            onClick = {
                                if (state.verificationRetryAvailable) {
                                    AmazonIapManager.retryVerification()
                                } else {
                                    AmazonIapManager.prepareSubscription()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmazonProductRow(option: AmazonPurchaseOption) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F2937), RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Text(option.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (option.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(option.description, color = Color(0xFFCBD5E1), fontSize = 14.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(option.price, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            TvButton(text = "Buy with Amazon") {
                AmazonIapManager.purchase(option)
            }
        }
    }
}
