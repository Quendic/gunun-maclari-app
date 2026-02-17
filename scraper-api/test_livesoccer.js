const puppeteer = require('puppeteer');

(async () => {
    const browser = await puppeteer.launch({
        headless: "new",
        args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage']
    });
    const page = await browser.newPage();

    await page.setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36');

    console.log('Navigating to LiveSoccerTV...');
    try {
        // Use 'domcontentloaded' for faster check, and longer timeout
        await page.goto('https://www.livesoccertv.com/tr/', { waitUntil: 'domcontentloaded', timeout: 60000 });

        // Wait a few seconds for potential redirects/lazy loading
        await new Promise(r => setTimeout(r, 5000));

        const title = await page.title();
        console.log('Page Title:', title);

        if (title.includes('Just a moment')) {
            console.log('Blocked by Cloudflare challenge.');
        } else {
            console.log('Successfully reached the page!');

            const structure = await page.evaluate(() => {
                const results = [];
                const rows = Array.from(document.querySelectorAll('tr')).slice(0, 50);
                rows.forEach(row => {
                    results.push({
                        className: row.className,
                        cells: Array.from(row.querySelectorAll('td')).map(td => ({
                            className: td.className,
                            text: td.innerText.trim()
                        }))
                    });
                });
                return results;
            });
            console.log('Table Structure Sample:');
            console.log(JSON.stringify(structure, null, 2));
        }
    } catch (e) {
        console.error('Error:', e.message);
    } finally {
        await browser.close();
    }
})();
