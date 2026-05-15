# Local Deployment

Run the full Look-see audit pipeline on your laptop with one command.

## Prerequisites

- Docker 24+ with Compose v2 (BuildKit is required and is the default)
- ~8 GB free RAM
- ~10 GB free disk for images and the Maven cache

## Quick start

```bash
cp .env.local.example .env
docker compose up --build
```

The first build resolves Maven dependencies and compiles `LookseeCore` plus 13 services; expect 10-20 minutes on first run. Subsequent runs hit the BuildKit cache (`id=looksee-m2`) and start in seconds.

Once everything is healthy:

| Service | URL | Notes |
|---|---|---|
| Angular UI | http://localhost:4200 | Local stack, Auth0 bypassed |
| CrawlerAPI | http://localhost:9080 | REST API; `/actuator/health` returns `UP` |
| Neo4j browser | http://localhost:7474 | login: `neo4j` / `looksee-local` |
| Pub/Sub emulator | http://localhost:8085 | REST API for the GCP emulator |

The other 11 Java services run on the internal docker network only; they communicate via Pub/Sub and Neo4j.

## What the stack contains

```
docker compose up --build
├── neo4j                    (Neo4j 5.20 community)
├── neo4j-bootstrap          (one-shot: applies create-indexes-and-constraints.cql)
├── pubsub-emulator          (gcloud beta emulators pubsub start)
├── pubsub-bootstrap         (one-shot: creates topics + subscriptions)
├── crawlerapi               (Java 21, port 9080)
├── pagebuilder
├── auditmanager
├── contentaudit
├── visualdesignaudit
├── informationarchitectureaudit
├── journeyexecutor
├── journeyexpander
├── auditservice
├── elementenrichment
├── journeymapcleanup
├── journeyerrors
├── broadcaster
└── ui                       (Angular 17, port 4200)
```

## How Auth0 is bypassed

The CrawlerAPI's `SecurityConfig` is annotated `@Profile("!local")`, so under the `local` profile it is replaced by `LocalSecurityConfig` which permits all requests. Set `SPRING_PROFILES_ACTIVE=local` in `.env` (the default in `.env.local.example`). To use a real Auth0 tenant instead:

```env
SPRING_PROFILES_ACTIVE=
AUTH0_DOMAIN=your-tenant.us.auth0.com
AUTH0_CLIENT_ID=...
AUTH0_AUDIENCE=https://api.your-domain.com
```

Then rebuild just CrawlerAPI and the UI:

```bash
docker compose up -d --no-deps --build crawlerapi ui
```

## Verifying the stack

```bash
# Containers up
docker compose ps

# CrawlerAPI health
curl -s http://localhost:9080/actuator/health
# -> {"status":"UP"}

# Pub/Sub topics
curl -s http://localhost:8085/v1/projects/looksee-local/topics

# Neo4j indexes (from inside the neo4j container)
docker compose exec neo4j cypher-shell -u neo4j -p looksee-local 'SHOW INDEXES'

# UI
open http://localhost:4200
```

## Common operations

```bash
# View logs for one service
docker compose logs -f crawlerapi

# Rebuild a single service after a code change
docker compose up -d --no-deps --build crawlerapi

# Reset all state (drops Neo4j data and Pub/Sub topics)
docker compose down -v

# Open a shell in CrawlerAPI
docker compose exec crawlerapi sh
```

## Architecture notes

- **LookseeCore** is built from source inside each service Dockerfile via a shared BuildKit cache mount (`id=looksee-m2`). The first service build populates the cache; subsequent services reuse it, so total build time grows by Maven download time only once.
- **Spring Cloud GCP** picks up `PUBSUB_EMULATOR_HOST` automatically and routes all Pub/Sub traffic to the emulator container — no GCP project or service-account JSON is needed.
- **Health checks** use Spring Actuator `/actuator/health` on each Java service. Compose waits for `crawlerapi: service_healthy` before starting the UI.
- **The 11 internal services** intentionally do not publish host ports. To debug one, drop a `docker-compose.override.yml` with a `ports:` entry; Compose merges it automatically.

## Limitations

- The Pusher real-time broadcast is a no-op locally (Pusher keys are empty). The UI still loads; live audit progress just isn't pushed.
- Stripe, Segment, and GCP Vision/NLP integrations are not wired locally. Code paths that touch them will log warnings and short-circuit.
- The broadcaster's upstream Dockerfile expects pre-baked GCP credentials and a Gmail credentials JSON; the local Dockerfile (`Dockerfile.local`) skips both. Email notifications from the broadcaster will be disabled locally.
