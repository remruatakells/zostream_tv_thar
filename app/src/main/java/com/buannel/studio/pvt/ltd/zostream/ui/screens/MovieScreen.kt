package com.buannel.studio.pvt.ltd.zostream.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.buannel.studio.pvt.ltd.zostream.model.Movie
import com.buannel.studio.pvt.ltd.zostream.ui.components.MovieCard
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager

@Composable
fun MovieScreen(
    onMovieSelected: (Movie) -> Unit,
    categoryName: String? = null,
    viewModel: MovieBrowserViewModel = viewModel()
) {
    val context = LocalContext.current
    val token = SessionManager.getAccessToken(context)
    val uid = SessionManager.getUserId(context)
    val age = SessionManager.getAgeRestriction(context)
    val childMode = SessionManager.getParentalMode(context).equals("kids", ignoreCase = true)

    val movies = viewModel.movies.value
    val loading = viewModel.isLoading.value
    val loadingMore = viewModel.isLoadingMore.value
    val error = viewModel.error.value
    val firstMovieFocusRequester = remember { FocusRequester() }

    LaunchedEffect(token, uid, age, childMode, categoryName) {
        if (token.isNotBlank() && uid.isNotBlank()) {
            viewModel.loadFirstPage(token, uid, age, childMode, categoryName)
        }
    }

    LaunchedEffect(movies.firstOrNull()?.id, movies.firstOrNull()?.num) {
        if (movies.isNotEmpty()) {
            firstMovieFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            loading && movies.isEmpty() -> {
                MovieGridSkeleton()
            }

            error != null && movies.isEmpty() -> {
                Text(
                    text = error,
                    color = Color(0xFFFFB4AB),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(40.dp)
                )
            }

            movies.isEmpty() -> {
                Text(
                    text = "No movies found",
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(40.dp)
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 35.dp, top = 35.dp, end = 35.dp, bottom = 35.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    itemsIndexed(
                        items = movies,
                        key = { index, movie -> movieGridKey(index, movie) }
                    ) { index, movie ->
                        if (index >= movies.lastIndex - 6) {
                            LaunchedEffect(movies.size) {
                                viewModel.loadNextPage()
                            }
                        }

                        MovieCard(
                            movie = movie,
                            cardModifier = if (index == 0) {
                                Modifier.focusRequester(firstMovieFocusRequester)
                            } else {
                                Modifier
                            },
                            onClick = { onMovieSelected(it) }
                        )
                    }

                    if (loadingMore) {
                        item {
                            Text(
                                text = "Loading more...",
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun movieGridKey(index: Int, movie: Movie): String {
    val movieKey = movie.id?.takeIf { it.isNotBlank() } ?: "num-${movie.num}"
    return "$movieKey-$index"
}

@Composable
private fun MovieGridSkeleton() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 35.dp, top = 35.dp, end = 35.dp, bottom = 35.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        items(24) {
            Column(modifier = Modifier.width(220.dp)) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    shape = RoundedCornerShape(8.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(0.75f)
                        .height(18.dp),
                    shape = RoundedCornerShape(5.dp)
                )
            }
        }
    }
}

@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val transition = rememberInfiniteTransition(label = "movie_grid_shimmer")
    val xOffset by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "movie_grid_shimmer_offset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        start = Offset(xOffset - 300f, 0f),
        end = Offset(xOffset, 300f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush)
    )
}
