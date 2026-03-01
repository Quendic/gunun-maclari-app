/**
 * M3U Processing Routes
 * ---------------------
 * M3U URL'lerini alıp filtreleyen API endpoint'leri.
 */

const express = require('express');
const router = express.Router();
const { fetchAndParseM3U, deduplicateAndSort, parseM3UContent } = require('./m3u-parser');

// Kullanıcı bazlı işlem durumu (basit in-memory store)
const processingStatus = new Map();

/**
 * POST /process-m3u
 * Body: { "m3uUrl": "https://example.com/playlist.m3u" }
 * 
 * Bir M3U URL'sini alır, spor kanallarını filtreler ve JSON döndürür.
 */
router.post('/process-m3u', async (req, res) => {
    const { m3uUrl } = req.body;

    if (!m3uUrl) {
        return res.status(400).json({
            success: false,
            error: 'M3U URL gereklidir.',
            hint: 'Body\'de {"m3uUrl": "https://..."} gönderin.'
        });
    }

    // URL format kontrolü
    try {
        new URL(m3uUrl);
    } catch {
        return res.status(400).json({
            success: false,
            error: 'Geçersiz URL formatı.',
            hint: 'Lütfen http:// veya https:// ile başlayan geçerli bir URL girin.'
        });
    }

    try {
        console.log(`[M3U Route] İşlem başlatılıyor: ${m3uUrl.substring(0, 60)}...`);
        const startTime = Date.now();

        // M3U'yu indir ve parse et
        const rawChannels = await fetchAndParseM3U(m3uUrl);

        // Grupla ve sırala
        const channels = deduplicateAndSort(rawChannels);

        const duration = ((Date.now() - startTime) / 1000).toFixed(1);
        const totalStreams = channels.reduce((sum, ch) => sum + ch.streams.length, 0);

        console.log(`[M3U Route] ✅ Tamamlandı: ${channels.length} kanal, ${totalStreams} stream (${duration}s)`);

        return res.status(200).json({
            success: true,
            data: {
                channelCount: channels.length,
                totalStreams,
                processedIn: `${duration}s`,
                channels
            }
        });

    } catch (err) {
        console.error(`[M3U Route] ❌ Hata: ${err.message}`);
        return res.status(500).json({
            success: false,
            error: err.message
        });
    }
});

/**
 * POST /process-m3u-text
 * Body: { "m3uContent": "#EXTM3U\n#EXTINF:..." }
 * 
 * M3U içeriğini doğrudan metin olarak alır (URL yerine).
 * Küçük dosyalar veya test için kullanışlıdır.
 */
router.post('/process-m3u-text', async (req, res) => {
    const { m3uContent } = req.body;

    if (!m3uContent) {
        return res.status(400).json({
            success: false,
            error: 'M3U içeriği gereklidir.',
            hint: 'Body\'de {"m3uContent": "#EXTM3U\\n..."} gönderin.'
        });
    }

    try {
        const startTime = Date.now();
        const rawChannels = parseM3UContent(m3uContent);
        const channels = deduplicateAndSort(rawChannels);
        const duration = ((Date.now() - startTime) / 1000).toFixed(1);
        const totalStreams = channels.reduce((sum, ch) => sum + ch.streams.length, 0);

        return res.status(200).json({
            success: true,
            data: {
                channelCount: channels.length,
                totalStreams,
                processedIn: `${duration}s`,
                channels
            }
        });
    } catch (err) {
        return res.status(500).json({
            success: false,
            error: err.message
        });
    }
});

module.exports = router;
