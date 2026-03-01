/**
 * M3U Parser Engine
 * -----------------
 * Büyük M3U dosyalarını (100MB+) stream ederek okur,
 * sadece spor/maç kanallarını filtreler ve normalize eder.
 */

const https = require('https');
const http = require('http');
const { URL } = require('url');

// ── Hedef Kanal Pattern'leri ──────────────────────────────────────
const SPORT_CHANNEL_PATTERNS = [
    // beIN Sports ailesi
    /bein\s*sport/i,
    /be\s*in\s*sport/i,
    // S Sport
    /s\s*sport/i,
    /s\s*spor/i,
    // Tivibu Spor
    /tivibu\s*spor/i,
    // Smart Spor (Spor Smart)
    /smart\s*spor/i,
    /spor\s*smart/i,
    // Tabii Spor
    /tabii\s*spor/i,
];

// ── Blacklist (Spor Gibi Görünüp Olmayan Kanallar) ───────────────
const BLACKLIST_PATTERNS = [
    /sport\s*news/i,
    /sport.*magazine/i,
    /radio/i,
    /podcast/i,
    /sport.*betting/i,
    /survivor/i,          // Survivor Exxen Cup vb.
    /exxen\s*tv/i,        // Exxen TV (genel kanal, spor değil)
    /exxen(?!.*spor)/i,   // Exxen ama spor olmayan (Exxen diziler vb.)
];

/**
 * Kanal adını normalize eder
 * Örn: "TR: BEIN SPORTS 1 FHD" → "beIN Sports 1"
 */
function normalizeChannelName(name) {
    let clean = name
        // Ülke prefix'lerini kaldır
        .replace(/^(TR|TUR|TURKEY|TURKIYE|EN|UK|US|DE|FR|IT|ES|PT)\s*[:\|]\s*/i, '')
        // Kalite göstergelerini kaldır ve boşluk temizle
        .replace(/\b(UHD|4K|FHD|FULL\s*HD|1080[pi]?|720[pi]?|HD|SD|HEVC|H\.?265|H\.?264)\b/gi, '')
        .replace(/\s+/g, ' ')
        .trim();

    // ── beIN Sports normalizasyonu ──
    const beinMatch = clean.match(/be\s*in\s*sport\s*s?\s*(\d+)?/i);
    if (beinMatch) {
        const num = beinMatch[1] || '';
        return `beIN Sports ${num}`.trim();
    }

    // ── S Sport normalizasyonu ──
    const ssportMatch = clean.match(/s\s*spor\s*t?\s*(plus)?\s*(\d+)?/i);
    if (ssportMatch && !clean.match(/euro/i)) {
        const isPlus = !!ssportMatch[1] || clean.toLowerCase().includes('plus');
        const num = ssportMatch[2] || '';
        return `${isPlus ? 'S Sport Plus' : 'S Sport'} ${num}`.trim();
    }

    // ── Tivibu Spor normalizasyonu ──
    const tivMatch = clean.match(/tivibu\s*spor\s*(\d+)?/i);
    if (tivMatch) {
        const num = tivMatch[1] || '1';
        return `Tivibu Spor ${num}`;
    }

    // ── Spor Smart (Smart Spor) normalizasyonu ──
    const smartMatch = clean.match(/(smart|spor)\s*(spor|smart)\s*(\d+)?/i);
    if (smartMatch) {
        const num = smartMatch[3] || '1';
        return `Spor Smart ${num}`;
    }

    // ── Tabii Spor normalizasyonu ──
    const tabiiMatch = clean.match(/tabii\s*spor\s*(\d+)?/i);
    if (tabiiMatch) {
        const num = tabiiMatch[1] || '1';
        return `Tabii Spor ${num}`;
    }

    return clean;
}

/**
 * Kalite bilgisini çıkarır
 */
function extractQuality(name) {
    if (/UHD|4K/i.test(name)) return '4K UHD';
    if (/FHD|FULL\s*HD|1080/i.test(name)) return 'FHD';
    if (/\bHD\b|720/i.test(name)) return 'HD';
    return 'SD';
}

/**
 * Bir kanalın spor kanalı olup olmadığını kontrol eder
 */
function isSportChannel(name) {
    // Blacklist kontrolü
    if (BLACKLIST_PATTERNS.some(p => p.test(name))) return false;
    // Whitelist kontrolü
    return SPORT_CHANNEL_PATTERNS.some(p => p.test(name));
}

/**
 * Sadece Türkiye odaklı kanalları filtreler
 * Yabancı kanalları (DE:, UK:, US:, FR: vb.) çıkarır
 * @param {Array} channels - Kanal listesi
 * @param {boolean} turkishOnly - Sadece TR kanalları mı
 * @returns {Array}
 */
function filterByRegion(channels, turkishOnly = true) {
    if (!turkishOnly) return channels;

    const foreignPrefixes = /^(DE|UK|US|FR|IT|ES|PT|NL|BE|AT|CH|PL|RU|AR|BR|GR|BG|RO|HU|CZ|SE|NO|DK|FI|HR|RS|AL|MK|BA|ME|SI|SK)\s*[:\-|]/i;
    // Kesinlikle yabancı olan kanallar
    const foreignPatterns = [
        /^DE:/i, /^UK[:\-]/i, /^US[:\-]/i, /^FR:/i, /^IT\s*:/i,
    ];

    return channels.filter(ch => {
        // Orijinal isimde yabancı prefix varsa filtrele
        if (foreignPrefixes.test(ch.originalName)) return false;
        // Ama TR: prefix'i olanları koru
        if (/^TR/i.test(ch.originalName)) return true;
        // Prefix'siz olanları da koru (muhtemelen TR)
        return true;
    });
}

/**
 * M3U içeriğini parse eder ve spor kanallarını filtreler
 * @param {string} content - M3U dosya içeriği
 * @returns {Array<{name: string, originalName: string, url: string, quality: string, group: string}>}
 */
function parseM3UContent(content) {
    const lines = content.split(/\r?\n/);
    const channels = [];
    let currentName = '';
    let currentGroup = '';
    let currentOriginalName = '';

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line) continue;

        if (line.startsWith('#EXTINF')) {
            // Display name (virgülden sonraki kısım)
            const commaIdx = line.lastIndexOf(',');
            if (commaIdx !== -1) {
                currentOriginalName = line.substring(commaIdx + 1).trim();
            }

            // Group-title
            const groupMatch = line.match(/group-title="(.*?)"/);
            if (groupMatch) {
                currentGroup = groupMatch[1];
            }

            currentName = currentOriginalName;

        } else if (!line.startsWith('#') && currentName) {
            // Bu bir URL satırı
            if (isSportChannel(currentName)) {
                const quality = extractQuality(currentName);
                const normalizedName = normalizeChannelName(currentName);

                channels.push({
                    name: normalizedName,
                    originalName: currentOriginalName,
                    url: line,
                    quality,
                    group: currentGroup
                });
            }
            // Reset
            currentName = '';
            currentGroup = '';
            currentOriginalName = '';
        }
    }

    return channels;
}

/**
 * URL'den M3U dosyasını stream ederek okur
 * Büyük dosyalar (100MB+) için bellek-dostu yaklaşım
 * @param {string} m3uUrl - M3U dosyasının URL'si
 * @returns {Promise<Array>} Filtrelenmiş kanal listesi
 */
function fetchAndParseM3U(m3uUrl) {
    return new Promise((resolve, reject) => {
        const parsedUrl = new URL(m3uUrl);
        const protocol = parsedUrl.protocol === 'https:' ? https : http;

        const request = protocol.get(m3uUrl, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
            },
            timeout: 60000  // 60 saniye timeout (büyük dosyalar için)
        }, (response) => {
            // Redirect takibi
            if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
                return fetchAndParseM3U(response.headers.location).then(resolve).catch(reject);
            }

            if (response.statusCode !== 200) {
                return reject(new Error(`M3U indirme hatası: HTTP ${response.statusCode}`));
            }

            let data = '';
            let totalBytes = 0;
            const MAX_SIZE = 500 * 1024 * 1024; // 500MB limit

            response.on('data', (chunk) => {
                totalBytes += chunk.length;
                if (totalBytes > MAX_SIZE) {
                    response.destroy();
                    return reject(new Error('M3U dosyası çok büyük (500MB limit aşıldı)'));
                }
                data += chunk;
            });

            response.on('end', () => {
                try {
                    console.log(`[M3U Parser] ${(totalBytes / 1024 / 1024).toFixed(1)}MB okundu, parse ediliyor...`);
                    const channels = parseM3UContent(data);
                    console.log(`[M3U Parser] ${channels.length} spor kanalı bulundu.`);
                    resolve(channels);
                } catch (err) {
                    reject(new Error(`M3U parse hatası: ${err.message}`));
                }
            });

            response.on('error', (err) => reject(new Error(`İndirme hatası: ${err.message}`)));
        });

        request.on('error', (err) => reject(new Error(`Bağlantı hatası: ${err.message}`)));
        request.on('timeout', () => {
            request.destroy();
            reject(new Error('M3U indirme zaman aşımına uğradı (60s)'));
        });
    });
}

/**
 * Kanalları isme göre gruplar ve en iyi kaliteyi üste koyar
 * @param {Array} channels - Ham kanal listesi
 * @returns {Array} Gruplanmış ve sıralanmış kanallar
 */
function deduplicateAndSort(channels) {
    const grouped = {};

    for (const ch of channels) {
        if (!grouped[ch.name]) {
            grouped[ch.name] = [];
        }
        grouped[ch.name].push(ch);
    }

    // Her grup içinde kaliteye göre sırala ve AYNI KALİTEDEN TEKRARLARI SİL
    const qualityOrder = { '4K UHD': 0, 'FHD': 1, 'HD': 2, 'SD': 3 };

    const result = [];
    for (const [name, streams] of Object.entries(grouped)) {
        // Aynı kaliteden sadece bir tanesini tutmak için Set kullanıyoruz
        const seenQualities = new Set();

        // Önce kaliteye göre sırala
        streams.sort((a, b) => (qualityOrder[a.quality] || 9) - (qualityOrder[b.quality] || 9));

        const uniqueStreams = [];
        for (const s of streams) {
            if (!seenQualities.has(s.quality)) {
                uniqueStreams.push({
                    url: s.url,
                    quality: s.quality,
                    originalName: s.originalName,
                    group: s.group
                });
                seenQualities.add(s.quality);
            }
        }

        result.push({
            name,
            streams: uniqueStreams
        });
    }

    // Kanal adına göre sırala
    result.sort((a, b) => a.name.localeCompare(b.name, 'tr'));

    return result;
}

module.exports = {
    parseM3UContent,
    fetchAndParseM3U,
    normalizeChannelName,
    isSportChannel,
    extractQuality,
    deduplicateAndSort,
    filterByRegion
};
