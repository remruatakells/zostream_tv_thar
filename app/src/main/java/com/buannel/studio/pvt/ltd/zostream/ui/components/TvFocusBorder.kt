package com.buannel.studio.pvt.ltd.zostream.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults

@Composable
fun tvFocusedItemBorder() = ClickableSurfaceDefaults.border(
    focusedBorder = Border(
        border = BorderStroke(3.dp, Color.White),
        inset = 0.dp,
        shape = RoundedCornerShape(8.dp)
    ),
    pressedBorder = Border(
        border = BorderStroke(3.dp, Color(0xFFBFDBFE)),
        inset = 0.dp,
        shape = RoundedCornerShape(8.dp)
    )
)
