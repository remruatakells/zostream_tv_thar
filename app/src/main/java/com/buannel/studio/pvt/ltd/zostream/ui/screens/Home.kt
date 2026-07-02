package com.buannel.studio.pvt.ltd.zostream.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.buannel.studio.pvt.ltd.zostream.R
import com.buannel.studio.pvt.ltd.zostream.model.Movie
import com.buannel.studio.pvt.ltd.zostream.ui.screens.catalog.CatalogBrowserViewModel
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Home(
    modifier: Modifier = Modifier,
    viewModel: CatalogBrowserViewModel = hiltViewModel(),
    onMovieSelected: (Movie) -> Unit = {},
    onMovieIdSelected: (String) -> Unit = {},
    onCategoryViewAll: (String) -> Unit = {}
) {

    val context = LocalContext.current

    val token = SessionManager.getAccessToken(context)
    val uid = SessionManager.getUserId(context)
    val xmode = SessionManager.getParentalMode(context)
    val age = SessionManager.getAgeRestriction(context)

    // ✅ Load API data once
    LaunchedEffect(Unit) {
        viewModel.loadBannersFromApi()

        if (token.isNotEmpty() && uid.isNotEmpty()) {
            viewModel.loadHomeFromApi(token, xmode, uid, age)
        }
    }

    val categoryList by viewModel.categoryList.collectAsStateWithLifecycle()
    val bannerList by viewModel.bannerList.collectAsStateWithLifecycle()
    val carouselFocusRequester = remember { FocusRequester() }

    // Request focus safely: the FocusRequester may not be attached immediately after composition.
    LaunchedEffect(carouselFocusRequester) {
        repeat(10) {
            try {
                carouselFocusRequester.requestFocus()
                return@LaunchedEffect
            } catch (e: IllegalStateException) {
                // Not attached yet — wait and retry
                delay(50)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 25.dp)
    ) {

        // =======================
        // 🎬 CAROUSEL
        // =======================
        item {

            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val carouselHeight = screenHeight * 0.7f

            var isFocused by remember { mutableStateOf(false) }

            val carouselState = rememberCarouselState()
            val activeBannerTargetId = bannerList
                .getOrNull(carouselState.activeItemIndex)
                ?.targetId
                .orEmpty()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(carouselHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = if (isFocused) 3.dp else 0.dp,
                        color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .focusRequester(carouselFocusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .clickable(enabled = activeBannerTargetId.isNotBlank()) {
                        onMovieIdSelected(activeBannerTargetId)
                    }
            ) {

                if (bannerList.isNotEmpty()) {

                    Carousel(
                        itemCount = bannerList.size,
                        carouselState = carouselState,
                        modifier = Modifier.fillMaxSize()
                    ) { index ->

                        val banner = bannerList[index]
                        val backgroundColor = MaterialTheme.colorScheme.background
                        val imageUrl = banner.thumbnailUrl ?: banner.mediaUrl

                        Box {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                placeholder = painterResource(id = R.drawable.placeholder),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                contentAlignment = Alignment.BottomStart,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        val brush = Brush.horizontalGradient(
                                            listOf(backgroundColor, Color.Transparent)
                                        )
                                        drawRect(brush)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {

                                    Text(
                                        text = banner.title.orEmpty(),
                                        style = MaterialTheme.typography.displaySmall
                                    )

                                    if (!banner.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = banner.description,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }
                        }
                    }

                } else {
                    ShimmerBox(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        // =======================
        // 📂 CATEGORY LIST
        // =======================
        if (categoryList.isEmpty()) {

            item {
                HomeRowsSkeleton()
            }

        } else {

            items(categoryList) { category ->

                CategoryRow(
                    categoryName = category.name,
                    movies = category.movieList,
                    onMovieSelected = onMovieSelected,
                    onViewAll = onCategoryViewAll
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun HomeRowsSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        repeat(3) {
            Column {
                ShimmerBox(
                    modifier = Modifier
                        .padding(start = 35.dp, bottom = 15.dp)
                        .width(190.dp)
                        .height(24.dp),
                    shape = RoundedCornerShape(6.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 35.dp)
                ) {
                    repeat(5) {
                        Column(
                            modifier = Modifier.width(220.dp)
                        ) {
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
        }
    }
}

@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val transition = rememberInfiniteTransition(label = "home_shimmer")
    val xOffset by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "home_shimmer_offset"
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
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
