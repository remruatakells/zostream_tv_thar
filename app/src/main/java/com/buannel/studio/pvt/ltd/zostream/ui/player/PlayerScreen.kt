package com.buannel.studio.pvt.ltd.zostream.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.buannel.studio.pvt.ltd.zostream.ui.screens.TvLoader
import com.buannel.studio.pvt.ltd.zostream.ui.screens.tvDpadClick

@Composable
fun PlayerScreen(
    streamUrl: String,
    title: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // 🔥 Create player
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    var isBuffering by remember { mutableStateOf(true) }

    // 🎬 Load media
    LaunchedEffect(streamUrl) {
        if (streamUrl.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri(streamUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    // 🔄 Player listener
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
        }
    }

    // 🔥 Release player
    DisposableEffect(Unit) {
        onDispose {
            player.stop()
            player.release()

            // 👉 IMPORTANT for your backend:
            // call STOP STREAM API here
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // 🎬 Video Player
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    this.player = player
                    useController = true

                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 🔄 Loading indicator
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    TvLoader()

                }
            }
            return
        }

        // 🎬 Title overlay
        if (title.isNotEmpty()) {
            Text(
                text = title,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color(0x66000000))
                    .padding(10.dp)
            )
        }

        // 🔙 Back button
        Text(
            text = "← Back",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 60.dp, start = 16.dp)
                .background(Color(0x66000000))
                .padding(10.dp)
                .tvDpadClick {
                    onBack()
                }
        )
    }
}
