const puppeteer = require('puppeteer');

(async () => {
    const browser = await puppeteer.launch({ headless: "new", args: ['--no-sandbox'] });
    const page = await browser.newPage();
    const url = "https://www.sporekrani.com/home/league/trendyol-super-lig";
    await page.goto(url, { waitUntil: 'domcontentloaded' });

    const structure = await page.evaluate(() => {
        const link = document.querySelector('a[href*="/home/match/"]');
        if (!link) return { error: "No match link found" };

        const parent = link.parentElement;
        const grandParent = parent ? parent.parentElement : null;

        // Get up to 5 previous siblings of the link or its parent
        const getSiblings = (el) => {
            let siblings = [];
            let current = el.previousElementSibling;
            let count = 0;
            while (current && count < 5) {
                siblings.push({
                    tagName: current.tagName,
                    className: current.className,
                    text: current.innerText ? current.innerText.substring(0, 50) : ""
                });
                current = current.previousElementSibling;
                count++;
            }
            return siblings;
        };

        return {
            linkTag: link.tagName,
            linkClass: link.className,
            parentTag: parent ? parent.tagName : null,
            parentClass: parent ? parent.className : null,
            siblingsOfLink: getSiblings(link),
            siblingsOfParent: parent ? getSiblings(parent) : [],
            grandParentInnerHTML: grandParent ? grandParent.innerHTML.substring(0, 500) : null
        };
    });

    console.log("Structure:", JSON.stringify(structure, null, 2));
    await browser.close();
})();
