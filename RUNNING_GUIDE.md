# Running Guide — GitHub Repository Analyzer
### How to start, test, and use the application

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Option 1 — Run with Docker Compose (Recommended)](#option-1--run-with-docker-compose-recommended)
- [Option 2 — Run Backend Manually (Spring Boot)](#option-2--run-backend-manually-spring-boot)
- [Option 3 — Run Frontend Manually (React)](#option-3--run-frontend-manually-react)
- [Option 4 — Run Both Manually Together](#option-4--run-both-manually-together)
- [Environment Setup](#environment-setup)
- [Verify Everything is Running](#verify-everything-is-running)
- [API curl Examples](#api-curl-examples)
  - [Analyze a Repository](#analyze-a-repository)
  - [Offline Mode](#offline-mode)
  - [Compare Two Repositories](#compare-two-repositories)
  - [Cache Management](#cache-management)
  - [Health & Info Endpoints](#health--info-endpoints)
- [Postman Collection](#postman-collection)
- [Common Errors & Fixes](#common-errors--fixes)

---

## Prerequisites

Install these before starting:

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 17+ | https://adoptium.net |
| Maven | 3.9+ | https://maven.apache.org/download.cgi |
| Node.js | 20+ | https://nodejs.org |
| npm | 10+ | Comes with Node.js |
| Docker Desktop | 24+ | https://www.docker.com/products/docker-desktop |
| Docker Compose | 2+ | Comes with Docker Desktop |
| Git | Any | https://git-scm.com |

**Verify your installs:**
```bash
java -version
mvn -version
node -version
npm -version
docker -version
docker-compose -version
```

---

## Project Structure

```
github-repository-analyzer/
├── backend/          ← Spring Boot (Java 17) — runs on port 8080
├── frontend/         ← React + TypeScript    — runs on port 3000
├── docker-compose.yml
├── .env.example
└── RUNNING_GUIDE.md  ← this file
```

---

## Option 1 — Run with Docker Compose (Recommended)

This starts both backend and frontend together with one command.

### Step 1 — Clone and navigate

```bash
git clone https://github.com/your-org/github-repository-analyzer.git
cd github-repository-analyzer
```

### Step 2 — Create your environment file

```bash
# Windows CMD
copy .env.example .env

# Mac / Linux
cp .env.example .env
```

Open `.env` and set your GitHub token:
```
GITHUB_API_TOKEN=ghp_your_actual_token_here
```

> **Get a GitHub token:** Go to https://github.com/settings/tokens → Generate new token (classic) → select `public_repo` scope only → copy the token.
>
> Without a token you get 60 API calls/hour. With a token: 5000/hour.

### Step 3 — Build and start

```bash
docker-compose up --build
```

First run takes 3–5 minutes (downloads dependencies, builds images).

### Step 4 — Open the app

| Service | URL |
|---------|-----|
| Frontend (React UI) | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health Check | http://localhost:8080/actuator/health |

### Stop the application

```bash
# Stop (keeps containers)
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop, remove containers AND images
docker-compose down --rmi all
```

### Restart without rebuilding

```bash
docker-compose up
```

---

## Option 2 — Run Backend Manually (Spring Boot)

Use this when you want to develop or debug the backend without Docker.

### Step 1 — Set environment variable

**Windows CMD:**
```cmd
set GITHUB_API_TOKEN=ghp_your_actual_token_here
```

**Windows PowerShell:**
```powershell
$env:GITHUB_API_TOKEN = "ghp_your_actual_token_here"
```

**Mac / Linux:**
```bash
export GITHUB_API_TOKEN=ghp_your_actual_token_here
```

### Step 2 — Build and run

```bash
cd backend

# Build (skip tests for speed)
mvn clean package -DskipTests

# Run with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Or run the JAR directly:**
```bash
java -jar target/github-analyzer-backend-1.0.0.jar --spring.profiles.active=dev
```

### Step 3 — Verify backend is up

```bash
curl http://localhost:8080/actuator/health
```
Expected: `{"status":"UP"}`

### Run backend tests

```bash
cd backend

# Unit tests only
mvn test

# Unit + integration tests
mvn verify -P integration-tests

# With coverage report (opens at target/site/jacoco/index.html)
mvn verify jacoco:report
```

---

## Option 3 — Run Frontend Manually (React)

Use this when developing or debugging the UI without Docker.

### Step 1 — Install dependencies

```bash
cd frontend
npm install
```

### Step 2 — Configure the backend URL

Create a `.env.local` file in the `frontend/` folder:
```
REACT_APP_API_BASE_URL=http://localhost:8080
```

### Step 3 — Start the dev server

```bash
npm start
```

The browser opens automatically at http://localhost:3000

Hot reload is enabled — any file change refreshes the browser instantly.

### Run frontend tests

```bash
cd frontend

# Run all tests once (CI mode)
npm test -- --watchAll=false

# Run with coverage report
npm test -- --watchAll=false --coverage

# Watch mode (re-runs on file change)
npm test
```

### Build for production

```bash
npm run build
# Output is in frontend/build/
```

---

## Option 4 — Run Both Manually Together

Open **two terminals**:

**Terminal 1 — Backend:**
```bash
cd backend
set GITHUB_API_TOKEN=ghp_your_token   # Windows CMD
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Terminal 2 — Frontend:**
```bash
cd frontend
npm install
npm start
```

Then open http://localhost:3000 in your browser.

---

## Environment Setup

Full list of environment variables with defaults:

```bash
# ── GitHub API ─────────────────────────────────────────────────────────────
GITHUB_API_TOKEN=                    # Your PAT (empty = 60 req/hr unauthenticated)

# ── Spring Boot ────────────────────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=dev           # dev | prod
GITHUB_API_CONNECT_TIMEOUT_MS=3000   # Connection timeout in ms
GITHUB_API_READ_TIMEOUT_MS=10000     # Read timeout in ms

# ── Cache ──────────────────────────────────────────────────────────────────
CACHE_TTL_MINUTES=10                 # How long a fresh cache entry lives
CACHE_STALE_TTL_MINUTES=60           # How long a stale entry is kept as fallback
CACHE_MAX_ENTRIES=500                # Max entries before LRU eviction

# ── CORS ───────────────────────────────────────────────────────────────────
CORS_ALLOWED_ORIGINS=http://localhost:3000

# ── React ──────────────────────────────────────────────────────────────────
REACT_APP_API_BASE_URL=http://localhost:8080
```

---

## Verify Everything is Running

Run these checks in order to confirm both services are healthy:

```bash
# 1. Backend health check
curl http://localhost:8080/actuator/health

# 2. Backend app info
curl http://localhost:8080/actuator/info

# 3. Quick API smoke test
curl "http://localhost:8080/api/analyze?owner=facebook&repo=react"

# 4. Frontend (should return HTML)
curl http://localhost:3000
```

---

## API curl Examples

All examples assume the backend is running at `http://localhost:8080`.

---

### Analyze a Repository

#### Basic analysis — facebook/react

```bash
curl -X GET "http://localhost:8080/api/analyze?owner=facebook&repo=react" \
  -H "Accept: application/json"
```

#### Analysis — microsoft/vscode

```bash
curl -X GET "http://localhost:8080/api/analyze?owner=microsoft&repo=vscode" \
  -H "Accept: application/json"
```

#### Analysis — torvalds/linux

```bash
curl -X GET "http://localhost:8080/api/analyze?owner=torvalds&repo=linux" \
  -H "Accept: application/json"
```

#### Pretty-print the JSON response (requires jq)

```bash
curl -s "http://localhost:8080/api/analyze?owner=facebook&repo=react" | jq .
```

#### Get only the health score from the response

```bash
curl -s "http://localhost:8080/api/analyze?owner=facebook&repo=react" | jq '.healthScore'
```

#### Get only the alerts

```bash
curl -s "http://localhost:8080/api/analyze?owner=facebook&repo=react" | jq '.alerts'
```

#### Get only the language distribution

```bash
curl -s "http://localhost:8080/api/analyze?owner=facebook&repo=react" | jq '.languages.percentages'
```

**Sample Response:**
```json
{
  "owner": "facebook",
  "repo": "react",
  "stats": {
    "stars": 220000,
    "forks": 45000,
    "watchers": 6700,
    "openIssues": 854,
    "defaultBranch": "main",
    "description": "The library for web and native user interfaces",
    "license": "MIT"
  },
  "healthScore": {
    "score": 88,
    "grade": "B",
    "breakdown": {
      "commitActivityScore": 25,
      "issueRatioScore": 18,
      "communityScore": 25,
      "documentationScore": 20
    }
  },
  "alerts": [
    {
      "type": "HIGH_ISSUE_BACKLOG",
      "message": "High issue backlog - Maintenance concern",
      "severity": "WARNING"
    }
  ],
  "languages": {
    "primaryLanguage": "JavaScript",
    "percentages": {
      "JavaScript": 82.5,
      "TypeScript": 12.3,
      "CSS": 5.2
    }
  },
  "contributorActivity": {
    "totalContributors": 1623,
    "totalCommitsLast52Weeks": 892,
    "lastCommitDate": "2026-06-14T18:30:00Z",
    "topContributors": [
      { "login": "gaearon", "totalCommits": 2145, "weeksActive": 48 }
    ]
  },
  "source": "LIVE",
  "cachedAt": null,
  "_links": {
    "self":    { "href": "/api/analyze?owner=facebook&repo=react" },
    "compare": { "href": "/api/compare?ownerA=facebook&repoA=react" },
    "github":  { "href": "https://github.com/facebook/react" },
    "cache-stats": { "href": "/api/cache/stats" }
  }
}
```

---

### Offline Mode

Enable offline mode to get cached or demo data without calling the GitHub API.

#### Analyze with offline mode ON

```bash
curl -X GET "http://localhost:8080/api/analyze?owner=facebook&repo=react" \
  -H "X-Offline-Mode: true" \
  -H "Accept: application/json"
```

The response will have `"source": "CACHE"` or `"source": "MOCK"`.

#### Check what source was returned

```bash
curl -s "http://localhost:8080/api/analyze?owner=facebook&repo=react" \
  -H "X-Offline-Mode: true" | jq '.source'
```

#### Pre-populated offline mock repos (always have demo data)

```bash
# facebook/react
curl -s "http://localhost:8080/api/analyze?owner=facebook&repo=react" \
  -H "X-Offline-Mode: true" | jq '{source: .source, score: .healthScore.score}'

# microsoft/vscode
curl -s "http://localhost:8080/api/analyze?owner=microsoft&repo=vscode" \
  -H "X-Offline-Mode: true" | jq '{source: .source, score: .healthScore.score}'

# torvalds/linux
curl -s "http://localhost:8080/api/analyze?owner=torvalds&repo=linux" \
  -H "X-Offline-Mode: true" | jq '{source: .source, score: .healthScore.score}'
```

---

### Compare Two Repositories

#### Compare facebook/react vs vuejs/vue

```bash
curl -X GET "http://localhost:8080/api/compare?ownerA=facebook&repoA=react&ownerB=vuejs&repoB=vue" \
  -H "Accept: application/json"
```

#### Compare microsoft/vscode vs microsoft/typescript

```bash
curl -X GET "http://localhost:8080/api/compare?ownerA=microsoft&repoA=vscode&ownerB=microsoft&repoB=TypeScript" \
  -H "Accept: application/json"
```

#### Compare with offline mode

```bash
curl -X GET "http://localhost:8080/api/compare?ownerA=facebook&repoA=react&ownerB=vuejs&repoB=vue" \
  -H "X-Offline-Mode: true" \
  -H "Accept: application/json"
```

#### Get just the winner and summary

```bash
curl -s "http://localhost:8080/api/compare?ownerA=facebook&repoA=react&ownerB=vuejs&repoB=vue" \
  | jq '{winner: .winner, delta: .healthScoreDelta, summary: .summary}'
```

**Sample Response:**
```json
{
  "repoA": { "owner": "facebook", "repo": "react", ... },
  "repoB": { "owner": "vuejs",    "repo": "vue",   ... },
  "winner": "facebook/react",
  "healthScoreDelta": 12,
  "summary": "facebook/react is healthier by 12 points"
}
```

---

### Cache Management

#### View current cache statistics

```bash
curl -X GET "http://localhost:8080/api/cache/stats" \
  -H "Accept: application/json"
```

**Sample Response:**
```json
{
  "totalEntries": 4,
  "hits": 12,
  "misses": 4,
  "staleHits": 1,
  "hitRate": 0.75
}
```

#### Evict a specific repository from cache

```bash
# Evict facebook/react
curl -X DELETE "http://localhost:8080/api/cache/facebook/react"

# Evict microsoft/vscode
curl -X DELETE "http://localhost:8080/api/cache/microsoft/vscode"
```

Expected response: `204 No Content`

#### Clear the entire cache

```bash
curl -X DELETE "http://localhost:8080/api/cache"
```

Expected response: `204 No Content`

#### Demonstrate cache HIT — call twice, second is from cache

```bash
# First call — LIVE from GitHub API
curl -s "http://localhost:8080/api/analyze?owner=facebook&repo=react" | jq '.source'
# Output: "LIVE"

# Second call within 10 minutes — from cache
curl -s "http://localhost:8080/api/analyze?owner=facebook&repo=react" | jq '.source'
# Output: "CACHE"
```

---

### Health & Info Endpoints

#### Service health check

```bash
curl http://localhost:8080/actuator/health
```
```json
{ "status": "UP" }
```

#### Application info

```bash
curl http://localhost:8080/actuator/info
```
```json
{
  "app": {
    "name": "GitHub Repository Analyzer",
    "version": "1.0.0",
    "description": "Analyzes GitHub repositories with health scoring and alerts"
  }
}
```

#### Swagger UI (browser only)

Open in browser: http://localhost:8080/swagger-ui.html

This gives you a full interactive API explorer to try all endpoints without curl.

---

### Validation Error Examples

These show the error responses when you send bad input:

#### Missing owner parameter → 400

```bash
curl -s "http://localhost:8080/api/analyze?repo=react" | jq .
```
```json
{
  "timestamp": "2026-06-15T10:00:00Z",
  "status": 400,
  "code": "MISSING_PARAMETER",
  "message": "Required parameter 'owner' is missing",
  "path": "/api/analyze"
}
```

#### Invalid characters in owner → 400

```bash
curl -s "http://localhost:8080/api/analyze?owner=invalid%20owner&repo=react" | jq .
```
```json
{
  "timestamp": "2026-06-15T10:00:00Z",
  "status": 400,
  "code": "INVALID_INPUT",
  "message": "analyze.owner: Must be alphanumeric with hyphens/underscores/dots, max 100 chars",
  "path": "/api/analyze"
}
```

#### Repository not found → 404

```bash
curl -s "http://localhost:8080/api/analyze?owner=facebook&repo=this-does-not-exist" | jq .
```
```json
{
  "timestamp": "2026-06-15T10:00:00Z",
  "status": 404,
  "code": "REPO_NOT_FOUND",
  "message": "Repository not found: facebook/this-does-not-exist",
  "path": "/api/analyze"
}
```

---

## Postman Collection

Import the following as a **Postman Collection** (create a new collection → Add requests manually):

| Request Name | Method | URL | Headers |
|---|---|---|---|
| Analyze facebook/react | GET | `http://localhost:8080/api/analyze?owner=facebook&repo=react` | — |
| Analyze microsoft/vscode | GET | `http://localhost:8080/api/analyze?owner=microsoft&repo=vscode` | — |
| Analyze offline mode | GET | `http://localhost:8080/api/analyze?owner=facebook&repo=react` | `X-Offline-Mode: true` |
| Compare react vs vue | GET | `http://localhost:8080/api/compare?ownerA=facebook&repoA=react&ownerB=vuejs&repoB=vue` | — |
| Cache stats | GET | `http://localhost:8080/api/cache/stats` | — |
| Clear cache | DELETE | `http://localhost:8080/api/cache` | — |
| Evict one repo | DELETE | `http://localhost:8080/api/cache/facebook/react` | — |
| Health check | GET | `http://localhost:8080/actuator/health` | — |
| App info | GET | `http://localhost:8080/actuator/info` | — |
| Swagger UI | GET | `http://localhost:8080/swagger-ui.html` | — |
| Missing param error | GET | `http://localhost:8080/api/analyze?repo=react` | — |
| Invalid input error | GET | `http://localhost:8080/api/analyze?owner=bad owner&repo=react` | — |
| Repo not found error | GET | `http://localhost:8080/api/analyze?owner=facebook&repo=fake-nonexistent-repo-xyz` | — |

---

## Common Errors & Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `Port 8080 already in use` | Another process is on 8080 | Run `netstat -aon \| findstr :8080` (Windows) or `lsof -i :8080` (Mac/Linux) to find and kill the process |
| `Port 3000 already in use` | Another process is on 3000 | Kill the process or change `REACT_APP_PORT=3001` in `.env` |
| `401 Unauthorized from GitHub` | Token is wrong or expired | Generate a new PAT at https://github.com/settings/tokens and update `.env` |
| `403 Forbidden from GitHub` | Token lacks `public_repo` scope | Re-generate token with `public_repo` scope |
| `60 req/hr rate limit hit` | No token configured | Add `GITHUB_API_TOKEN` to `.env` |
| `Cannot connect to Docker` | Docker Desktop not running | Open Docker Desktop and wait for it to start |
| `mvn: command not found` | Maven not installed or not on PATH | Install Maven and add to PATH |
| `node: command not found` | Node.js not installed | Install from https://nodejs.org |
| `JAVA_HOME not set` | Java not configured | Set `JAVA_HOME` to your JDK folder |
| `Source: MOCK in response` | GitHub API unreachable or offline mode ON | Check network / toggle off offline mode in UI |
| Frontend shows blank page | Backend not running | Start backend first, then frontend |
| CORS error in browser console | Frontend URL not allowed | Check `CORS_ALLOWED_ORIGINS` in `.env` matches your frontend URL |

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────┐
│              QUICK COMMANDS CHEAT SHEET                     │
├─────────────────────────────────────────────────────────────┤
│ START (Docker)    docker-compose up --build                 │
│ STOP  (Docker)    docker-compose down                       │
│                                                             │
│ START Backend     mvn spring-boot:run (in /backend)         │
│ START Frontend    npm start           (in /frontend)        │
│                                                             │
│ TEST  Backend     mvn test            (in /backend)         │
│ TEST  Frontend    npm test -- --watchAll=false               │
│                                                             │
│ URLS                                                        │
│   UI         →   http://localhost:3000                      │
│   API        →   http://localhost:8080/api/analyze          │
│   Swagger    →   http://localhost:8080/swagger-ui.html      │
│   Health     →   http://localhost:8080/actuator/health      │
└─────────────────────────────────────────────────────────────┘
```
