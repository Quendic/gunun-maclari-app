# Scrape Live Football Matches from BeIN Sports

## Goal
Scrape live football match data (Time, Home Team, Away Team, Score/Status) from `https://beinsports.com.tr/canli-skor` and display it in the Android application.

## Strategy
1.  **Source**: Use the official BeIN Sports Live Score page.
2.  **Tooling**: Use Android `WebView` with `evaluateJavascript`.
3.  **Parsing Logic**:
    - Target `TR` elements in the DOM.
    - Extract text content.
    - Validate rows by checking for time patterns (`HH:mm`).
    - Parse columns (`TD`s) to extract Teams and Scores.
    - Handle edge cases: Live scores (e.g., `(0-0)`), future matches, Postponed/Finished statuses.
4.  **Fallback**: If parsing fails, dump the page structure to logs (`BEIN_DEBUG`) for analysis.

## Execution Steps
1.  **Initialize WebView**: Set `userAgent` to a modern desktop browser to avoid mobile redirects or simplified views.
2.  **Load URL**: `https://beinsports.com.tr/canli-skor`.
3.  **Inject JS**:
    - Iterate all `tr` elements.
    - Filter for rows containing time (`\d{2}:\d{2}`).
    - Extract cell text.
    - Identify "Center" element (Score or Status).
    - Map `Home Team` (Left of center) and `Away Team` (Right of center).
4.  **Data Handling**:
    - Return JSON array to Android.
    - Map JSON to `Match` objects.
    - Update UI.

## Troubleshooting
- **Empty List**: Check `BEIN_DEBUG` logs. If the page structure has changed, update the JS parser logic.
- **Wrong Teams**: Ensure the "Score/Status" logic correctly identifies the middle element to separate teams.
