package com.buannel.studio.pvt.ltd.zostream.ui.screens

import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import com.buannel.studio.pvt.ltd.zostream.R
import com.buannel.studio.pvt.ltd.zostream.ui.theme.TvComposeIntroductionTheme
import com.buannel.studio.pvt.ltd.zostream.utils.QRUtils

class LoginUiState {
    var phone by mutableStateOf("")
    var otp by mutableStateOf("")
    var selectedCountryCode by mutableStateOf("91")
    var otpVisible by mutableStateOf(false)
    var resendVisible by mutableStateOf(false)
    var resendText by mutableStateOf("Resend in")
    var loginButtonText by mutableStateOf("Send OTP")
    var qrButtonVisible by mutableStateOf(true)
    var qrButtonText by mutableStateOf("Show QR")
    var qrTimerText by mutableStateOf("Zo Stream")
    var qrToken by mutableStateOf("")
    var phoneError by mutableStateOf<String?>(null)
    var otpError by mutableStateOf<String?>(null)
}

data class LoginCountryOption(
    val name: String,
    val emoji: String,
    val code: String
) {
    fun displayName(): String {
        val prefix = if (emoji.isBlank()) "" else "$emoji "
        return "$prefix$name (+$code)"
    }
}

interface LoginCallbacks {
    fun onShowQr()
    fun onSubmitLogin(phone: String, otp: String)
    fun onCountrySelected(countryCode: String)
}

object LoginComposeHost {
    @JvmStatic
    fun install(
        activity: ComponentActivity,
        state: LoginUiState,
        countries: List<LoginCountryOption>,
        callbacks: LoginCallbacks
    ) {
        activity.setContent {
            TvComposeIntroductionTheme {
                LoginScreen(
                    state = state,
                    countries = countries,
                    callbacks = callbacks
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(
    state: LoginUiState,
    countries: List<LoginCountryOption>,
    callbacks: LoginCallbacks
) {
    var showCountryDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F2)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.70f)
                .fillMaxHeight(0.70f)
                .clip(RoundedCornerShape(20.dp))
                .background(loginBackgroundBrush())
                .border(1.dp, Color(0x1F000000), RoundedCornerShape(20.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickLoginPanel(
                state = state,
                onShowQr = callbacks::onShowQr,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .padding(vertical = 10.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x66000000),
                                Color.Transparent
                            )
                        )
                    )
            )

            OtpLoginPanel(
                state = state,
                onCountryClick = { showCountryDialog = true },
                onSubmit = { callbacks.onSubmitLogin(state.phone, state.otp) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showCountryDialog) {
        CountryPickerDialog(
            countries = countries,
            selectedCode = state.selectedCountryCode,
            onDismiss = { showCountryDialog = false },
            onSelected = { code ->
                callbacks.onCountrySelected(code)
                showCountryDialog = false
            }
        )
    }
}

@Composable
private fun QuickLoginPanel(
    state: LoginUiState,
    onShowQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Quick Login",
            color = Color.Black,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .padding(13.dp)
                .size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageResource(R.drawable.icon_transparent)
                    }
                },
                update = { imageView ->
                    if (state.qrToken.isBlank()) {
                        imageView.setImageResource(R.drawable.icon_transparent)
                    } else {
                        QRUtils.generateQR(imageView, state.qrToken)
                    }
                }
            )

            if (state.qrButtonVisible) {
                TvLoginButton(
                    text = state.qrButtonText,
                    onClick = onShowQr,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .padding(horizontal = 0.dp)
                )
            }
        }

        Text(
            text = state.qrTimerText,
            color = Color(0xFF262626),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun OtpLoginPanel(
    state: LoginUiState,
    onCountryClick: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var numberPadTarget by remember { mutableStateOf<NumberPadTarget?>(null) }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "OTP Login",
            color = Color.Black,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier
                .padding(13.dp)
                .size(width = 200.dp, height = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x80CACACA))
                    .border(1.dp, Color(0x331F93F0), RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountryCodeButton(
                    text = "+${state.selectedCountryCode}",
                    onClick = onCountryClick,
                    modifier = Modifier
                        .width(62.dp)
                        .fillMaxHeight()
                )

                NumberField(
                    value = state.phone,
                    hint = "Phone no",
                    onClick = { numberPadTarget = NumberPadTarget.PHONE },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 6.dp, end = 12.dp)
                )
            }

            ErrorText(state.phoneError)

            if (state.otpVisible) {
                Spacer(Modifier.height(10.dp))
                NumberField(
                    value = state.otp,
                    hint = "Enter OTP",
                    onClick = { numberPadTarget = NumberPadTarget.OTP },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x80CACACA))
                        .border(1.dp, Color(0x331F93F0), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp)
                )
                ErrorText(state.otpError)
            }

            if (state.resendVisible) {
                Text(
                    text = state.resendText,
                    color = Color(0xFF666565),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp, bottom = 5.dp)
                )
            }

            TvLoginButton(
                text = state.loginButtonText,
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .padding(top = 14.dp)
            )
        }

        Text(text = "", fontSize = 16.sp)
    }

    numberPadTarget?.let { target ->
        val value = if (target == NumberPadTarget.PHONE) state.phone else state.otp
        val maxLength = if (target == NumberPadTarget.PHONE) 15 else 8

        NumberPadDialog(
            title = if (target == NumberPadTarget.PHONE) "Enter phone number" else "Enter OTP",
            value = value,
            onDigit = { digit ->
                if (value.length < maxLength) {
                    if (target == NumberPadTarget.PHONE) {
                        state.phone = value + digit
                        state.phoneError = null
                    } else {
                        state.otp = value + digit
                        state.otpError = null
                    }
                }
            },
            onDelete = {
                if (target == NumberPadTarget.PHONE) {
                    state.phone = state.phone.dropLast(1)
                    state.phoneError = null
                } else {
                    state.otp = state.otp.dropLast(1)
                    state.otpError = null
                }
            },
            onDone = { numberPadTarget = null },
            onDismiss = { numberPadTarget = null }
        )
    }
}

private enum class NumberPadTarget {
    PHONE,
    OTP
}

@Composable
private fun CountryPickerDialog(
    countries: List<LoginCountryOption>,
    selectedCode: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .height(360.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF7F7F7))
                .border(1.dp, Color(0x33000000), RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = "Select country",
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(countries) { country ->
                        CountryRow(
                            country = country,
                            selected = country.code == selectedCode,
                            onClick = { onSelected(country.code) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryRow(
    country: LoginCountryOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val background = when {
        focused -> Color(0xFFEAF3FF)
        selected -> Color(0xFFE8EEF8)
        else -> Color.Transparent
    }
    val border = if (focused) Color(0xFF1F93F0) else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(2.dp, border, RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = country.displayName(),
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CountryCodeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color(0xFFE0F2FE) else Color.Transparent)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) Color(0xFF0284C7) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TvLoginButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = if (focused) 1.04f else 1f
                scaleY = if (focused) 1.04f else 1f
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (focused) Color(0xFFDBEAFE) else Color(0xFFBFDBFE))
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) Color(0xFF2563EB) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Color(0x14FFFFFF) else Color.Transparent)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) Color(0xFF1F93F0) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = value.ifBlank { hint },
            color = if (value.isBlank()) Color(0x99000000) else Color.Black,
            fontSize = 17.sp
        )
    }
}

@Composable
private fun NumberPadDialog(
    title: String,
    value: String,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstKeyFocusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(330.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF7F7F7))
                .border(1.dp, Color(0x33000000), RoundedCornerShape(18.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = value.ifBlank { "—" },
                color = Color(0xFF1F2937),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Delete", "0", "Done")
            ).forEachIndexed { rowIndex, keys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    keys.forEachIndexed { columnIndex, label ->
                        NumberPadKey(
                            label = label,
                            onClick = {
                                when (label) {
                                    "Delete" -> onDelete()
                                    "Done" -> onDone()
                                    else -> onDigit(label.single())
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .padding(vertical = 4.dp)
                                .then(
                                    if (rowIndex == 0 && columnIndex == 0) {
                                        Modifier.focusRequester(firstKeyFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            firstKeyFocusRequester.requestFocus()
        }
    }
}

@Composable
private fun NumberPadKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Color(0xFFDBEAFE) else Color(0xFFE5E7EB))
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) Color(0xFF2563EB) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.Black,
            fontSize = if (label.length == 1) 20.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorText(text: String?) {
    if (!text.isNullOrBlank()) {
        Text(
            text = text,
            color = Color(0xFFD32F2F),
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp)
        )
    }
}

private fun loginBackgroundBrush(): Brush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFF9FBFF),
        Color(0xFFEFF3FA)
    )
)
