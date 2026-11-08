package com.buannel.studio.pvt.ltd.zostream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.buannel.studio.pvt.ltd.zostream.model.Movie
import com.buannel.studio.pvt.ltd.zostream.ui.components.MovieCard
import com.buannel.studio.pvt.ltd.zostream.ui.components.tvFocusedItemBorder

@Composable
fun CategoryRow(
    sectionId: String,
    categoryName: String,
    movies: List<Movie>,
    onMovieSelected: (Movie) -> Unit,
    onViewAll: (String) -> Unit = {}
) {
    Column {

        Text(
            text = categoryName,
            modifier = Modifier.padding(
                start = 35.dp,
                end = 35.dp,
                bottom = 15.dp
            )
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 35.dp),
            modifier = Modifier.height(180.dp)
        ) {
            items(movies) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieSelected(it) }
                )
            }

            if (!sectionId.equals("continue_watching", ignoreCase = true) &&
                !categoryName.equals("Continue Watching", ignoreCase = true)) {
                item {
                    ViewAllCard(
                        onClick = { onViewAll(sectionId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewAllCard(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(96.dp)
            .height(132.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(36.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF111827),
                focusedContainerColor = Color(0xFF2563EB)
            ),
            glow = ClickableSurfaceDefaults.glow(
                focusedGlow = Glow(
                    Color(0xFF384FFF),
                    16.dp
                )
            ),
            border = tvFocusedItemBorder()
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ">",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}
