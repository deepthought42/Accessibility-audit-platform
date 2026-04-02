# Contribution Guidelines

Thank you for your interest in contributing to the Visual Design Audit project! This document provides guidelines and instructions for contributing.

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Create a feature branch from `master`
4. Make your changes
5. Run the test suite to ensure nothing is broken
6. Submit a pull request

## Development Setup

### Prerequisites

- Java 17 (Eclipse Temurin recommended)
- Maven 3.x
- Git

### Building the Project

```bash
# Clone the repository
git clone https://github.com/deepthought42/visualDesignAudit.git
cd visualDesignAudit

# Build the project
mvn clean package

# Run tests
mvn test
```

## Commit Message Format

We follow [Conventional Commits](https://www.conventionalcommits.org/).

**Format:** `<type>(<optional scope>): <description>`

### Examples

```
feat: add user login feature
fix(payment): resolve checkout bug
chore(deps): update Docker base image
test(margin): add unit tests for isMultipleOf8
docs: update README with test coverage table
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `chore`: Routine maintenance (dependencies, CI/CD, etc.)
- `refactor`: Code improvements without changing behavior
- `style`: Code style changes (formatting, whitespace)
- `test`: Add or update tests
- `perf`: Performance improvements
- `ci`: CI/CD pipeline changes

## Testing

All code changes should include appropriate tests. We aim for at least 95% code coverage.

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=MarginAuditTest

# Run a specific test method
mvn test -Dtest=MarginAuditTest#testIsMultipleOf8_with8px
```

### Writing Tests

- Place test files in `src/test/java/` mirroring the main source structure
- Use JUnit 4 (`@Test`, `@Before`, `@RunWith`)
- Use Mockito for mocking dependencies (`@Mock`, `@InjectMocks`, `@RunWith(MockitoJUnitRunner.class)`)
- Name test methods descriptively: `testMethodName_scenarioDescription`
- Test both positive and negative cases
- Test boundary conditions (e.g., exact threshold values for WCAG contrast)

### Test Structure Example

```java
@RunWith(MockitoJUnitRunner.class)
public class MyAuditTest {

    @Mock
    private SomeDependency dependency;

    @InjectMocks
    private MyAudit myAudit;

    @Test
    public void testExecute_withValidInput() {
        // Arrange
        PageState pageState = mock(PageState.class);
        // ...

        // Act
        Audit result = myAudit.execute(pageState, auditRecord, designSystem);

        // Assert
        assertNotNull(result);
        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
    }
}
```

## Code Style

- Use Java naming conventions (camelCase for methods, PascalCase for classes)
- Keep methods focused and reasonably sized
- Add Javadoc to public methods
- Follow existing code patterns in the project

## Pull Request Process

1. Ensure all tests pass (`mvn test`)
2. Update documentation if your changes affect the public API or behavior
3. Write clear commit messages following the Conventional Commits format
4. Reference any related issues in your PR description
5. Request a review from a maintainer

## Reporting Issues

If you find a bug or have a feature request, please create an issue on the GitHub repository with:

- A clear title and description
- Steps to reproduce (for bugs)
- Expected vs. actual behavior
- Any relevant logs or screenshots
