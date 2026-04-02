# Contribution Guidelines

## Commit Message Format
We follow [Conventional Commits](https://www.conventionalcommits.org/).

**Format:**

## Examples:

feat: add user login feature
fix(payment): resolve checkout bug
chore(deps): update Docker base image


**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `chore`: Routine maintenance
- `refactor`: Code improvements
- `style`: Code style changes
- `test`: Add/update tests

When writing a commit message, please follow the following guidelines:
- Use the present tense.
- Include the issue number in the commit message if it is related to an issue.

## Design by Contract

This project follows **Design by Contract (DbC)** principles. All contributions must maintain these contracts:

### Preconditions

- Use `Objects.requireNonNull(param, "param must not be null")` for null checks on public and internal method parameters.
- Use `IllegalArgumentException` for value constraints (e.g., empty strings, out-of-range values).
- Do **not** use `assert` for preconditions, as assertions can be disabled at runtime.

### Postconditions

- Validate return values before returning from methods that construct domain objects.
- For `execute()` methods, always verify the `Audit` object is non-null before persisting.

### Class Invariants

- Document the class invariant in the class-level Javadoc, listing all `@Autowired` dependencies that must be non-null after construction.

### Javadoc

- Use `@pre` tags to document preconditions.
- Use `@post` tags to document postconditions.
- Every public method must have a Javadoc comment with its contract.

### Example

```java
/**
 * Audits form structure for WCAG compliance.
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies are non-null
 * after Spring construction.</p>
 */
@Component
public class FormStructureAudit implements IExecutablePageStateAudit {

    /**
     * {@inheritDoc}
     *
     * @pre {@code page_state != null}
     * @pre {@code audit_record != null}
     * @post returned {@code Audit} is non-null and persisted
     */
    @Override
    public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
        Objects.requireNonNull(page_state, "page_state must not be null");
        Objects.requireNonNull(audit_record, "audit_record must not be null");

        // ... audit logic ...

        Objects.requireNonNull(audit, "Postcondition failed: audit must not be null");
        return auditService.save(audit);
    }
}
```
