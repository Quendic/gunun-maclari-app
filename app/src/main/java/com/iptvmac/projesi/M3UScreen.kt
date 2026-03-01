@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.iptvmac.projesi

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun M3USetupScreen(onComplete: () -> Unit) {
    var m3uUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                scope.launch {
                    isLoading = true
                    processLocalFile(context, it, onComplete = { success, msg ->
                        isLoading = false
                        if (success) onComplete()
                        else errorMessage = msg
                    })
                }
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp).widthIn(max = 500.dp)
        ) {
            Text(
                "M3U Listenizi Ekleyin",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "İçeriği görmek için IPTV listenizi (URL veya Dosya) eklemelisiniz.",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = m3uUrl,
                onValueChange = { m3uUrl = it },
                label = { Text("M3U URL", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            if (m3uUrl.isNotEmpty()) {
                                isLoading = true
                                scope.launch {
                                    processUrl(m3uUrl, onComplete = { success, msg ->
                                        isLoading = false
                                        if (success) onComplete()
                                        else errorMessage = msg
                                    })
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.colors(containerColor = Color(0xFF38BDF8))
                    ) {
                        Text("URL Kaydet")
                    }

                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.colors(containerColor = Color(0xFF1E293B))
                    ) {
                        Text("Dosya Seç")
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

suspend fun processUrl(url: String, onComplete: (Boolean, String?) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            AuthManager.setUserM3UUrl(url)
            val apiUrl = "http://10.0.2.2:3000/process-m3u"
            
            val connection = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            
            val jsonBody = JSONObject().put("m3uUrl", url).toString()
            Log.d("M3U_DEBUG", "Sending POST to $apiUrl with body: $jsonBody")
            connection.outputStream.write(jsonBody.toByteArray())
            
            Log.d("M3U_DEBUG", "Response Code: ${connection.responseCode}")
            if (connection.responseCode == 200) {
                val responseStr = connection.inputStream.bufferedReader().readText()
                val responseJson = JSONObject(responseStr)
                if (responseJson.optBoolean("success")) {
                    val channelsArray = responseJson.getJSONObject("data").getJSONArray("channels").toString()
                    AuthManager.saveChannels(channelsArray)
                    ChannelManager.loadFromFirestore(channelsArray)
                    onComplete(true, null)
                } else {
                    onComplete(false, responseJson.optString("error", "Bilinmeyen hata"))
                }
            } else {
                onComplete(false, "Sunucu hatası: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            onComplete(false, e.message)
        }
    }
}

suspend fun processLocalFile(context: android.content.Context, uri: Uri, onComplete: (Boolean, String?) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val m3uText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""

            val apiUrl = "http://10.0.2.2:3000/process-m3u-text"
            val connection = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            
            val jsonBody = JSONObject().put("m3uContent", m3uText).toString()
            Log.d("M3U_DEBUG", "Sending POST to $apiUrl. Content length: ${m3uText.length}")
            connection.outputStream.write(jsonBody.toByteArray())
            
            Log.d("M3U_DEBUG", "Response Code: ${connection.responseCode}")
            if (connection.responseCode == 200) {
                val responseStr = connection.inputStream.bufferedReader().readText()
                val responseJson = JSONObject(responseStr)
                if (responseJson.optBoolean("success")) {
                    val channelsArray = responseJson.getJSONObject("data").getJSONArray("channels").toString()
                    AuthManager.saveChannels(channelsArray)
                    ChannelManager.loadFromFirestore(channelsArray)
                    onComplete(true, null)
                } else {
                    onComplete(false, responseJson.optString("error", "Bilinmeyen hata"))
                }
            } else {
                onComplete(false, "Sunucu hatası: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            onComplete(false, e.message)
        }
    }
}
