const axios = require('axios');
const cheerio = require('cheerio');
const puppeteer = require('puppeteer');

const TARGET_LEAGUES = [
    "Süper Lig", "Premier Lig", "La Liga", "Serie A",
    "UEFA Şampiyonlar Ligi", "UEFA Avrupa Ligi", "UEFA Konferans Ligi",
    "Premier League", "Champions League", "Europa League", "Conference League"
];

const SOURCES = {
    MACREHBERI: "https://www.macrehberi.com/spor/futbol",
    LIVESOCCER: "https://www.livesoccertv.com/tr/"
};

let memoryCache = { date: null, data: null };

// Helper to get today's date in Turkish format
function getTodayInfo() {
    const now = new Date();
    const options = { timeZone: 'Europe/Istanbul' };
    const trDate = new Intl.DateTimeFormat('tr-TR', { ...options, year: 'numeric', month: '2-digit', day: '2-digit' }).format(now);
    const [d, m, y] = trDate.split('.');
    const isoToday = `${y}-${m}-${d}`;
    const months = ["Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"];
    const trDayMonth = `${parseInt(d)} ${months[now.getMonth()]}`;
    return { trDate, isoToday, trDayMonth };
}

async function scrapeMacRehberi() {
    console.log("[SOURCES] Trying MacRehberi...");
    const { isoToday } = getTodayInfo();
    try {
        const response = await axios.get(SOURCES.MACREHBERI, {
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
                                // Extract cleaner league name for exhibit
                                let displayLeague = matchedLeague;
                                const awayName = event.awayTeam ? event.awayTeam.name : "";
                                if (awayName) {
                                    const leagueRegex = new RegExp(`${awayName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s+(.*?)\\s+maçı`);
                                    const leagueMatch = fullTitle.match(leagueRegex);
                                    if (leagueMatch && leagueMatch[1]) {
                                        displayLeague = leagueMatch[1].trim();
                                    }
                                }

                                // Extract channels - Improved logic
                                let channels = "";
                                if (item.recordedAt && Array.isArray(item.recordedAt)) {
                                    // Flatten and map names from potential nested arrays
                                    channels = item.recordedAt.flat(2)
                                        .filter(c => c && c.name)
                                        .map(c => c.name.trim())
                                        .join(', ');
                                }

                                // Fallback: Extract from fullTitle if recordedAt is empty
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

async function scrapeLiveSoccerTV(browser) {
    console.log("[SOURCES] Trying LiveSoccerTV with Puppeteer...");
    const { trDayMonth } = getTodayInfo();
    try {
        const page = await browser.newPage();
        await page.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");
        await page.goto(SOURCES.LIVESOCCER, { waitUntil: 'domcontentloaded', timeout: 30000 });

        const matches = await page.evaluate((targets, todayStr) => {
            const results = [];
            let currentComp = "";
            let currentDate = "";
            const rows = Array.from(document.querySelectorAll('tr'));

            rows.forEach(row => {
                if (row.className.includes('date') || (row.cells.length === 1 && row.cells[0].className === 'date')) {
                    currentDate = row.innerText.trim();
                } else if (row.querySelector('td.competition')) {
                    currentComp = row.innerText.trim();
                } else if (row.className.includes('matchrow')) {
                    const isToday = currentDate.includes(todayStr) || currentDate.toLowerCase().includes('bugün');
                    const isWomenComp = currentComp.toLowerCase().includes('kadın') ||
                        currentComp.toLowerCase().includes('kadınlar') ||
                        currentComp.toLowerCase().includes('afc') ||
                        currentComp.toLowerCase().includes('asya');
                    const leagueMatch = !isWomenComp ? targets.find(t => currentComp.includes(t)) : null;

                    if (isToday && leagueMatch) {
                        const cells = row.cells;
                        const matchText = cells[2] ? cells[2].innerText.trim() : "";
                        const teams = matchText.split(/\s\d+\s-\s\d+\s|\svs\s|\s-\s/);
                        results.push({
                            time: cells[1] ? cells[1].innerText.trim() : "",
                            home: teams[0] ? teams[0].trim() : matchText,
                            away: teams[1] ? teams[1].trim() : "",
                            league: leagueMatch,
                            channel: cells[3] ? cells[3].innerText.trim() : "Yayın Yok",
                            isLive: row.className.includes('livematch')
                        });
                    }
                }
            });
            return results;
        }, TARGET_LEAGUES, trDayMonth);
        await page.close();
        return matches;
    } catch (e) {
        console.error("LiveSoccer Puppeteer Fail:", e.message);
        return [];
    }
}

module.exports = async (req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Content-Type', 'application/json');

    const { trDate } = getTodayInfo();
    if (memoryCache.date === trDate && memoryCache.data) {
        return res.status(200).json(memoryCache.data);
    }

    let allMatches = [];

    // 1. Try MacRehberi (Fastest)
    allMatches = await scrapeMacRehberi();

    // 2. If nothing found or to merge data, try LiveSoccer (Fallback/Backup)
    if (allMatches.length < 5) {
        let browser = null;
        try {
            browser = await puppeteer.launch({ headless: "new", args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage'] });
            const liveMatches = await scrapeLiveSoccerTV(browser);
            allMatches = [...allMatches, ...liveMatches];
        } catch (e) {
            console.error("Browser Launch Fail:", e.message);
        } finally {
            if (browser) await browser.close();
        }
    }

    // Deduplicate by home team and time
    const finalData = allMatches.filter((v, i, a) =>
        a.findIndex(t => (t.home === v.home && t.time === v.time)) === i
    );

    memoryCache = { date: trDate, data: finalData };
    res.status(200).json(finalData);
};
