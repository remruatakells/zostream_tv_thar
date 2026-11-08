package com.buannel.studio.pvt.ltd.zostream.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ParentalControlScreen(
    onAdultSelected: () -> Unit,
    onKidsSelected: () -> Unit
) {
    val adultFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        adultFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060A12))
            .padding(horizontal = 56.dp, vertical = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Who's watching?",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Zo Stream will adjust the content based on the viewing mode.",
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewingModeCard(
                    title = "Adult",
                    badge = "Full Catalog",
                    description = "Show the complete movie and series catalog for this account.",
                    actionText = "Continue as Adult",
                    modifier = Modifier.focusRequester(adultFocusRequester),
                    onClick = onAdultSelected
                )
                ViewingModeCard(
                    title = "Kids",
                    badge = "Family Safe",
                    description = "Use a child-friendly mode and hide age-restricted content.",
                    actionText = "Continue as Kids",
                    onClick = onKidsSelected
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ViewingModeCard(
    title: String,
    badge: String,
    description: String,
    actionText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        label = "viewingModeCardScale"
    )
    val shape = RoundedCornerShape(10.dp)
    val borderColor = if (focused) Color(0xFFBFDBFE) else Color(0xFF334155)
    val backgroundColor = if (focused) Color(0xFF1D4ED8) else Color(0xFF111827)

    Column(
        modifier = modifier
            .width(330.dp)
            .height(230.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(backgroundColor, shape)
            .border(2.dp, borderColor, shape)
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick { onClick() }
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = badge,
                color = if (focused) Color.White else Color(0xFF93C5FD),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                color = if (focused) Color(0xFFEFF6FF) else Color(0xFFCBD5E1),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Text(
            text = actionText,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (focused) Color(0xFF0F172A) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
