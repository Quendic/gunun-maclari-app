/**
 * M3U Parser Test Script
 * ----------------------
 * Örnek M3U verileriyle parser'ı test eder.
 * Çalıştır: node test-parser.js
 */

const { parseM3UContent, deduplicateAndSort, normalizeChannelName, isSportChannel, extractQuality } = require('./m3u-parser');

// ── Örnek M3U İçeriği ──────────────────────────────────────────────
const SAMPLE_M3U = `#EXTM3U
#EXTINF:-1 tvg-id="bein1.tr" tvg-name="beIN Sports 1" tvg-logo="https://example.com/logo.png" group-title="Sports",TR: BEIN SPORTS 1 FHD
http://stream1.example.com/bein1/index.m3u8
#EXTINF:-1 tvg-id="bein1.tr" tvg-name="beIN Sports 1 HD" tvg-logo="https://example.com/logo.png" group-title="Sports",TR: BEIN SPORTS 1 HD
http://stream2.example.com/bein1hd/index.m3u8
#EXTINF:-1 tvg-id="bein2.tr" tvg-name="beIN Sports 2" group-title="Sports",TR: beIN SPORTS 2 FHD
http://stream1.example.com/bein2/index.m3u8
#EXTINF:-1 tvg-id="bein3.tr" tvg-name="beIN Sports 3" group-title="Sports",BEIN SPORTS 3
http://stream1.example.com/bein3/index.m3u8
#EXTINF:-1 tvg-id="ssport.tr" tvg-name="S Sport" group-title="Sports",TR: S SPORT FHD
http://stream1.example.com/ssport/index.m3u8
#EXTINF:-1 tvg-id="ssport2.tr" tvg-name="S Sport 2" group-title="Sports",S SPOR 2 HD
http://stream1.example.com/ssport2/index.m3u8
#EXTINF:-1 tvg-id="trt1.tr" tvg-name="TRT 1" group-title="Turkish",TR: TRT 1 HD
http://stream1.example.com/trt1/index.m3u8
#EXTINF:-1 tvg-id="trtspr.tr" tvg-name="TRT SPOR" group-title="Sports",TRT SPOR FHD
http://stream1.example.com/trtspor/index.m3u8
#EXTINF:-1 tvg-id="atv.tr" tvg-name="ATV" group-title="Turkish",TR: ATV FHD
http://stream1.example.com/atv/index.m3u8
#EXTINF:-1 tvg-id="aspor.tr" tvg-name="A Spor" group-title="Sports",TR: A SPOR HD
http://stream1.example.com/aspor/index.m3u8
#EXTINF:-1 tvg-id="exxen.tr" tvg-name="Exxen Spor 1" group-title="Sports",Exxen Spor 1 FHD
http://stream1.example.com/exxen1/index.m3u8
#EXTINF:-1 tvg-id="euro1.tr" tvg-name="Eurosport 1" group-title="Sports",Eurosport 1 HD
http://stream1.example.com/euro1/index.m3u8
#EXTINF:-1 tvg-id="tv8.tr" tvg-name="TV8" group-title="Turkish",TR: TV 8 HD
http://stream1.example.com/tv8/index.m3u8
#EXTINF:-1 tvg-id="tv85.tr" tvg-name="TV8,5" group-title="Turkish",TV8,5 FHD
http://stream1.example.com/tv85/index.m3u8
#EXTINF:-1 tvg-id="cnn.tr" tvg-name="CNN Turk" group-title="News",TR: CNN TURK HD
http://stream1.example.com/cnn/index.m3u8
#EXTINF:-1 tvg-id="show.tr" tvg-name="Show TV" group-title="Entertainment",TR: SHOW TV FHD
http://stream1.example.com/show/index.m3u8
#EXTINF:-1 tvg-id="star.tr" tvg-name="Star TV" group-title="Entertainment",Star TV HD
http://stream1.example.com/star/index.m3u8
#EXTINF:-1 tvg-id="fox.tr" tvg-name="FOX TV" group-title="Entertainment",FOX TV FHD
http://stream1.example.com/fox/index.m3u8
#EXTINF:-1 tvg-id="kanal7.tr" tvg-name="Kanal 7" group-title="Entertainment",Kanal 7 HD
http://stream1.example.com/kanal7/index.m3u8
#EXTINF:-1 tvg-id="kanal.d" tvg-name="Kanal D" group-title="Entertainment",TR: KANAL D HD
http://stream1.example.com/kanald/index.m3u8
#EXTINF:-1 tvg-id="bein4.tr" tvg-name="beIN Sports 4" group-title="Sports",TR: BEIN SPORTS 4 FHD
http://stream1.example.com/bein4/index.m3u8
#EXTINF:-1 tvg-id="bein5.tr" tvg-name="beIN Sports 5" group-title="Sports",TURKIYE: BEIN SPORTS 5 4K
http://stream1.example.com/bein5/index.m3u8
#EXTINF:-1 tvg-id="cbcsport.az" tvg-name="CBC Sport" group-title="Sports",CBC Sport HD
http://stream1.example.com/cbcsport/index.m3u8
`;

// ── Test Runner ────────────────────────────────────────────────────
console.log('═══════════════════════════════════════════════════');
console.log('  M3U Parser Test Suite');
console.log('═══════════════════════════════════════════════════\n');

// Test 1: isSportChannel
console.log('📋 Test 1: Spor Kanalı Filtreleme');
console.log('─────────────────────────────────');
const testNames = [
    ['TR: BEIN SPORTS 1 FHD', true],
    ['S SPOR 2 HD', true],
    ['TRT SPOR FHD', true],
    ['TR: A SPOR HD', true],
    ['CNN TURK HD', false],
    ['Show TV FHD', false],
    ['Fox TV', false],
    ['Eurosport 1 HD', true],
    ['Exxen Spor 1 FHD', true],
    ['TR: ATV FHD', true],
    ['TR: TV 8 HD', true],
    ['TV8,5 FHD', true],
    ['CBC Sport HD', true],
    ['Kanal D HD', false],
];

let passed = 0;
let failed = 0;
for (const [name, expected] of testNames) {
    const result = isSportChannel(name);
    const status = result === expected ? '✅' : '❌';
    if (result !== expected) {
        console.log(`  ${status} "${name}" → ${result} (beklenen: ${expected})`);
        failed++;
    } else {
        passed++;
    }
}
console.log(`  Sonuç: ${passed}/${passed + failed} başarılı\n`);

// Test 2: Kanal Adı Normalizasyonu
console.log('📋 Test 2: Kanal Adı Normalizasyonu');
console.log('────────────────────────────────────');
const normalizeTests = [
    ['TR: BEIN SPORTS 1 FHD', 'beIN Sports 1'],
    ['TR: beIN SPORTS 2 FHD', 'beIN Sports 2'],
    ['BEIN SPORTS 3', 'beIN Sports 3'],
    ['TR: S SPORT FHD', 'S Sport'],
    ['S SPOR 2 HD', 'S Sport 2'],
    ['TRT SPOR FHD', 'TRT Spor'],
    ['TR: A SPOR HD', 'A Spor'],
    ['Eurosport 1 HD', 'Eurosport 1'],
    ['TR: TRT 1 HD', 'TRT 1'],
    ['TR: ATV FHD', 'ATV'],
    ['TV8,5 FHD', 'TV8,5'],
    ['CBC Sport HD', 'CBC Sport'],
    ['TURKIYE: BEIN SPORTS 5 4K', 'beIN Sports 5'],
];

passed = 0;
failed = 0;
for (const [input, expected] of normalizeTests) {
    const result = normalizeChannelName(input);
    const status = result === expected ? '✅' : '❌';
    if (result !== expected) {
        console.log(`  ${status} "${input}" → "${result}" (beklenen: "${expected}")`);
        failed++;
    } else {
        passed++;
    }
}
console.log(`  Sonuç: ${passed}/${passed + failed} başarılı\n`);

// Test 3: Kalite Çıkarma
console.log('📋 Test 3: Kalite Çıkarma');
console.log('──────────────────────────');
const qualityTests = [
    ['TR: BEIN SPORTS 1 FHD', 'FHD'],
    ['S SPOR 2 HD', 'HD'],
    ['TURKIYE: BEIN SPORTS 5 4K', '4K UHD'],
    ['BEIN SPORTS 3', 'SD'],
];

passed = 0;
failed = 0;
for (const [input, expected] of qualityTests) {
    const result = extractQuality(input);
    const status = result === expected ? '✅' : '❌';
    if (result !== expected) {
        console.log(`  ${status} "${input}" → "${result}" (beklenen: "${expected}")`);
        failed++;
    } else {
        passed++;
    }
}
console.log(`  Sonuç: ${passed}/${passed + failed} başarılı\n`);

// Test 4: Tam Parse
console.log('📋 Test 4: Tam M3U Parse');
console.log('─────────────────────────');
const rawChannels = parseM3UContent(SAMPLE_M3U);
console.log(`  Toplam satır: ${SAMPLE_M3U.split('\n').length}`);
console.log(`  Bulunan spor kanalı: ${rawChannels.length}`);

// Filtresi çalışıyor mu kontrol et
const nonSportChannels = rawChannels.filter(ch =>
    ['CNN', 'Show', 'Star', 'Fox', 'Kanal'].some(n => ch.originalName.includes(n))
);
console.log(`  Filtrelenen non-sport: ${nonSportChannels.length === 0 ? '✅ Doğru (0)' : '❌ Hata (' + nonSportChannels.length + ')'}`);

// Test 5: Grupla ve Sırala
console.log('\n📋 Test 5: Gruplama ve Sıralama');
console.log('─────────────────────────────────');
const grouped = deduplicateAndSort(rawChannels);
console.log(`  Benzersiz kanal: ${grouped.length}`);
console.log('  Kanal listesi:');
for (const ch of grouped) {
    const streams = ch.streams.map(s => s.quality).join(', ');
    console.log(`    📺 ${ch.name} → ${ch.streams.length} stream (${streams})`);
}

console.log('\n═══════════════════════════════════════════════════');
console.log('  Tüm testler tamamlandı!');
console.log('═══════════════════════════════════════════════════');
