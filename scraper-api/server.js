const express = require('express');
const path = require('path');
const apiHandler = require('./api/index');
const app = express();
const port = 3000;

// LOGO SERVING: Serve logos directory as static files
app.use('/logos', express.static('c:/Users/Yunus Emre/Desktop/IPTV-Mac-Projesi/logos'));

app.get('/api/matches', apiHandler);

app.listen(port, () => {
    console.log(`-----------------------------------------------`);
    console.log(`🚀 IPTV Scraper API is running on port ${port}`);
    console.log(`📂 Serving Logos from: c:/Users/Yunus Emre/Desktop/IPTV-Mac-Projesi/logos`);
    console.log(`🔗 Match API: http://localhost:${port}/api/matches`);
    console.log(`-----------------------------------------------`);
});
