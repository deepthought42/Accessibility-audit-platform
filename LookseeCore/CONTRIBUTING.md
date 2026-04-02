# Contributing to LookseeCore

Thank you for your interest in contributing to LookseeCore.

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Neo4j database (for integration tests)
- Google Cloud SDK (optional, for GCP features)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/deepthought42/lookseecore.git
   cd lookseecore
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run tests:
   ```bash
   mvn test
   ```

## Development Guidelines

### Code Style

- Use Java 17 language features where appropriate
- Follow existing naming conventions (snake_case for parameters, camelCase for fields)
- Use Lombok annotations (`@Getter`, `@Setter`, `@NoArgsConstructor`) for boilerplate reduction
- Use SLF4J logging (not `System.out.println` or `printStackTrace`)

### Design by Contract (DbC)

This project follows Design by Contract principles. All new code **must** comply with these conventions. See [docs/DESIGN_BY_CONTRACT.md](docs/DESIGN_BY_CONTRACT.md) for full details.

#### Preconditions
Every public method must validate its parameters with `assert` statements at the method entry:

```java
/**
 * Finds a domain by host for a given user
 * @param host the host of the domain
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

**Parameter assertion rules:**
- Object parameters: `assert param != null;`
- String parameters: `assert param != null;` and `assert !param.isEmpty();`
- Numeric ID parameters (database IDs): `assert id > 0;`
- Collection parameters: `assert param != null;`

#### Invariants
All model/entity classes must document class invariants in the class-level Javadoc:

```java
/**
 * Represents a domain with its protocol, host, and path.
 *
 * invariant: host != null
 * invariant: protocol != null
 * invariant: colors != null
 */
```

#### Javadoc Requirements
- Every public method must have Javadoc with `@param`, `@return`, and `precondition:` comments
- Every model class must document `invariant:` conditions in class-level Javadoc
- Preconditions are listed after a blank line at the end of the Javadoc block

#### Where NOT to add assertions
- No-arg constructors (used for deserialization/ORM)
- Simple Lombok-generated getters/setters
- `equals()` / `hashCode()` / `toString()` overrides

### Testing

All new code should include unit tests. The project uses:

- **JUnit 5** (Jupiter) for test framework
- **Mockito** for mocking dependencies
- **Spring Boot Test** for integration tests

#### Test organization

Tests are organized by package under `src/test/java/`:

| Source Package | Test Location |
|---|---|
| `com.looksee.models.enums` | `com/looksee/models/enums/` |
| `com.looksee.models` | `com/looksee/models/` |
| `com.looksee.models.dto` | `com/looksee/models/dto/` |
| `com.looksee.models.audit` | `com/looksee/models/audit/` |
| `com.looksee.exceptions` | `com/looksee/exceptions/` |
| `com.looksee.services` | `services/` |
| `com.looksee.utils` | `com/looksee/utils/` and `utils/` |
| `com.looksee.config` | `com/looksee/config/` and `config/` |

#### Test configuration

Test configuration files are in `src/test/resources/`:
- `application-test.properties` - Disables GCP, Neo4j, and Pub/Sub for unit tests
- `application-test.yml` - YAML-based test configuration

#### Writing tests

- Prefer plain unit tests over Spring context tests for speed
- Use `@ExtendWith(MockitoExtension.class)` and `@Mock`/`@InjectMocks` for service tests
- Test edge cases: null inputs, empty collections, boundary values
- Test all public methods including getters/setters for model classes
- Test enum `create()` methods with valid, invalid, null, and case-insensitive inputs

### Branching

- `main` - Stable release branch
- Feature branches - `feature/description` or `claude/description`
- Bug fix branches - `fix/description`

### Pull Requests

1. Create a feature branch from `main`
2. Make your changes with clear, atomic commits
3. Add or update tests for your changes
4. Ensure all tests pass: `mvn test`
5. Update documentation if needed
6. Submit a pull request with a clear description

### Commit Messages

Use descriptive commit messages:
- `feat: add new audit type for video structure`
- `fix: handle null page state in PageService`
- `test: add unit tests for ColorUtils`
- `docs: update README with new configuration options`
- `refactor: extract color comparison to utility method`
