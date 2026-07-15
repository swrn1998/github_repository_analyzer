# Backend Code Guide & Review Cheat Sheet
### Understand every file, how it links, and how to defend it

> Read this in order. It follows the **path of a single request**, so by the end you'll
> understand how the files connect — not just what each one does in isolation.

---

## 1. The Big Picture — what happens on one request

When the browser calls `GET /api/analyze?owner=facebook&repo=react`, the request travels
through the layers like this:

```
HTTP request
   │
   ▼
AnalyzerController        ← receives HTTP, validates input
   │
   ▼
AnalyzerService (impl)    ← the "brain": decides online vs offline, orchestrates everything
   │
   ├──► CacheService          (do we already have this saved?)
   ├──► GitHubApiClient       (call GitHub REST API)
   ├──► HealthScoreCalculator (compute the 0–100 score)
   ├──► AlertEngine           (raise the 4 conditional alerts)
   ├──► Enrichers             (language %, HATEOAS links)
   └──► MockDataProvider      (demo data when offline)
   │
   ▼
AnalysisResponse          ← the JSON object returned to the browser
   │
   ▼
HTTP response (JSON)
```

**One sentence to memorize:** *"The controller handles HTTP, the service orchestrates the
business logic, and everything the service needs is injected as a separate interface."*

---

## 2. Layer-by-layer file guide

Each entry below has three parts:
- **Does** — what the file is for
- **Links** — which files it talks to
- **Say in review** — a one-liner you can say out loud to defend it

---

### LAYER 0 — Application startup

#### `GithubAnalyzerApplication.java`
- **Does:** The entry point. `main()` boots Spring Boot, which starts the embedded Tomcat server on port 8080 and creates all the beans.
- **Links:** Nothing directly — Spring auto-discovers every `@Component`, `@Service`, `@RestController` and wires them together.
- **Say in review:** *"`@SpringBootApplication` enables auto-configuration and component scanning. `@EnableScheduling` is there because my cache uses a scheduled eviction task."*

#### `config/WebClientConfig.java`
- **Does:** Creates the `WebClient` bean (Spring's HTTP client) with connect/read timeouts.
- **Links:** Injected into `SpringWebClientGitHubApiClient`.
- **Say in review:** *"Timeouts are critical for resilience — without them a slow GitHub response would hang my service forever."*

#### `config/CorsConfig.java`
- **Does:** Allows the React app (`localhost:3000`) to call the backend. Browsers block cross-origin calls by default.
- **Links:** Applies to all `/api/**` endpoints.
- **Say in review:** *"CORS is needed because the frontend runs on a different port than the backend."*

---

### LAYER 1 — Controller (the HTTP front door)

#### `controller/AnalyzerController.java`
- **Does:** Defines the REST endpoints `GET /api/analyze` and `GET /api/compare`. Extracts query params, reads the `X-Offline-Mode` header, validates input, calls the service, returns the result.
- **Links:** Calls `AnalyzerService`. Builds an `AnalysisRequest` from the params.
- **Say in review:** *"This class is intentionally thin — it only handles HTTP concerns. No business logic lives here; it delegates to the service. That's Single Responsibility."*

#### `controller/CacheController.java`
- **Does:** Endpoints to view cache stats and clear/evict cache entries (`/api/cache/...`).
- **Links:** Calls `CacheService`.
- **Say in review:** *"Operational endpoints so I can inspect and reset the cache without restarting the service."*

#### `controller/advice/GlobalExceptionHandler.java`
- **Does:** Catches exceptions thrown anywhere in a request and converts them into clean JSON error responses with the right HTTP status (404, 503, 429, 400, 500).
- **Links:** Catches the custom exceptions from the `exception/` package.
- **Say in review:** *"`@RestControllerAdvice` centralizes error handling. The user gets a clean message; the stack trace stays in the logs. No internal details leak."*

---

### LAYER 2 — The Service (the orchestrator / "brain")

#### `service/AnalyzerService.java` (interface)
- **Does:** The contract: `analyze(request)` and `compare(a, b)`. Just method signatures.
- **Links:** Implemented by `AnalyzerServiceImpl`. The controller depends on this *interface*, not the implementation.
- **Say in review:** *"Programming to an interface — this is Dependency Inversion. The controller doesn't care how analysis works, only that something fulfils this contract."*

#### `service/impl/AnalyzerServiceImpl.java`  ⭐ MOST IMPORTANT FILE
- **Does:** The heart of the app. Decides:
  - **Offline mode** → cache-first (CACHE → STALE → MOCK), never call network.
  - **Online mode** → network-first (call GitHub; on failure fall back to stale, else error).
  Then it assembles the final `AnalysisResponse` by calling the scorer, alert engine, and enrichers.
- **Links:** Uses *every* collaborator — `GitHubApiClient`, `CacheService`, `HealthScoreCalculator`, `AlertEngine`, `MockDataProvider`, the two enrichers.
- **Say in review:** *"This is the Facade — it hides the complexity of six collaborators behind one `analyze()` call. It's also where my degradation strategy lives: live → stale → error online, cache → mock offline."*
- **Learn this file line-by-line first** — it's where all the wiring becomes visible.

---

### LAYER 3 — Talking to GitHub

#### `service/github/GitHubApiClient.java` (interface)
- **Does:** Contract for fetching raw data from GitHub (repo details, contributors, languages, commits, contents).
- **Links:** Implemented by `SpringWebClientGitHubApiClient`. Used by `AnalyzerServiceImpl`.
- **Say in review:** *"An interface so I could swap the real GitHub client for a fake one in tests, or for a different HTTP library later."*

#### `service/github/impl/SpringWebClientGitHubApiClient.java`
- **Does:** The real implementation. Uses `WebClient` to make the HTTP calls, attaches the auth token, maps HTTP errors (404 → `RepoNotFoundException`, 429 → `RateLimitExceededException`, other → `GitHubApiException`).
- **Links:** Uses the `WebClient` bean from `WebClientConfig`. Returns the `dto/github/*` objects. Throws the `exception/*` types.
- **Say in review:** *"This is the only class that knows GitHub's URLs and JSON shapes. If GitHub changes, I change one file."*

---

### LAYER 4 — Caching (Option A: resilience)

#### `service/cache/CacheService.java` (interface)
- **Does:** Contract: `get` (fresh only), `getStale` (expired too), `put`, `evict`, `clear`, `getStats`.
- **Links:** Implemented by `InMemoryCacheService`. Used by `AnalyzerServiceImpl`.
- **Say in review:** *"Two read methods — `get` for fresh data, `getStale` for the failure fallback. That split is what enables 'serve stale on failure'."*

#### `service/cache/impl/InMemoryCacheService.java`
- **Does:** A thread-safe `ConcurrentHashMap` cache with TTL. Counts hits/misses. Has a scheduled task that evicts old entries, and LRU-style eviction when full.
- **Links:** Stores `CacheEntry` objects. Returns `CacheStats`.
- **Say in review:** *"No database — the requirement says none. `ConcurrentHashMap` is thread-safe. TTL keeps data fresh; the stale window lets me serve old data when GitHub is down."*

#### `service/cache/CacheEntry.java`
- **Does:** Wraps a cached `AnalysisResponse` with timestamps (`createdAt`, `expiresAt`) and an `isExpired()` check.
- **Links:** Stored inside `InMemoryCacheService`.
- **Say in review:** *"This is how I know whether a cache entry is fresh, stale, or should be evicted."*

#### `service/cache/CacheStats.java`
- **Does:** A small read-only snapshot of hits/misses/hit-rate.
- **Links:** Returned by `InMemoryCacheService.getStats()`, exposed via `CacheController`.
- **Say in review:** *"Lightweight observability — I can see if my cache is actually helping."*

---

### LAYER 5 — Health score (Strategy pattern) ⭐ design-pattern showcase

#### `service/scoring/HealthScoreCalculator.java` (interface)
- **Does:** Contract: `calculate(repoData) → HealthScore`.
- **Links:** Implemented by `WeightedHealthScoreCalculator`. Used by `AnalyzerServiceImpl`.

#### `service/scoring/impl/WeightedHealthScoreCalculator.java`
- **Does:** Adds up the four score components and maps the total to a grade.
- **Links:** Holds the 4 `ScoreComponent` beans + `ScoreGradeMapper`.
- **Say in review:** *"This is the Strategy pattern. Each scoring dimension is a separate strategy; I just sum them. To add a 5th dimension I add a class — I don't touch this one. That's Open/Closed."*

#### `service/scoring/component/ScoreComponent.java` (interface)
- **Does:** Contract for one scoring dimension: `score()`, `getMaxScore()`, `getName()`.

#### `service/scoring/component/BaseScoreComponent.java` (abstract)
- **Does:** Template Method — handles null-safety and clamping the score to [0, max]; subclasses only implement the raw calculation.
- **Say in review:** *"Template Method — the clamping logic is shared in the base class; only the actual calculation differs per dimension."*

#### The four components:
- `CommitActivityScoreComponent.java` — score from how recently the repo was committed to (the 90-day logic).
- `IssueRatioScoreComponent.java` — score from open-issue count (the 50-issue logic).
- `CommunityEngagementScoreComponent.java` — score from stars/forks/contributors (log scale).
- `DocumentationQualityScoreComponent.java` — score from README/license/description/topics.
- **Say in review:** *"Four small focused classes, each with one job. Easy to test in isolation — which is why each has its own test class."*

#### `service/scoring/ScoreGradeMapper.java`
- **Does:** Turns a number (0–100) into a letter grade (A–F).
- **Say in review:** *"Extracted so the grade thresholds live in one place and are unit-testable."*

---

### LAYER 6 — Alerts (Chain-of-Responsibility-style) ⭐ design-pattern showcase

#### `service/alert/AlertEngine.java` (interface)
- **Does:** Contract: `evaluate(repoData) → List<Alert>`.

#### `service/alert/impl/RuleBasedAlertEngine.java`
- **Does:** Runs every alert rule; collects all that apply. Multiple alerts can fire at once.
- **Links:** Holds a list of `AlertRule` beans (Spring injects all of them automatically).
- **Say in review:** *"Spring injects every `AlertRule` into a list. To add a new alert I write one class — the engine never changes. Open/Closed again."*

#### The four rules (each implements `AlertRule`):
- `InactiveRepoRule.java` — fires if no commit in 90 days.
- `HighIssueBacklogRule.java` — fires if open issues > 50.
- `NoTestInfrastructureRule.java` — fires if no test files/folders detected.
- `SecurityVulnerabilityRule.java` — fires if risky files (.env, secrets.yml) are present.
- **Say in review:** *"Each rule is self-contained: it knows when it applies and what alert to produce. These map exactly to the 4 conditional alerts in the requirement."*

---

### LAYER 7 — Enrichment & offline data

#### `service/enrichment/LanguageDistributionEnricher.java`
- **Does:** Turns GitHub's raw byte counts per language into percentages + primary language.
- **Say in review:** *"GitHub gives raw bytes; the UI wants percentages. This does that conversion."*

#### `service/enrichment/HateoasLinkEnricher.java`
- **Does:** Builds the `_links` object (self, compare, github) on every response.
- **Say in review:** *"HATEOAS — the response tells the client what it can do next. That's the requirement's 'HATEOAS principle'."*

#### `service/mock/MockDataProvider.java` (interface) + `impl/StaticMockDataProvider.java`
- **Does:** Provides demo data for offline mode / unknown repos. Always returns something valid (never null).
- **Say in review:** *"Null Object pattern — it always returns a valid response, so the rest of my code never has to null-check. Used only in offline mode now."*

---

### LAYER 8 — The data shapes

#### `domain/*` — the clean objects MY app uses
- `AnalysisRequest` — the input (owner, repo, offlineMode).
- `AnalysisResponse` — the full output JSON.
- `RepoStats`, `HealthScore`, `ScoreBreakdown`, `Alert`, `LanguageDistribution`, `ContributorActivity`, `ContributorSummary`, `ComparisonResponse` — pieces of the output.
- `enums/DataSource` (LIVE/CACHE/STALE/MOCK), `enums/AlertType`, `enums/Severity`.
- **Say in review:** *"These are my domain model — immutable objects built with the Builder pattern. They're separate from the GitHub DTOs on purpose."*

#### `dto/github/*` — the raw shapes GitHub sends
- `GitHubRepoDTO`, `GitHubContributorDTO`, `GitHubCommitDTO`, `GitHubContentDTO`.
- **Say in review:** *"I deliberately separate GitHub's JSON (DTOs) from my own domain objects. If GitHub changes its format, the blast radius is just the DTO + the client, not my whole app."*

#### `dto/internal/RepoData.java`
- **Does:** Bundles all the raw GitHub data together so I can pass one object to the scorer and alert engine instead of 5 parameters.
- **Say in review:** *"A parameter object — keeps my method signatures clean."*

#### `exception/*`
- `GitHubApiException` (→ 503), `RepoNotFoundException` (→ 404), `RateLimitExceededException` (→ 429).
- **Say in review:** *"Custom exceptions so the `GlobalExceptionHandler` can map each failure to the correct HTTP status."*

---

### LAYER 9 — Configuration files

- `resources/application.yml` — default config (port 8080, cache TTL, GitHub URL, token from env var).
- `application-dev.yml` / `application-prod.yml` — per-environment overrides (verbose logs in dev, restricted in prod).
- **Say in review:** *"Config comes from environment variables, not hardcoded — that's the 12-Factor 'config' principle. The token is never in the code."*

---

## 3. Design patterns map (memorize this table)

| Pattern | File(s) | One-line why |
|---------|---------|--------------|
| Facade | `AnalyzerServiceImpl` | Hides 6 collaborators behind one method |
| Strategy | `ScoreComponent` + 4 impls | Each scoring dimension is swappable |
| Template Method | `BaseScoreComponent` | Shared clamping, per-class calculation |
| Chain of Responsibility | `RuleBasedAlertEngine` + rules | All applicable rules fire |
| Builder | `AnalysisResponse`, `RepoStats` | Readable construction of big objects |
| Proxy (caching) | `InMemoryCacheService` | Sits in front of the GitHub call |
| Null Object | `StaticMockDataProvider` | Always returns valid data, never null |
| Dependency Inversion | every interface + impl | Depend on contracts, not concretes |

---

## 4. How to defend the "why so many files" question

If a reviewer asks *"isn't this over-engineered?"*, your honest answer:

> *"The functional core is about five classes — controller, service, GitHub client, cache,
> and the response object. The extra classes exist deliberately to satisfy the case study's
> explicit asks for SOLID, design patterns documentation, and testability. Splitting the
> scorer into four strategies and the alerts into four rules means each one is independently
> unit-tested and I can add a new rule without touching existing code. That's a conscious
> trade-off: more files, but each is small, single-purpose, and testable."*

That answer turns the size into evidence of intent, not bloat.
