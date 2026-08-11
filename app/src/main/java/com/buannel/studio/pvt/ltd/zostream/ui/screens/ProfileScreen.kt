package com.buannel.studio.pvt.ltd.zostream.ui.screens

import android.app.Activity
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.buannel.studio.pvt.ltd.zostream.api.Api
import com.buannel.studio.pvt.ltd.zostream.model.User
import com.buannel.studio.pvt.ltd.zostream.response.UserResponse
import com.buannel.studio.pvt.ltd.zostream.utils.AuthHeader
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                }
            )
        }
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
    onLogout: () -> Unit
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
            }
        }
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
