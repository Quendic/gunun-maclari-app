const express = require('express');
const path = require('path');
const apiHandler = require('./api/index');
const m3uRoutes = require('./m3u-routes');
const app = express();
const port = 3000;

// ── Middleware ──
app.use(express.json({ limit: '50mb' }));  // Büyük M3U içerikleri için
app.use(express.urlencoded({ extended: true }));
app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    next();
});

// LOGO SERVING: Serve logos directory as static files
const logosPath = path.join(__dirname, '../logos');
app.use('/logos', express.static(logosPath));

// ── Routes ──
app.get('/api/matches', apiHandler);
app.use('/', m3uRoutes);  // POST /process-m3u, POST /process-m3u-text

app.listen(port, '0.0.0.0', () => {
    console.log(`-----------------------------------------------`);
    console.log(`🚀 IPTV Scraper API is running on port ${port}`);
    console.log(`📂 Serving Logos from: ${logosPath}`);
    console.log(`🔗 Match API: http://localhost:${port}/api/matches`);
    console.log(`🔗 M3U Parser: POST http://localhost:${port}/process-m3u`);
    console.log(`🔗 M3U Text:   POST http://localhost:${port}/process-m3u-text`);
    console.log(`-----------------------------------------------`);
});
