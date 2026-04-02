# Contributing to PageBuilder

Thank you for your interest in contributing! This document outlines the workflow and conventions used in this project.

## Prerequisites

- Java 17 (Temurin recommended)
- Maven 3.8+
- Docker (for integration/build testing)
- The LookseeCore JAR installed locally (see `scripts/download-core.sh`)

## Development Workflow

1. **Fork and clone** the repository.
2. **Create a feature branch** from `master`:
   ```bash
   git checkout -b feat/my-feature master
   ```
3. **Install the core dependency** if you haven't already:
   ```bash
   bash scripts/download-core.sh
   mvn install:install-file \
     -Dfile=libs/core-$(sed -n 's:.*<looksee-core.version>\(.*\)</looksee-core.version>.*:\1:p' pom.xml | head -n1).jar \
     -DgroupId=com.looksee -DartifactId=core \
     -Dversion=$(sed -n 's:.*<looksee-core.version>\(.*\)</looksee-core.version>.*:\1:p' pom.xml | head -n1) \
     -Dpackaging=jar
   ```
4. **Make your changes** and add or update tests as needed.
5. **Run the test suite** and confirm coverage:
   ```bash
   mvn test
   ```
   A JaCoCo coverage gate enforces a minimum 90 % line-coverage ratio. The HTML report is generated at `target/site/jacoco/index.html`.
6. **Commit** using the conventions below.
7. **Push** and open a pull request against `master`.

## Design by Contract

This project follows **Design by Contract** (DbC) principles. When adding or modifying code:

- Document **preconditions** (what callers must satisfy) in the Javadoc.
- Document **postconditions** (what the method guarantees) in the Javadoc.
- Document **class invariants** (properties that always hold) in the class-level Javadoc.
- Add `assert` statements for key invariants that should be checked at runtime during development.
- Run with `-ea` (enable assertions) during development and testing.

## Commit Message Format

We follow [Conventional Commits](https://www.conventionalcommits.org/). This enables automated versioning via [Semantic Release](https://github.com/semantic-release/semantic-release).

### Format

```
<type>(<optional scope>): <description>
```

### Types

| Type | Purpose |
|------|---------|
| `feat` | New feature (triggers a **minor** version bump) |
| `fix` | Bug fix (triggers a **patch** version bump) |
| `docs` | Documentation-only changes |
| `chore` | Routine maintenance (deps, CI config, etc.) |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `style` | Formatting, whitespace, or linting changes |
| `test` | Adding or updating tests |

### Examples

```
feat: add user login feature
fix(payment): resolve checkout bug
chore(deps): update Docker base image
test: add AuditController coverage for DOMAIN path
docs: update README with test instructions
```

### Guidelines

- Use the **present tense** ("add feature", not "added feature").
- Include the **issue number** when the commit relates to a tracked issue (e.g., `fix: resolve crash on empty URL (#42)`).
- Keep the subject line under **72 characters**.

## Testing

- **Unit tests** live under `src/test/java/` mirroring the main source tree.
- Tests use **JUnit 5** (`@Test`, `@ExtendWith`) and **Mockito** for mocking.
- Static utility methods are mocked using `MockedStatic`.
- Aim for **90 %+** line coverage on any code you add or change.

## Code Style

- Follow existing conventions in the codebase.
- Use meaningful variable names.
- Keep methods focused and short where possible.
- Add Javadoc with precondition/postcondition/invariant documentation for all public methods and classes.

## Pull Requests

- PRs are tested automatically via the `docker-ci-test.yml` GitHub Actions workflow.
- All tests must pass and the Docker build must succeed before merging.
- Provide a clear description of what your PR does and why.

## Reporting Issues

If you find a bug or have a feature request, please open a GitHub issue with:
- A clear title and description.
- Steps to reproduce (for bugs).
- Expected vs. actual behavior.
