const axios = require('axios');
const cheerio = require('cheerio');

async function check() {
    const url = "https://www.macrehberi.com/spor/futbol";
    try {
        const response = await axios.get(url, {
            headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36' }
        });
        const $ = cheerio.load(response.data);
        const scripts = $('script[type="application/ld+json"]');

        scripts.each((i, el) => {
            try {
                const json = JSON.parse($(el).text());
                const items = Array.isArray(json) ? json : [json];
                items.forEach(item => {
                    if (item['@type'] === 'BroadcastEvent' && item.broadcastOfEvent) {
                        console.log("Title:", item.name);
                    }
                });
            } catch (e) { }
        });
    } catch (e) {
        console.error(e.message);
    }
}

check();
