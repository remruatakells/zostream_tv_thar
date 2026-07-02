package com.buannel.studio.pvt.ltd.zostream.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.buannel.studio.pvt.ltd.zostream.model.Movie
import com.buannel.studio.pvt.ltd.zostream.ui.components.tvFocusedItemBorder

@Composable
fun AlsoLikeItems(
    alsoLike: Movie,
    onClick: (Movie) -> Unit = {}
) {

    Column(
        modifier = Modifier
            .width(150.dp)
            .padding(end = 12.dp)
    ) {

        // 🔥 TV Surface (same as MovieCard)
        Surface(
            onClick = { onClick(alsoLike) },
            modifier = Modifier
                .padding(8.dp)
                .widthIn(max = 320.dp)
                .aspectRatio(16f / 9f),

            glow = ClickableSurfaceDefaults.glow(
                focusedGlow = Glow(
                    Color(0xFF384FFF),
                    16.dp
                )
            ),

            scale = ClickableSurfaceDefaults.scale(
                focusedScale = 1.08f
            ),

            border = tvFocusedItemBorder()
        ) {
            AsyncImage(
                model = alsoLike.coverImg,
                contentDescription = alsoLike.title,
                modifier = Modifier.fillMaxSize(), // ✅ VERY IMPORTANT
                contentScale = ContentScale.Crop   // or Fit (see below)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = alsoLike.title,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
