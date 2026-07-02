package com.buannel.studio.pvt.ltd.zostream.ui.screens.details

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.buannel.studio.pvt.ltd.zostream.ui.screens.EpisodeItem
import com.buannel.studio.pvt.ltd.zostream.ui.screens.SeasonItem
import com.buannel.studio.pvt.ltd.zostream.ui.screens.TvButton
import com.buannel.studio.pvt.ltd.zostream.utils.DetailsViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import com.buannel.studio.pvt.ltd.zostream.model.Episode
import com.buannel.studio.pvt.ltd.zostream.model.Movie
import com.buannel.studio.pvt.ltd.zostream.model.Subscription
import com.buannel.studio.pvt.ltd.zostream.ui.screens.AlsoLikeItems
import com.buannel.studio.pvt.ltd.zostream.utils.FullScreenErrorDialog
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager

@Composable
fun DetailsScreen(
    movieId: String,
    accessToken: String,
    userId: String,
    deviceId: String,
    onPlay: (Movie, Episode?, String?, Subscription?, String) -> Unit,
    onQrPayment: (String, Boolean?, Int?, String?) -> Unit
) {
    val viewModel: DetailsViewModel = viewModel()
    val subscription = viewModel.subscription.value
    val ppvDetails = viewModel.ppv.value
    val movie = viewModel.movie.value
    val seasons = viewModel.seasons.value
    val episodes = viewModel.episodes.value
    val loading = viewModel.isLoading.value
    val alsoLike = viewModel.alsoLikeMovies.value

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val hasAccess = subscription?.isActive == true
    val hasPPV = ppvDetails?.isRented == true
    val context = LocalContext.current

    var selectedMovieId by remember(movieId) { mutableStateOf(movieId) }

    LaunchedEffect(selectedMovieId) {
        viewModel.loadData(accessToken, userId, deviceId, selectedMovieId)
    }

    if (loading) {
        DetailsSkeleton(screenHeight = screenHeight)
        return
    }

    movie?.let { currentMovie ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = currentMovie.coverImg,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.70f)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.70f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = screenHeight * 0.50f)
            ) {

                Row(modifier = Modifier.padding(horizontal = 40.dp)) {
                    Card(
                        onClick = {},
                        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
                        scale = CardDefaults.scale(focusedScale = 1.1f),
                        colors = CardDefaults.colors(
                            containerColor = Color.DarkGray,
                            focusedContainerColor = Color.Gray
                        ),
                        modifier = Modifier.size(150.dp, 230.dp)
                    ) {
                        AsyncImage(
                            model = currentMovie.poster,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.width(25.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            currentMovie.title,
                            fontSize = 32.sp,
                            color = Color.White
                        )

                        Text(
                            currentMovie.genre.replace(", ", " • "),
                            fontSize = 16.sp,
                            color = Color.Gray
                        )

                        Text(
                            "${currentMovie.views} | ${currentMovie.duration}",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(15.dp))

                        Text(
                            currentMovie.description,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.LightGray
                        )

                        Spacer(Modifier.height(20.dp))

                        Row {
                            val isOwner = SessionManager.getIsDeviceOwner(context)

                            // use Season 1 -> Episode 1 as the source of truth for season-wide access
                            val firstSeason = seasons.firstOrNull()
                            val firstEpisode = firstSeason?.episodes?.firstOrNull()

                            // --- UPDATED LOGIC ---
                            // Check if the season should behave as PPV, Premium, or Free based on Ep 1
                            val seasonRequiresPpv = currentMovie.isSeason && firstEpisode?.isPayPerView == true

                            val isFree = if (currentMovie.isSeason && firstEpisode != null) {
                                !firstEpisode.isPremium && !firstEpisode.isPayPerView
                            } else {
                                !currentMovie.isPremium && !currentMovie.isPayPerView
                            }
                            // ---------------------

                            val canWatch = when {
                                isFree -> true // ✅ Ep 1 is free or Movie is free

                                currentMovie.isSeason && seasonRequiresPpv -> hasPPV
                                currentMovie.isSeason && !seasonRequiresPpv -> hasAccess

                                currentMovie.isPayPerView -> hasPPV && isOwner

                                else -> hasAccess
                            }

                            val buttonText = when {
                                isFree -> "Watch Now"

                                currentMovie.isSeason && seasonRequiresPpv -> if (hasPPV) "Watch Now" else "Rent Season"
                                currentMovie.isSeason -> if (hasAccess) "Watch Now" else "Subscribe"

                                currentMovie.isPayPerView -> if (hasPPV) "Watch Now" else "Rent Now"

                                hasAccess -> "Watch Now"
                                else -> "Subscribe"
                            }

                            TvButton(
                                text = buttonText,
                                onClick = {
                                    when {
                                        canWatch -> {
                                            if (currentMovie.isSeason) {
                                                val playableEpisode = firstEpisode ?: episodes.firstOrNull()

                                                if (playableEpisode != null) {
                                                    onPlay(
                                                        currentMovie,
                                                        playableEpisode,
                                                        firstSeason?.id ?: playableEpisode.seasonId,
                                                        subscription,
                                                        "episode"
                                                    )
                                                } else {
                                                    FullScreenErrorDialog.show(
                                                        context,
                                                        "Episode Not Found",
                                                        "This series has no playable episode yet.",
                                                        true,
                                                        {}
                                                    )
                                                }
                                            } else {
                                                onPlay(currentMovie, null, null, subscription, "movie")
                                            }
                                        }

                                        currentMovie.isSeason && seasonRequiresPpv -> {
                                            if (isOwner) {
                                                // Use episode ID for PPV rental of a season
                                                onQrPayment(firstEpisode?.id ?: "", true, subscription?.id, "episode")
                                            } else {
                                                FullScreenErrorDialog.show(
                                                    context,
                                                    "Access Restricted",
                                                    "Only the account owner is authorized to make purchases or subscribe.",
                                                    true,
                                                    {}
                                                )
                                            }
                                        }

                                        currentMovie.isPayPerView || (!hasAccess && !isFree) -> {
                                            if (isOwner) {
                                                val isPpv = currentMovie.isPayPerView
                                                onQrPayment(currentMovie.id, isPpv, subscription?.id, if(currentMovie.isSeason) "episode" else "movie")
                                            } else {
                                                FullScreenErrorDialog.show(
                                                    context,
                                                    "Access Restricted",
                                                    "Only the account owner is authorized to make purchases or subscribe.",
                                                    true,
                                                    {}
                                                )
                                            }
                                        }
                                    }
                                }
                            )

                            Spacer(Modifier.width(10.dp))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (currentMovie.isSeason) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(seasons) { season ->
                            SeasonItem(
                                season = season,
                                isSelected = season.id == viewModel.selectedSeason.value?.id,
                                onClick = { viewModel.selectSeason(season) }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                    ) {
                        items(episodes) { episode ->
                            EpisodeItem(
                                episode = episode,
                                onClick = {
                                    onPlay(
                                        currentMovie,
                                        episode,
                                        episode.seasonId,
                                        subscription,
                                        "episode"
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "You may also like",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(Modifier.height(15.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(alsoLike) { item ->
                            AlsoLikeItems(
                                alsoLike = item,
                                onClick = { clicked ->
                                    selectedMovieId = clicked.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsSkeleton(screenHeight: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.70f)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.70f)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = screenHeight * 0.50f)
        ) {
            Row(modifier = Modifier.padding(horizontal = 40.dp)) {
                ShimmerBox(
                    modifier = Modifier.size(150.dp, 230.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.width(25.dp))

                Column(Modifier.weight(1f)) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(38.dp),
                        shape = RoundedCornerShape(6.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.32f)
                            .height(18.dp),
                        shape = RoundedCornerShape(5.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.22f)
                            .height(18.dp),
                        shape = RoundedCornerShape(5.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    repeat(3) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(if (it == 2) 0.58f else 0.72f)
                                .height(16.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ShimmerBox(
                            modifier = Modifier.size(width = 130.dp, height = 44.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ShimmerBox(
                            modifier = Modifier.size(width = 110.dp, height = 44.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            ShimmerBox(
                modifier = Modifier
                    .padding(start = 24.dp)
                    .width(170.dp)
                    .height(22.dp),
                shape = RoundedCornerShape(6.dp)
            )

            Spacer(Modifier.height(15.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                repeat(5) {
                    Column(modifier = Modifier.width(150.dp)) {
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
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val transition = rememberInfiniteTransition(label = "details_shimmer")
    val xOffset by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "details_shimmer_offset"
    )

    val baseColor = Color(0xFF242424)
    val highlightColor = Color(0xFF3A3A3A)
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(xOffset - 300f, 0f),
        end = Offset(xOffset, 300f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush)
    )
}
