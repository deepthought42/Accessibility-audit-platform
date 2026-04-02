# Accessibility-audit-platform

Monorepo for Look-see / accessibility audit services: APIs, workers, UI, extensions, and infrastructure.

Each top-level directory is a former standalone repository. Former `origin` URLs are listed in `LEGACY_REMOTES.txt` for reference.

## Packages (top-level folders)

| Directory | Role |
|-----------|------|
| `audit-service` | Audit service |
| `AuditManager` | Audit manager |
| `contentAudit` | Content audit |
| `CrawlerAPI` | Crawler API |
| `element-enrichment` | Element enrichment |
| `front-end-audit-broadcaster` | Front-end audit broadcaster |
| `informationArchitectureAudit` | Information architecture audit |
| `journey-map-cleanup` | Journey map cleanup |
| `journeyErrors` | Journey errors |
| `journeyExecutor` | Journey executor |
| `journeyExpander` | Journey expander |
| `look-see-api-gateway` | API gateway |
| `look-see-front-end-broadcaster` | Front-end broadcaster |
| `Look-see-UI-v3` | Web UI |
| `look-see-VSCode-plugin` | VS Code plugin |
| `LookseeChromeExtension` | Chrome extension |
| `LookseeCore` | Shared core library |
| `LookseeIaC` | Infrastructure as code |
| `page-audit-enrichment` | Page audit enrichment |
| `PageBuilder` | Page builder |
| `qa-testbed` | QA testbed |
| `visualDesignAudit` | Visual design audit |

## GitHub

Create a repository named `Accessibility-audit-platform`, then:

```bash
git remote add origin https://github.com/<org-or-user>/Accessibility-audit-platform.git
git push -u origin main
```

Individual project remotes in `LEGACY_REMOTES.txt` can be archived or left as read-only once this monorepo is canonical.
