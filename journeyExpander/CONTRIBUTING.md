# Contribution Guidelines

## Commit Message Format
We follow [Conventional Commits](https://www.conventionalcommits.org/).

**Format:**
```
<type>(<optional scope>): <description>
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `chore`: Routine maintenance (deps, CI, config)
- `refactor`: Code improvements (no behavior change)
- `style`: Code style changes (formatting, whitespace)
- `test`: Add or update tests

### Examples
```
feat: add user login feature
fix(payment): resolve checkout bug
chore(deps): update Docker base image
test: add comprehensive AuditController unit tests
docs: update README with architecture and testing sections
```

## Development Setup

1. **Clone** the repository.
2. **Ensure** Java 17+ and Maven 3.8+ are installed.
3. **Build**: `mvn clean package` (automatically downloads `com.looksee:core` via `scripts/download-core.sh`).
4. **Run tests**: `mvn test`.

## Design by Contract

This project follows Design by Contract principles:

- **Public methods** validate all inputs and return well-defined error responses.
- **Private methods** use `assert` statements to enforce preconditions that callers
  must satisfy. Assertions are enabled at runtime via `-ea` in the Dockerfile.
- **Javadoc** documents preconditions, postconditions, and parameter constraints
  for all non-trivial methods.

When contributing new code:
- Document preconditions and postconditions in Javadoc.
- Use assertions for internal invariants and preconditions in private methods.
- Validate external input at system boundaries (controller endpoints).

## Testing Guidelines

- All new code should include corresponding unit tests.
- Target **90%+ code coverage** as measured by JaCoCo.
- Tests use **JUnit 5** with **Mockito** for mocking dependencies.
- Use `mockito-inline` for static method mocking (e.g., `BrowserUtils`, `BrowserService`, `ElementStateUtils`).
- Run `mvn jacoco:report` after tests to generate coverage reports at `target/site/jacoco/index.html`.

## Pull Requests

- Branch from `main`.
- Keep PRs focused on a single concern.
- Ensure all tests pass before requesting review.
- CI automatically runs tests on PRs to `main`.
