package com.buannel.studio.pvt.ltd.zostream.ui.screens


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (focused) 1.1f else 1f,
        label = ""
    )

    val backgroundColor = if (focused) {
        Color(0xFF4197FF) // focused bg
    } else {
        Color(0x0C6B6B6B) // default bg (with alpha)
    }

    val borderColor = if (focused) {
        Color(0xFFAEC7FD) // focused stroke
    } else {
        Color(0x6BD1D1D1) // default stroke
    }

    Box(
        modifier = Modifier
            .width(160.dp)
            .height(45.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(20.dp) // same as XML
            )
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White // XML uses white text
        )
    }
}