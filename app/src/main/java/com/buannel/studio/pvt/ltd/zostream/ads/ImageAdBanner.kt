package com.buannel.studio.pvt.ltd.zostream.ads

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.buannel.studio.pvt.ltd.zostream.api.Api
import com.buannel.studio.pvt.ltd.zostream.ui.screens.tvDpadClick
import com.buannel.studio.pvt.ltd.zostream.utils.DeviceUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

@Composable
fun ImageAdBanner(placement: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ad by remember(placement) { mutableStateOf<ImageAd?>(null) }
    var focused by remember { mutableStateOf(false) }
    var impressionId by remember(ad?.trackingToken) { mutableStateOf<String?>(null) }
    val service = remember { runCatching { Api.createService(AdApi::class.java) }.getOrNull() }

    LaunchedEffect(placement, service) {
        if (service == null) {
            Log.w("ZoStreamAds", "Ad API is unavailable for $placement")
            return@LaunchedEffect
        }

        val result = runCatching { service.serve(placement) }
        result.exceptionOrNull()?.let {
            Log.w("ZoStreamAds", "Ad request failed for $placement", it)
        }
        val response = result.getOrNull()
        Log.d(
            "ZoStreamAds",
            "Ad response placement=$placement success=${response?.success} campaign=${response?.servedAd()?.campaignId}"
        )
        ad = response
            ?.servedAd()
            ?.takeIf {
                it.type.equals("image", ignoreCase = true) &&
                    listOf(it.mediaUrl, it.proxyMediaUrl, it.thumbnailUrl).any { url -> !url.isNullOrBlank() }
            }
    }

    val currentAd = ad ?: return
    // Prefer the signed same-origin proxy on TV. It avoids CDN/TLS failures
    // observed on some Android TV devices while retaining direct-media fallback.
    val imageUrl = currentAd.proxyMediaUrl?.takeIf { it.isNotBlank() }
        ?: currentAd.mediaUrl?.takeIf { it.isNotBlank() }
        ?: currentAd.thumbnailUrl
    // Keep the image model stable while focus state changes. This avoids a new
    // Coil request every time a D-pad focus border is drawn.
    val imageRequest = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    suspend fun ensureImpression(): String? {
        impressionId?.let { return it }
        if (service == null) return null
        val id = UUID.randomUUID().toString().also { impressionId = it }
        runCatching {
            service.record(
                AdEventRequest(
                    trackingToken = currentAd.trackingToken,
                    eventId = id,
                    event = "impression",
                    deviceId = DeviceUtils.getDeviceId(context)
                )
            )
        }
        return id
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 3f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.DarkGray)
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick {
                scope.launch {
                    ensureImpression()?.let { viewedId ->
                        withTimeoutOrNull(1_500) {
                            runCatching {
                                service?.record(
                                    AdEventRequest(
                                        trackingToken = currentAd.trackingToken,
                                        eventId = UUID.randomUUID().toString(),
                                        event = "click",
                                        impressionEventId = viewedId,
                                        deviceId = DeviceUtils.getDeviceId(context)
                                    )
                                )
                            }
                        }
                    }
                    (currentAd.targetUrl ?: currentAd.adUrl)?.takeIf { it.isNotBlank() }?.let {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    }
                }
            }
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = currentAd.name ?: "Advertisement",
            contentScale = ContentScale.Crop,
            onSuccess = { scope.launch { ensureImpression() } },
            onError = {
                Log.w("ZoStreamAds", "Unable to load ${placement} TV ad image")
            },
            modifier = Modifier.matchParentSize()
        )
        // An overlay border leaves image constraints unchanged. A border on
        // the parent changes the measured image size on focus and triggers a
        // visible reload on some Android TV devices.
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    if (focused) 3.dp else 1.dp,
                    if (focused) Color(0xFF38BDF8) else Color.White.copy(alpha = .2f),
                    RoundedCornerShape(14.dp)
                )
        )
        Text(
            text = "Ad",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = .7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
