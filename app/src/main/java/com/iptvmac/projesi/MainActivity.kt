package com.iptvmac.projesi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.tv.foundation.lazy.list.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.tv.foundation.PivotOffsets
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
import androidx.compose.ui.graphics.graphicsLayer
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import java.util.*
import kotlinx.coroutines.*
import org.json.JSONObject
import android.net.Uri
import android.util.Log
import android.app.DownloadManager
import android.content.Context
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

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
    
    // Update States
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<Pair<String, String>?>(null) } // Version, URL

    LaunchedEffect(Unit) {
        // Check for updates
        scope.launch(Dispatchers.IO) {
            try {
                val githubRepo = "Quendic/gunun-maclari-app"
                val releaseUrl = "https://api.github.com/repos/$githubRepo/releases/latest"
                val response = java.net.URL(releaseUrl).readText()
                val json = JSONObject(response)
                val latestVersion = json.getString("tag_name").replace("v", "")
                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                
                if (isNewerVersion(currentVersion, latestVersion)) {
                    val assets = json.getJSONArray("assets")
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.getString("name").endsWith(".apk")) {
                            updateInfo = latestVersion to asset.getString("browser_download_url")
                            withContext(Dispatchers.Main) { showUpdateDialog = true }
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    val tvLazyListState = rememberTvLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF020617), Color(0xFF0F172A))
                )
            )
    ) {
        val grouped = remember(matches) { matches.groupBy { it.league } }
        val lastLeagueIndex = remember(grouped) { grouped.size - 1 }
        
        TvLazyColumn(
            state = tvLazyListState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(clip = false)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                        if (tvLazyListState.firstVisibleItemIndex == 0 && tvLazyListState.firstVisibleItemScrollOffset <= 0) {
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                },
            verticalArrangement = Arrangement.spacedBy(48.dp),
            contentPadding = PaddingValues(start = 58.dp, end = 58.dp, top = 60.dp, bottom = 150.dp),
            pivotOffsets = PivotOffsets(parentFraction = 0.5f)
        ) {
            item(key = "header") {
                MainHeader()
            }

            if (isLoading && matches.isEmpty()) {
                repeat(3) { rowIndex ->
                    item(key = "shimmer_row_$rowIndex") {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(150.dp, 24.dp)
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            TvLazyRow(
                                horizontalArrangement = Arrangement.spacedBy(32.dp),
                                contentPadding = PaddingValues(vertical = 40.dp),
                                pivotOffsets = PivotOffsets(parentFraction = 0.5f)
                            ) {
                                items(3, key = { "shimmer_$it" }) { ShimmerMatchCard() }
                            }
                        }
                    }
                }
            } else {
                grouped.entries.forEachIndexed { index, (league, leagueMatches) ->
                    item(key = "league_$league") {
                        MatchRow(
                            title = league, 
                            matches = leagueMatches, 
                            isFirstRow = index == 0,
                            onMatchSelected = { handleMatchClick(it) },
                            onScrollToHeader = { scope.launch { tvLazyListState.animateScrollToItem(0) } }
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

        // Update Dialog
        if (showUpdateDialog && updateInfo != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text("Yeni Güncelleme Mevcut!", color = Color.White) },
                text = { 
                    Text(
                        "Uygulamanın yeni bir sürümü (v${updateInfo!!.first}) mevcut. Şimdi indirip yüklemek ister misiniz?",
                        color = Color(0xFF94A3B8)
                    ) 
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            showUpdateDialog = false
                            downloadAndInstallApk(context, updateInfo!!.second, updateInfo!!.first)
                        }
                    ) {
                        Text("Güncelle")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Daha Sonra", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

private fun isNewerVersion(current: String, latest: String): Boolean {
    try {
        val currParts = current.split(".").map { it.toInt() }
        val lateParts = latest.split(".").map { it.toInt() }
        for (i in 0 until maxOf(currParts.size, lateParts.size)) {
            val curr = if (i < currParts.size) currParts[i] else 0
            val late = if (i < lateParts.size) lateParts[i] else 0
            if (late > curr) return true
            if (curr > late) return false
        }
    } catch (e: Exception) {}
    return false
}

private fun downloadAndInstallApk(context: Context, url: String, version: String) {
    val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "gunun-maclari-v$version.apk")
    if (destination.exists()) destination.delete()

    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle("Günün Maçları Güncelleniyor")
        .setDescription("Yeni sürüm indiriliyor...")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationUri(Uri.fromFile(destination))

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = manager.enqueue(request)

    val onComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id == downloadId) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = manager.query(query)
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val uri = manager.getUriForDownloadedFile(downloadId)
                        if (uri != null) {
                            installApk(context, uri)
                        } else {
                            Log.e("DownloadManager", "Downloaded file URI is null for downloadId: $downloadId")
                        }
                    } else {
                        val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                        Log.e("DownloadManager", "Download failed for downloadId: $downloadId, status: $status, reason: $reason")
                    }
                } else {
                    Log.e("DownloadManager", "Cursor is empty for downloadId: $downloadId")
                }
                cursor.close()
                context.unregisterReceiver(this)
            }
        }
    }
    context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
}

private fun installApk(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
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
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 20.dp),
            pivotOffsets = PivotOffsets(parentFraction = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (isFirstRow && event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                        if (event.type == KeyEventType.KeyDown) onScrollToHeader()
                        return@onPreviewKeyEvent true
                    }
                    false
                }
        ) {
            items(matches, key = { it.id }) { match ->
                MatchCard(
                    match = match, 
                    onClick = { onMatchSelected(match) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun MatchCard(match: Match, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val channels = remember(match.channel) {
        match.channel.split(",").map { it.trim() }
            .filter { ChannelManager.findStreams(it).isNotEmpty() }
            .take(2)
    }

    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF1E293B),
            focusedContainerColor = Color(0xFF334155)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.5.dp, Color(0xFF38BDF8)),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        modifier = modifier
            .width(260.dp)
            .height(130.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Team
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFF262F3F), RoundedCornerShape(12.dp))
                        .padding(6.dp)
                ) {
                    GlideImage(
                        model = match.homeLogoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    ) {
                        it.override(100, 100) // Lower resolution for TV GPU
                          .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                          .dontAnimate()
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = match.homeTeam,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            // VS & Info
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.3f)) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = match.time,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "VS", 
                    color = Color(0xFF64748B), 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    channels.forEach { channelName ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF262F3F), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = channelName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }

            // Away Team
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFF262F3F), RoundedCornerShape(12.dp))
                        .padding(6.dp)
                ) {
                    GlideImage(
                        model = match.awayLogoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    ) {
                        it.override(100, 100)
                          .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                          .dontAnimate()
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = match.awayTeam,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ShimmerMatchCard() {
    Box(
        modifier = Modifier
            .width(270.dp)
            .height(128.dp)
            .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
    )
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
