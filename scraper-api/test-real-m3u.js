/**
 * Gerçek M3U Dosyası Test Script
 * Kullanıcının kendi M3U dosyasını okuyup parser'dan geçirir.
 */
const fs = require('fs');
const path = require('path');
const { parseM3UContent, deduplicateAndSort, filterByRegion } = require('./m3u-parser');

const M3U_PATH = path.join(process.env.USERPROFILE, 'Desktop', 'IPTV Editör', 'tv_channels_yemre.ellialtioglu894_plus.m3u');

console.log('═══════════════════════════════════════════════════');
console.log('  Gerçek M3U Dosyası Testi');
console.log('═══════════════════════════════════════════════════\n');

// Dosya var mı kontrol
if (!fs.existsSync(M3U_PATH)) {
    console.error(`❌ Dosya bulunamadı: ${M3U_PATH}`);
    process.exit(1);
}

const stats = fs.statSync(M3U_PATH);
console.log(`📂 Dosya: ${path.basename(M3U_PATH)}`);
console.log(`📊 Boyut: ${(stats.size / 1024 / 1024).toFixed(2)} MB`);

const startTime = Date.now();
const content = fs.readFileSync(M3U_PATH, 'utf-8');
console.log(`📄 Satır sayısı: ${content.split('\n').length}`);
console.log(`⏳ Okuma süresi: ${Date.now() - startTime}ms\n`);

// Parse et
const parseStart = Date.now();
const rawChannels = parseM3UContent(content);
const parseDuration = Date.now() - parseStart;

console.log(`🔍 Toplam bulunan spor kanalı (tüm ülkeler): ${rawChannels.length}`);
console.log(`⏱️  Parse süresi: ${parseDuration}ms\n`);

// Sadece TR kanallarını filtrele
const trChannels = filterByRegion(rawChannels, true);
console.log(`🇹🇷 Sadece TR kanalları: ${trChannels.length}`);
console.log(`🌍 Filtrelenen yabancı kanal: ${rawChannels.length - trChannels.length}\n`);

// Grupla (TR kanalları)
const channels = deduplicateAndSort(trChannels);

console.log(`📺 Benzersiz kanal sayısı: ${channels.length}`);
console.log(`📊 Toplam stream: ${rawChannels.length}\n`);

console.log('────────────────────────────────────────────────────');
console.log('  Bulunan Spor Kanalları');
console.log('────────────────────────────────────────────────────');

for (const ch of channels) {
    const qualities = ch.streams.map(s => s.quality).join(', ');
    const originals = ch.streams.map(s => s.originalName).slice(0, 3).join(' | ');
    console.log(`\n  📺 ${ch.name} (${ch.streams.length} stream)`);
    console.log(`     Kaliteler: ${qualities}`);
    console.log(`     Orijinal: ${originals}`);
}

console.log('\n═══════════════════════════════════════════════════');
console.log(`  Toplam: ${channels.length} kanal, ${rawChannels.length} stream`);
console.log('═══════════════════════════════════════════════════');
