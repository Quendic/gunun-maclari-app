const https = require('https');

const GITHUB_BASE = "https://raw.githubusercontent.com/luukhopman/football-logos/master/logos";

const teamsToCheck = [
    // Italy
    { league: "Italy - Serie A", name: "Internazionale" },
    { league: "Italy - Serie A", name: "FC Internazionale" },
    { league: "Italy - Serie A", name: "Inter Milan" },

    // Spain
    { league: "Spain - LaLiga", name: "Atletico Madrid" },
    { league: "Spain - LaLiga", name: "Atlético Madrid" },
    { league: "Spain - LaLiga", name: "Club Atlético de Madrid" },

    // Germany
    { league: "Germany - Bundesliga", name: "Bayern Munchen" },
    { league: "Germany - Bundesliga", name: "Bayern Munich" },
    { league: "Germany - Bundesliga", name: "FC Bayern München" },

    // England
    { league: "England - Premier League", name: "Chelsea FC" },
    { league: "England - Premier League", name: "Chelsea" },
];

const checkUrl = (team) => {
    return new Promise((resolve) => {
        // Simple encoding
        const encodedLeague = encodeURIComponent(team.league);
        const encodedName = team.name.replace(/ /g, '%20').replace(/ü/g, '%C3%BC').replace(/ö/g, '%C3%B6').replace(/Ü/g, '%C3%9C').replace(/Ö/g, '%C3%96');

        const url = `${GITHUB_BASE}/${encodedLeague}/${encodedName}.png`;

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
    console.log("Checking alternatives...");
    const results = await Promise.all(teamsToCheck.map(checkUrl));

    results.forEach(r => {
        const icon = r.status === 200 ? "✅" : "❌";
        console.log(`${icon} [${r.status}] ${r.league} / ${r.team}`);
    });
})();
