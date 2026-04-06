# QA Testbed

Static HTML test pages used for QA and automated testing of the Look-see accessibility audit platform. These pages contain intentional accessibility issues and patterns for validating audit detection rules.

## Overview

The QA Testbed provides a collection of test fixture pages that exercise specific accessibility violations and edge cases. These pages are used to:

- Validate that audit services correctly detect known accessibility issues
- Regression-test audit rule changes against a stable set of fixtures
- Provide reproducible test scenarios for development and debugging

## Usage

### For Development

Point a local Look-see instance at these test pages to verify audit behavior:

1. Serve the test pages locally (e.g., `python3 -m http.server 8000` from this directory)
2. Start a page or domain audit against `http://localhost:8000/<test-page>.html`
3. Verify that the expected audit findings are reported

### For CI/CD

Test pages can be referenced in integration tests to validate audit rule correctness against known fixtures.

## Adding Test Pages

When adding new test pages:

1. Create an HTML file with intentional accessibility issues targeting specific audit rules
2. Document which audit rules the page is designed to test (via HTML comments or filename conventions)
3. Include both passing and failing examples for each rule to validate true positives and true negatives

## Related Services

- **[contentAudit](../contentAudit/)** -- Content accessibility audits (alt text, readability, paragraphing)
- **[visualDesignAudit](../visualDesignAudit/)** -- Visual design audits (color contrast, typography, imagery)
- **[informationArchitectureAudit](../informationArchitectureAudit/)** -- IA audits (headers, tables, forms, links, metadata)
