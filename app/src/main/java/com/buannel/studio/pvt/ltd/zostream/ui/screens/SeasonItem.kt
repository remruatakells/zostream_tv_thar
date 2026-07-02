package com.buannel.studio.pvt.ltd.zostream.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.buannel.studio.pvt.ltd.zostream.model.Season
import com.buannel.studio.pvt.ltd.zostream.ui.components.tvFocusedItemBorder

@Composable
fun SeasonItem(
    season: Season,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,

        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color.White else Color.DarkGray,
            focusedContainerColor = if (isSelected) Color.White else Color.Gray
        ),

        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                Color(0xFF384FFF),
                16.dp
            )
        ),

        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1.08f
        ),

        border = tvFocusedItemBorder(),

        modifier = Modifier
            .padding(end = 10.dp)
    ) {

        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = season.title ?: "Season ${season.seasonNumber}",
                fontSize = 14.sp,
                color = if (isSelected) Color.Black else Color.White
            )
        }
    }
}
