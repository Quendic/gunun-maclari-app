@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi::class)
package com.iptvmac.projesi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.tv.material3.*
import androidx.tv.foundation.lazy.list.*
import androidx.tv.foundation.PivotOffsets
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import java.util.*
import kotlinx.coroutines.*
import org.json.JSONObject
import android.net.Uri
import android.util.Log

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalGlideComposeApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            androidx.tv.material3.MaterialTheme(
                colorScheme = androidx.tv.material3.MaterialTheme.colorScheme.copy(
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
    
    var isSubscribed by remember { mutableStateOf<Boolean?>(null) }
    var m3uSynced by remember { mutableStateOf<Boolean?>(null) }
    
    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var userProfile by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showProfileMenu by remember { mutableStateOf(false) }
    
    val apiUrl = "http://89.144.10.224:3000/api/matches"

    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                AuthManager.getUserFullProfile { profile ->
                    userProfile = profile
                }

                // 1. Abonelik Kontrolü
                AuthManager.checkSubscriptionStatus { subscribed ->
                    isSubscribed = subscribed
                    if (subscribed) {
                        // 2. Kanal Kontrolü (Firestore'dan)
                        AuthManager.getChannels { json ->
                            if (json != null) {
                                ChannelManager.loadFromFirestore(json)
                                m3uSynced = true
                            } else {
                                m3uSynced = false
                            }
                        }
                    } else {
                        m3uSynced = false
                    }
                }

                // 3. Maç Listesini Çek
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

    LaunchedEffect(Unit) {
        loadData()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isSubscribed == null || (isSubscribed == true && m3uSynced == null) -> {
                LoadingPlaceholder()
            }
            isSubscribed == false -> {
                SubscriptionRequiredScreen(onRefresh = { loadData() })
            }
            m3uSynced == false -> {
                M3USetupScreen(onComplete = { m3uSynced = true })
            }
            else -> {
                MatchListContent(
                    matches = matches, 
                    isLoading = isLoading,
                    userProfile = userProfile,
                    showProfileMenu = showProfileMenu,
                    onToggleProfile = { showProfileMenu = !showProfileMenu },
                    onUpdateList = {
                        m3uSynced = false
                        showProfileMenu = false
                    }
                )
            }
        }
    }
}

@Composable
fun LoadingPlaceholder() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020617)), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF38BDF8))
    }
}

@Composable
fun SubscriptionRequiredScreen(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF020617), Color(0xFF1E1B4B)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(modifier = Modifier.size(80.dp).background(Color(0xFF38BDF8).copy(alpha = 0.1f), CircleShape).padding(16.dp), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            androidx.tv.material3.Text("Premium Üyelik Gerekli", style = androidx.tv.material3.MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(12.dp))
            androidx.tv.material3.Text("İçeriğe erişmek için aktif bir aboneliğiniz olmalıdır.", color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(48.dp))
            androidx.tv.material3.Button(onClick = onRefresh, modifier = Modifier.width(260.dp), colors = ButtonDefaults.colors(containerColor = Color(0xFF38BDF8), contentColor = Color.White)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.tv.material3.Text("Durumu Kontrol Et")
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MatchListContent(
    matches: List<Match>, 
    isLoading: Boolean,
    userProfile: Map<String, Any>?,
    showProfileMenu: Boolean,
    onToggleProfile: () -> Unit,
    onUpdateList: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val tvLazyListState = rememberTvLazyListState()
    
    var showStreamDialog by remember { mutableStateOf(false) }
    var availableStreams by remember { mutableStateOf<List<Stream>>(emptyList()) }
    var selectedMatchName by remember { mutableStateOf("") }

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

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF020617), Color(0xFF0F172A))))
    ) {
        val grouped = remember(matches) { matches.groupBy { it.league } }
        
        TvLazyColumn(
            state = tvLazyListState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(48.dp),
            contentPadding = PaddingValues(start = 58.dp, end = 58.dp, top = 60.dp, bottom = 150.dp),
            pivotOffsets = PivotOffsets(parentFraction = 0.5f)
        ) {
            item(key = "header") { 
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MainHeader()
                    
                    ProfileSection(
                        userProfile = userProfile,
                        isExpanded = showProfileMenu,
                        onToggle = onToggleProfile,
                        onLogout = {
                            AuthManager.signOut(context) {
                                context.startActivity(Intent(context, LoginActivity::class.java))
                                (context as? Activity)?.finish()
                            }
                        },
                        onUpdateList = onUpdateList
                    )
                }
            }

            if (isLoading && matches.isEmpty()) {
                repeat(3) { rowIndex ->
                    item {
                        Column {
                            Box(modifier = Modifier.size(150.dp, 24.dp).background(Color.White.copy(0.05f), RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(20.dp))
                            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                items(3) { ShimmerMatchCard() }
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

        if (showStreamDialog) {
            // Use MaterialAlertDialog but content should be custom UI to look good on TV
            MaterialAlertDialog(
                onDismissRequest = { showStreamDialog = false },
                title = { 
                    Column {
                        androidx.tv.material3.Text("Yayın Seçiniz", color = Color.White, style = androidx.tv.material3.MaterialTheme.typography.headlineSmall)
                        androidx.tv.material3.Text(selectedMatchName, color = Color(0xFF94A3B8), style = androidx.tv.material3.MaterialTheme.typography.labelMedium)
                    }
                },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                        items(availableStreams) { stream ->
                            androidx.tv.material3.Surface(
                                onClick = {
                                    showStreamDialog = false
                                    launchPlayer(stream)
                                },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color.White.copy(0.05f),
                                    focusedContainerColor = Color.White.copy(0.15f)
                                )
                            ) {
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF38BDF8))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    androidx.tv.material3.Text(text = stream.name, color = Color.White, modifier = Modifier.weight(1f))
                                    androidx.tv.material3.Text(stream.quality, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    MaterialTextButton(onClick = { showStreamDialog = false }) {
                        androidx.tv.material3.Text("İptal", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MatchRow(title: String, matches: List<Match>, isFirstRow: Boolean, onMatchSelected: (Match) -> Unit, onScrollToHeader: () -> Unit) {
    Column {
        androidx.tv.material3.Text(text = title, style = androidx.tv.material3.MaterialTheme.typography.headlineSmall, color = Color(0xFF94A3B8), modifier = Modifier.padding(bottom = 20.dp), fontWeight = FontWeight.Bold)
        TvLazyRow(horizontalArrangement = Arrangement.spacedBy(32.dp), contentPadding = PaddingValues(horizontal = 48.dp, vertical = 20.dp), pivotOffsets = PivotOffsets(0.5f)) {
            items(matches, key = { it.id }) { match ->
                MatchCard(match = match, onClick = { onMatchSelected(match) })
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun MatchCard(match: Match, onClick: () -> Unit) {
    val channels = remember(match.channel) {
        match.channel.split(",").map { it.trim() }.filter { ChannelManager.findStreams(it).isNotEmpty() }.take(2)
    }

    androidx.tv.material3.Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF1E293B), focusedContainerColor = Color(0xFF334155)),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(border = BorderStroke(2.5.dp, Color(0xFF38BDF8)), shape = RoundedCornerShape(12.dp))),
        modifier = Modifier.width(260.dp).height(130.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(50.dp).background(Color(0xFF262F3F), RoundedCornerShape(12.dp)).padding(6.dp)) {
                    GlideImage(model = match.homeLogoUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.height(4.dp))
                androidx.tv.material3.Text(text = match.homeTeam, color = Color.White, maxLines = 1, textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.3f)) {
                androidx.tv.material3.Text(text = match.time, style = androidx.tv.material3.MaterialTheme.typography.titleLarge, color = Color(0xFF38BDF8), fontWeight = FontWeight.Black)
                androidx.tv.material3.Text(text = "VS", color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    channels.forEach { channelName ->
                        Box(modifier = Modifier.background(Color(0xFF0F172A), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            androidx.tv.material3.Text(text = channelName.uppercase(), style = androidx.tv.material3.MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), maxLines = 1, fontWeight = FontWeight.Black, fontSize = 8.sp)
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(50.dp).background(Color(0xFF262F3F), RoundedCornerShape(12.dp)).padding(6.dp)) {
                    GlideImage(model = match.awayLogoUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.height(4.dp))
                androidx.tv.material3.Text(text = match.awayTeam, color = Color.White, maxLines = 1, textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ShimmerMatchCard() {
    Box(modifier = Modifier.width(270.dp).height(128.dp).background(Color(0xFF1E293B), RoundedCornerShape(16.dp)))
}

@Composable
fun MainHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Box(modifier = Modifier.size(6.dp, 32.dp).background(Color(0xFF38BDF8), RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.width(16.dp))
        androidx.tv.material3.Text(text = "GÜNÜN MAÇLARI", style = androidx.tv.material3.MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Black)
    }
}

private fun parseJsonResponse(jsonString: String): List<Match> {
    val list = mutableListOf<Match>()
    try {
        val arr = org.json.JSONArray(jsonString)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(Match(id = i.toString(), league = obj.optString("league"), homeTeam = obj.optString("home"), awayTeam = obj.optString("away"), time = obj.optString("time"), channel = obj.optString("channel"), isLive = obj.optBoolean("isLive"), homeLogo = obj.optString("homeLogo"), awayLogo = obj.optString("awayLogo")))
        }
    } catch (e: Exception) {}
    return list
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ProfileSection(
    userProfile: Map<String, Any>?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onLogout: () -> Unit,
    onUpdateList: () -> Unit
) {
    val displayName = userProfile?.get("displayName") as? String ?: "Profil"
    val photoUrl = userProfile?.get("photoUrl") as? String
    val isPremium = userProfile?.get("isPremium") as? Boolean ?: false
    
    val focusRequester = remember { FocusRequester() }

    // Menü açıldığında ilk butona odaklan
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            focusRequester.requestFocus()
        }
    }

    Column(horizontalAlignment = Alignment.End) {
        androidx.tv.material3.Surface(
            onClick = onToggle,
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF1E293B).copy(alpha = 0.8f),
                focusedContainerColor = Color(0xFF334155)
            ),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(border = BorderStroke(2.dp, Color(0xFF38BDF8)), shape = RoundedCornerShape(24.dp))
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.size(24.dp).background(Color(0xFF38BDF8).copy(0.1f), CircleShape)) {
                    if (photoUrl != null) {
                        GlideImage(model = photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    }
                }
                androidx.tv.material3.Text(
                    text = displayName,
                    color = Color.White,
                    style = androidx.tv.material3.MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isExpanded) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    androidx.tv.material3.Text(
                        text = "ABONELİK DURUMU",
                        style = androidx.tv.material3.MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontSize = 9.sp
                    )
                    androidx.tv.material3.Text(
                        text = if (isPremium) "Premium (Aktif)" else "Ücretsiz",
                        color = if (isPremium) Color(0xFF22C55E) else Color(0xFFFACC15),
                        style = androidx.tv.material3.MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    androidx.tv.material3.Button(
                        onClick = onUpdateList,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .focusRequester(focusRequester),
                        colors = ButtonDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.White.copy(0.1f)),
                        scale = ButtonDefaults.scale(focusedScale = 1.0f)
                    ) {
                        androidx.tv.material3.Text("Liste Güncelle", color = Color.White, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    androidx.tv.material3.Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Red.copy(0.1f)),
                        scale = ButtonDefaults.scale(focusedScale = 1.0f)
                    ) {
                        androidx.tv.material3.Text("Çıkış Yap", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.tv.material3.Text(
                        text = "Kapatmak için Geri yapın",
                        style = androidx.tv.material3.MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569),
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
