# GitHub Repository Analyzer

> A production-ready full-stack microservice that analyzes GitHub repositories and returns enriched metrics — health scores, contributor activity, language distribution, security indicators, and conditional alerts.

[![CI/CD Pipeline](https://github.com/your-org/github-repository-analyzer/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/your-org/github-repository-analyzer/actions)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue)](https://react.dev/)
[![Docker](https://img.shields.io/badge/Docker-ready-blue)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Design Approach](#design-approach)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Quick Start](#quick-start)
- [Environment Setup](#environment-setup)
- [Running Locally](#running-locally)
- [API Reference](#api-reference)
- [Offline Mode](#offline-mode)
- [Health Score Algorithm](#health-score-algorithm)
- [Conditional Alerts](#conditional-alerts)
- [Design Patterns](#design-patterns)
- [CI/CD Pipeline](#cicd-pipeline)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Configuration Reference](#configuration-reference)

---

## Overview

The GitHub Repository Analyzer accepts a GitHub `owner/repository` as input, fetches data from the GitHub REST API, and returns enriched analytics including:

- Repository statistics (stars, forks, watchers, open issues)
- Computed **health score** (0–100) with letter grade
- **Contributor activity** and commit patterns
- **Language distribution** with percentages
- **Conditional alerts** for inactive repos, high issue backlogs, missing tests, and security vulnerabilities
- **Offline mode** with in-memory cache and mock data fallback

### Why Option A — Resilience & Offline Mode

This implementation follows **Option A** of the architectural choices for these reasons:

1. GitHub's unauthenticated API rate limit is 60 req/hr — caching is **required**, not optional
2. The problem statement explicitly mandates offline mode and cached/mock fallback
3. A TTL-based in-memory cache satisfies the "no DB required" constraint perfectly
4. Serving stale data with a clear UI indicator is a better UX than an error screen

Elements from Option B (structured logging, health endpoints) and Option C (health score computation, trust boundaries) are included as supporting concerns.

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     React Frontend (Port 3000)               │
│  SearchBar → Dashboard → HealthScore | Alerts | Charts       │
└──────────────────────────┬───────────────────────────────────┘
                           │ HTTP REST (JSON)
                           ▼
┌──────────────────────────────────────────────────────────────┐
│              Spring Boot Microservice (Port 8080)            │
│                                                              │
│  AnalyzerController                                          │
│       └── AnalyzerService (Facade)                           │
│             ├── GitHubApiClient (WebClient)                  │
│             ├── InMemoryCacheService (TTL + Stale)           │
│             ├── HealthScoreCalculator (Strategy)             │
│             ├── AlertEngine (Chain of Responsibility)        │
│             └── MockDataProvider (Offline fallback)          │
└──────────────────────────┬───────────────────────────────────┘
                           │ HTTPS
                           ▼
                  GitHub REST API (api.github.com)
```

See [architecture/01_HLD_System_Overview.md](docs/architecture/01_HLD_System_Overview.md) for full diagrams.

---

## Design Approach

### Backend Design Principles

**SOLID** — Every class has one reason to change. The `AnalyzerService` orchestrates; the `HealthScoreCalculator` scores; the `AlertEngine` evaluates rules. New alert types or scoring dimensions are added by creating new classes, not modifying existing ones.

**12-Factor App** — Config from environment variables, logs to stdout, stateless processes, dev/prod parity via Docker.

**HATEOAS** — Every API response includes `_links` for related endpoints, making the API self-discoverable.

**Resilience First** — Three degradation tiers: LIVE → CACHE → STALE → MOCK. The service never returns a 500 when it can return useful data.

### Frontend Design Principles

- **Context API** over Redux — the state surface is small (offline toggle + current analysis)
- **Custom Hooks** encapsulate API logic; components stay pure
- **TypeScript throughout** — response shapes are typed to match the backend OpenAPI contract
- **Parallel API calls** for repository comparison (`Promise.all`)

---

## Tech Stack

| Layer | Technology | Version | Reason |
|-------|-----------|---------|--------|
| Frontend | React | 18.x | Component model, hooks, Context API |
| Frontend | TypeScript | 5.x | Type safety against API contract |
| Frontend | Axios | 1.x | HTTP client with interceptors |
| Frontend | Recharts | 2.x | Lightweight React-native charts |
| Frontend | React Router | 6.x | SPA routing |
| Backend | Java | 17 | LTS, records, sealed classes |
| Backend | Spring Boot | 3.2 | Auto-config, Actuator, WebClient |
| Backend | Spring WebFlux (WebClient) | 6.x | Non-blocking GitHub API calls |
| Build | Maven | 3.9 | Dependency management |
| Build | Node.js / npm | 20.x | Frontend build |
| Testing | JUnit 5 | 5.x | Backend unit/integration tests |
| Testing | Mockito | 5.x | Mocking in unit tests |
| Testing | MockWebServer | 4.x | Mock GitHub API in tests |
| Testing | Jest + RTL | 29.x | Frontend component tests |
| CI/CD | GitHub Actions | — | Primary pipeline |
| CI/CD | Jenkins | 2.x | Enterprise pipeline (Jenkinsfile) |
| Container | Docker | 24.x | Multi-stage builds |
| Container | Docker Compose | 2.x | Local orchestration |

---

## Features

| Feature | Status | Details |
|---------|--------|---------|
| Repository statistics | ✅ | Stars, forks, watchers, open issues |
| Health score | ✅ | 0–100 composite score, A–F grade |
| Contributor activity | ✅ | Top contributors, 52-week commit chart |
| Language distribution | ✅ | Pie chart with percentages |
| Conditional alerts | ✅ | 4 rule-based alerts |
| Offline mode | ✅ | Toggle in UI, header-driven on backend |
| In-memory TTL cache | ✅ | 10 min TTL, stale fallback |
| Repository comparison | ✅ | Side-by-side with winner highlight |
| HATEOAS links | ✅ | `_links` on every response |
| OpenAPI/Swagger UI | ✅ | Available at `/swagger-ui.html` |
| Health endpoints | ✅ | `/actuator/health`, `/actuator/info` |
| Docker containerization | ✅ | Multi-stage Dockerfile for each service |
| CI/CD pipeline | ✅ | GitHub Actions + Jenkinsfile |

---

## Quick Start

### Prerequisites

- Java 17+
- Node.js 20+
- Maven 3.9+
- Docker & Docker Compose (for containerized run)
- A GitHub Personal Access Token (PAT) — [create one here](https://github.com/settings/tokens)

### Fastest path — Docker Compose

```bash
# 1. Clone the repo
git clone https://github.com/your-org/github-repository-analyzer.git
cd github-repository-analyzer

# 2. Set your GitHub token
cp .env.example .env
# Edit .env and set GITHUB_API_TOKEN=your_token_here

# 3. Start everything
docker-compose up --build

# 4. Open the app
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

---

## Environment Setup

Copy `.env.example` to `.env` and configure:

```bash
# ── Backend ─────────────────────────────────────────────────
# GitHub Personal Access Token (required for >60 req/hr limit)
GITHUB_API_TOKEN=ghp_your_token_here

# Spring profile: dev | prod
SPRING_PROFILES_ACTIVE=dev

# Cache configuration
CACHE_TTL_MINUTES=10
CACHE_STALE_TTL_MINUTES=60
CACHE_MAX_ENTRIES=500

# GitHub API timeouts (milliseconds)
GITHUB_API_CONNECT_TIMEOUT_MS=3000
GITHUB_API_READ_TIMEOUT_MS=10000

# ── Frontend ─────────────────────────────────────────────────
REACT_APP_API_BASE_URL=http://localhost:8080
```

> **Security note:** Never commit `.env` to version control. The `.gitignore` already excludes it. In CI/CD, use GitHub Secrets or Jenkins Credentials.

---

## Running Locally

### Option 1 — Docker Compose (recommended)

```bash
docker-compose up --build
```

This starts both services, wires them together on an internal network, and applies health checks.

### Option 2 — Run services separately

**Backend:**
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# Available at: http://localhost:8080
```

**Frontend:**
```bash
cd frontend
npm install
npm start
# Available at: http://localhost:3000
```

### Verify the backend is running

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

curl "http://localhost:8080/api/analyze?owner=facebook&repo=react"
# Expected: Full AnalysisResponse JSON
```

---

## API Reference

Base URL: `http://localhost:8080`

### GET /api/analyze

Analyze a GitHub repository.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `owner` | string | yes | GitHub username or organization |
| `repo` | string | yes | Repository name |

**Headers:**

| Header | Type | Default | Description |
|--------|------|---------|-------------|
| `X-Offline-Mode` | boolean | false | Return cached/mock data without calling GitHub |

**Example Request:**
```bash
curl "http://localhost:8080/api/analyze?owner=microsoft&repo=vscode"
```

**Example Response (200 OK):**
```json
{
  "owner": "microsoft",
  "repo": "vscode",
  "stats": {
    "stars": 158000,
    "forks": 27800,
    "watchers": 3100,
    "openIssues": 8200,
    "defaultBranch": "main",
    "description": "Visual Studio Code",
    "license": "MIT",
    "createdAt": "2015-09-03T15:35:00Z",
    "updatedAt": "2026-06-14T20:00:00Z"
  },
  "healthScore": {
    "score": 82,
    "grade": "B",
    "breakdown": {
      "commitActivityScore": 25,
      "issueRatioScore": 12,
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
    "primaryLanguage": "TypeScript",
    "percentages": { "TypeScript": 91.2, "JavaScript": 6.8, "CSS": 2.0 },
    "rawBytes": { "TypeScript": 89400000, "JavaScript": 6700000, "CSS": 1900000 }
  },
  "contributorActivity": {
    "totalContributors": 1947,
    "totalCommitsLast52Weeks": 2134,
    "lastCommitDate": "2026-06-14T18:00:00Z",
    "topContributors": [
      { "login": "bpasero", "totalCommits": 3241, "weeksActive": 52 }
    ]
  },
  "source": "LIVE",
  "cachedAt": null,
  "_links": {
    "self": { "href": "/api/analyze?owner=microsoft&repo=vscode" },
    "compare": { "href": "/api/compare?ownerA=microsoft&repoA=vscode" },
    "github": { "href": "https://github.com/microsoft/vscode" },
    "cache-stats": { "href": "/api/cache/stats" }
  }
}
```

**Error Responses:**

| Status | Code | When |
|--------|------|------|
| 400 | `INVALID_INPUT` | owner or repo is blank/invalid format |
| 404 | `REPO_NOT_FOUND` | Repository does not exist on GitHub |
| 503 | — | GitHub API down — response still returned with `source: STALE` or `MOCK` |

---

### GET /api/compare

Compare two repositories side by side.

```bash
curl "http://localhost:8080/api/compare?ownerA=facebook&repoA=react&ownerB=vuejs&repoB=vue"
```

---

### GET /api/cache/stats

View current cache statistics.

```json
{
  "totalEntries": 12,
  "hits": 87,
  "misses": 23,
  "staleHits": 4,
  "hitRate": 0.79
}
```

---

### GET /actuator/health

```json
{ "status": "UP" }
```

---

### Swagger UI

Full interactive API documentation available at:
```
http://localhost:8080/swagger-ui.html
```

---

## Offline Mode

Offline mode allows the application to function when the GitHub API is unavailable.

### How to enable

**Via UI:** Toggle the "Offline Mode" switch in the top navigation bar.

**Via API header:**
```bash
curl -H "X-Offline-Mode: true" "http://localhost:8080/api/analyze?owner=facebook&repo=react"
```

### Data source priority

```
Request with X-Offline-Mode: true
  └── Check cache → HIT  → return with source: "CACHE"
  └── Check cache → MISS → return mock data with source: "MOCK"

Request with X-Offline-Mode: false (normal)
  └── Check cache → HIT  (fresh)  → return with source: "CACHE"
  └── Check cache → MISS → Call GitHub API
       └── SUCCESS → cache + return with source: "LIVE"
       └── FAIL    → Check stale cache
            └── STALE FOUND → return with source: "STALE" + warning
            └── NO STALE    → return mock data with source: "MOCK"
```

---

## Health Score Algorithm

The health score is a weighted composite of four dimensions, each scored 0–25 (total: 0–100).

| Dimension | Max | Scoring Logic |
|-----------|-----|---------------|
| Commit Activity | 25 | Full score if commit in last 7 days. Proportional decay up to 90 days. Zero if no commit in 90+ days. |
| Issue Ratio | 25 | Full score if open issues < 10. Scaled down as issues grow. Zero if open issues > 200. |
| Community Engagement | 25 | Log-scaled combination of stars + forks + watchers. Prevents "winner takes all". |
| Documentation Quality | 25 | +7 for README, +7 for LICENSE, +6 for description, +5 for topics/tags. |

**Grade mapping:**

| Score | Grade |
|-------|-------|
| 90–100 | A |
| 75–89 | B |
| 60–74 | C |
| 40–59 | D |
| 0–39 | F |

### Trust Boundaries

Health scores are heuristic, not authoritative:
- Stars can be gamed
- Issue count reflects team culture, not just bugs
- "No tests" detection is file-path heuristic — not proof of no tests
- Data freshness depends on cache TTL — `source` field tells you how fresh

---

## Conditional Alerts

| Condition | Message | Severity |
|-----------|---------|----------|
| No commits in 90 days | "Repository appears inactive" | WARNING |
| Open issues > 50 | "High issue backlog - Maintenance concern" | WARNING |
| No test files/folders detected | "Testing infrastructure not found" | WARNING |
| Vulnerable dependency keywords found | "Security vulnerabilities detected - Review dependencies" | CRITICAL |

Multiple alerts can fire simultaneously.

---

## Design Patterns

| Pattern | Where |
|---------|-------|
| Facade | `AnalyzerService` hides 4 GitHub API calls behind one method |
| Strategy | `ScoreComponent` — each scoring dimension is interchangeable |
| Chain of Responsibility | `AlertEngine` evaluates rules independently |
| Builder | `AnalysisResponse` construction |
| Proxy (Cache) | `InMemoryCacheService` transparently wraps GitHub API calls |
| Null Object | `MockDataProvider` returns valid empty-ish data instead of null |
| HATEOAS | `_links` on every response |
| Observer | React Context propagates offline state to all consumers |

Full pattern explanations in [architecture/09_Design_Patterns_Used.md](docs/architecture/09_Design_Patterns_Used.md).

---

## CI/CD Pipeline

```
push to main
  │
  ├── [Backend] Build (Maven) → Test (JUnit) → SpotBugs analysis
  ├── [Frontend] Install → Build → Test (Jest) → ESLint
  │
  ├── Docker Build (multi-stage, backend + frontend)
  ├── Docker Push to registry (main branch only)
  └── Deploy via docker-compose
```

Pipeline files:
- GitHub Actions: `.github/workflows/ci-cd.yml`
- Jenkins: `Jenkinsfile`

---

## Project Structure

```
github-repository-analyzer/
├── backend/          Spring Boot microservice (Java 17)
├── frontend/         React application (TypeScript)
├── docs/             Architecture artifacts
├── .github/          GitHub Actions workflows
├── Jenkinsfile       Jenkins pipeline
├── docker-compose.yml
└── README.md
```

---

## Testing

### Backend

```bash
cd backend

# Unit tests
mvn test

# Integration tests
mvn verify -P integration-tests

# With coverage report
mvn verify jacoco:report
# Report: backend/target/site/jacoco/index.html
```

### Frontend

```bash
cd frontend

# Run all tests once
npm test -- --watchAll=false

# With coverage
npm test -- --watchAll=false --coverage
```

---

## Configuration Reference

### Backend (`application.yml`)

| Key | Default | Description |
|-----|---------|-------------|
| `github.api.token` | — | PAT for authenticated API calls |
| `github.api.base-url` | `https://api.github.com` | GitHub API base URL |
| `github.api.connect-timeout-ms` | `3000` | Connection timeout |
| `github.api.read-timeout-ms` | `10000` | Read timeout |
| `cache.ttl-minutes` | `10` | Cache entry TTL |
| `cache.stale-ttl-minutes` | `60` | How long stale entries are retained |
| `cache.max-entries` | `500` | Maximum cache size |

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m 'feat: add my feature'`
4. Push the branch: `git push origin feature/my-feature`
5. Open a Pull Request

---

## License

MIT License — see [LICENSE](LICENSE) for details.
