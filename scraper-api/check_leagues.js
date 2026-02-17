const puppeteer = require('puppeteer');

(async () => {
    const browser = await puppeteer.launch({ headless: "new", args: ['--no-sandbox'] });
    const page = await browser.newPage();

    // Check multiple potential URLs
    const urls = [
        "https://www.sporekrani.com/home/league/uefa-sampiyonlar-ligi",
        "https://www.sporekrani.com/home/league/sampiyonlar-ligi",
        "https://www.sporekrani.com/home/league/uefa-avrupa-ligi"
    ];

    for (const url of urls) {
        try {
            console.log(`Checking ${url}...`);
            await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 10000 });

            // Just check if page loaded successfully (title or content)
            const title = await page.title();
            const hasMatches = await page.evaluate(() => document.querySelectorAll('a[href*="/home/match/"]').length);

            console.log(`URL: ${url}`);
            console.log(`Title: ${title}`);
            console.log(`Matches found: ${hasMatches}`);

            // Check headers too
            const debugInfo = await page.evaluate(() => {
                const headers = Array.from(document.querySelectorAll('div.text-center.text-primary')).map(e => e.innerText);
                return { headers: headers.slice(0, 5) };
            });
            console.log("Headers:", debugInfo.headers);
            console.log('---');
        } catch (e) {
            console.log(`Error checking ${url}: ${e.message}`);
        }
    }

    await browser.close();
})();
