# GitHub Repository Analyzer — Frontend

React JS frontend for the GitHub Repository Analyzer.

## Setup

```bash
npm install
npm start
```

Runs on `http://localhost:3000`. API calls are proxied to `http://localhost:8080` via the `proxy` field in `package.json`.

## Pages

- `/` — Analyze a single repository
- `/compare` — Compare two repositories side by side
- `/trends` — View historical metrics over time

## Offline Mode

Use the toggle in the header to switch to offline/demo mode. When enabled, the backend serves mock data instead of calling GitHub API.
