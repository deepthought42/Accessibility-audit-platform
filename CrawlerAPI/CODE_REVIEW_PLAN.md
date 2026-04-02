# Code Review Findings and Remediation Plan

## Review scope and method
- Ran a baseline build check with `mvn test -q`.
- Reviewed high-risk configuration and API/controller classes for reliability, security, and observability issues.

## Findings

### 1) Build is currently blocked by dependency resolution and local system dependency coupling (P0)
**Evidence**
- Build imports BOMs from Maven Central and fails before dependency versions are resolved.
- The project also uses a local `system` dependency (`libs/core-<version>.jar`) that is not present by default.

**Impact**
- CI/build reproducibility is broken.
- Local setup is fragile and environment-dependent.

**Fix plan**
1. Replace `system` dependency usage for `com.looksee:core` with one of:
   - private Maven repository artifact publication (preferred), or
   - explicit install step (`mvn install:install-file`) in setup scripts/CI.
2. Add a repository strategy section to README for Maven mirrors/proxy requirements where Central is restricted.
3. Add a preflight script/goal that validates required artifacts and fails with actionable guidance.

---

### 2) Exception handling leaks stack traces and can return `null` from API handlers (P1)
**Evidence**
- Multiple direct `printStackTrace()` calls in production code.
- `AuditRecordController#getElementIssueMap` catches generic exceptions and returns `null`.

**Impact**
- Sensitive details can leak into logs/stdout.
- API consumers may receive ambiguous responses or trigger downstream NPEs.

**Fix plan**
1. Replace `printStackTrace()` with structured `log.error("...", exception)` in all controllers/services.
2. Replace `null` returns in handlers with explicit `ResponseStatusException` or domain exceptions handled by `@ControllerAdvice`.
3. Add tests for error-path response codes and payloads.

---

### 3) Report export path appears to use incorrect identifier when resolving elements (P1)
**Evidence**
- In report generation loops, the code fetches elements using `ux_issue_service.getElement(audit_id)` while iterating issue messages.
- Debug output references `message.getId()`, suggesting mismatch between intended and actual lookup key.

**Impact**
- Wrong/missing element selectors in exported report data.
- Potentially skipped issues or misleading outputs.

**Fix plan**
1. Verify lookup contract for `getElement(...)` and use the correct identifier (`message.getId()` or dedicated element id).
2. Add focused unit/integration tests for issue-to-element mapping in both Excel and PDF export flows.
3. Remove debug `System.out.println` statements.

---

### 4) JWT validation appears incomplete (issuer/timestamp validated, audience missing) (P1)
**Evidence**
- Security config wires issuer + timestamp validators only.
- No explicit audience validator for API tokens.

**Impact**
- Tokens minted for a different audience might pass validation if issuer/timestamps are valid.

**Fix plan**
1. Add an audience validator tied to the expected API audience from configuration.
2. Add security tests for invalid-audience token rejection.
3. Document required Auth0 properties and expected claims.

---

### 5) Logging consistency and operational hygiene (P2)
**Evidence**
- Mixed usage of logger + stdout.
- Debug messages are present in production API paths.

**Impact**
- Inconsistent observability and noisy logs.

**Fix plan**
1. Standardize on SLF4J logging with proper levels (`debug/info/warn/error`).
2. Remove all direct stdout logging from runtime code.
3. Optionally add static analysis checks (Checkstyle/PMD/SpotBugs) to prevent regressions.

## Execution order
1. **Stabilize build/dependencies (P0)**.
2. **Fix error handling + null-return API behavior (P1)**.
3. **Correct report element lookup and add regression tests (P1)**.
4. **Harden JWT validation with audience checks (P1)**.
5. **Logging cleanup and static analysis guardrails (P2)**.

## Definition of done
- `mvn test` passes in CI and local dev setup.
- No `printStackTrace`/`System.out.println` in production source.
- Export endpoints validated with tests for correct issue-element mapping.
- JWT tests verify issuer, timestamp, and audience checks.
- Setup docs clearly describe dependency/bootstrap prerequisites.
