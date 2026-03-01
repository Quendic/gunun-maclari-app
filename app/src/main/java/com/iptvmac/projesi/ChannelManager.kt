package com.iptvmac.projesi

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Serializable

data class Stream(
    val name: String,
    val url: String,
    val quality: String = "Unknown", // FHD, HD, SD
    val group: String = ""
) : Serializable

object ChannelManager {
    private val allStreams = mutableListOf<Stream>()
    private var isInitialized = false

    val isDataLoaded: Boolean get() = allStreams.isNotEmpty()

    fun initialize(context: Context) {
        if (isInitialized) return
        // ... existing logic can stay for default channels if any, or we can skip it
    }

    fun loadFromFirestore(json: String) {
        try {
            allStreams.clear()
            val channelsArr = org.json.JSONArray(json)
            for (i in 0 until channelsArr.length()) {
                val channelObj = channelsArr.getJSONObject(i)
                val channelName = channelObj.getString("name")
                val streamsArr = channelObj.getJSONArray("streams")
                
                for (j in 0 until streamsArr.length()) {
                    val streamObj = streamsArr.getJSONObject(j)
                    allStreams.add(Stream(
                        name = channelName, // Use normalized channel name
                        url = streamObj.getString("url"),
                        quality = streamObj.optString("quality", "SD"),
                        group = streamObj.optString("group", "")
                    ))
                }
            }
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun findStreams(channelName: String): List<Stream> {
        if (channelName.isEmpty()) return emptyList()

        val cleanTarget = cleanName(channelName)
        
        return allStreams.filter { stream ->
            val cleanStream = cleanName(stream.name)
            cleanStream == cleanTarget
        }.sortedBy { 
            when (it.quality) {
                "4K UHD" -> 0
                "FHD" -> 1
                "HD" -> 2
                else -> 3
            }
        }
    }

    private fun cleanName(input: String): String {
        return input.lowercase()
            .replace("tr:", "")
            .replace("tr ", "")
            .replace("turkiye:", "")
            .replace("ı", "i")
            .replace("İ", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace("sport", "spor") // Sport -> Spor normalization
            .replace(Regex("\\b(fhd|hd|sd|uhd|4k|hevc|1080p|720p)\\b"), "")
            .replace("-", "")
            .replace(".", "")
            .replace(" ", "") // Remove all spaces for "S SPOR2" vs "S SPORT 2" matching
            .trim()
    }
}
