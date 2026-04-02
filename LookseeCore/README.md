# Look-see Core

A comprehensive Spring Boot library for web auditing, browser automation, and UX analysis.

## Features

- **Browser Automation**: Selenium-based web scraping and interaction
- **Audit Management**: Comprehensive UX and accessibility auditing
- **Neo4j Integration**: Graph database persistence for complex relationships
- **Google Cloud Integration**: Image analysis, NLP, and cloud storage
- **Journey Mapping**: User journey discovery and analysis
- **Design System Analysis**: Color palette, typography, and visual consistency

## Quick Start

### Adding to Your Project

Add the following dependency to your project:

```xml
<dependency>
    <groupId>com.looksee</groupId>
    <artifactId>core</artifactId>
    <version>0.3.24</version>
</dependency>
```

### Auto-Configuration

This library includes Spring Boot auto-configuration that automatically registers all repositories and services when included as a dependency. No additional configuration is required!

The auto-configuration will:
- Scan and register all `@Repository` beans in `com.looksee.models.repository`
- Scan and register all `@Service` beans in `com.looksee.services`
- Configure Neo4j repositories with proper transaction management
- Enable component scanning for all library packages

### Configuration

You can configure the library behavior in your `application.yml` or `application.properties`:

```yaml
looksee:
  core:
    enabled: true  # Enable/disable auto-configuration (default: true)
    neo4j:
      connection-timeout: 30000
      max-connection-pool-size: 50
      connection-pooling-enabled: true

spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: your-password

# Optional: Selenium WebDriver configuration
selenium:
  urls: "http://selenium-hub:4444/wd/hub,http://localhost:4444/wd/hub"
  connectionTimeout: 30000      # Connection timeout in milliseconds (default: 30000)
  maxRetries: 3                 # Maximum retry attempts (default: 3)
  implicitWaitEnabled: true     # Enable implicit waits (default: true)
  implicitWaitTimeout: 10000    # Implicit wait timeout in milliseconds (default: 10000)

# Optional: Real-time messaging with Pusher
pusher:
  appId: "your-pusher-app-id"
  key: "your-pusher-key"
  secret: "your-pusher-secret"
  cluster: "your-pusher-cluster"
  encrypted: true  # default: true

# Optional: Google Cloud Pub/Sub configuration
pubsub:
  # Configure only the topics your service needs
  audit_update: "your-audit-update-topic"
  error_topic: "your-error-topic"
  page_audit_topic: "your-page-audit-topic"
  page_built: "your-page-built-topic"
  url_topic: "your-url-topic"
  journey_verified: "your-journey-verified-topic"
  journey_candidate: "your-journey-candidate-topic"
  discarded_journey_topic: "your-discarded-journey-topic"
```

#### Real-time Messaging (Optional)

If you need real-time messaging capabilities, configure Pusher credentials as shown above. The `MessageBroadcaster` service will be automatically available when Pusher is configured.

For detailed Pusher configuration options and troubleshooting, see [Pusher Configuration Guide](docs/PUSHER_CONFIGURATION.md).

#### Selenium WebDriver (Optional)

If you need browser automation capabilities, configure Selenium WebDriver URLs and settings. The `SeleniumConfiguration` will automatically initialize the browser connection helper with the provided configuration.

```yaml
selenium:
  urls: "http://selenium-hub:4444/wd/hub,http://localhost:4444/wd/hub"
  connectionTimeout: 30000      # Connection timeout in milliseconds (default: 30000)
  maxRetries: 3                 # Maximum retry attempts (default: 3)
  implicitWaitEnabled: true     # Enable implicit waits (default: true)
  implicitWaitTimeout: 10000    # Implicit wait timeout in milliseconds (default: 10000)
```

**Configuration Options:**
- `urls` (required): Comma-separated list of Selenium hub URLs
- `connectionTimeout`: WebDriver connection timeout in milliseconds
- `maxRetries`: Maximum number of connection retry attempts
- `implicitWaitEnabled`: Whether to enable implicit waits for element finding
- `implicitWaitTimeout`: Timeout for implicit waits in milliseconds

**Environment Variables:**
You can also configure via environment variables:
- `SELENIUM_URLS`
- `SELENIUM_CONNECTION_TIMEOUT`
- `SELENIUM_MAX_RETRIES`
- `SELENIUM_IMPLICIT_WAIT_ENABLED`
- `SELENIUM_IMPLICIT_WAIT_TIMEOUT`

The configuration is completely optional - if not provided, the SeleniumConfiguration bean will not be created.

#### Google Cloud Pub/Sub (Optional)

The library includes conditional Pub/Sub publishers that are only created when their corresponding topics are configured. This allows each service to configure only the topics it needs without causing bean creation errors for unused publishers.

Available publisher beans:
- `PubSubAuditUpdatePublisherImpl` - Created when `pubsub.audit_update` is configured
- `PubSubErrorPublisherImpl` - Created when `pubsub.error_topic` is configured
- `PubSubPageAuditPublisherImpl` - Created when `pubsub.page_audit_topic` is configured
- `PubSubPageBuiltPublisherImpl` - Created when `pubsub.page_built` is configured
- `PubSubPageCreatedPublisherImpl` - Created when `pubsub.page_built` is configured (shares same topic)
- `PubSubUrlMessagePublisherImpl` - Created when `pubsub.url_topic` is configured
- `PubSubJourneyVerifiedPublisherImpl` - Created when `pubsub.journey_verified` is configured
- `PubSubJourneyCandidatePublisherImpl` - Created when `pubsub.journey_candidate` is configured
- `PubSubDiscardedJourneyPublisherImpl` - Created when `pubsub.discarded_journey_topic` is configured

**Example configurations for different services:**

Service A (only needs error and audit topics):
```yaml
pubsub:
  error_topic: "service-a-errors"
  audit_update: "service-a-audits"
```

Service B (only needs journey-related topics):
```yaml
pubsub:
  journey_verified: "verified-journeys"
  journey_candidate: "candidate-journeys"
  discarded_journey_topic: "discarded-journeys"
```

### Usage Examples

#### Domain Management
```java
@Autowired
private DomainService domainService;

// Create and save a domain
Domain domain = new Domain("https", "example.com", "/", "logo.png");
domain = domainService.save(domain);

// Find domain by key
Domain found = domainService.findByKey("domainKey", "username");
```

#### Browser Automation
```java
@Autowired
private BrowserService browserService;

// Navigate and interact with web pages
Browser browser = new Browser();
browser.navigate("https://example.com");
browser.takeScreenshot();

// The SeleniumConfiguration automatically configures WebDriver URLs
// when selenium.urls property is provided
```

#### Audit Management
```java
@Autowired
private AuditService auditService;

// Perform and save audits
AuditRecord record = auditService.performAudit(pageState);
auditService.save(record);
```

#### Design System Integration
```java
@Autowired
private DesignSystemService designSystemService;

// Save design system information
DesignSystem designSystem = new DesignSystem();
designSystem = designSystemService.save(designSystem);
```

#### Real-time Messaging (Optional)
```java
@Autowired(required = false)
private MessageBroadcaster messageBroadcaster;

// Broadcast messages to clients via Pusher
if (messageBroadcaster != null) {
    try {
        messageBroadcaster.broadcastTest(test, "example.com");
        messageBroadcaster.broadcastAudit("example.com", audit);
    } catch (JsonProcessingException e) {
        // Handle serialization error
    }
}
```

#### Google Cloud Pub/Sub Integration (Optional)
```java
// Only inject the publishers your service has configured
@Autowired(required = false)
private PubSubAuditUpdatePublisherImpl auditPublisher;

@Autowired(required = false)
private PubSubErrorPublisherImpl errorPublisher;

@Autowired(required = false)
private PubSubJourneyVerifiedPublisherImpl journeyPublisher;

public void publishAuditUpdate(String auditJson) {
    if (auditPublisher != null) {
        try {
            auditPublisher.publish(auditJson);
        } catch (ExecutionException | InterruptedException e) {
            // Handle publishing error
            log.error("Failed to publish audit update", e);
        }
    }
}

public void publishError(String errorJson) {
    if (errorPublisher != null) {
        try {
            errorPublisher.publish(errorJson);
        } catch (ExecutionException | InterruptedException e) {
            // Handle publishing error
            log.error("Failed to publish error", e);
        }
    }
}
```

## Troubleshooting

### Common Issues

#### 1. "No qualifying bean of type 'AuditRecordRepository' available"

This error occurs when Spring Boot cannot find the repository beans. The auto-configuration should resolve this automatically, but if you're still experiencing issues:

**Solution**: Ensure your main application class has `@SpringBootApplication` and is in a parent package of `com.looksee`:

```java
@SpringBootApplication
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

**Alternative Solution**: If your application is in a different package structure, explicitly enable component scanning:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"your.package", "com.looksee"})
@EnableNeo4jRepositories(basePackages = {"your.package.repository", "com.looksee.models.repository"})
public class YourApplication {
    // ...
}
```

#### 2. Neo4j Connection Issues

Ensure your Neo4j configuration is correct:

```yaml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: your-password
    database: neo4j  # Optional, defaults to 'neo4j'
```

#### 3. Pub/Sub Configuration Issues

**Error**: `BeanCreationException: Could not resolve placeholder 'pubsub.page_audit_topic'`

This error occurs when a Pub/Sub publisher bean is being created but its required property is not configured.

**Solution**: Only configure the Pub/Sub topics your service actually needs. The publishers are now conditionally created based on property presence:

```yaml
# Only configure topics you use
pubsub:
  error_topic: "my-error-topic"  # This will create PubSubErrorPublisherImpl
  audit_update: "my-audit-topic" # This will create PubSubAuditUpdatePublisherImpl
  # Other topics are not configured, so their publishers won't be created
```

**Alternative**: If you need all publishers but some topics are optional, you can provide empty/placeholder values:

```yaml
pubsub:
  error_topic: "error-topic"
  audit_update: "audit-topic"
  page_audit_topic: ""  # Empty but present - bean will be created
```

#### 4. Selenium Configuration Issues

**Error**: No Selenium URLs configured or WebDriver connection failures.

**Solution**: Configure Selenium WebDriver URLs if you're using browser automation features:

```yaml
selenium:
  urls: "http://selenium-hub:4444/wd/hub,http://localhost:4444/wd/hub"
  connectionTimeout: 30000  # Optional: Adjust timeouts as needed
  maxRetries: 3            # Optional: Configure retry behavior
```

If you don't need browser automation, you can simply omit this configuration and the SeleniumConfiguration bean won't be created.

#### 5. Disabling Auto-Configuration

If you need to disable the auto-configuration:

```yaml
looksee:
  core:
    enabled: false
```

Then manually configure the components you need:

```java
@Configuration
@ComponentScan(basePackages = "com.looksee.services")
@EnableNeo4jRepositories(basePackages = "com.looksee.models.repository")
public class LookseeCoreManualConfiguration {
    // Your manual configuration
}
```

## Design by Contract

This project enforces **Design by Contract (DbC)** principles across all layers. Every public method documents and enforces its contract through:

- **Preconditions**: `assert` statements at method entry validate all parameters
- **Invariants**: Class-level Javadoc documents what must always be true about object state
- **Javadoc contracts**: All preconditions are documented with `precondition:` comments

### Example

```java
/**
 * Finds a domain by its host for a given user.
 * @param host the host of the domain to find
 * @param username the username of the user
 * @return the domain if found, or null if not found
 *
 * precondition: host != null
 * precondition: !host.isEmpty()
 * precondition: username != null
 * precondition: !username.isEmpty()
 */
public Domain findByHostForUser(String host, String username) {
    assert host != null;
    assert !host.isEmpty();
    assert username != null;
    assert !username.isEmpty();
    return domain_repo.findByHostForUser(host, username);
}
```

### Enabling Assertions at Runtime

Assertions are disabled by default in the JVM. To enable them (recommended for development and testing):

```bash
java -ea -jar your-application.jar
```

Or in Maven tests (already enabled by default):
```bash
mvn test
```

For detailed DbC conventions, see [docs/DESIGN_BY_CONTRACT.md](docs/DESIGN_BY_CONTRACT.md).

## Architecture

### Core Components

- **Services**: Business logic layer (`com.looksee.services`)
- **Repositories**: Data access layer (`com.looksee.models.repository`)
- **Models**: Domain entities and DTOs (`com.looksee.models`)
- **Utils**: Utility classes and helpers (`com.looksee.utils`)
- **Browsing**: Browser automation components (`com.looksee.browsing`)

### Key Services

- `DomainService`: Domain management and operations
- `AuditService`: Audit execution and management
- `BrowserService`: Browser automation and page interaction
- `DesignSystemService`: Design system analysis
- `JourneyService`: User journey mapping and analysis

### Key Repositories

- `AuditRecordRepository`: Audit record persistence
- `DomainRepository`: Domain data storage
- `PageStateRepository`: Page state persistence
- `ElementStateRepository`: Element state storage

## Requirements

- Java 17 or higher
- Spring Boot 2.6.x or higher
- Neo4j Database
- Google Cloud Platform (for image analysis and storage features)

## Development

### Building

```bash
mvn clean install
```

### Testing

```bash
mvn test
```

The project includes a comprehensive test suite covering:

- **Enum classes** (37 enums): Factory methods, case-insensitive creation, round-trip serialization, null/invalid input handling
- **Model/entity classes**: Constructors, getters/setters, key generation, equality, cloning, business logic
- **DTO classes**: Constructor variants, field access, serialization readiness
- **Exception classes**: Default/custom messages, HTTP status annotations, inheritance hierarchy
- **Audit model classes**: Audit records, scores, statistics, issue messages, recommendations
- **Rule classes**: Rule factory, rule type enum, all rule implementations
- **Journey classes**: Journey lifecycle, step types, domain maps
- **Message classes**: All message types for inter-service communication
- **Utility classes**: Color analysis, audit scoring, form classification, content readability, journey utils
- **Service classes**: Business logic with mocked repositories using Mockito
- **Configuration classes**: Property binding, auto-configuration, Pusher/Selenium setup
- **Browsing classes**: Coordinates, element nodes, value domains, form fields
- **Design system and competitive analysis**: Model construction and field access
- **VS Code plugin**: Tree structures, session tracking, test mapping

Test configuration disables external dependencies (Neo4j, GCP, Pub/Sub) for fast, isolated unit testing.

### Publishing

```bash
mvn clean deploy
```

## Project Structure

```
src/
├── main/java/com/looksee/
│   ├── audits/performance/    # Performance audit detail models
│   ├── browsing/              # Browser automation (Selenium)
│   │   ├── form/              # Form field extraction
│   │   ├── helpers/           # Action and connection helpers
│   │   └── table/             # Table parsing
│   ├── config/                # Spring Boot auto-configuration
│   ├── exceptions/            # Custom exception types
│   ├── gcp/                   # Google Cloud Platform integrations
│   ├── integrations/          # External API clients
│   ├── mapper/                # JSON/Base64 deserializers
│   ├── models/                # Domain entities
│   │   ├── audit/             # Audit records, scores, issues
│   │   ├── competitiveanalysis/ # Competitor profiling
│   │   ├── designsystem/      # Design system snapshots
│   │   ├── dto/               # Data transfer objects
│   │   ├── enums/             # 37 enum types
│   │   ├── journeys/          # User journey models
│   │   ├── message/           # Inter-service messages
│   │   ├── repository/        # Neo4j repositories
│   │   ├── rules/             # Validation rules
│   │   └── serializer/        # Custom deserializers
│   ├── services/              # Business logic layer
│   ├── utils/                 # Utility classes
│   └── vscodePlugin/          # VS Code extension support
└── test/java/                 # Test suite
    ├── browser/               # Browser automation tests
    ├── com/looksee/
    │   ├── browsing/          # Browsing component tests
    │   ├── config/            # Configuration tests
    │   ├── exceptions/        # Exception tests
    │   ├── mapper/            # Mapper tests
    │   ├── models/            # Model, DTO, enum, audit tests
    │   ├── utils/             # Utility tests
    │   └── vscodePlugin/      # VS Code plugin tests
    ├── config/                # Auto-config tests
    ├── services/              # Service tests
    └── utils/                 # Additional utility tests
```

## Build Environment Note

If `mvn test` fails with dependency BOM resolution errors (HTTP 403 from `https://repo.maven.apache.org/maven2`), verify your environment has access to Maven Central or configure your corporate artifact proxy mirror in Maven `settings.xml`.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines.

## License

[Add your license information here]
