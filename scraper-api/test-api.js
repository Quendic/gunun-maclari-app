/**
 * API Test Script
 * Sunucu çalışırken: node test-api.js
 */
const http = require('http');

const testData = JSON.stringify({
    m3uContent: `#EXTM3U
#EXTINF:-1 group-title="Sports",TR: BEIN SPORTS 1 FHD
http://test.com/bein1
#EXTINF:-1 group-title="Sports",TR: BEIN SPORTS 1 HD
http://test.com/bein1hd
#EXTINF:-1 group-title="Sports",TR: S SPORT HD
http://test.com/ssport
#EXTINF:-1 group-title="News",CNN TURK HD
http://test.com/cnn
#EXTINF:-1 group-title="Sports",Eurosport 1 FHD
http://test.com/euro1
#EXTINF:-1 group-title="Entertainment",Show TV FHD
http://test.com/show
#EXTINF:-1 group-title="Sports",TR: A SPOR HD
http://test.com/aspor
#EXTINF:-1 group-title="Sports",TRT SPOR FHD
http://test.com/trtspor`
});

const options = {
    hostname: 'localhost',
    port: 3000,
    path: '/process-m3u-text',
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(testData)
    }
};

console.log('🔗 POST http://localhost:3000/process-m3u-text');
console.log('─────────────────────────────────────────────\n');

const req = http.request(options, (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
        const result = JSON.parse(data);
        console.log(`✅ Status: ${res.statusCode}`);
        console.log(`📊 Kanal sayısı: ${result.data.channelCount}`);
        console.log(`📊 Toplam stream: ${result.data.totalStreams}`);
        console.log(`⏱️  İşlem süresi: ${result.data.processedIn}\n`);
        console.log('📺 Bulunan kanallar:');
        for (const ch of result.data.channels) {
            const streams = ch.streams.map(s => s.quality).join(', ');
            console.log(`   ${ch.name} → ${ch.streams.length} stream (${streams})`);
        }
        console.log('\n✅ API endpoint testi başarılı!');
    });
});

req.on('error', (err) => {
    console.error(`❌ Hata: ${err.message}`);
    console.log('💡 Sunucu çalışıyor mu? → node server.js');
});

req.write(testData);
req.end();
