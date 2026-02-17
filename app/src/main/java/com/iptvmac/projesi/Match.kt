package com.iptvmac.projesi

import java.io.Serializable

data class Match(
    val id: String,
    val league: String,
    val homeTeam: String,
    val awayTeam: String,
    val time: String,
    val channel: String,
    val isLive: Boolean = false,
    val homeLogo: String? = null,
    val awayLogo: String? = null
) : Serializable {
    val homeLogoUrl: String get() = if (!homeLogo.isNullOrEmpty()) homeLogo else LogoManager.getLogoUrl(homeTeam, league)
    val awayLogoUrl: String get() = if (!awayLogo.isNullOrEmpty()) awayLogo else LogoManager.getLogoUrl(awayTeam, league)
}

object LogoManager {
    private const val GITHUB_BASE = "https://raw.githubusercontent.com/luukhopman/football-logos/master/logos"

    fun getLogoUrl(teamName: String?, league: String): String {
        if (teamName.isNullOrEmpty()) return ""

        val name = teamName.trim()
        
        // 1. Determine Team Name Override
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
            name.contains("Real Madrid", true) -> "Real Madrid"

            // Italy - Serie A
            name.contains("Cagliari", true) -> "Cagliari Calcio"
            name.contains("Lecce", true) -> "US Lecce"
            name.contains("Juventus", true) -> "Juventus FC"
            name.contains("Milan", true) -> "AC Milan"
            name.contains("Inter", true) -> "Inter"
            name.contains("Roma", true) -> "AS Roma"
            name.contains("Lazio", true) -> "SS Lazio"
            name.contains("Napoli", true) -> "SSC Napoli"
            
            // England
            name.contains("Arsenal", true) -> "Arsenal FC"
            name.contains("Aston Villa", true) -> "Aston Villa"
            name.contains("Chelsea", true) -> "Chelsea FC"
            name.contains("Liverpool", true) -> "Liverpool FC"
            name.contains("City", true) && name.contains("Manchester", true) -> "Manchester City"
            name.contains("United", true) && name.contains("Manchester", true) -> "Manchester United"

            // France
            name.contains("Paris", true) || name.contains("PSG", true) -> "Paris Saint-Germain"
            name.contains("Monaco", true) -> "AS Monaco"
            name.contains("Lille", true) -> "LOSC Lille"
            name.contains("Lyon", true) -> "Olympique Lyonnais"
            name.contains("Marseille", true) -> "Olympique Marseille"

            // Germany
            name.contains("Dortmund", true) -> "Borussia Dortmund"
            name.contains("Bayern", true) -> "Bayern Munich"
            name.contains("Leverkusen", true) -> "Bayer 04 Leverkusen"
            name.contains("Leipzig", true) -> "RB Leipzig"

            // Portugal
            name.contains("Benfica", true) -> "SL Benfica"
            name.contains("Sporting", true) -> "Sporting CP"
            name.contains("Porto", true) -> "FC Porto"

            // Italy - Additions
            name.contains("Atalanta", true) -> "Atalanta BC"
            name.contains("Inter", true) -> "Inter Milan"

            else -> null
        }

        // 2. Normalize if no override
        if (finalName == null) {
            finalName = name
                .replace("İ", "I").replace("ı", "i")
                .replace("Ğ", "G").replace("ğ", "g")
                .replace("Ş", "S").replace("ş", "s")
                .replace("Ç", "C").replace("ç", "c")
                .replace("á", "a").replace("é", "e")
                .replace("í", "i").replace("ó", "o")
                .replace("ú", "u")
        }

        // 3. Determine Folder (Smart Logic: Check team mapping first, then league)
        // If we know the team belongs to a specific league, force that folder even if playing in CL
        val folder = when {
            // Known Turkish Teams
            finalName.equals("Besiktas JK", true) || 
            finalName.equals("Galatasaray", true) || 
            finalName.equals("Fenerbahce", true) ||
            finalName.equals("Trabzonspor", true) -> "Türkiye - Süper Lig"

            // Known Spanish Teams
            finalName.equals("Real Madrid", true) || 
            finalName.equals("FC Barcelona", true) ||
            finalName.equals("Atletico de Madrid", true) -> "Spain - LaLiga"

             // Known English Teams
            finalName.equals("Liverpool FC", true) || 
            finalName.equals("Arsenal FC", true) ||
            finalName.equals("Manchester City", true) -> "England - Premier League"

            // Known Italian Teams
            finalName.equals("Juventus FC", true) || 
            finalName.equals("AC Milan", true) ||
            finalName.equals("Inter", true) ||
            finalName.equals("Atalanta BC", true) -> "Italy - Serie A"

            // Known German Teams
            finalName.equals("Borussia Dortmund", true) ||
            finalName.equals("FC Bayern Munchen", true) -> "Germany - Bundesliga"

            // Known French Teams
            finalName.equals("Paris Saint-Germain", true) ||
            finalName.equals("AS Monaco", true) -> "France - Ligue 1"

            // Known Italian Teams
            finalName.equals("Juventus FC", true) || 
            finalName.equals("AC Milan", true) ||
            finalName.equals("Inter Milan", true) ||
            finalName.equals("Atalanta BC", true) -> "Italy - Serie A"

            // Known German Teams
            finalName.equals("Borussia Dortmund", true) ||
            finalName.equals("Bayern Munich", true) ||
            finalName.equals("Bayer 04 Leverkusen", true) -> "Germany - Bundesliga"

            // Known French Teams
            finalName.equals("Paris Saint-Germain", true) ||
            finalName.equals("AS Monaco", true) ||
            finalName.equals("Olympique Marseille", true) -> "France - Ligue 1"

            // Known Portuguese Teams
            finalName.equals("SL Benfica", true) ||
            finalName.equals("Sporting CP", true) ||
            finalName.equals("FC Porto", true) -> "Portugal - Liga Portugal"
            
            // Default League-based logic
            league.contains("Süper Lig", true) -> "Türkiye - Süper Lig"
            league.contains("La Liga", true) -> "Spain - LaLiga"
            league.contains("Premier", true) -> "England - Premier League"
            league.contains("Serie A", true) -> "Italy - Serie A"
            league.contains("Şampiyonlar", true) || league.contains("Champions", true) -> "International - UEFA Champions League"
            league.contains("Avrupa", true) || league.contains("Europa", true) -> "International - UEFA Europa League"
            league.contains("Konferans", true) || league.contains("Conference", true) -> "International - UEFA Europa Conference League"
            else -> "Türkiye - Süper Lig"
        }

        // Manually percent-encode
        fun manualEncode(s: String): String {
            return s.replace(" ", "%20")
                .replace("ü", "%C3%BC").replace("Ü", "%C3%9C")
                .replace("ö", "%C3%B6").replace("Ö", "%C3%96")
                .replace("é", "%C3%A9").replace("É", "%C3%89")
                .replace("á", "%C3%A1").replace("Á", "%C3%81")
                .replace("í", "%C3%AD").replace("Í", "%C3%8D")
        }

        val encodedFolder = manualEncode(folder)
        val encodedFile = manualEncode(finalName!!)

        return "$GITHUB_BASE/$encodedFolder/$encodedFile.png"
    }
}
