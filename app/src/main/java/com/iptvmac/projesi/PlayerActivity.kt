package com.iptvmac.projesi

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource

class PlayerActivity : ComponentActivity() {

    private val channelPool = mapOf(
        "beIN SPORTS 1" to "http://tinyurlatlas02.xyz:8080/yemre.ellialtioglu894/1JZRc2V8CJ/114038",
        "beIN SPORTS 3" to "http://tinyurlatlas02.xyz:8080/yemre.ellialtioglu894/1JZRc2V8CJ/114041",
        "S Sport 2" to "http://tinyurlatlas02.xyz:8080/yemre.ellialtioglu894/1JZRc2V8CJ/102337"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val match = intent.getSerializableExtra("match") as? Match
        val streamUrl = channelPool[match?.channel] ?: ""

        setContent {
            VideoPlayerScreen(streamUrl)
        }
    }
}

@Composable
fun VideoPlayerScreen(url: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
            val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(url)))
            setMediaSource(hlsMediaSource)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        factory = {
            StyledPlayerView(it).apply {
                player = exoPlayer
                useController = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    )
}
