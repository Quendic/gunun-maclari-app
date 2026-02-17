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

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val inputStream = context.assets.open("channels.m3u")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var currentStreamName = ""
            var currentGroup = ""

            reader.forEachLine { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty()) {
                    if (trimmedLine.startsWith("#EXTINF")) {
                        // Parse metadata
                        // format: #EXTINF:-1 tvg-id="id" tvg-name="name" tvg-logo="url" group-title="group",Display Name
                        
                        // Extract display name (last part after comma)
                        val lastCommaIndex = trimmedLine.lastIndexOf(',')
                        if (lastCommaIndex != -1) {
                            currentStreamName = trimmedLine.substring(lastCommaIndex + 1).trim()
                        }
                        
                        // Extract group-title
                        val groupMatch = Regex("group-title=\"(.*?)\"").find(trimmedLine)
                        if (groupMatch != null) {
                            currentGroup = groupMatch.groupValues[1]
                        }

                    } else if (!trimmedLine.startsWith("#")) {
                        // It's a URL
                        if (currentStreamName.isNotEmpty()) {
                            val quality = when {
                                currentStreamName.contains("UHD", true) || currentStreamName.contains("4K", true) -> "4K UHD"
                                currentStreamName.contains("FHD", true) -> "FHD"
                                currentStreamName.contains("HD", true) -> "HD"
                                currentStreamName.contains("SD", true) -> "SD"
                                else -> "SD" // Default
                            }
                            
                            allStreams.add(Stream(currentStreamName, trimmedLine, quality, currentGroup))
                            currentStreamName = "" // Reset
                        }
                    }
                }
            }
            reader.close()
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
            when {
                it.name.contains("4K", true) || it.name.contains("UHD", true) -> 0
                it.name.contains("FHD", true) -> 1
                it.name.contains("HD", true) -> 2
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
