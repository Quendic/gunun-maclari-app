package com.iptvmac.projesi

import java.io.Serializable

data class Match(
    val id: String,
    val league: String,
    val homeTeam: String,
    val awayTeam: String,
    val time: String,
    val channel: String,
    val isLive: Boolean = false
) : Serializable {
    val homeLogoUrl: String get() = LogoManager.getLogoUrl(homeTeam, league)
    val awayLogoUrl: String get() = LogoManager.getLogoUrl(awayTeam, league)
}

object LogoManager {
    private const val GITHUB_BASE = "https://raw.githubusercontent.com/luukhopman/football-logos/master/logos"

    fun getLogoUrl(teamName: String?, league: String): String {
        if (teamName.isNullOrEmpty()) return ""
        
        val folder = when {
            league.contains("Süper Lig", true) -> "Türkiye - Süper Lig"
            league.contains("La Liga", true) -> "Spain - LaLiga"
            league.contains("Premier", true) -> "England - Premier League"
            league.contains("Serie A", true) -> "Italy - Serie A"
            else -> "Türkiye - Süper Lig"
        }

        val name = teamName.trim()
        
        // 1. First handle specific overrides for teams that have completely different names in the repo
        var finalName = when {
            // Türkiye - Süper Lig
            name.contains("Besiktas", true) -> "Besiktas JK"
            name.contains("Basaksehir", true) -> "Basaksehir FK"
            name.contains("Gaziantep", true) -> "Gaziantep FK"
            name.contains("Genclerbirligi", true) -> "Genclerbirligi Ankara"
            name.contains("Rizespor", true) -> "Caykur Rizespor"
            name.contains("Kasimpasa", true) -> "Kasimpasa"
            name.contains("Karagumruk", true) || name.contains("Karagümrük", true) -> "Fatih Karagümrük"
            name.contains("Göztepe", true) -> "Göztepe"
            name.contains("Eyüp", true) -> "Eyüpspor"
            
            // Spain - LaLiga
            name.contains("Barcelona", true) -> "FC Barcelona"
            name.contains("Girona", true) -> "Girona FC"
            name.contains("Real Sociedad", true) -> "Real Sociedad"
            name.contains("Atletico", true) -> "Atletico de Madrid"
            name.contains("Villarreal", true) -> "Villarreal CF"
            name.contains("Malaga", true) -> "Malaga CF"
            name.contains("Sevilla", true) -> "Sevilla FC"
            name.contains("Valencia", true) -> "Valencia CF"
            name.contains("Betis", true) -> "Real Betis Balompie"
            name.contains("Getafe", true) -> "Getafe CF"

            // Italy - Serie A
            name.contains("Cagliari", true) -> "Cagliari Calcio"
            name.contains("Lecce", true) -> "US Lecce"
            
            else -> null
        }

        // 2. If no override, normalize based on repo naming patterns
        if (finalName == null) {
            finalName = name
                .replace("İ", "I").replace("ı", "i")
                .replace("Ğ", "G").replace("ğ", "g")
                .replace("Ş", "S").replace("ş", "s")
                .replace("Ç", "C").replace("ç", "c")
                // Repo keeps 'ü' and 'ö' for Turkish teams
                .replace("á", "a").replace("é", "e")
                .replace("í", "i").replace("ó", "o")
                .replace("ú", "u")
        }

        // Manually percent-encode for common special characters to ensure GitHub raw URL compatibility
        fun manualEncode(s: String): String {
            return s.replace(" ", "%20")
                .replace("ü", "%C3%BC").replace("Ü", "%C3%9C")
                .replace("ö", "%C3%B6").replace("Ö", "%C3%96")
        }

        val encodedFolder = manualEncode(folder)
        val encodedFile = manualEncode(finalName!!)

        return "$GITHUB_BASE/$encodedFolder/$encodedFile.png"
    }
}
