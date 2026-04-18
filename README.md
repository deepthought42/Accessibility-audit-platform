<!--
  Meta keywords for search engines & GitHub topics:
  WCAG 2.2, WCAG 2.1, accessibility audit, a11y, ADA compliance, Section 508,
  automated accessibility testing, color contrast checker, alt text audit,
  screen reader testing, keyboard navigation, axe alternative, Pa11y alternative,
  Lighthouse accessibility, Angular, Spring Boot, Neo4j, headless browser,
  Selenium, WCAG scanner, inclusive design, EN 301 549
-->

<p align="center">
  <a href="https://look-see.com">
    <img src="Look-see-UI-v3/src/assets/Wordmark_Red.png" alt="Look-see logo" width="220" />
  </a>
</p>

<h1 align="center">Look-see — Automated WCAG 2.2 Accessibility Audit Platform</h1>

<p align="center">
  <strong>Find, fix, and prove every accessibility issue on your website — from color contrast to keyboard journeys — with one open-source platform.</strong>
</p>

<p align="center">
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/license-MIT-blue.svg" /></a>
  <img alt="WCAG 2.2 AA" src="https://img.shields.io/badge/WCAG-2.2%20AA-success" />
  <img alt="ADA & Section 508" src="https://img.shields.io/badge/compliance-ADA%20%7C%20Section%20508%20%7C%20EN%20301%20549-informational" />
  <img alt="Java" src="https://img.shields.io/badge/Java-17%20%7C%2021-orange?logo=openjdk&logoColor=white" />
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-2.6-6DB33F?logo=springboot&logoColor=white" />
  <img alt="Angular" src="https://img.shields.io/badge/Angular-17-DD0031?logo=angular&logoColor=white" />
  <img alt="Neo4j" src="https://img.shields.io/badge/Neo4j-graph-008CC1?logo=neo4j&logoColor=white" />
  <img alt="GCP" src="https://img.shields.io/badge/Cloud-GCP%20Cloud%20Run-4285F4?logo=googlecloud&logoColor=white" />
  <img alt="PRs welcome" src="https://img.shields.io/badge/PRs-welcome-brightgreen" />
</p>

<p align="center">
  <img src="Look-see-UI-v3/src/assets/Banner_1.png" alt="Look-see — stay on trend, stay on brand, stay ahead. Accessibility score dashboard screenshot." />
  <!-- TODO(design): replace with a bespoke a11y-superhero hero image pairing the Look-see logo with WCAG category icons. -->
</p>

<p align="center">
  <a href="#quick-start">Quick start</a> ·
  <a href="#what-look-see-audits">What it audits</a> ·
  <a href="#architecture">Architecture</a> ·
  <a href="#deploy-to-your-own-cloud">Deploy</a> ·
  <a href="#roadmap">Roadmap</a> ·
  <a href="https://look-see.com">look-see.com</a>
</p>

---

## What is Look-see?

**Look-see is an open-source accessibility audit platform** that continuously crawls your website, drives it in a real headless browser, and grades every page against **WCAG 2.2 / 2.1 AA**, **ADA**, **Section 508**, and **EN 301 549**. Unlike one-page scanners, Look-see follows real user journeys — clicking, navigating, and validating end-to-end flows — so the issues it reports are the ones your users actually hit.

It ships as a monorepo of small services: a crawler API, a page builder, four specialized audit workers (content, visual design, information architecture, journeys), an Angular UI, a Chrome extension, and a VS Code extension for real-time feedback in your editor.

A hosted commercial version is coming to [**look-see.com**](https://look-see.com) — self-host in the meantime with the instructions below.

## Why teams choose Look-see

- 🎯 **Comprehensive WCAG 2.2 coverage** — not just a quick scan. Covers perceivable, operable, understandable, and robust success criteria across every page and journey.
- 🧭 **Journey-aware, not page-aware** — drives multi-step flows (login, checkout, search) in a real browser and catches issues only visible mid-flow.
- 🎨 **Dedicated visual-design audits** — color contrast (AA/AAA), typography scale, imagery quality, whitespace, and brand consistency.
- ✍️ **Content audits** — alt text quality, readability grade, paragraph structure, and plain-language checks powered by Google Cloud NLP.
- 🏗️ **Information architecture audits** — heading hierarchy, landmarks, table semantics, form labels, link text, and metadata.
- ⚡ **Live audit progress** — see scores update in real time via WebSocket as workers finish.
- 🧩 **Meet developers where they work** — Chrome extension, VS Code extension, and an Angular dashboard.
- 🔓 **MIT-licensed and self-hostable** — your data never leaves your cloud. One `terraform apply` on GCP.
- 🧠 **Graph-native** — built on Neo4j so relationships between pages, elements, journeys, and issues are first-class.

## What Look-see audits

Every page is scored across four WCAG-aligned categories:

| Category | WCAG success criteria covered | Example checks |
|---|---|---|
| **Visual Design** | 1.4.1 Use of Color · 1.4.3 Contrast (Minimum) · 1.4.6 Contrast (Enhanced) · 1.4.11 Non-text Contrast · 1.4.12 Text Spacing | Color contrast (AA & AAA), typography scale, image quality, whitespace, imagery contrast |
| **Content** | 1.1.1 Non-text Content · 3.1.5 Reading Level · 1.3.1 Info & Relationships | Alt text presence & quality, readability grade, paragraph structure, plain-language |
| **Information Architecture** | 1.3.1 Info & Relationships · 2.4.6 Headings & Labels · 3.3.2 Labels or Instructions · 4.1.2 Name/Role/Value | Heading hierarchy, table semantics, form labels, link text, landmarks, page metadata |
| **Journeys** | 2.1.1 Keyboard · 2.4.3 Focus Order · 2.4.7 Focus Visible · 3.2.2 On Input | Keyboard reachability, focus order, interactive element discovery, multi-step flow validation |

## Who it's for

- **Accessibility leads & a11y consultants** who need repeatable, auditable WCAG reports across large sites.
- **Product & engineering teams** shipping under ADA, Section 508, or European Accessibility Act deadlines.
- **Agencies** running periodic audits for clients, with white-labelable reports.
- **Open-source maintainers** who want a Pa11y / axe-core alternative that handles full user journeys, not just static pages.

## Quick Start

### Prerequisites

- **Java 17+** (Eclipse Temurin recommended; `CrawlerAPI` requires Java 21)
- **Maven 3.9+**
- **Node.js 18.19+** and **npm 10+** (for the Angular UI)
- **Neo4j** 5.x (local Docker is fine)
- **Docker** (optional, for containerized runs)
- **Google Cloud SDK** (optional, only for GCP integrations like Vision/NLP)

### 1. Clone and build the shared core

All Java services depend on the `LookseeCore` library — build it once, locally.

```bash
git clone https://github.com/deepthought42/Accessibility-audit-platform.git
cd Accessibility-audit-platform
(cd LookseeCore && mvn clean install -DskipTests)
```

### 2. Run a service

```bash
cd AuditManager                       # or any other service directory
mvn clean package -DskipTests
java -ea -jar target/*.jar            # -ea enables Design-by-Contract assertions
```

### 3. Run the web UI

```bash
cd Look-see-UI-v3
npm install
ng serve
# open http://localhost:4200
```

### 4. Run anything in Docker

Every backend service ships a `Dockerfile`:

```bash
cd <service-directory>
docker build -t looksee/<service-name> .
docker run -p 8080:8080 looksee/<service-name>
```

## Architecture

Look-see is a pub/sub-driven monorepo. A single **CrawlerAPI** fronts the platform; work flows asynchronously through Google Cloud Pub/Sub topics to specialized workers, then back to the UI over a Pusher WebSocket.

```
  Browser ──HTTP──▶ CrawlerAPI ──▶ (url) ──▶ PageBuilder ──▶ (page_created)
                                                                  │
                                                                  ▼
                                                            AuditManager
                                                                  │
                                            ┌─────────────────────┼─────────────────────┐
                                            ▼                     ▼                     ▼
                                      contentAudit         visualDesignAudit   informationArchitectureAudit
                                            │                     │                     │
                                            └───────────▶ (audit_update) ◀──────────────┘
                                                                  │
                                                                  ▼
                                              audit-service ──Pusher──▶ Look-see UI
```

**Shared across every Java service: [LookseeCore](LookseeCore/)** (`A11yCore` Maven artifact) — Neo4j models, Spring Data repositories, Selenium WebDriver automation, GCP integrations (Storage, Vision, NLP, Pub/Sub), and Pusher broadcasting.

> For the full pub/sub topic list, message payloads, and journey-pipeline diagram, see the expanded [Architecture details](#architecture-details) below.

## Packages

Each top-level directory is a service or library. Click through for service-specific docs.

### Backend services (Spring Boot)

| Service | Role | Java |
|---|---|---|
| [`CrawlerAPI`](CrawlerAPI/) | Public REST API — web crawling, domains, users, billing | 21 |
| [`PageBuilder`](PageBuilder/) | Builds page state models from crawled URLs with headless Chrome | 17 |
| [`AuditManager`](AuditManager/) | Orchestrates page audit lifecycle; routes pub/sub events | 17 |
| [`contentAudit`](contentAudit/) | Alt text, readability, paragraph structure | 17 |
| [`visualDesignAudit`](visualDesignAudit/) | Color contrast, typography, imagery, whitespace | 17 |
| [`informationArchitectureAudit`](informationArchitectureAudit/) | Headings, tables, forms, links, metadata | 17 |
| [`audit-service`](audit-service/) | Processes audit progress and broadcasts live updates | 17 |
| [`look-see-front-end-broadcaster`](look-see-front-end-broadcaster/) | Pushes `page_created` and `audit_update` events to the UI | 17 |
| [`element-enrichment`](element-enrichment/) | Enriches elements with visual and semantic metadata | 17 |
| [`journeyExecutor`](journeyExecutor/) | Executes and validates user journey paths | 17 |
| [`journeyExpander`](journeyExpander/) | Expands verified journeys by discovering interactive elements | 17 |
| [`journey-map-cleanup`](journey-map-cleanup/) | Cleans stale journey candidates in domain maps | 17 |
| [`journeyErrors`](journeyErrors/) | Dead-letter handler for failed journey candidates | 17 |

### Libraries, UIs, and extensions

| Package | Role |
|---|---|
| [`LookseeCore`](LookseeCore/) | Shared core library — models, persistence, browser, GCP, messaging |
| [`Look-see-UI-v3`](Look-see-UI-v3/) | Angular 17 web dashboard |
| [`LookseeChromeExtension`](LookseeChromeExtension/) | Chrome extension for in-page accessibility issue detection |
| [`look-see-VSCode-plugin`](look-see-VSCode-plugin/) | VS Code extension for real-time WCAG 2.2 analysis in the editor |
| [`LookseeIaC`](LookseeIaC/) | Terraform IaC for GCP deployment |
| [`page-audit-enrichment`](page-audit-enrichment/) | Page audit data enrichment utilities |
| [`qa-testbed`](qa-testbed/) | QA test fixture pages for accessibility testing |

## Tech Stack

| Layer | Technology |
|---|---|
| Backend services | Java 17 / 21, Spring Boot 2.6, Maven |
| Shared library | `LookseeCore` (`A11yCore` Maven artifact) |
| Database | Neo4j (graph) via Spring Data Neo4j |
| Messaging | Google Cloud Pub/Sub (async) · Pusher (real-time WebSocket) |
| Browser automation | Selenium WebDriver 3.141.59 |
| Cloud platform | Google Cloud — Cloud Run, Storage, Vision, NLP, Secret Manager |
| Frontend | Angular 17, TypeScript 5.5, Tailwind CSS, Angular Material |
| Authentication | Auth0 (OAuth2 / JWT) |
| Payments | Stripe (subscription billing) |
| Analytics | Segment |
| Infrastructure | Terraform (GCP), GitHub Actions (CI/CD) |
| Container runtime | Docker, Google Cloud Run |

## Deploy to your own cloud

Look-see deploys to **Google Cloud Run** via a single Terraform package. See the full deployment guide — required variables, Secret Manager wiring, Pub/Sub topics, and a sample GitHub Actions workflow — in:

→ **[LookseeIaC/GCP/README.md](LookseeIaC/GCP/README.md)**

## CI/CD

The monorepo CI (`.github/workflows/ci.yml`) uses path-based change detection to build and test only affected modules. Each service also ships its own workflows:

- **`docker-ci-test.yml`** — on PR: build, test, Docker build verification
- **`docker-ci-release.yml`** — on merge to `main`: test, semantic version bump, Docker image push to Docker Hub

## Roadmap

- 🌐 **look-see.com** — hosted SaaS launch (coming soon)
- 📊 **Trend dashboards** — score deltas over time per page and per journey
- 🤖 **Auto-fix suggestions** — LLM-generated remediation PRs
- 🌍 **PDF & Office document audits** — go beyond the browser
- 🏷️ **White-label reports** — for agencies and consultants

Follow the [Issues](https://github.com/deepthought42/Accessibility-audit-platform/issues) tab to track progress.

## Contributing

PRs are very welcome. Good first issues are tagged in each service's repo. Please:

1. Fork and branch from `main`.
2. Run `mvn install -DskipTests` in `LookseeCore` before building any dependent service.
3. Keep changes scoped to one service where possible — CI will only rebuild what you touched.
4. Enable Java assertions (`-ea`) when running locally; Look-see uses Design-by-Contract checks.

## License

Look-see is released under the [MIT License](LICENSE). © 2024–2026 Look-see contributors.

Individual subdirectories may retain their original Apache 2.0 license files from before the monorepo migration; the MIT license at the repository root governs the project as a whole going forward.

---

## Architecture details

### Pub/Sub message flow

```
                                    Look-see Platform Architecture
                                    ==============================

  User Browser
       |
       v
  [Look-see-UI-v3]  <-----(Pusher WebSocket)-----  [front-end-broadcaster]
       |                                                     ^
       | HTTP/REST                                           |
       v                                                     |
  [CrawlerAPI]                                               |
       |                                                     |
       | publishes AuditStartMessage                         |
       v                                                     |
  (url topic)                                     (page_created topic)
       |                                                     |
       v                                                     |
  [PageBuilder] -----publishes PageBuiltMessage------>-------+
       |                                                     |
       | publishes PageBuiltMessage         publishes PageBuiltMessage
       v                                                     v
  (page_created topic)                          [element-enrichment]
       |
       v
  [AuditManager]
       |
       | publishes PageAuditMessage
       v
  (page_audit topic)
       |
       +-------------------+-------------------+
       |                   |                   |
       v                   v                   v
  [contentAudit]   [visualDesign    [informationArchitecture
                    Audit]           Audit]
       |                   |                   |
       +-------------------+-------------------+
       |
       | publishes AuditProgressUpdate
       v
  (audit_update topic)
       |
       v
  [audit-service] -----(Pusher WebSocket)-----> [Look-see-UI-v3]


  Journey Pipeline (domain-level audits)
  ======================================

  [PageBuilder]
       |
       | publishes VerifiedJourneyMessage
       v
  (journey_verified topic)
       |
       v
  [journeyExpander]
       |
       | publishes JourneyCandidateMessage
       v
  (journey_candidate topic)
       |
       v
  [journeyExecutor]
       |
       +---> (journey_verified topic)    [re-enter expansion loop]
       +---> (discarded_journey topic)   [journey rejected]
       +---> (audit_error topic)         [processing failed]

  Dead-letter & Cleanup
  =====================

  (journey_candidate dead-letter) ---> [journeyErrors]     marks failed journeys as ERROR
  (scheduled trigger)             ---> [journey-map-cleanup] cleans stale CANDIDATE journeys
```

### Key Pub/Sub topics

| Topic | Publishers | Subscribers | Payload |
|---|---|---|---|
| `url` | CrawlerAPI | PageBuilder | `AuditStartMessage` (URL, audit level, account) |
| `page_created` | PageBuilder | AuditManager, front-end-broadcaster | `PageBuiltMessage` (account, page, audit record IDs) |
| `page_audit` | AuditManager | contentAudit, visualDesignAudit, informationArchitectureAudit | `PageAuditMessage` (page audit, page, account IDs) |
| `audit_update` | Audit services | audit-service | `AuditProgressUpdate` (progress, category, status) |
| `audit_error` | All services | (logging/monitoring) | Error details |
| `journey_verified` | PageBuilder, journeyExecutor | journeyExpander | `VerifiedJourneyMessage` (journey, account, audit record) |
| `journey_candidate` | journeyExpander | journeyExecutor | `JourneyCandidateMessage` (journey, browser, map) |
| `journey_discarded` | journeyExecutor | CrawlerAPI | `DiscardedJourneyMessage` |
| `journey_completion_cleanup` | (scheduled) | journey-map-cleanup | Trigger payload |

### Monorepo history

Each top-level directory is a former standalone repository. Former `origin` URLs are listed in [`LEGACY_REMOTES.txt`](LEGACY_REMOTES.txt) for reference. The monorepo uses a **single new history** at the root (per-repo commit histories are not included). To preserve old SHAs, clone the URLs in `LEGACY_REMOTES.txt` into separate folders or use `git filter-repo` to replay histories under subdirectories.
