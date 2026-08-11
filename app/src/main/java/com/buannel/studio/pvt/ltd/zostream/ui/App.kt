package com.buannel.studio.pvt.ltd.zostream.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.buannel.studio.pvt.ltd.zostream.R
import com.buannel.studio.pvt.ltd.zostream.api.Api
import com.buannel.studio.pvt.ltd.zostream.ui.components.QrPaymentScreenUI
import com.buannel.studio.pvt.ltd.zostream.ui.player.PlayerActivity
import com.buannel.studio.pvt.ltd.zostream.ui.screens.MainNavigation
import com.buannel.studio.pvt.ltd.zostream.ui.screens.details.DetailsScreen
import com.buannel.studio.pvt.ltd.zostream.utils.AppDialog
import com.buannel.studio.pvt.ltd.zostream.utils.AuthHeader
import com.buannel.studio.pvt.ltd.zostream.utils.FullScreenErrorDialog
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val DETAILS_PAYMENT_REFRESH_KEY = "details_payment_refresh"

private fun getErrorString(errorObj: JsonObject?, key: String, fallback: String): String {
    if (errorObj == null) return fallback

    if (errorObj.has(key) && !errorObj.get(key).isJsonNull) {
        return errorObj.get(key).asString
    }

    if (errorObj.has("error") && errorObj.get("error").isJsonObject) {
        val nestedError = errorObj.getAsJsonObject("error")
        if (nestedError.has(key) && !nestedError.get(key).isJsonNull) {
            return nestedError.get(key).asString
        }
    }

    return fallback
}

private fun isDeviceRevokedError(code: String?, title: String?, message: String?): Boolean {
    val normalizedCode = code.orEmpty().trim().uppercase()
    val normalizedTitle = title.orEmpty().trim().lowercase()
    val normalizedMessage = message.orEmpty().trim().lowercase()

    return normalizedCode == "DEVICE_REVOKED" ||
            normalizedTitle.contains("device access changed") ||
            normalizedMessage.contains("device was removed") ||
            normalizedMessage.contains("no longer linked") ||
            normalizedMessage.contains("sign in again on this device")
}

@OptIn(UnstableApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun App(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (!navController.popBackStack()) {
            showExitDialog = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = "/"
    ) {

        composable("/") {
            MainNavigation(
                onMovieSelected = {
                    navController.navigate("/movie/${it.id}")
                },
                onMovieIdSelected = {
                    navController.navigate("/movie/$it")
                },
            )
        }

        composable(
            route = "/qr-payment?isPpv={isPpv}&movieId={movieId}&amount={amount}&contentType={contentType}&subscriptionId={subscriptionId}",
            arguments = listOf(
                navArgument("isPpv") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("movieId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("amount") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("contentType") { // ✅ ADD THIS
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("subscriptionId") { // ✅ ADD THIS
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->

            val isPpv = backStackEntry.arguments?.getBoolean("isPpv") ?: false
            val movieId = backStackEntry.arguments?.getString("movieId")
            val amount = backStackEntry.arguments?.getString("amount")?.toDoubleOrNull()
            val contentType = backStackEntry.arguments?.getString("contentType")
            val subscriptionId = backStackEntry.arguments?.getString("subscriptionId")?.toIntOrNull()

            QrPaymentScreenUI(
                isPpv = isPpv,
                movieId = movieId,
                amount = amount,
                contentType = contentType,
                subscriptionId = subscriptionId,
                onSuccess = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(DETAILS_PAYMENT_REFRESH_KEY, System.currentTimeMillis())
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "/movie/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val movieId = backStackEntry.arguments?.getString("id") ?: ""
            val paymentRefreshKey by backStackEntry.savedStateHandle
                .getStateFlow(DETAILS_PAYMENT_REFRESH_KEY, 0L)
                .collectAsState()

            DetailsScreen(
                movieId = movieId,
                refreshKey = paymentRefreshKey,
                accessToken = SessionManager.getAccessToken(context),
                userId = SessionManager.getUserId(context),
                deviceId = SessionManager.getUserDeviceId(context),

                onPlay = { movie, episode, seasonId, subscription, type ->

                    val accessToken = SessionManager.getAccessToken(context)
                    val deviceToken = SessionManager.getUserDeviceId(context)
                    val userId = SessionManager.getUserId(context)

                    val contentId = if (type == "episode") {
                        episode?.id
                    } else {
                        movie.id
                    }

                    if (contentId.isNullOrEmpty()) {
                        FullScreenErrorDialog.show(
                            context,
                            "Playback Error",
                            "Unable to determine playable content.",
                            true
                        ) {}
                        return@DetailsScreen
                    }

                    val body = JsonObject().apply {
                        addProperty("subscription_id", subscription?.id)
                        addProperty("movie_id", contentId)
                        addProperty("user_id", userId)
                        addProperty("season_id", seasonId)
                        addProperty("type", type)
                    }

                    Api.getApi().startStream(
                        AuthHeader.bearer(accessToken),
                        deviceToken,
                        body
                    ).enqueue(object : Callback<JsonObject> {

                        @OptIn(UnstableApi::class)
                        override fun onResponse(
                            call: Call<JsonObject>,
                            response: Response<JsonObject>
                        ) {

                            if (!response.isSuccessful) {

                                val errorJson = response.errorBody()?.string()

                                val gson = Gson()
                                val errorObj = gson.fromJson(errorJson, JsonObject::class.java)

                                val code = getErrorString(errorObj, "code", "")
                                val title = getErrorString(errorObj, "title", "Error")
                                val message = getErrorString(errorObj, "message", "Something went wrong")

                                if (isDeviceRevokedError(code, title, message)) {
                                    (context as? Activity)?.let { activity ->
                                        SessionManager.logoutWithReason(
                                            activity,
                                            message.ifBlank {
                                                "Your device was removed from this plan after renewal. Please sign in again to continue."
                                            }
                                        )
                                    }
                                    return
                                }

                                if (message.contains("rent")) {

                                    if (!SessionManager.getIsDeviceOwner(context)) {
                                        Toast.makeText(context, "Only owner can rent", Toast.LENGTH_SHORT).show()
                                        return
                                    }

                                    AppDialog.show(
                                        context,
                                        R.drawable.warning,
                                        title,
                                        message,
                                        true,
                                        false,
                                        {
                                            val isPpv = true
                                            val amountValue = movie.ppvAmount ?: 0.0

                                            navController.navigate(
                                                "/qr-payment?isPpv=$isPpv" +
                                                        "&movieId=$contentId" +
                                                        "&amount=$amountValue"+
                                                        "&contentType=$type" +
                                                        "&subscriptionId=${subscription?.id}"
                                            )
                                        },
                                        null
                                    )
                                } else if (message.contains("subscri")){

                                    if (!SessionManager.getIsDeviceOwner(context)) {
                                        Toast.makeText(context, "Only owner can subscribe", Toast.LENGTH_SHORT).show()
                                        return
                                    }

                                    AppDialog.show(
                                        context,
                                        R.drawable.warning,
                                        title,
                                        message,
                                        true,
                                        false,
                                        {
                                            navController.navigate("/qr-payment?isPpv=false")
                                        },
                                        null
                                    )
                                } else {
                                    AppDialog.show(
                                        context,
                                        R.drawable.warning,
                                        title,
                                        message,
                                        false,
                                        false,
                                        null,
                                        null
                                    )
                                }

                                return
                            }

                            val res = response.body()

                            if (res?.get("status")?.asString == "success") {

                                val streamUrl = try {
                                    val movieLinks = res.getAsJsonObject("movie_links")
                                    when {
                                        movieLinks == null -> null
                                        movieLinks.has("links") && !movieLinks.get("links").isJsonNull -> {
                                            val linksElement = movieLinks.get("links")
                                            when {
                                                linksElement.isJsonPrimitive -> linksElement.asString
                                                linksElement.isJsonObject -> {
                                                    val linksObject = linksElement.asJsonObject
                                                    when {
                                                        linksObject.has("hls") && !linksObject.get("hls").isJsonNull ->
                                                            linksObject.get("hls").asString

                                                        linksObject.has("url") && !linksObject.get("url").isJsonNull ->
                                                            linksObject.get("url").asString

                                                        linksObject.has("dash_url") && !linksObject.get("dash_url").isJsonNull ->
                                                            linksObject.get("dash_url").asString

                                                        else -> null
                                                    }
                                                }
                                                else -> null
                                            }
                                        }
                                        else -> null
                                    }
                                } catch (_: Exception) {
                                    null
                                }

                                val streamToken =
                                    if (res.has("stream_token") && !res.get("stream_token").isJsonNull)
                                        res.get("stream_token").asString
                                    else
                                        ""

                                val maxQuality =
                                    if (res.has("max_quality") && !res.get("max_quality").isJsonNull)
                                        res.get("max_quality").asString
                                    else
                                        ""

                                val watchPosition =
                                    if (res.has("watch_position") && !res.get("watch_position").isJsonNull)
                                        res.get("watch_position").asString
                                    else
                                        ""

                                if (!streamUrl.isNullOrEmpty()) {

                                    val intent = Intent(context, PlayerActivity::class.java)

                                    val num = episode?.episodeNumber ?: ""

                                    intent.putExtra("episode", num)
                                    intent.putExtra("streamUrl", streamUrl)
                                    intent.putExtra("title", movie.title)
                                    intent.putExtra("subscriptionId", subscription?.id)
                                    intent.putExtra("movieId", contentId)
                                    intent.putExtra("movieNum", movie.num)
                                    intent.putExtra("streamToken", streamToken)
                                    intent.putExtra("watchPosition", watchPosition)
                                    intent.putExtra("maxQuality", maxQuality)
                                    intent.putExtra("type", type)
                                    intent.putExtra("isEpisode", type == "episode")

                                    context.startActivity(intent)

                                } else {
                                    FullScreenErrorDialog.show(
                                        context,
                                        "Playback Error",
                                        "No stream URL was returned for this content.",
                                        true
                                    ) {}
                                }

                            } else {
                                val message =
                                    if (res != null && res.has("message") && !res.get("message").isJsonNull)
                                        res.get("message").asString
                                    else
                                        "Something went wrong"

                                FullScreenErrorDialog.show(
                                    context,
                                    "Playback Error",
                                    message,
                                    true
                                ) {}
                            }
                        }

                        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                            FullScreenErrorDialog.show(
                                context,
                                "Network Error",
                                t.message ?: "Unable to start playback.",
                                true
                            ) {}
                        }
                    })
                },

                onQrPayment = { movieId, isPPV, subscriptionId, type ->

                    if (isPPV == true) {
                        navController.navigate(
                            "/qr-payment?isPpv=true" +
                                    "&movieId=${movieId}" +
                                    "&amount=${0}" +
                                    "&contentType=$type" +
                                    "&subscriptionId=${subscriptionId}"
                        )
                    } else {
                        navController.navigate("/qr-payment?isPpv=false")
                    }
                }
            )
        }

        composable(
            route = "/player?url={url}&title={title}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->

            val currentContext = LocalContext.current
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""

            LaunchedEffect(url) {
                val intent = Intent(currentContext, PlayerActivity::class.java)
                intent.putExtra("streamUrl", url)
                intent.putExtra("title", title)
                currentContext.startActivity(intent)
            }
        }
    }

    if (showExitDialog) {
        ExitConfirmDialog(
            onExit = {
                showExitDialog = false
                (context as? Activity)?.finish()
            },
            onDismiss = {
                showExitDialog = false
            }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ExitConfirmDialog(
    onExit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(420.dp)
                    .background(Color(0xFF101827), RoundedCornerShape(8.dp))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Exit App",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Are you sure you want to exit Zo Stream TV?",
                    color = Color(0xFFD1D5DB)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onDismiss) {
                        Text("Stay")
                    }
                    Button(onClick = onExit) {
                        Text("Exit")
                    }
                }
            }
        }
    }
}
