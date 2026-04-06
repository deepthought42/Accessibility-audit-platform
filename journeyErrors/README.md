# Journey Errors (Dead Letter Handler)

Spring Boot microservice that handles failed journey candidate messages. When a journey candidate fails processing in the normal pipeline, this service receives the dead-letter message and marks the journey status as `ERROR`.

## What It Does

Receives `JourneyCandidateMessage` events from a Pub/Sub dead-letter topic. If the journey is still in `CANDIDATE` status (hasn't been processed by another service), it updates the journey status to `ERROR` in Neo4j to prevent infinite reprocessing.

## Tech Stack

- **Java 17** / **Maven** / **Spring Boot 2.6.13**
- **A11yCore 0.5.0** -- shared models, persistence, GCP integration
- **Neo4j** -- graph database for journey state storage
- **Google Cloud Pub/Sub** -- dead-letter message consumption

## Prerequisites

- Java 17+
- Maven 3.x
- Neo4j instance
- LookseeCore installed locally (`cd LookseeCore && mvn clean install -DskipTests`)

## Build & Run

```bash
# Install core dependency
cd ../LookseeCore && mvn clean install -DskipTests && cd ../journeyErrors

# Build
mvn clean package -DskipTests

# Run (assertions enabled)
java -ea -jar target/*.jar
```

## Testing

```bash
mvn clean test
```

## Pub/Sub Message Format

The service listens for `JourneyCandidateMessage` on its dead-letter subscription:

```json
{
  "accountId": 1,
  "journey": {
    "id": 10,
    "status": "CANDIDATE",
    "candidateKey": "abc123"
  },
  "browser": "CHROME",
  "auditRecordId": 100,
  "mapId": 1
}
```
