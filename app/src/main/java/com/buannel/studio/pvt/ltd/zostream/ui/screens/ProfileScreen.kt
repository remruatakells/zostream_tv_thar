package com.buannel.studio.pvt.ltd.zostream.ui.screens

import android.app.Activity
import android.widget.ImageView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.buannel.studio.pvt.ltd.zostream.api.Api
import com.buannel.studio.pvt.ltd.zostream.model.User
import com.buannel.studio.pvt.ltd.zostream.response.UserResponse
import com.buannel.studio.pvt.ltd.zostream.utils.AuthHeader
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager
import com.buannel.studio.pvt.ltd.zostream.utils.QRUtils
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private data class SupportCategory(val name: String, val issues: List<String>)

private val supportCategories = listOf(
    SupportCategory("Video & Playback", listOf(
        "Video Won't Play", "Loading / Buffering", "Playback Stops or Freezes",
        "App Crashes During Playback", "No Audio", "Audio and Video Out of Sync",
        "Poor Video Quality", "Quality Selection Problem",
        "Seek / Fast-forward Problem", "Continue Watching / Resume Problem",
        "Fullscreen Problem", "Picture-in-Picture Problem",
        "Live Channel Problem", "Other Playback Problem"
    )),
    SupportCategory("Login & OTP", listOf(
        "OTP Not Received", "WhatsApp OTP Not Received", "OTP Invalid or Expired",
        "Login Failed", "Phone Number Not Recognized",
        "Unable to Access Existing Account",
        "Session Expired / Logged Out Automatically", "QR Login Problem",
        "Other Login Problem"
    )),
    SupportCategory("Subscription & Payment", listOf(
        "Payment Successful but Subscription Inactive",
        "Amount Deducted but Plan Not Activated", "Payment Failed",
        "Payment Stuck on Pending", "Subscription Expiry Date Incorrect",
        "Renewal Problem", "Plan Upgrade / Change Problem", "Wrong Plan Activated",
        "Duplicate Payment", "Payment History / Receipt Problem", "Refund Problem",
        "Other Subscription Problem"
    )),
    SupportCategory("Movie & Episode", listOf(
        "Movie Won't Play", "Movie or Episode Missing", "Wrong Movie / Episode",
        "Episode Order Incorrect", "Subtitle Missing",
        "Subtitle Incorrect or Out of Sync", "Audio Language / Track Problem",
        "Poster or Thumbnail Incorrect",
        "Title / Description / Release Date Incorrect",
        "Search Cannot Find Content", "Other Content Problem"
    )),
    SupportCategory("Device & TV", listOf(
        "Device Limit Reached", "Remove or Change Device", "Device Not Recognized",
        "Owner Device Detected Incorrectly", "Owner-only Action Blocked",
        "Android TV Login Problem", "Android TV Playback Problem",
        "TV Remote / D-pad Problem", "QR Code Scanning Problem",
        "Screen Casting Problem", "App Not Compatible with Device",
        "Other Device Problem"
    )),
    SupportCategory("PPV & Rental", listOf(
        "Paid but Content Still Locked", "Rented Content Won't Play",
        "Rental Expiry Date Incorrect", "Rental Expired Too Early",
        "Wrong Content Unlocked", "PPV Payment Failed or Pending",
        "Duplicate PPV Payment", "PPV Refund Problem", "Other Rental Problem"
    )),
    SupportCategory("Account & Profile", listOf(
        "Change Phone Number", "Update Profile Information", "Account Inactive",
        "Delete Account", "Unauthorized Login", "Unknown Device on Account",
        "Account Security Concern", "Notification Problem",
        "Privacy or Personal Data Request", "Other Account Problem"
    )),
    SupportCategory("Other & Feedback", listOf(
        "Request a Movie or Series", "Feature Request",
        "App Design / Usability Feedback", "Accessibility Problem",
        "Bug Not Listed Above", "General Complaint", "Suggestion", "Compliment",
        "Other"
    ))
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val userId = SessionManager.getUserId(context).orEmpty()
    val accessToken = SessionManager.getAccessToken(context).orEmpty()

    var user by remember { mutableStateOf<User?>(null) }
    var parentalMode by remember {
        mutableStateOf(SessionManager.getParentalMode(context))
    }
    var ageRestrictionEnabled by remember {
        mutableStateOf(SessionManager.getAgeRestriction(context))
    }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSupport by remember { mutableStateOf(false) }

    LaunchedEffect(userId, accessToken) {
        isLoading = true
        errorMessage = null

        if (userId.isBlank() || accessToken.isBlank()) {
            isLoading = false
            user = null
            errorMessage = "Please sign in again to view your profile."
            return@LaunchedEffect
        }

        Api.getApi().findUser(
            AuthHeader.bearer(accessToken),
            userId
        ).enqueue(object : Callback<UserResponse> {
            override fun onResponse(
                call: Call<UserResponse>,
                response: Response<UserResponse>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.data != null) {
                    user = response.body()?.data
                } else {
                    errorMessage = "Unable to load profile."
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                isLoading = false
                errorMessage = t.message ?: "Unable to load profile."
            }
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060A12))
            .padding(horizontal = 42.dp, vertical = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        when {
            isLoading -> Text(
                text = "Loading profile...",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            errorMessage != null -> ProfileErrorState(
                message = errorMessage ?: "Unable to load profile.",
                onLogout = {
                    (context as? Activity)?.let { SessionManager.logout(it) }
                }
            )

            user != null -> ProfileContent(
                user = user!!,
                parentalMode = parentalMode,
                ageRestrictionEnabled = ageRestrictionEnabled,
                onParentalModeChange = { mode ->
                    parentalMode = mode
                    SessionManager.setParentalMode(context, mode)
                },
                onAgeRestrictionChange = { enabled ->
                    ageRestrictionEnabled = enabled
                    SessionManager.setAgeRestriction(context, enabled)
                },
                onLogout = {
                    (context as? Activity)?.let { SessionManager.logout(it) }
                },
                onSupport = { showSupport = true }
            )
        }
    }

    if (showSupport) {
        SupportDialog(
            userId = userId,
            onDismiss = { showSupport = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileContent(
    user: User,
    parentalMode: String,
    ageRestrictionEnabled: Boolean,
    onParentalModeChange: (String) -> Unit,
    onAgeRestrictionChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onSupport: () -> Unit
) {
    val logoutFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        logoutFocusRequester.requestFocus()
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.Top
    ) {
        ProfileSummaryCard(user = user, parentalMode = parentalMode)

        Column(
            modifier = Modifier
                .width(558.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.width(538.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account Details",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                ProfileActionButton(
                    text = "Logout",
                    compact = true,
                    modifier = Modifier.focusRequester(logoutFocusRequester),
                    onClick = onLogout
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ProfileInfoCard("Email", user.mail.orEmptyText())
                ProfileInfoCard("Phone", user.call.orEmptyText())
            }

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ProfileInfoCard("Address", listOf(user.veng, user.khua).joinFilled())
                ProfileInfoCard("Date of Birth", user.dob.orEmptyText())
            }

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ProfileInfoCard("Device", user.device_name.orEmptyText())
                ProfileInfoCard("Last Login", user.lastLogin.orEmptyText())
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Settings",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsItem(
                    title = "Parental Control",
                    value = if (parentalMode == SessionManager.MODE_KIDS) "Kids" else "Adult",
                    enabled = parentalMode == SessionManager.MODE_KIDS,
                    onClick = {
                        val nextMode = if (parentalMode == SessionManager.MODE_KIDS) {
                            SessionManager.MODE_ADULT
                        } else {
                            SessionManager.MODE_KIDS
                        }
                        onParentalModeChange(nextMode)
                    }
                )

                SettingsItem(
                    title = "Age Restriction",
                    value = if (ageRestrictionEnabled) "On" else "Off",
                    enabled = ageRestrictionEnabled,
                    onClick = { onAgeRestrictionChange(!ageRestrictionEnabled) }
                )

                SettingsItem(
                    title = "Help / Support",
                    value = "Open",
                    enabled = false,
                    onClick = onSupport
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SupportDialog(
    userId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appVersion = remember(context) {
        try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }
    var categoryIndex by remember { mutableStateOf<Int?>(null) }
    var selectedIssue by remember { mutableStateOf<String?>(null) }
    val category = categoryIndex?.let(supportCategories::get)
    val firstChoiceFocus = remember { FocusRequester() }

    LaunchedEffect(categoryIndex, selectedIssue) {
        if (selectedIssue == null) {
            firstChoiceFocus.requestFocus()
        }
    }

    val goBack = {
        when {
            selectedIssue != null -> selectedIssue = null
            categoryIndex != null -> categoryIndex = null
            else -> onDismiss()
        }
    }

    Dialog(onDismissRequest = goBack) {
        Row(
            modifier = Modifier
                .width(if (selectedIssue == null) 680.dp else 920.dp)
                .background(Color(0xFF111827), RoundedCornerShape(14.dp))
                .border(2.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                .padding(28.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(540.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = when {
                        selectedIssue != null -> "Scan to chat"
                        category != null -> category.name
                        else -> "Choose a support category"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        selectedIssue != null ->
                            "Scan the QR code with your phone to open a prefilled WhatsApp message."
                        category != null ->
                            "Choose the issue that best matches your problem."
                        else ->
                            "Choose what you need help with before opening WhatsApp."
                    },
                    color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.bodyLarge
                )

                if (selectedIssue == null) {
                    val choices: List<String> =
                        category?.issues ?: supportCategories.map { it.name }
                    LazyColumn(
                        modifier = Modifier
                            .width(620.dp)
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        itemsIndexed(choices) { index, label ->
                            SupportChoice(
                                label = label,
                                modifier = if (index == 0) {
                                    Modifier.focusRequester(firstChoiceFocus)
                                } else {
                                    Modifier
                                },
                                onClick = {
                                    if (category == null) {
                                        categoryIndex = index
                                    } else {
                                        selectedIssue = label
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Category: ${category?.name.orEmpty()}",
                        color = Color(0xFF93C5FD),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Issue: $selectedIssue",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SupportChoice(label = "Back", onClick = goBack)
                    SupportChoice(label = "Close", onClick = onDismiss)
                }
            }

            if (selectedIssue != null && category != null) {
                val message =
                    "Hello Zo Stream Support,\n\n" +
                            "I need help with the following issue:\n" +
                            "• Category: *${category.name}*\n" +
                            "• Issue: *$selectedIssue*\n\n" +
                            "Platform Details:\n" +
                            "• Platform: *Android TV*\n" +
                            "• Device Type: *tv*\n" +
                            "• App Version: *$appVersion*\n\n" +
                            "User Details:\n" +
                            "• User ID: *${userId.ifBlank { "Guest" }}*\n\n" +
                            "Additional details:\n"
                val whatsappUrl =
                    "https://wa.me/918837076347?text=" +
                            URLEncoder.encode(message, StandardCharsets.UTF_8.toString())

                AndroidView(
                    modifier = Modifier
                        .size(320.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    factory = { context ->
                        ImageView(context).also {
                            it.scaleType = ImageView.ScaleType.FIT_CENTER
                        }
                    },
                    update = { QRUtils.generateQR(it, whatsappUrl) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SupportChoice(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                if (focused) Color(0xFF2563EB) else Color(0xFF1E293B),
                shape
            )
            .border(
                2.dp,
                if (focused) Color(0xFFBAE6FD) else Color(0xFF475569),
                shape
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsItem(
    title: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.03f else 1f,
        label = "settingsItemScale"
    )
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier
            .width(538.dp)
            .height(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                color = if (focused) Color(0xFF1D4ED8) else Color(0xFF111827),
                shape = shape
            )
            .border(
                width = 2.dp,
                color = if (focused) Color(0xFFBFDBFE) else Color(0xFF263244),
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick { onClick() }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .width(58.dp)
                .height(32.dp)
                .background(
                    color = if (enabled) Color(0xFF22C55E) else Color(0xFF334155),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileSummaryCard(
    user: User,
    parentalMode: String
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .background(Color(0xFF111827), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!user.img.isNullOrBlank()) {
            AsyncImage(
                model = user.img,
                contentDescription = user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1D4ED8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.initials(),
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = user.name.orEmptyText(),
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = user.uid.orEmptyText(),
            color = Color(0xFFCBD5E1),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(20.dp))
        ProfileStatusText(
            label = "Viewing Mode",
            value = parentalMode.replaceFirstChar { it.uppercase() }
        )
        ProfileStatusText(
            label = "Account",
            value = if (user.isACActive) "Active" else "Inactive"
        )
        ProfileStatusText(
            label = "Profile",
            value = if (user.isAccountComplete) "Complete" else "Incomplete"
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileInfoCard(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .height(80.dp)
            .background(Color(0xFF111827), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF263244), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = Color(0xFF93C5FD),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileStatusText(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            color = Color(0xFF94A3B8),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileActionButton(
    text: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.05f else 1f,
        label = "profileActionButtonScale"
    )

    Box(
        modifier = modifier
            .width(if (compact) 140.dp else 180.dp)
            .height(if (compact) 44.dp else 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                color = if (focused) Color(0xFFDC2626) else Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 2.dp,
                color = if (focused) Color(0xFFFECACA) else Color(0xFF334155),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileErrorState(
    message: String,
    onLogout: () -> Unit
) {
    val logoutFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        logoutFocusRequester.requestFocus()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Profile",
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = message,
            color = Color(0xFFCBD5E1),
            style = MaterialTheme.typography.titleMedium
        )
        ProfileActionButton(
            text = "Logout",
            modifier = Modifier.focusRequester(logoutFocusRequester),
            onClick = onLogout
        )
    }
}

private fun String?.orEmptyText(): String {
    return if (this.isNullOrBlank()) "Not available" else this
}

private fun List<String?>.joinFilled(): String {
    val value = filterNot { it.isNullOrBlank() }.joinToString(", ")
    return value.ifBlank { "Not available" }
}

private fun String?.initials(): String {
    val value = this.orEmptyText()
    return value
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "U" }
}
