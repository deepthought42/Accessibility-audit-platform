# Design by Contract (DbC) Conventions

This document describes the Design by Contract conventions used throughout the LookseeCore project.

## Overview

Design by Contract is a software correctness approach where each component defines:

- **Preconditions**: What must be true before a method executes (caller's responsibility)
- **Postconditions**: What the method guarantees after execution (method's responsibility)
- **Invariants**: What must always be true about an object's state (class's responsibility)

LookseeCore enforces contracts using Java `assert` statements and documents them in Javadoc.

## Preconditions

Every public method validates its parameters at method entry using `assert` statements.

### Parameter Assertion Rules

| Parameter Type | Assertions |
|---|---|
| Object | `assert param != null;` |
| String | `assert param != null;` and `assert !param.isEmpty();` |
| Numeric ID (database) | `assert id > 0;` |
| Collection | `assert param != null;` |
| Primitive (int, double, boolean) | No assertion (cannot be null) |

### Example: Service Method

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

### Example: Constructor

```java
/**
 * Constructs a new Domain.
 * @param protocol the protocol (http/https)
 * @param host the domain host
 * @param path the domain path
 * @param logo_url the URL of the domain logo
 *
 * precondition: protocol != null
 * precondition: host != null
 * precondition: path != null
 * precondition: logo_url != null
 */
public Domain(String protocol, String host, String path, String logo_url) {
    assert protocol != null;
    assert host != null;
    assert path != null;
    assert logo_url != null;
    // ... initialization
}
```

## Invariants

Class invariants are documented in the class-level Javadoc using `invariant:` comments. They describe what must always be true about an object's state after construction.

### Example: Model Class

```java
/**
 * Represents a page state captured during browsing.
 *
 * invariant: url != null
 * invariant: browser != null
 * invariant: elements != null
 * invariant: src != null when page has content
 */
@Node
@Getter
@Setter
public class PageState extends LookseeObject {
    // ...
}
```

## Where NOT to Add Assertions

The following are exempted from DbC assertions:

- **No-arg constructors**: Used by ORM (Neo4j) and JSON deserialization; parameters are set after construction
- **Lombok-generated getters/setters**: `@Getter` and `@Setter` annotations generate simple accessors
- **`equals()` / `hashCode()` / `toString()`**: Standard Java contract methods
- **Private/internal methods**: Contracts are enforced at public API boundaries
- **Primitive parameters**: Cannot be null (`int`, `double`, `boolean`, `long` when not an ID)

## Javadoc Contract Format

### Method Javadoc

```java
/**
 * Brief description of what the method does.
 *
 * @param paramName description of the parameter
 * @return description of the return value
 * @throws ExceptionType when this exception occurs
 *
 * precondition: paramName != null
 * precondition: !paramName.isEmpty()
 * postcondition: return value != null (when applicable)
 */
```

### Class Javadoc

```java
/**
 * Brief description of the class.
 *
 * invariant: fieldName != null
 * invariant: collection != null
 * invariant: numericField >= 0
 */
```

## Enabling Assertions

Java assertions are **disabled by default** at runtime. To enable them:

### Development / Testing

```bash
# JVM flag
java -ea -jar application.jar

# Maven (assertions enabled by default in surefire)
mvn test

# Specific package
java -ea:com.looksee... -jar application.jar
```

### Production

Assertions are typically left disabled in production for performance. The documented preconditions in Javadoc still serve as API contracts for callers, even when assertions are not checked at runtime.

## Layer-Specific Conventions

### Service Layer (`com.looksee.services`)

- All public methods enforce preconditions
- Repository-delegating methods validate all parameters before delegation
- Methods with `@Retry` or `@Synchronized` still enforce preconditions first

### Model Layer (`com.looksee.models`)

- Parameterized constructors enforce preconditions for all non-null parameters
- Class-level invariants are documented in Javadoc
- Business logic methods (e.g., `addElement()`, `addColor()`) enforce preconditions
- `generateKey()` implementations may have specific preconditions based on required fields

### Utility Layer (`com.looksee.utils`)

- All static utility methods enforce preconditions
- Methods that accept URLs, file paths, or external data validate non-null and non-empty

### GCP Layer (`com.looksee.gcp`)

- PubSub publishers enforce non-null message preconditions
- Cloud API utility methods validate input data before API calls

## Migration Guide

When adding new code to the project:

1. Add `assert` statements for all parameters in public methods
2. Add `precondition:` lines to Javadoc for each assertion
3. Add `invariant:` lines to class-level Javadoc for new model classes
4. Run tests with `-ea` to verify assertions pass
5. Ensure no existing tests break with assertions enabled
