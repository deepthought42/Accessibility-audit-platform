# Front-End Broadcaster

Spring Boot microservice that bridges backend audit events to the frontend UI in real time via Pusher WebSockets.

## What It Does

Receives `PageBuiltMessage` events via Google Cloud Pub/Sub. When a page state is found, it:

1. Loads the page state from Neo4j
2. Constructs a `PageStateDto` with the audit record context
3. Looks up the associated user account
4. Broadcasts the page state to the user's Pusher channel for live UI updates

## Tech Stack

- **Java 17** / **Maven** / **Spring Boot 2.6.13**
- **A11yCore 0.5.0** -- shared models, persistence, GCP integration
- **Neo4j** -- graph database for page/account state
- **Pusher** -- real-time WebSocket broadcasting to frontend
- **Google Cloud Pub/Sub** -- async event consumption

## Key Components

| Class | Role |
|-------|------|
| `AuditController` | Pub/Sub push endpoint; processes page-built events |
| `MessageBroadcaster` | Formats and sends events to Pusher channels |
| `PusherConnector` | Manages Pusher client connection and configuration |
| `AccountService` | Looks up user accounts for channel routing |

## Prerequisites

- Java 17+
- Maven 3.x
- Neo4j instance
- Pusher account (key, app ID, cluster, secret)
- LookseeCore installed locally (`cd LookseeCore && mvn clean install -DskipTests`)

## Build & Run

```bash
# Install core dependency
cd ../LookseeCore && mvn clean install -DskipTests && cd ../look-see-front-end-broadcaster

# Build
mvn clean package -DskipTests

# Run
java -ea -jar target/*.jar
```

## Testing

```bash
mvn clean test
```

## Pub/Sub Message Format

### PageBuiltMessage

```json
{
  "accountId": 16961,
  "pageId": 2970,
  "auditRecordId": 2114
}
```

### AuditUpdateMessage

```json
{
  "auditRecordId": 3356,
  "category": "CONTENT",
  "level": "PAGE",
  "progress": 1.0,
  "message": "Content Audit Complete!"
}
```
