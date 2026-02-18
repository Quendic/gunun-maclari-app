package com.iptvmac.projesi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import java.util.*
import kotlinx.coroutines.*

@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF020617),
                    surface = Color(0xFF1E293B),
                    primary = Color(0xFF38BDF8),
                    onPrimary = Color.White
                )
            ) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val apiUrl = "http://89.144.10.224:3000/api/matches"
    
    var showStreamDialog by remember { mutableStateOf(false) }
    var availableStreams by remember { mutableStateOf<List<Stream>>(emptyList()) }
    var selectedMatchName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ChannelManager.initialize(context)
            try {
                val response = java.net.URL(apiUrl).readText()
                val list = parseJsonResponse(response)
                
                // Filter matches: Only keep those that have at least one stream in our M3U
                val filteredList = list.filter { match ->
                    val channels = match.channel.split(",").map { it.trim() }
                    channels.any { ChannelManager.findStreams(it).isNotEmpty() }
                }

                withContext(Dispatchers.Main) {
                    matches = filteredList
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    fun launchPlayer(stream: Stream) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra("stream_url", stream.url)
            putExtra("stream_name", stream.name)
        }
        context.startActivity(intent)
    }

    fun handleMatchClick(match: Match) {
        val channels = match.channel.split(",").map { it.trim() }
        val streams = channels.flatMap { ChannelManager.findStreams(it) }

        if (streams.isEmpty()) {
            android.widget.Toast.makeText(context, "Yayın bulunamadı: ${match.channel}", android.widget.Toast.LENGTH_SHORT).show()
        } else if (streams.size == 1) {
            launchPlayer(streams.first())
        } else {
            selectedMatchName = "${match.homeTeam} - ${match.awayTeam}"
            availableStreams = streams
            showStreamDialog = true
        }
    }

    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF020617), Color(0xFF0F172A))
                )
            )
    ) {
        val grouped = matches.groupBy { it.league }
        
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(48.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 60.dp)
        ) {
            item {
                MainHeader()
            }

            if (isLoading && matches.isEmpty()) {
                repeat(3) {
                    item {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(150.dp, 24.dp)
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(32.dp),
                                contentPadding = PaddingValues(vertical = 20.dp)
                            ) {
                                items(3) { ShimmerMatchCard() }
                            }
                        }
                    }
                }
            } else {
                grouped.entries.forEachIndexed { index, (league, leagueMatches) ->
                    item {
                        MatchRow(
                            title = league, 
                            matches = leagueMatches, 
                            isFirstRow = index == 0,
                            onMatchSelected = { handleMatchClick(it) },
                            onScrollToHeader = { scope.launch { lazyListState.animateScrollToItem(0) } }
                        )
                    }
                }
            }
        }

        // Stream Selection Dialog
        if (showStreamDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showStreamDialog = false },
                title = { 
                    Column {
                        Text("Yayın Seçiniz", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text(selectedMatchName, color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableStreams) { stream ->
                            Surface(
                                onClick = {
                                    showStreamDialog = false
                                    launchPlayer(stream)
                                },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stream.name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(stream.quality, color = Color(0xFF38BDF8), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showStreamDialog = false }) {
                        Text("İptal", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun MatchRow(
    title: String, 
    matches: List<Match>, 
    isFirstRow: Boolean,
    onMatchSelected: (Match) -> Unit,
    onScrollToHeader: () -> Unit
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    
    Column {
        Text(
            text = title, 
            style = MaterialTheme.typography.headlineSmall, 
            color = Color(0xFF94A3B8), 
            modifier = Modifier.padding(bottom = 20.dp),
            fontWeight = FontWeight.Bold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            modifier = Modifier
                .focusGroup()
                .focusProperties {
                    this.enter = { firstItemFocusRequester }
                }
                .onPreviewKeyEvent { event ->
                    if (isFirstRow && 
                        event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                        if (event.type == KeyEventType.KeyDown) {
                            onScrollToHeader()
                        }
                        return@onPreviewKeyEvent true
                    }
                    false
                }
        ) {
            itemsIndexed(matches) { index, match ->
                MatchCard(
                    match = match, 
                    onClick = { onMatchSelected(match) },
                    modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun MatchCard(match: Match, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF1E293B),
            focusedContainerColor = Color(0xFF334155)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.5.dp, Color(0xFF38BDF8)),
                shape = RoundedCornerShape(20.dp)
            )
        ),
        modifier = modifier.width(360.dp).height(170.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Team
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    GlideImage(
                        model = match.homeLogoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = match.homeTeam,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }

            // VS & Info
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.3f)) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = match.time,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "VS", 
                    color = Color(0xFF64748B), 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Channel Badges - Only show available ones
                val channels = match.channel.split(",").map { it.trim() }
                    .filter { ChannelManager.findStreams(it).isNotEmpty() }
                    .take(2)
                
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    channels.forEach { channelName ->
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = channelName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Away Team
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    GlideImage(
                        model = match.awayLogoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = match.awayTeam,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ShimmerMatchCard() {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.05f),
    )

    val brush = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
    )

    Box(
        modifier = Modifier
            .width(360.dp)
            .height(170.dp)
            .background(Color(0xFF1E293B), RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush, RoundedCornerShape(20.dp))
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Box(modifier = Modifier.size(6.dp, 32.dp).background(Color(0xFF38BDF8), RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "GÜNÜN MAÇLARI",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

private fun parseJsonResponse(jsonString: String): List<Match> {
    val list = mutableListOf<Match>()
    try {
        val arr = org.json.JSONArray(jsonString)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(Match(
                id = i.toString(),
                league = obj.optString("league"),
                homeTeam = obj.optString("home"),
                awayTeam = obj.optString("away"),
                time = obj.optString("time"),
                channel = obj.optString("channel"),
                isLive = obj.optBoolean("isLive"),
                homeLogo = obj.optString("homeLogo"),
                awayLogo = obj.optString("awayLogo")
            ))
        }
    } catch (e: Exception) {}
    return list
}
