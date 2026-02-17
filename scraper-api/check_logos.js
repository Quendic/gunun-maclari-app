const https = require('https');

const GITHUB_BASE = "https://raw.githubusercontent.com/luukhopman/football-logos/master/logos";

const teamsToCheck = [
    { league: "Italy - Serie A", name: "Juventus FC" }, // I changed this recently
    { league: "Italy - Serie A", name: "Juventus" }, // Check this too
    { league: "Italy - Serie A", name: "Inter" },
    { league: "Italy - Serie A", name: "FC Internazionale Milano" },
    { league: "Italy - Serie A", name: "AC Milan" },
    { league: "Italy - Serie A", name: "Milan" },
    { league: "Italy - Serie A", name: "AS Roma" },
    { league: "Italy - Serie A", name: "Roma" },
    { league: "Italy - Serie A", name: "SS Lazio" },
    { league: "Italy - Serie A", name: "SSC Napoli" },
    { league: "Italy - Serie A", name: "Atalanta BC" },
    { league: "England - Premier League", name: "Manchester City" },
    { league: "England - Premier League", name: "Manchester City FC" },
    { league: "England - Premier League", name: "Manchester United" },
    { league: "England - Premier League", name: "Manchester United FC" },
    { league: "England - Premier League", name: "Arsenal FC" },
    { league: "England - Premier League", name: "Arsenal" },
    { league: "England - Premier League", name: "Liverpool FC" },
    { league: "England - Premier League", name: "Liverpool" },
    { league: "Spain - LaLiga", name: "Real Madrid" },
    { league: "Spain - LaLiga", name: "Real Madrid CF" },
    { league: "Spain - LaLiga", name: "FC Barcelona" },
    { league: "Spain - LaLiga", name: "Atletico de Madrid" },
    { league: "Portugal - Liga Portugal", name: "SL Benfica" },
    { league: "Portugal - Liga Portugal", name: "Sporting CP" },
    { league: "Portugal - Liga Portugal", name: "FC Porto" },
    { league: "France - Ligue 1", name: "Paris Saint-Germain" },
    { league: "France - Ligue 1", name: "Paris Saint Germain" },
    { league: "Germany - Bundesliga", name: "FC Bayern Munchen" },
    { league: "Germany - Bundesliga", name: "Borussia Dortmund" }
];

const checkUrl = (team) => {
    return new Promise((resolve) => {
        const encodedLeague = encodeURIComponent(team.league);
        const encodedName = encodeURIComponent(team.name);
        // Note: Check manual encoding logic from Kotlin if standard encodeURIComponent differs. 
        // Kotlin manual: space -> %20. encodeURIComponent: space -> %20.
        // Kotlin: ü -> %C3%BC. encodeURIComponent: ü -> %C3%BC.
        // Seems compatible for basic chars.

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
    console.log("Checking logo existence...");
    const results = await Promise.all(teamsToCheck.map(checkUrl));

    results.forEach(r => {
        const icon = r.status === 200 ? "✅" : "❌";
        console.log(`${icon} [${r.status}] ${r.league} / ${r.team}`);
    });
})();
