# Accessibility-audit-platform

Monorepo for Look-see / accessibility audit services: APIs, workers, UI, extensions, and infrastructure.

Each top-level directory is a former standalone repository. Former `origin` URLs are listed in `LEGACY_REMOTES.txt` for reference.

This migration uses a **single new history** at the root (previous per-repo commit histories are not included). To keep old SHAs, clone the URLs in `LEGACY_REMOTES.txt` into separate folders or use `git filter-repo` to replay histories under subdirectories.

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

---

## LookseeIaC GCP Deployment Configuration

This section documents every variable, secret, and property required to deploy the platform using the `LookseeIaC/GCP` Terraform package. All values are designed to be provided via `TF_VAR_` environment variables (e.g. GitHub Actions Secrets) so that no credentials file is ever committed to the repository.

### Table of Contents (Deployment)

1. [Required Terraform Variables](#required-terraform-variables)
2. [Optional Terraform Variables](#optional-terraform-variables)
3. [Container Images](#container-images)
4. [GCP APIs to Enable](#gcp-apis-to-enable)
5. [GCP Authentication](#gcp-authentication)
6. [GCP Secret Manager (auto-created)](#gcp-secret-manager-auto-created)
7. [PubSub Topics (auto-created)](#pubsub-topics-auto-created)
8. [Application-Level Properties (Spring Boot)](#application-level-properties-spring-boot)
9. [GitHub Actions Secrets Reference](#github-actions-secrets-reference)

---

### Required Terraform Variables

These have no defaults and **must** be provided. In GitHub Actions, set each as a repository secret named `TF_VAR_<variable>`.

#### GCP Project

| Variable | Type | Description |
|---|---|---|
| `project_id` | string | Your GCP project ID |
| `vpc_name` | string | Name for the VPC network |
| `subnet_cidr` | string | Subnet CIDR range (e.g. `10.0.0.0/24`) |
| `labels` | map(string) | Resource labels as JSON (e.g. `{"team":"eng","app":"looksee"}`) |

#### Neo4j Database

| Variable | Type | Description |
|---|---|---|
| `neo4j_password` | string (sensitive) | Neo4j admin password |
| `neo4j_username` | string | Neo4j username (e.g. `neo4j`) |
| `neo4j_db_name` | string | Neo4j database name (e.g. `neo4j`) |
| `neo4j_bolt_uri` | string | Neo4j bolt connection URI (e.g. `bolt://host:7687`) |

#### Auth0

| Variable | Type | Description |
|---|---|---|
| `auth0_client_id` | string (sensitive) | Auth0 application client ID |
| `auth0_client_secret` | string (sensitive) | Auth0 application client secret |
| `auth0_domain` | string (sensitive) | Auth0 tenant domain (e.g. `look-see.us.auth0.com`) |
| `auth0_audience` | string (sensitive) | Auth0 API audience identifier |

#### Pusher (real-time WebSocket messaging)

| Variable | Type | Description |
|---|---|---|
| `pusher_key` | string (sensitive) | Pusher API key |
| `pusher_app_id` | string | Pusher application ID |
| `pusher_cluster` | string | Pusher cluster region (e.g. `us2`) |
| `pusher_secret` | string (sensitive) | Pusher secret key |

#### SMTP (email)

| Variable | Type | Description |
|---|---|---|
| `smtp_username` | string (sensitive) | SMTP username |
| `smtp_password` | string (sensitive) | SMTP password |

---

### Optional Terraform Variables

These have sensible defaults. Override only if needed.

| Variable | Default | Description |
|---|---|---|
| `region` | `us-central1` | GCP region for all resources |
| `environment` | `dev` | Environment name (dev, prod, etc.) |
| `credentials_file` | `null` | Path to GCP SA JSON key file. Leave null to use ADC or Workload Identity Federation |
| `selenium_instance_count` | `1` | Number of Selenium Chrome browser instances to deploy |

---

### Container Images

All container image variables have defaults pointing to Docker Hub. Override to use a private registry or pin specific versions.

| Variable | Default Image |
|---|---|
| `page_builder_image` | `docker.io/deepthought42/page-builder:latest` |
| `api_image` | `docker.io/deepthought42/crawler-api:latest` |
| `audit_manager_image` | `docker.io/deepthought42/audit-manager:latest` |
| `audit_service_image` | `docker.io/deepthought42/audit-update-service:latest` |
| `journey_executor_image` | `docker.io/deepthought42/journey-executor:latest` |
| `journey_expander_image` | `docker.io/deepthought42/journey-expander:latest` |
| `content_audit_image` | `docker.io/deepthought42/content-audit:latest` |
| `visual_design_audit_image` | `docker.io/deepthought42/visual-design-audit:latest` |
| `information_architecture_audit_image` | `docker.io/deepthought42/information-architecture-audit:latest` |
| `selenium_image` | `docker.io/selenium/standalone-chrome:3.141.59` |

---

### GCP APIs to Enable

These APIs must be enabled on your GCP project before running `terraform apply`:

```bash
gcloud services enable \
  run.googleapis.com \
  pubsub.googleapis.com \
  secretmanager.googleapis.com \
  compute.googleapis.com \
  vpcaccess.googleapis.com \
  iam.googleapis.com \
  --project=${PROJECT_ID}
```

| API | Purpose |
|---|---|
| `run.googleapis.com` | Cloud Run services |
| `pubsub.googleapis.com` | Pub/Sub topics and subscriptions |
| `secretmanager.googleapis.com` | Secret Manager for credentials |
| `compute.googleapis.com` | VPC network and Neo4j VM |
| `vpcaccess.googleapis.com` | Serverless VPC Access connector |
| `iam.googleapis.com` | Service account creation and IAM bindings |

---

### GCP Authentication

Two options for authenticating Terraform with GCP:

1. **Workload Identity Federation (recommended for CI/CD)** -- Configure OIDC federation between GitHub Actions and GCP. No key files needed. Use the `google-github-actions/auth` action in your workflow.
2. **Service account key file** -- Set `TF_VAR_credentials_file` to the path of a JSON key file. Store the key contents as a GitHub Actions secret and write it to a file during the workflow.

The authenticated identity requires at minimum: **Project Editor**, **Secret Manager Admin**, **Pub/Sub Admin**, and **Service Account Admin** roles.

---

### GCP Secret Manager (auto-created)

Terraform automatically creates these secrets in GCP Secret Manager and wires them as environment variables into the Cloud Run services:

| Secret ID | Source Variable | Used By |
|---|---|---|
| `neo4j-password` | `neo4j_password` | All Cloud Run services |
| `neo4j-username` | `neo4j_username` | All Cloud Run services |
| `neo4j-db-name` | `neo4j_db_name` | All Cloud Run services |
| `neo4j-bolt-uri` | (output from neo4j-db module) | All Cloud Run services |
| `pusher-key` | `pusher_key` | audit-service |
| `pusher-app-id` | `pusher_app_id` | audit-service |
| `pusher-cluster` | `pusher_cluster` | audit-service |
| `pusher-secret` | `pusher_secret` | audit-service |
| `smtp-username` | `smtp_username` | (secrets module) |
| `smtp-password` | `smtp_password` | (secrets module) |

---

### PubSub Topics (auto-created)

Terraform creates these Pub/Sub topics and wires them to Cloud Run services via push subscriptions:

| Topic Name | Spring Property | Services |
|---|---|---|
| `url` | `pubsub.url_topic` | API (publisher), Page Builder (subscriber) |
| `page_created` | `pubsub.page_built` | Page Builder (publisher), Audit Manager (subscriber) |
| `page_audit` | `pubsub.page_audit_topic` | Audit Manager (publisher), Audit Service / Content Audit / Visual Design Audit / Info Architecture Audit (subscribers) |
| `journey_verified` | `pubsub.journey_verified` | Page Builder (publisher), Journey Expander (subscriber) |
| `journey_discarded` | `pubsub.discarded_journey_topic` | API, Journey Executor |
| `audit_error` | `pubsub.error_topic` | All services (publisher) |
| `audit_update` | `pubsub.audit_update` | Audit Manager, Audit Service, Content Audit, Visual Design Audit, Info Architecture Audit |
| `journey_candidate` | `pubsub.journey_candidate` | Journey Expander (publisher), Journey Executor (subscriber) |
| `journey_completion_cleanup` | — | (topic created, not yet wired) |

---

### Application-Level Properties (Spring Boot)

These properties are set in each service's `application.properties` / `application.yml` files. Most are wired automatically by Terraform, but the following are **not currently managed by Terraform** and may need manual configuration or additional secrets if used in production:

| Property | Service(s) | Notes |
|---|---|---|
| `stripe.secretKey` | CrawlerAPI | Stripe payment API key |
| `stripe.agency_basic_price_id` | CrawlerAPI | Stripe price ID |
| `stripe.agency_pro_price_id` | CrawlerAPI | Stripe price ID |
| `stripe.company_basic_price_id` | CrawlerAPI | Stripe price ID |
| `stripe.company_pro_price_id` | CrawlerAPI | Stripe price ID |
| `stripe.checkout_success_url` | CrawlerAPI | Checkout redirect URL |
| `stripe.checkout_cancel_url` | CrawlerAPI | Checkout redirect URL |
| `segment.analytics.writeKey` | CrawlerAPI | Segment analytics write key |
| `gcp.api.key` | PageBuilder | Google Cloud Vision API key |
| `integrations.encryption.key` | CrawlerAPI | AES-GCM encryption key for integration configs |
| `spring.mail.host` | CrawlerAPI | SMTP host (only user/pass are in Terraform) |
| `spring.sendgrid.api-key` | CrawlerAPI | SendGrid API key (if used instead of SMTP) |

#### Properties Wired Automatically by Terraform

These are injected into Cloud Run containers as environment variables or Secret Manager references:

| Property | How It's Set |
|---|---|
| `spring.data.neo4j.uri` | Secret Manager reference |
| `spring.data.neo4j.username` | Secret Manager reference |
| `spring.data.neo4j.password` | Secret Manager reference |
| `spring.data.neo4j.database` | Secret Manager reference |
| `spring.cloud.gcp.project-id` | Environment variable from `project_id` |
| `spring.cloud.gcp.region` | Environment variable from `region` |
| `pusher.key` | Secret Manager reference (audit-service only) |
| `pusher.appId` | Secret Manager reference (audit-service only) |
| `pusher.cluster` | Secret Manager reference (audit-service only) |
| `pusher.secret` | Secret Manager reference (audit-service only) |
| `pubsub.*` topics | Environment variables from Pub/Sub module outputs |
| `SELENIUM_URLS` | Environment variable from Selenium module outputs |

---

### GitHub Actions Secrets Reference

Below is the complete list of GitHub Actions secrets to configure. Each secret maps to a `TF_VAR_` environment variable in your Terraform workflow.

#### Required Secrets

| GitHub Secret Name | Maps To | Example Value |
|---|---|---|
| `TF_VAR_PROJECT_ID` | `TF_VAR_project_id` | `my-gcp-project-123` |
| `TF_VAR_VPC_NAME` | `TF_VAR_vpc_name` | `looksee-vpc` |
| `TF_VAR_SUBNET_CIDR` | `TF_VAR_subnet_cidr` | `10.0.0.0/24` |
| `TF_VAR_LABELS` | `TF_VAR_labels` | `{"team":"platform","app":"looksee"}` |
| `TF_VAR_NEO4J_PASSWORD` | `TF_VAR_neo4j_password` | (sensitive) |
| `TF_VAR_NEO4J_USERNAME` | `TF_VAR_neo4j_username` | `neo4j` |
| `TF_VAR_NEO4J_DB_NAME` | `TF_VAR_neo4j_db_name` | `neo4j` |
| `TF_VAR_NEO4J_BOLT_URI` | `TF_VAR_neo4j_bolt_uri` | `bolt://10.0.0.5:7687` |
| `TF_VAR_AUTH0_CLIENT_ID` | `TF_VAR_auth0_client_id` | (sensitive) |
| `TF_VAR_AUTH0_CLIENT_SECRET` | `TF_VAR_auth0_client_secret` | (sensitive) |
| `TF_VAR_AUTH0_DOMAIN` | `TF_VAR_auth0_domain` | `look-see.us.auth0.com` |
| `TF_VAR_AUTH0_AUDIENCE` | `TF_VAR_auth0_audience` | `https://api.look-see.com` |
| `TF_VAR_PUSHER_KEY` | `TF_VAR_pusher_key` | (sensitive) |
| `TF_VAR_PUSHER_APP_ID` | `TF_VAR_pusher_app_id` | `1149968` |
| `TF_VAR_PUSHER_CLUSTER` | `TF_VAR_pusher_cluster` | `us2` |
| `TF_VAR_PUSHER_SECRET` | `TF_VAR_pusher_secret` | (sensitive) |
| `TF_VAR_SMTP_USERNAME` | `TF_VAR_smtp_username` | (sensitive) |
| `TF_VAR_SMTP_PASSWORD` | `TF_VAR_smtp_password` | (sensitive) |

#### GCP Authentication Secrets (pick one approach)

| GitHub Secret Name | Purpose |
|---|---|
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | Workload Identity Federation provider (recommended) |
| `GCP_SERVICE_ACCOUNT_EMAIL` | Service account email for WIF |
| *or* `GCP_SA_KEY` | Base64-encoded service account JSON key (alternative) |

#### Docker Hub Secrets (for CI image push)

| GitHub Secret Name | Purpose |
|---|---|
| `DOCKER_USERNAME` | Docker Hub username (already configured in existing workflows) |
| `DOCKER_PASSWORD` | Docker Hub password/token (already configured in existing workflows) |

#### Example GitHub Actions Workflow Snippet

```yaml
jobs:
  terraform:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: LookseeIaC/GCP
    steps:
      - uses: actions/checkout@v4

      - name: Authenticate to GCP
        uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: ${{ secrets.GCP_WORKLOAD_IDENTITY_PROVIDER }}
          service_account: ${{ secrets.GCP_SERVICE_ACCOUNT_EMAIL }}

      - uses: hashicorp/setup-terraform@v3

      - name: Terraform Init
        run: terraform init

      - name: Terraform Plan
        run: terraform plan -out=tf.plan
        env:
          TF_VAR_project_id: ${{ secrets.TF_VAR_PROJECT_ID }}
          TF_VAR_region: "us-central1"
          TF_VAR_environment: "dev"
          TF_VAR_vpc_name: ${{ secrets.TF_VAR_VPC_NAME }}
          TF_VAR_subnet_cidr: ${{ secrets.TF_VAR_SUBNET_CIDR }}
          TF_VAR_labels: ${{ secrets.TF_VAR_LABELS }}
          TF_VAR_neo4j_password: ${{ secrets.TF_VAR_NEO4J_PASSWORD }}
          TF_VAR_neo4j_username: ${{ secrets.TF_VAR_NEO4J_USERNAME }}
          TF_VAR_neo4j_db_name: ${{ secrets.TF_VAR_NEO4J_DB_NAME }}
          TF_VAR_neo4j_bolt_uri: ${{ secrets.TF_VAR_NEO4J_BOLT_URI }}
          TF_VAR_auth0_client_id: ${{ secrets.TF_VAR_AUTH0_CLIENT_ID }}
          TF_VAR_auth0_client_secret: ${{ secrets.TF_VAR_AUTH0_CLIENT_SECRET }}
          TF_VAR_auth0_domain: ${{ secrets.TF_VAR_AUTH0_DOMAIN }}
          TF_VAR_auth0_audience: ${{ secrets.TF_VAR_AUTH0_AUDIENCE }}
          TF_VAR_pusher_key: ${{ secrets.TF_VAR_PUSHER_KEY }}
          TF_VAR_pusher_app_id: ${{ secrets.TF_VAR_PUSHER_APP_ID }}
          TF_VAR_pusher_cluster: ${{ secrets.TF_VAR_PUSHER_CLUSTER }}
          TF_VAR_pusher_secret: ${{ secrets.TF_VAR_PUSHER_SECRET }}
          TF_VAR_smtp_username: ${{ secrets.TF_VAR_SMTP_USERNAME }}
          TF_VAR_smtp_password: ${{ secrets.TF_VAR_SMTP_PASSWORD }}

      - name: Terraform Apply
        if: github.ref == 'refs/heads/main'
        run: terraform apply tf.plan
```

---

### Terraform Backend (Remote State)

The GCS backend is configured in `LookseeIaC/GCP/versions.tf` but requires backend config values at init time (no bucket names are committed to the repo). Provide them via `-backend-config` flags or a backend config file:

```bash
terraform init \
  -backend-config="bucket=my-tf-state-bucket" \
  -backend-config="prefix=looksee/dev"
```

In GitHub Actions, set the `TF_STATE_BUCKET` secret and pass it during init:

```yaml
env:
  TF_CLI_ARGS_init: "-backend-config=bucket=${{ secrets.TF_STATE_BUCKET }} -backend-config=prefix=looksee/${{ github.ref_name }}"
```

### Optional Secrets (Gap 3 Variables)

The following variables are optional (default to empty string). When provided, Terraform creates the corresponding Secret Manager entries and wires them into the API and/or Page Builder Cloud Run services:

| GitHub Secret Name | Terraform Variable | Wired To |
|---|---|---|
| `TF_VAR_GCP_API_KEY` | `gcp_api_key` | API, Page Builder (`gcp.api.key`) |
| `TF_VAR_INTEGRATIONS_ENCRYPTION_KEY` | `integrations_encryption_key` | API (`integrations.encryption.key`) |
| `TF_VAR_SMTP_HOST` | `smtp_host` | API (`spring.mail.host`) |
