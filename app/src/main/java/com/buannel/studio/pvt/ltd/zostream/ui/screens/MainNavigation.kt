package com.buannel.studio.pvt.ltd.zostream.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.buannel.studio.pvt.ltd.zostream.R
import com.buannel.studio.pvt.ltd.zostream.model.Movie
import kotlinx.coroutines.delay

private enum class MainDestination(
    val label: String,
    @DrawableRes val iconRes: Int
) {
    Home("Home", R.drawable.ic_nav_home),
    Search("Search", R.drawable.ic_nav_search),
    Movies("Movie", R.drawable.ic_nav_movie),
    Profile("Profile", R.drawable.ic_nav_profile)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainNavigation(
    onMovieSelected: (Movie) -> Unit,
    onMovieIdSelected: (String) -> Unit
) {
    var selectedDestination by rememberSaveable { mutableStateOf(MainDestination.Home) }
    var selectedMovieCategory by rememberSaveable { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MainNavigationDrawer(
            selectedDestination = selectedDestination,
            onDestinationSelected = {
                if (it == MainDestination.Movies) {
                    selectedMovieCategory = null
                }
                selectedDestination = it
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            when (selectedDestination) {
                MainDestination.Home -> Home(
                    onMovieSelected = onMovieSelected,
                    onMovieIdSelected = onMovieIdSelected,
                    onCategoryViewAll = { categoryName ->
                        selectedMovieCategory = categoryName
                        selectedDestination = MainDestination.Movies
                    }
                )
                MainDestination.Search -> SearchScreen(onMovieSelected = onMovieSelected)
                MainDestination.Movies -> MovieScreen(
                    onMovieSelected = onMovieSelected,
                    categoryName = selectedMovieCategory
                )
                MainDestination.Profile -> ProfileScreen()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MainNavigationDrawer(
    selectedDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var drawerCanExpand by remember { mutableStateOf(false) }
    val drawerWidth by animateDpAsState(
        targetValue = if (expanded) 190.dp else 72.dp,
        label = "drawerWidth"
    )

    LaunchedEffect(Unit) {
        delay(500)
        drawerCanExpand = true
    }

    Column(
        modifier = Modifier
            .width(drawerWidth)
            .fillMaxHeight()
            .background(Color(0xFF0B0F18))
            .onFocusChanged { focusState ->
                expanded = drawerCanExpand && focusState.hasFocus
            }
            .padding(horizontal = 10.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (expanded) "ZoStream" else "ZS",
                color = Color.White,
                style = if (expanded) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Bold
            )
        }

        MainDestination.entries.forEach { destination ->
            MainNavigationItem(
                label = destination.label,
                iconRes = destination.iconRes,
                selected = destination == selectedDestination,
                expanded = expanded,
                onClick = { onDestinationSelected(destination) }
            )
        }
    }
}

@Composable
private fun MainNavigationItem(
    label: String,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    expanded: Boolean = true,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.05f else 1f,
        label = "navigationItemScale"
    )
    val backgroundColor = when {
        focused -> Color(0xFF2563EB)
        selected -> Color(0xFF1E293B)
        else -> Color.Transparent
    }
    val borderColor = if (focused) Color(0xFFBFDBFE) else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick { onClick() }
            .padding(horizontal = if (expanded) 18.dp else 0.dp),
        contentAlignment = if (expanded) Alignment.CenterStart else Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.spacedBy(14.dp) else Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = if (selected || focused) Color.White else Color(0xFFCBD5E1),
                modifier = Modifier.size(24.dp)
            )

            if (expanded) {
                Text(
                    text = label,
                    color = if (selected || focused) Color.White else Color(0xFFCBD5E1),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyTopLevelScreen(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = subtitle,
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
