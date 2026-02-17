const isVercel = process.env.VERCEL === '1';

// Conditional requirements
let chromium = null;
let puppeteer = null;

if (isVercel) {
    chromium = require('@sparticuz/chromium');
    puppeteer = require('puppeteer-core');
} else {
    puppeteer = require('puppeteer');
}

const LEAGUE_SOURCES = [
    { name: "Trendyol Süper Lig", url: "https://www.sporekrani.com/home/league/trendyol-super-lig" },
    { name: "İspanya La Liga", url: "https://www.sporekrani.com/home/league/ispanya-la-liga" },
    { name: "İngiltere Premier Lig", url: "https://www.sporekrani.com/home/league/ingiltere-premier-lig" },
    { name: "İtalya Serie A", url: "https://www.sporekrani.com/home/league/italya-serie-a" },
    { name: "UEFA Şampiyonlar Ligi", url: "https://www.sporekrani.com/home/league/uefa-sampiyonlar-ligi" },
    { name: "UEFA Avrupa Ligi", url: "https://www.sporekrani.com/home/league/uefa-avrupa-ligi" },
    { name: "UEFA Konferans Ligi", url: "https://www.sporekrani.com/home/league/uefa-avrupa-konferans-ligi" }
];

// SERVER-SIDE CACHE (In-memory)
let memoryCache = {
    date: null,
    data: null
};

module.exports = async (req, res) => {
    // CORS Headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Content-Type', 'application/json');

    // Calculate Today's Date (TR Time)
    const today = new Date().toLocaleDateString('tr-TR', { timeZone: 'Europe/Istanbul' });

    // 1. CHECK SERVER CACHE
    if (memoryCache.date === today && memoryCache.data) {
        console.log(`[CACHE HIT] Returning matches for ${today} from memory.`);
        return res.status(200).json(memoryCache.data);
    }

    console.log(`[CACHE MISS] Fetching fresh data for ${today}...`);

    let browser = null;
    try {
        const launchOptions = isVercel ? {
            args: [...chromium.args, '--no-sandbox', '--disable-setuid-sandbox'],
            defaultViewport: chromium.defaultViewport,
            executablePath: await chromium.executablePath(),
            headless: chromium.headless,
            ignoreHTTPSErrors: true,
        } : {
            headless: "new",
            args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage']
        };

        browser = await puppeteer.launch(launchOptions);

        const scrapeLeague = async (source) => {
            const page = await browser.newPage();
            // TURBO: Block images, CSS, and Fonts
            await page.setRequestInterception(true);
            page.on('request', (req) => {
                if (['image', 'stylesheet', 'font', 'media'].includes(req.resourceType())) {
                    req.abort();
                } else {
                    req.continue();
                }
            });

            await page.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            try {
                // Wait logic: 'domcontentloaded' is much faster than 'networkidle2'
                await page.goto(source.url, { waitUntil: 'domcontentloaded', timeout: 15000 });

                return await page.evaluate((leagueName) => {
                    const results = [];
                    const matchLinks = document.querySelectorAll('a[href*="/home/match/"]');

                    matchLinks.forEach(link => {
                        let tempEl = link;
                        let headerText = "";
                        let searchLimit = 0;

                        while (tempEl && searchLimit < 30) {
                            let sibling = tempEl.previousElementSibling;
                            while (sibling) {
                                const siblingText = sibling.innerText ? sibling.innerText.trim().toUpperCase() : "";
                                if (siblingText === 'BUGÜN' || siblingText === 'YARIN' || /\d{2}\.\d{2}\.\d{4}/.test(siblingText)) {
                                    headerText = siblingText;
                                    break;
                                }
                                sibling = sibling.previousElementSibling;
                            }
                            if (headerText) break;
                            tempEl = tempEl.parentElement;
                            searchLimit++;
                        }

                        // Normalize dates for comparison
                        const todayParts = new Date().toLocaleDateString('tr-TR', { timeZone: 'Europe/Istanbul' }).split('.');
                        const todayStr = `${todayParts[0].padStart(2, '0')}.${todayParts[1].padStart(2, '0')}.${todayParts[2]}`;

                        // Check if header is "BUGÜN" or contains today's date
                        if (headerText === 'BUGÜN' || headerText.includes(todayStr)) {
                            const text = link.innerText;
                            const timeMatch = text.match(/(\d{2}:\d{2})/);
                            if (timeMatch) {
                                const time = timeMatch[1];
                                const container = link.closest('.match-box') || link.parentElement;
                                const logos = container.querySelectorAll('img');
                                const channels = Array.from(logos).map(i => i.alt || i.title).filter(t => t && !t.includes('logo'));

                                results.push({
                                    time,
                                    match: text.replace(time, "").replace(/Bugün/gi, "").replace(todayStr, "").trim(),
                                    league: leagueName,
                                    channel: channels.join(', ') || "beIN Sports"
                                });
                            }
                        }
                    });
                    return results;
                }, source.name);
            } catch (e) {
                console.error(`Error scraping ${source.name}:`, e.message);
                return [];
            } finally {
                await page.close();
            }
        };

        // Execute all in parallel
        const resultsArray = await Promise.all(LEAGUE_SOURCES.map(source => scrapeLeague(source)));
        const allMatches = resultsArray.flat();

        await browser.close();

        // Deduplication and Final Formatting
        const finalData = allMatches.map(m => {
            const parts = m.match.split(' - ');

            // Clean up team names
            const cleanTeam = (name) => name ? name.split('\n')[0].replace('chevron_right', '').trim() : "";
            const cleanHome = cleanTeam(parts[0]?.trim() || m.match);
            const cleanAway = cleanTeam(parts[1]?.trim() || "");

            // Simple safer channel cleaning
            let channels = [];
            if (m.channel) {
                channels = m.channel.split(',')
                    .map(c => c.trim())
                    .filter((c, index, self) => c && c.toLowerCase() !== 'futbol' && c.toLowerCase() !== 'logo' && self.indexOf(c) === index);
            }
            const cleanedChannel = channels.join(', ') || "Yayın Yok";

            return {
                time: m.time,
                home: cleanHome,
                away: cleanAway,
                league: m.league,
                channel: cleanedChannel,
                isLive: m.match.toLowerCase().includes('canlı')
            };
        }).filter((v, i, a) => a.findIndex(t => (t.home === v.home && t.time === v.time)) === i);

        // 2. UPDATE SERVER CACHE
        memoryCache = {
            date: today,
            data: finalData
        };

        res.status(200).json(finalData);

    } catch (error) {
        if (browser) await browser.close();
        res.status(500).json({ error: error.message });
    }
};
