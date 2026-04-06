# Journey Map Cleanup

Spring Boot microservice that cleans up stale journey candidates in domain maps to prevent orphaned data from accumulating.

## What It Does

Runs on a scheduled Pub/Sub trigger. For each domain map created within the last 7 days, it checks whether any new journeys were created in the last 30 minutes. If no recent journey activity is found, all remaining `CANDIDATE` journeys are marked as `ERROR` to prevent them from being reprocessed indefinitely.

## Tech Stack

- **Java 17** / **Maven** / **Spring Boot 2.6.13**
- **A11yCore 0.5.0** -- shared models, persistence, GCP integration
- **Neo4j** -- graph database for journey/domain map state
- **Google Cloud Pub/Sub** -- scheduled trigger via push subscription

## Prerequisites

- Java 17+
- Maven 3.x
- Neo4j instance
- LookseeCore installed locally (`cd LookseeCore && mvn clean install -DskipTests`)

## Build & Run

```bash
# Install core dependency
cd ../LookseeCore && mvn clean install -DskipTests && cd ../journey-map-cleanup

# Build
mvn clean package -DskipTests

# Run (assertions enabled)
java -ea -jar target/*.jar
```

## Docker

```bash
mvn clean package -DskipTests
docker build -t journey-map-cleanup .
docker run -p 8080:8080 journey-map-cleanup
```

## Testing

```bash
mvn clean test
```
