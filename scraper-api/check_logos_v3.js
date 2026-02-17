const https = require('https');

const GITHUB_BASE = "https://raw.githubusercontent.com/luukhopman/football-logos/master/logos";

const teamsToCheck = [
    // Spain - Try accents
    { league: "Spain - LaLiga", name: "Atlético de Madrid", encoded: "Atl%C3%A9tico%20de%20Madrid" },
    { league: "Spain - LaLiga", name: "Club Atlético de Madrid", encoded: "Club%20Atl%C3%A9tico%20de%20Madrid" },
    { league: "Spain - LaLiga", name: "Club Atlético de Madrid S.A.D.", encoded: "Club%20Atl%C3%A9tico%20de%20Madrid%20S.A.D." }, // Sometimes SAD

    // Germany
    { league: "Germany - Bundesliga", name: "Bayer 04 Leverkusen", encoded: "Bayer%2004%20Leverkusen" },
    { league: "Germany - Bundesliga", name: "Bayer Leverkusen", encoded: "Bayer%20Leverkusen" },

    // Others
    { league: "France - Ligue 1", name: "Olympique de Marseille" },
    { league: "France - Ligue 1", name: "Olympique Marseille" },
];

const checkUrl = (team) => {
    return new Promise((resolve) => {
        let url;
        if (team.encoded) {
            url = `${GITHUB_BASE}/${encodeURIComponent(team.league)}/${team.encoded}.png`;
        } else {
            url = `${GITHUB_BASE}/${encodeURIComponent(team.league)}/${encodeURIComponent(team.name)}.png`;
        }

        https.get(url, (res) => {
            resolve({
                team: team.name,
                league: team.league,
                status: res.statusCode,
                url: url
            });
        }).on('error', (e) => {
            resolve({ team: team.name, status: 'ERR', error: e.message });
        });
    });
};

(async () => {
    console.log("Checking V3...");
    const results = await Promise.all(teamsToCheck.map(checkUrl));

    results.forEach(r => {
        const icon = r.status === 200 ? "✅" : "❌";
        console.log(`${icon} [${r.status}] ${r.league} / ${r.team}`);
    });
})();
