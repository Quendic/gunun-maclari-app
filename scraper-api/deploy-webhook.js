const http = require('http');
const crypto = require('crypto');
const { execSync } = require('child_process');

const WEBHOOK_SECRET = 'iptv-deploy-secret-2026';
const PORT = 9000;
const REPO_DIR = '/root/iptv-repo';

function verifySignature(payload, signature) {
    if (!signature) return false;
    const hmac = crypto.createHmac('sha256', WEBHOOK_SECRET);
    hmac.update(payload);
    const digest = 'sha256=' + hmac.digest('hex');
    return crypto.timingSafeEqual(Buffer.from(digest), Buffer.from(signature));
}

const server = http.createServer((req, res) => {
    if (req.method === 'POST' && req.url === '/deploy') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            const signature = req.headers['x-hub-signature-256'];

            if (!verifySignature(body, signature)) {
                console.log(`[${new Date().toISOString()}] Invalid signature - rejected`);
                res.writeHead(403);
                res.end('Forbidden');
                return;
            }

            console.log(`[${new Date().toISOString()}] Valid webhook received - deploying...`);

            try {
                // Git pull
                const pullOutput = execSync('git pull origin master', {
                    cwd: REPO_DIR,
                    encoding: 'utf-8',
                    timeout: 30000
                });
                console.log('[GIT PULL]', pullOutput.trim());

                // npm install (in case package.json changed)
                const npmOutput = execSync('cd scraper-api && npm install --production', {
                    cwd: REPO_DIR,
                    encoding: 'utf-8',
                    timeout: 60000
                });
                console.log('[NPM INSTALL]', npmOutput.trim());

                // Restart PM2
                const pm2Output = execSync('pm2 restart iptv-scraper-api', {
                    encoding: 'utf-8',
                    timeout: 15000
                });
                console.log('[PM2 RESTART]', pm2Output.trim());

                res.writeHead(200);
                res.end('Deploy successful');
                console.log(`[${new Date().toISOString()}] Deploy completed successfully!`);
            } catch (err) {
                console.error(`[${new Date().toISOString()}] Deploy failed:`, err.message);
                res.writeHead(500);
                res.end('Deploy failed: ' + err.message);
            }
        });
    } else if (req.method === 'GET' && req.url === '/health') {
        res.writeHead(200);
        res.end('OK');
    } else {
        res.writeHead(404);
        res.end('Not found');
    }
});

server.listen(PORT, () => {
    console.log(`[${new Date().toISOString()}] Deploy webhook listening on port ${PORT}`);
});
