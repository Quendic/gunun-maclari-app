const puppeteer = require('puppeteer');

(async () => {
    const browser = await puppeteer.launch({
        headless: "new",
        args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage']
    });
    const page = await browser.newPage();
    await page.setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36');

    console.log('Navigating to Mac Rehberi...');
    try {
        await page.goto('https://www.macrehberi.com/spor/futbol', { waitUntil: 'domcontentloaded', timeout: 60000 });

        const data = await page.evaluate(() => {
            const scripts = Array.from(document.querySelectorAll('script[type="application/ld+json"]'));
            const matches = [];

            scripts.forEach(script => {
                try {
                    const json = JSON.parse(script.innerText);
                    // Check if it's a list or a single object
                    const items = Array.isArray(json) ? json : [json];

                    items.forEach(item => {
                        if (item['@type'] === 'BroadcastEvent' || item['@type'] === 'SportsEvent' || (item.broadcastOfEvent)) {
                            matches.push(item);
                        }
                    });
                } catch (e) { }
            });

            return matches.slice(0, 10); // Return first 10 for analysis
        });

        console.log('JSON-LD Matches Sample:');
        console.log(JSON.stringify(data, null, 2));

    } catch (e) {
        console.error('Error:', e.message);
    } finally {
        await browser.close();
    }
})();
