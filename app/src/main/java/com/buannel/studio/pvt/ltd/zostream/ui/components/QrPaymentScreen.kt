package com.buannel.studio.pvt.ltd.zostream.ui.components

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.buannel.studio.pvt.ltd.zostream.utils.QRUtils
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager
import com.buannel.studio.pvt.ltd.zostream.viewmodel.QrViewModel

@Composable
fun QrPaymentScreenUI(
    viewModel: QrViewModel = viewModel(),
    isPpv: Boolean,
    movieId: String?,
    amount: Double?,
    contentType: String?,
    subscriptionId: Int?,
    onSuccess: () -> Unit
) {

    val context = LocalContext.current
    val isOwnerDevice = SessionManager.getIsDeviceOwner(context)

    val qrUrl = viewModel.qrToken
    val timerText = viewModel.timerText
    val loading = viewModel.loading

    // ✅ AUTO CREATE QR
    LaunchedEffect(Unit) {
        if (isOwnerDevice) {
            viewModel.createQr(
                context = context,
                isPpv = isPpv,
                movieId = movieId,
                amount = amount,
                contentType = contentType,
                subscriptionId = subscriptionId
            )
        }
    }

    // ✅ AUTO NAVIGATE AFTER SUCCESS
    LaunchedEffect(viewModel.paymentSuccess) {
        if (viewModel.paymentSuccess) {
            onSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F2))
            .focusable(false),
        contentAlignment = Alignment.Center
    ) {

        Card(
            onClick = {}, // ✅ REQUIRED
            shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
            colors = CardDefaults.colors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .fillMaxHeight(0.7f)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {

                // 🔥 LEFT SIDE (QR)
                Column(
                    modifier = Modifier.weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Box(
                        modifier = Modifier.size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        when {
                            !isOwnerDevice -> Text(
                                "Only the account owner can subscribe or rent content.",
                                color = Color.Black
                            )

                            loading -> Text("Loading...", color = Color.Black)

                            qrUrl != null -> {

                                Box(contentAlignment = Alignment.Center) {

                                    AndroidView(
                                        factory = { context ->
                                            ImageView(context).apply {
                                                layoutParams = android.view.ViewGroup.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                            }
                                        },
                                        update = { imageView ->
                                            QRUtils.generateQR(imageView, qrUrl)
                                        },
                                        modifier = Modifier.size(200.dp)
                                    )

                                    // 🔥 SHOW RETRY WHEN EXPIRED
                                    if (timerText == "Expired") {
                                        TvQrButton(
                                            text = "Retry",
                                            autoFocus = true
                                        ) {
                                            viewModel.createQr(
                                                context = context,
                                                isPpv = isPpv,
                                                movieId = movieId,
                                                amount = amount,
                                                contentType = contentType,
                                                subscriptionId = subscriptionId
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = timerText,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,     // top transparent
                                    Color.Black,       // middle visible
                                    Color.Black,
                                    Color.Transparent      // bottom transparent
                                )
                            )
                        )
                )

                // 🔥 RIGHT SIDE
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp), // ✅ start + end margi
                    verticalArrangement = Arrangement.Center
                ) {

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Complete Your Payment",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "To proceed, please follow the steps below:",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. Open the Zo Stream mobile application",
                        fontSize = 16.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "2. Tap on the QR scan icon located at the top right corner",
                        fontSize = 16.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "3. Scan the QR code displayed on this screen",
                        fontSize = 16.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "4. Wait for the payment to be processed successfully",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun TvQrButton(
    text: String,
    autoFocus: Boolean = false,
    onClick: () -> Unit
) {
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    // 🔥 Auto focus when shown
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    Surface(
        onClick = onClick,

        modifier = Modifier
            .width(160.dp)
            .height(55.dp)
            .focusRequester(focusRequester), // 🔥 IMPORTANT

        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x0C6B6B6B),
            focusedContainerColor = Color(0xFF4197FF) // 🔥 focus color
        ),

        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(Color(0xFF384FFF), 16.dp)
        ),

        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1.08f
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White
            )
        }
    }
}
