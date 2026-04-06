# Element Enrichment

Spring Boot microservice that enriches page element states with visual and semantic metadata using headless browser inspection.

## What It Does

Receives `PageBuiltMessage` events via Google Cloud Pub/Sub, loads the page in a headless Chrome browser, and enriches each element's state with additional properties such as computed background colors. Enriched element states are saved back to Neo4j.

## Tech Stack

- **Java 17** / **Maven** / **Spring Boot 2.6.13**
- **A11yCore 0.5.0** -- shared models, persistence, browser automation, GCP integration
- **Neo4j** -- graph database for element/page state storage
- **Selenium** -- headless browser for element inspection
- **Google Cloud Pub/Sub** -- async message processing

## Prerequisites

- Java 17+
- Maven 3.x
- Neo4j instance
- LookseeCore installed locally (`cd LookseeCore && mvn clean install -DskipTests`)

## Build & Run

```bash
# Install core dependency
cd ../LookseeCore && mvn clean install -DskipTests && cd ../element-enrichment

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

The service listens for `PageBuiltMessage` on its configured Pub/Sub subscription:

```json
{
  "accountId": 16961,
  "pageId": 2970,
  "auditRecordId": 2114
}
```
