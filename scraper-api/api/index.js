const axios = require('axios');
const cheerio = require('cheerio');

const TARGET_LEAGUES = [
    "Süper Lig", "Premier Lig", "La Liga", "Serie A", "Ligue 1", "Bundesliga",
    "UEFA Şampiyonlar Ligi", "UEFA Avrupa Ligi", "UEFA Konferans Ligi", "UEFA Avrupa Konferans Ligi",
    "Premier League", "Champions League", "Europa League", "Conference League"
];

const SOURCES = {
    MACREHBERI: "https://www.macrehberi.com/spor/futbol"
};

let memoryCache = { date: null, data: null };

// Helper to get today's date in Turkish format
function getTodayInfo() {
    const now = new Date();
    // Istanbul time
    const istanbulNow = new Date(now.toLocaleString('en-US', { timeZone: 'Europe/Istanbul' }));

    // If before 09:00 AM, show yesterday's matches
    const effectiveDate = new Date(istanbulNow);
    if (istanbulNow.getHours() < 9) {
        effectiveDate.setDate(effectiveDate.getDate() - 1);
    }

    const trDate = new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(effectiveDate);
    const [d, m, y] = trDate.split('.');
    const isoToday = `${y}-${m}-${d}`;

    const months = ["Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"];
    const trDayMonth = `${parseInt(d)} ${months[effectiveDate.getMonth()]}`;

    return { trDate, isoToday, trDayMonth };
}

async function scrapeMacRehberi() {
    console.log("[SOURCES] Trying MacRehberi...");
    const { isoToday } = getTodayInfo();
    try {
        // Force the date via parameter to ensure we get matches for the transition period
        const urlWithDate = `${SOURCES.MACREHBERI}?date=${isoToday}`;
        console.log(`[SOURCES] Fetching: ${urlWithDate}`);

        const response = await axios.get(urlWithDate, {
            headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36' },
            timeout: 10000
        });
        const $ = cheerio.load(response.data);
        const scripts = $('script[type="application/ld+json"]');
        const matches = [];

        scripts.each((i, el) => {
            try {
                const json = JSON.parse($(el).text());
                const items = Array.isArray(json) ? json : [json];
                items.forEach(item => {
                    if (item['@type'] === 'BroadcastEvent' && item.broadcastOfEvent) {
                        const event = item.broadcastOfEvent;
                        if (event.startDate && event.startDate.startsWith(isoToday)) {
                            const fullTitle = item.name || "";

                            // Blacklist for Women/Youth/AFC etc. (Case-insensitive check)
                            const blacklist = ['kadin', 'kadın', 'bayan', 'women', 'gençlik', 'youth', 'u19', 'u21', 'u17', 'afc', 'asya'];
                            const titleLower = fullTitle.toLowerCase();
                            const isExcluded = blacklist.some(k => titleLower.includes(k));

                            const matchedLeague = !isExcluded ? TARGET_LEAGUES.find(t => fullTitle.includes(t)) : null;

                            if (matchedLeague) {
                                // Extract cleaner league name
                                let displayLeague = matchedLeague;
                                const awayName = event.awayTeam ? event.awayTeam.name : "";
                                if (awayName) {
                                    const leagueRegex = new RegExp(`${awayName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s+(.*?)\\s+maçı`);
                                    const leagueMatch = fullTitle.match(leagueRegex);
                                    if (leagueMatch && leagueMatch[1]) {
                                        displayLeague = leagueMatch[1].trim();
                                    }
                                }

                                // Extract channels
                                let channels = "";
                                if (item.recordedAt && Array.isArray(item.recordedAt)) {
                                    channels = item.recordedAt.flat(2)
                                        .filter(c => c && c.name)
                                        .map(c => c.name.trim())
                                        .join(', ');
                                }

                                if (!channels && fullTitle.includes('kanal')) {
                                    const channelMatch = fullTitle.match(/maçı,\s(.*)\skanal/);
                                    if (channelMatch && channelMatch[1]) {
                                        channels = channelMatch[1].trim();
                                    }
                                }

                                if (!channels) channels = "Yayın Yok";

                                matches.push({
                                    time: event.startDate.split('T')[1].substring(0, 5),
                                    home: event.homeTeam ? event.homeTeam.name : "Ev Sahibi",
                                    away: event.awayTeam ? event.awayTeam.name : "Deplasman",
                                    homeLogo: event.homeTeam ? event.homeTeam.logo : null,
                                    awayLogo: event.awayTeam ? event.awayTeam.logo : null,
                                    league: displayLeague,
                                    channel: channels,
                                    isLive: item.isLiveBroadcast === true
                                });
                            }
                        }
                    }
                });
            } catch (e) { }
        });
        return matches;
    } catch (e) {
        console.error("MacRehberi Axial Fail:", e.message);
        return [];
    }
}

module.exports = async (req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Content-Type', 'application/json');

    const { trDate } = getTodayInfo();

    // Check cache
    if (memoryCache.date === trDate && memoryCache.data && memoryCache.data.length > 0) {
        return res.status(200).json(memoryCache.data);
    }

    // Single source: MacRehberi
    const allMatches = await scrapeMacRehberi();

    // Deduplicate by home team and time
    const finalData = allMatches.filter((v, i, a) =>
        a.findIndex(t => (t.home === v.home && t.time === v.time)) === i
    );

    // Update cache
    if (finalData.length > 0) {
        memoryCache = { date: trDate, data: finalData };
    }

    res.status(200).json(finalData);
};
