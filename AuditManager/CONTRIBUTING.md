# Contribution Guidelines

## Commit Message Format

We follow [Conventional Commits](https://www.conventionalcommits.org/).

**Format:** `<type>(<optional scope>): <description>`

### Examples

```
feat: add user login feature
fix(payment): resolve checkout bug
chore(deps): update Docker base image
```

### Types

- `feat` -- New feature
- `fix` -- Bug fix
- `docs` -- Documentation only
- `chore` -- Routine maintenance (dependencies, CI, configs)
- `refactor` -- Code change that neither fixes a bug nor adds a feature
- `style` -- Formatting, whitespace, or other non-functional changes
- `test` -- Add or update tests

### Guidelines

- Use the present tense ("add feature", not "added feature").
- Reference the issue number when applicable (e.g., `fix(auth): handle expired tokens (#42)`).

## Pull Request Process

1. Fork the repository.
2. Create a feature branch from `master`.
3. Make your changes and add tests for new functionality.
4. Ensure all tests pass (`mvn test`).
5. Submit a pull request targeting `master`.
