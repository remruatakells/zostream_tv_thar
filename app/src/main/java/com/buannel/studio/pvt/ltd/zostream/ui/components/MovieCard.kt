package com.buannel.studio.pvt.ltd.zostream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.buannel.studio.pvt.ltd.zostream.R
import com.buannel.studio.pvt.ltd.zostream.model.Movie

/**
 * MovieCard displays a Movie object as a card.
 */
@Composable
fun MovieCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    onClick: (Movie) -> Unit = {}
) {
    // AI and catalog responses both expose cover_img. Fall back only for older
    // catalog records that have not yet been backfilled with a cover image.
    val imageUrl = movie.coverImg?.takeIf { it.isNotBlank() } ?: movie.poster

    Column(
        modifier = modifier.width(236.dp)
    ) {
        Surface(
            onClick = { onClick(movie) },
            modifier = cardModifier
                .padding(8.dp)
                .fillMaxWidth()
                .aspectRatio(16f / 9f),

            // ✅ Add glow, scale, and border
            glow = ClickableSurfaceDefaults.glow(
                focusedGlow = Glow(
                    Color(0xFF384FFF),
                    16.dp
                )
            ),
            scale = ClickableSurfaceDefaults.scale(
                focusedScale = 1.06f
            ),
            border = tvFocusedItemBorder(),
        ) {
            Box {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = movie.description,
                    placeholder = painterResource(id = R.drawable.placeholder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )

                WatchProgressBar(
                    movie = movie,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        Text(
            text = movie.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

@Composable
private fun WatchProgressBar(
    movie: Movie,
    modifier: Modifier = Modifier
) {
    if (movie.watchPosition <= 0 || movie.watchDuration <= 0) {
        return
    }

    val progress = (movie.watchPosition.toFloat() / movie.watchDuration.toFloat())
        .coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.White.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
