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
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*

@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B)
                )
            ) {
                MainScreen(onMatchSelected = { match ->
                    val intent = Intent(this, PlayerActivity::class.java)
                    intent.putExtra("match", match)
                    startActivity(intent)
                })
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen(onMatchSelected: (Match) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val apiUrl = "http://10.0.2.2:3000/api/matches"
    
    // Stream Selection State
    var showStreamDialog by remember { mutableStateOf(false) }
    var availableStreams by remember { mutableStateOf<List<Stream>>(emptyList()) }

    LaunchedEffect(Unit) {
        // Initialize Channel Manager
        withContext(Dispatchers.IO) {
            ChannelManager.initialize(context)
            try {
                val response = java.net.URL(apiUrl).readText()
                val list = parseJsonResponse(response)
                withContext(Dispatchers.Main) {
                    matches = list
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    // Launch Player helper
    fun launchPlayer(stream: Stream) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra("stream_url", stream.url)
            putExtra("stream_name", stream.name)
        }
        context.startActivity(intent)
    }

    // Handle Match Click
    fun handleMatchClick(match: Match) {
        val channels = match.channel.split(",").map { it.trim() }
        val streams = channels.flatMap { ChannelManager.findStreams(it) }

        if (streams.isEmpty()) {
            android.widget.Toast.makeText(context, "Yayın bulunamadı: ${match.channel}", android.widget.Toast.LENGTH_SHORT).show()
        } else if (streams.size == 1) {
            launchPlayer(streams.first())
        } else {
            availableStreams = streams
            showStreamDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text("IPTV MAÇLAR", style = MaterialTheme.typography.displaySmall, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            if (!isLoading || matches.isNotEmpty()) {
                val grouped = matches.groupBy { it.league }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    grouped.forEach { (league, leagueMatches) ->
                        item {
                            MatchRow(league, leagueMatches, onMatchSelected = { handleMatchClick(it) })
                        }
                    }
                }
            }
        }
        
        // Stream Selection Dialog
        if (showStreamDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showStreamDialog = false },
                title = { Text("Yayın Seçiniz", color = Color.White) },
                text = {
                    LazyColumn {
                        items(availableStreams) { stream ->
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showStreamDialog = false
                                    launchPlayer(stream)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${stream.name} (${stream.quality})", color = Color(0xFF00FFCC))
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
                containerColor = Color(0xFF1E293B)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MatchRow(title: String, matches: List<Match>, onMatchSelected: (Match) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(matches) { match ->
                MatchCard(match = match, onClick = { onMatchSelected(match) })
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun MatchCard(match: Match, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        modifier = Modifier.width(340.dp).height(140.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Home
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                GlideImage(model = match.homeLogoUrl, contentDescription = null, modifier = Modifier.size(54.dp), contentScale = ContentScale.Fit)
                Spacer(modifier = Modifier.height(8.dp))
                Text(match.homeTeam, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, textAlign = TextAlign.Center)
            }
            // VS
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.8f)) {
                Text(match.time, style = MaterialTheme.typography.headlineSmall, color = Color(0xFF00FFCC), fontWeight = FontWeight.Black)
                Text("VS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Detailed Channel Info
                val channels = match.channel.split(",").map { it.trim() }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    channels.forEach { channelName ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = channelName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            // Away
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                GlideImage(model = match.awayLogoUrl, contentDescription = null, modifier = Modifier.size(54.dp), contentScale = ContentScale.Fit)
                Spacer(modifier = Modifier.height(8.dp))
                Text(match.awayTeam, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

private fun parseJsonResponse(jsonString: String): List<Match> {
    val list = mutableListOf<Match>()
    try {
        val arr = org.json.JSONArray(jsonString)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(Match(i.toString(), obj.optString("league"), obj.optString("home"), obj.optString("away"), obj.optString("time"), obj.optString("channel"), obj.optBoolean("isLive")))
        }
    } catch (e: Exception) {}
    return list
}
