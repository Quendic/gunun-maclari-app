module.exports = {
    apps: [{
        name: "iptv-scraper-api",
        script: "./server.js",
        watch: false,
        env: {
            NODE_ENV: "production",
            PUPPETEER_EXECUTABLE_PATH: "", // Leave empty to use bundled Chrome
        },
        // Automatic restart on failure
        autorestart: true,
        max_memory_restart: '1G',
        // Delay between restarts
        exp_backoff_restart_delay: 100
    }]
};
