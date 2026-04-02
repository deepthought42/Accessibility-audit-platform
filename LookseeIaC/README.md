# LookseeIaC

> *Infrastructure that lets Look‑see run circles around inaccessible websites.*

---

## Table of Contents

1. [What's in the Box](#whats-in-the-box)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Required Google Cloud APIs](#required-google-cloud-apis)
5. [Getting Started](#getting-started)
6. [Configuration](#configuration)
7. [Module Overview](#module-overview)
8. [Example Usage](#example-usage)
9. [Project Structure](#project-structure)
10. [Local Dev & CI/CD](#local-dev--cicd)
11. [Security & Secrets](#security--secrets)
12. [Code Review Findings & Proposed Fixes](#code-review-findings--proposed-fixes)
13. [Neo4j Schema Management](#neo4j-schema-management)
14. [Cleaning Up](#cleaning-up)
15. [Contributing](#contributing)
16. [License](#license)

---

## What's in the Box

This repo is a **Terraform mono‑module** that spins up every Google Cloud resource Look‑see needs to crawl a site, run automated accessibility audits, and stash the results in a Neo4j graph database. In less time than it takes to microwave leftover pizza you'll get:

* A private **Neo4j Community** instance (or the URI of an existing one) (see [Module Overview](#module-overview) and [Example Usage](#example-usage) for details).
* A fleet of **Cloud Run services** (crawler, auditor, notifier, and a tiny API gateway) (see [Module Overview](#module-overview)).
* **Pub/Sub topics** that glue the micro‑services together (see [Module Overview](#module-overview)).
* **Secret Manager** entries for DB creds, SMTP, and Pusher keys (see [Module Overview](#module-overview)).
* **Service accounts** with minimum‑viable IAM roles.
* **Cloud Build** pipelines that build and deploy container images on every tag push.

*If you're curious how the pieces dance together, jump to the 60‑second tour below.*

---

## Architecture

Most workloads are serverless (Cloud Run + Pub/Sub), with one stateful Compute Engine VM for Neo4j when you do not point at an external Aura instance.

---

## Prerequisites

* **Google Cloud account** with billing enabled.
* **Terraform 1.6+** – any newer release is fine.
* **Google Cloud SDK** (`gcloud`) authenticated against your project.
* **Docker** – Cloud Build can build for you, but local Docker makes debugging faster.
* **Node 18+** – only needed if you hack on the micro‑services before pushing.
* **A Neo4j Aura instance** (free tier works) or credentials to an existing cluster.
* Optional: a service-account JSON key path for `TF_VAR_credentials_file` (if you are not using Application Default Credentials).

> *Need a refresher on CI/CD? Peek at my AWS blog post "Anatomy of a CI/CD Pipeline" for extra context.*

---

## Required Google Cloud APIs

Enable these once per project (replace `${PROJECT_ID}`):

```bash
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  pubsub.googleapis.com \
  secretmanager.googleapis.com \
  compute.googleapis.com \
  --project=${PROJECT_ID}
```

---

## Getting Started

```bash
# 1. Clone and enter the repo
$ git clone https://github.com/deepthought42/LookseeIaC.git
$ cd LookseeIaC

# 2. Export the bare‑minimum variables
$ export TF_VAR_project_id="my‑gcp‑proj"
$ export TF_VAR_region="us‑central1"
$ export TF_VAR_environment="dev"

# 3. Export your secrets (see next section)
$ export TF_VAR_neo4j_username=neo4j
$ export TF_VAR_neo4j_password="super‑secret"
$ export TF_VAR_neo4j_bolt_uri="bolt+s://xxxxx.databases.neo4j.io"
$ export TF_VAR_neo4j_db_name="neo4j"
$ export TF_VAR_smtp_username="postmaster@example.com"
$ export TF_VAR_smtp_password="s3ndM3Mail$"
$ export TF_VAR_pusher_key="app-key"
$ export TF_VAR_pusher_app_id="app-id"
$ export TF_VAR_pusher_cluster="us3"

# 4. Initialise and deploy
$ terraform init
$ terraform plan -out=tf.plan
$ terraform apply tf.plan
```

The first run takes about 5‑7 minutes while Cloud Build cooks the container images.

When `terraform apply` finishes it prints the public HTTPS URL of the API gateway and crawler service. Hit `/healthz` on either endpoint to confirm they're alive.

---

## Configuration

All tunables live in **`variables.tf`**. You can override them via `terraform.tfvars` or environment variables.

### Root Variables

> `credentials_file` is optional; if unset, the Google provider uses ADC from your local `gcloud auth application-default login` context.

| Variable                             | Description                      | Default       |
| ------------------------------------ | -------------------------------- | ------------- |
| `project_id`                         | GCP project to deploy into       | –             |
| `region`                             | Primary region for all resources | `us‑central1` |
| `environment`                        | Tag applied to every resource    | `dev`         |
| `neo4j_bolt_uri`                     | BOLT URI to your Aura instance   | –             |
| `neo4j_username` / `neo4j_password`  | DB creds                         | –             |
| `smtp_username` / `smtp_password`    | SMTP creds for outbound email    | –             |
| `pusher_app_id`                      | Pusher application ID            | –             |
| `pusher_key`                         | Pusher key (sensitive)           | –             |
| `pusher_cluster`                     | Pusher cluster (sensitive)       | –             |
| `pusher_secret`                      | Pusher secret (sensitive)        | –             |

> **Heads‑up.** Changing `environment` triggers *new* resources rather than updating in place. Handy for blue‑green swaps, but noisy if you do it by accident.

### Module Variables

Below is a summary of the variables for each module. (For full details, refer to the module's `variables.tf`.)

#### neo4j-db Module

| Variable                | Description                                                      | Default                       |
|-------------------------|------------------------------------------------------------------|-------------------------------|
| `project_id`            | The project ID                                                   | –                             |
| `region`                | The region                                                       | "us-central1"                 |
| `zone`                  | The zone                                                         | "us-central1-a"               |
| `vpc_network_name`      | The name of the VPC network                                      | –                             |
| `subnet_name`           | The name of the subnet                                           | –                             |
| `machine_type`          | The machine type                                                 | "e2-medium"                   |
| `image`                 | The image (e.g., "ubuntu-os-cloud/ubuntu-2204-lts")              | –                             |
| `disk_size`             | The disk size (in GB)                                            | 20                            |
| `disk_type`             | The disk type                                                    | "pd-ssd"                      |
| `source_ranges`         | The source ranges (CIDR blocks) for firewall rules               | ["10.0.0.0/8", "0.0.0.0/0"]   |
| `target_tags`           | The target tags (e.g., ["iap-tunnel"])                           | ["iap-tunnel"]                |
| `tags`                  | The tags (e.g., ["neo4j"])                                       | ["neo4j"]                     |
| `neo4j_password`        | Initial password for the neo4j admin user (sensitive)            | –                             |
| `neo4j_username`        | The username for the neo4j user (default "neo4j")                | –                             |
| `neo4j_db_name`         | The name of the neo4j database                                   | –                             |
| `service_account_email` | The service account email (used for IAM)                         | –                             |
| `environment`           | The environment (e.g., "dev", "prod")                            | –                             |

#### cloud_run Module

| Variable                  | Description (from variables.tf)                                                           | Default (if any)  |
|---------------------------|-------------------------------------------------------------------------------------------|-------------------|
| `project_id`                | The ID of the project where resources will be created                                     | –                 |
| `region`                    | The region where resources will be created                                                | –                 |
| `environment`               | The environment (dev, prod, etc)                                                          | –                 |
| `service_name`              | Name of the Cloud Run service                                                             | –                 |
| `image`                     | Container image to deploy                                                                 | –                 |
| `pubsub_topics`             | Map of PubSub topic names to publish messages to (map(string))                            | {}                |
| `service_account_email`     | Email of the service account for the Cloud Run service                                    | –                 |
| `labels`                    | Environment labels (map(string))                                                          | –                 |
| `topic_id`                  | The ID of the PubSub topic to subscribe to                                                | –                 |
| `vpc_connector_name`        | The name of the VPC connector to use                                                      | –                 |
| `port`                      | The port to run the service on                                                            | 8080              |
| `memory_allocation`         | Memory allocated for cloud run (e.g., "500M")                                             | "500M"            |
| `cpu_allocation`            | CPU allocation for cloud run (e.g., "1")                                                  | "1"               |
| `memory_limit`              | Memory limit for cloud run (e.g., "4Gi")                                                  | "4Gi"             |
| `cpu_limit`                 | CPU limit for cloud run (e.g., "2")                                                       | "2"               |
| `environment_variables`     | Map of environment variables (map(list(string))) (for secrets, use Secret Manager)        | {}                |
| `vpc_egress`                | The egress of the VPC connector (e.g., "all‑traffic")                                     | "all‑traffic"     |

#### pubsub/pubsub_topics Module

| Variable                               | Description                                             | Default        |
|----------------------------------------|---------------------------------------------------------|----------------|
| `project_id`                           | The ID of the project                                   | –              |
| `region`                               | The region of the project                               | –              |
| `labels`                               | A map of labels to apply to the topic (map(string))     | –              |
| `service_account_email`                | The email of the service account to use for the topic   | –              |
| `url_topic_name`                       | The name of the URL PubSub topic                        | –              |
| `page_created_topic_name`              | The name of the Page Created PubSub topic               | –              |
| `page_audit_topic_name`                | The name of the Page Audit PubSub topic                 | –              |
| `journey_verified_topic_name`          | The name of the Journey Verified PubSub topic           | –              |
| `journey_discarded_topic_name`         | The name of the Journey Discarded PubSub topic          | –              |
| `journey_candidate_topic_name`         | The name of the Journey Candidate PubSub topic          | –              |
| `audit_update_topic_name`              | The name of the Audit Update PubSub topic               | –              |
| `journey_completion_cleanup_topic_name`| The name of the Journey Completion Cleanup PubSub topic | –              |
| `audit_error_topic_name`               | The name of the Audit Error PubSub topic                | –              |

#### pubsub/push_subscription Module

| Variable                | Description                                                                 | Default           |
|-------------------------|-----------------------------------------------------------------------------|-------------------|
| `project_id`            | The project ID where the subscription will be created                       | –                 |
| `region`                | The region of the project                                                   | –                 |
| `environment`           | Environment (dev, prod, etc)                                                | "dev"             |
| `service_name`          | Name of the service                                                         | –                 |
| `subscription_name`     | Name of the PubSub subscription                                             | –                 |
| `topic_id`              | The ID of the PubSub topic to subscribe to                                  | –                 |
| `push_endpoint`         | The URL of the Cloud Run service that will receive messages                 | –                 |
| `service_account_email` | The email of the service account used for authentication                    | –                 |

#### vpc Module

| Variable             | Description                                                        | Default           |
|----------------------|--------------------------------------------------------------------|-------------------|
| `project_id`         | The ID of the GCP project                                          | –                 |
| `region`             | The region where resources will be created                         | "us-central1"     |
| `environment`        | The environment (dev, prod, etc)                                   | –                 |
| `vpc_name`           | Name of the VPC network                                            | "custom-vpc"      |
| `labels`             | Environment labels (map(string))                                   | –                 |
| `subnet_name`        | Name of the subnet                                                 | "private-subnet-1"|
| `subnet_cidr`        | CIDR range for the subnet                                          | "10.0.0.0/24"     |
| `ssh_source_ranges`  | CIDR ranges for standard SSH firewall (external) (list(string))    | –                 |
| `iap_ssh_iam_member` | IAM member for IAP SSH firewall                                    | "" (empty)        |

#### secrets Module

| Variable                | Description                                         | Default         |
|-------------------------|-----------------------------------------------------|-----------------|
| `project_id`            | The GCP project ID                                  | –               |
| `environment`           | The environment                                     | –               |
| `service_account_email` | Service account email                               | –               |
| `neo4j_password`        | Password for Neo4j database (sensitive)             | –               |
| `neo4j_username`        | Username for Neo4j database                         | "neo4j"         |
| `neo4j_db_name`         | Database name for Neo4j                             | "neo4j"         |
| `pusher_app_id`         | Pusher application ID                               | "1149968"       |
| `pusher_key`            | Pusher key (sensitive)                              | –               |
| `pusher_cluster`        | Pusher cluster (sensitive)                          | –               |
| `pusher_secret`         | Pusher secret (sensitive)                           | –               |
| `smtp_password`         | SMTP password (sensitive)                           | –               |
| `smtp_username`         | SMTP username (sensitive)                           | –               |

#### security/service_perimeter Module

| Variable                    | Description                                                                    | Default      |
|-----------------------------|--------------------------------------------------------------------------------|--------------|
| `access_policy_id`          | The ID of the access policy this perimeter belongs to                          | –            |
| `perimeter_name`            | Name of the service perimeter                                                  | –            |
| `project_numbers`           | List of project numbers to include in the perimeter (list(string))             | "" (empty)   |
| `restricted_services`       | List of GCP services to restrict (list(string))                                | "" (empty)   |
| `allowed_services`          | List of GCP services to allow within the VPC (list(string))                    | "" (empty)   |
| `access_level_names`        | List of access level resource names to include in the perimeter (list(string)) | "" (empty)   |
| `use_explicit_dry_run_spec` | Whether to use explicit dry-run spec (bool)                                    | false        |
| `labels`                    | A map of labels to apply to the service perimeter (map(string))                | "" (empty)   |
| `environment`               | Environment identifier (e.g., prod, staging, dev)                              | –            |
| `service_name`              | Name of the service or application this perimeter protects                     | –            |
| `perimeter_type`            | Type of perimeter ("regular" or "bridge")                                      | "" (empty)   |

#### API Module

| Variable                  | Description                                                        | Default                                               |
|---------------------------|--------------------------------------------------------------------|-------------------------------------------------------|
| `project_id`              | The GCP project ID                                                 | –                                                     |
| `region`                  | The region to deploy to                                            | –                                                     |
| `environment`             | Environment (prod, dev, etc)                                       | –                                                     |
| `image`                   | Container image to deploy                                          | "docker.io/deepthought42/crawler‑api:latest"          |
| `service_name`            | Name of the Cloud Run service                                      | "api"                                                 |
| `service_account_email`   | The email address of the service account to use for Cloud Run      | "service-account@project-id.iam.gserviceaccount.com"  |
| `vpc_connector_name`      | Name of the VPC connector                                          | "vpc-connector"                                       |
| `port`                    | The port to run the service on                                     | 9080                                                  |
| `url_topic_name`          | Name of the URL topic                                              | –                                                     |
| `neo4j_password_secret`   | Name of the Neo4j password secret                                  | "neo4j-password"                                      |
| `neo4j_username_secret`   | Name of the Neo4j username secret                                  | "neo4j-username"                                      |
| `neo4j_bolt_uri_secret`   | Name of the Neo4j bolt URI secret                                  | "neo4j-bolt-uri"                                      |
| `neo4j_db_name_secret`    | Name of the Neo4j database name secret                             | "neo4j-db-name"                                       |
| `pubsub_topics`           | Pubsub topics (map(string))                                        | –                                                     |
| `memory_allocation`       | Memory allocated for cloud run instance                            | –                                                     |

---

## Module Overview

- **modules/neo4j‑db**: Provisions a Neo4j Community Edition VM (using a startup script to install Neo4j, set an initial password, create a database, and run a CQL schema file (e.g., "create‑indexes‑and‑constraints.cql") at startup). Access is controlled via firewall rules (e.g., SSH via IAP) and VPC.
- **modules/cloud_run**: Deploys Cloud Run services (with VPC connector, Pub/Sub integration, and secret management) for micro‑services (page builder, journey expander, audit manager, audit service, journey executor, content audit, visual design audit, information architecture audit).
- **modules/pubsub**: Manages Pub/Sub topics (e.g., "url", "page‑created", "page‑audit", "journey‑verified", "journey‑discarded", "journey‑candidate", "audit‑update", "journey‑completion‑cleanup", "audit‑error") and push subscriptions (for Cloud Run endpoints).
- **modules/vpc**: Sets up a VPC (with a custom subnet) and firewall rules (e.g., SSH via IAP, internal traffic for Neo4j, etc.).
- **modules/secrets**: Manages Secret Manager entries (for Neo4j, SMTP, Pusher, etc.) so that sensitive data is never stored in Terraform state.
- **modules/security/service_perimeter**: (Optional) Configures a VPC Service Controls perimeter (e.g., "regular" or "bridge") to restrict access to GCP services.
- **modules/api**: Deploys the API gateway (Cloud Run) for Looksee services.

---

## Example Usage

### Deploying Neo4j Community

Below is an example Terraform snippet for deploying a Neo4j Community instance using the `neo4j‑db` module. (Adjust variables as needed.)

```hcl
module "neo4j_db" {
  source             = "./modules/neo4j‑db"
  project_id         = var.project_id
  region             = var.region
  zone               = var.zone
  vpc_network_name   = var.vpc_network_name
  machine_type       = "e2‑medium"
  image              = "ubuntu‑os‑cloud/ubuntu‑2204‑lts"
  disk_size          = 20
  tags               = ["neo4j"]
  neo4j_username     = "neo4j"
  neo4j_password     = var.neo4j_password
  neo4j_db_name      = "looksee"
  service_account_email = google_service_account.neo4j_instance_sa.email
  environment        = var.environment
}
```

**Notes:**
- The startup script (e.g., "install‑neo4j.sh") (or inline metadata_startup_script) downloads (e.g., via curl) a CQL file (e.g., "create‑indexes‑and‑constraints.cql") from your repository (or a public URL) and runs it (using `neo4j‑admin run`) to set up indexes and constraints.
- SSH access is enabled (via IAP) by default (using a firewall rule that allows TCP port 22 from Google Cloud IAP (`35.235.240.0/20`)).
- (Optional) You can also deploy a Cloud Run service (using the `cloud_run` module) that connects to this Neo4j instance.

---

## Project Structure

- **create‑indexes‑and‑constraints.cql**: (Located in the root folder) A CQL file (executed on the Neo4j VM) to create indexes and constraints.
- **variables.tf**: (Root) Defines variables (e.g., project_id, region, environment, secrets) for the entire project.
- **modules.tf**: (Root) Calls the Terraform modules (e.g., "neo4j‑db", "cloud_run", "pubsub", "vpc", "secrets", "api").
- **terraform.tfvars**: (Root) (Optional) Override variables (e.g., project_id, region, environment, secrets) for local or CI/CD use.
- **iam.tf**: (Root) (Optional) Defines IAM bindings (e.g., for service accounts).

---

## Local Dev & CI/CD

- **Local smoke test:** Each service (e.g., crawler, auditor, notifier, API) has a `make dev` (or equivalent) that builds and runs the container with mock environment variables.
- **CI/CD:** Pushing a tag (e.g., "v1.2.3") triggers Cloud Build (which builds the Docker image, pushes to Artifact Registry, and re‑deploys the Cloud Run service). (The pipeline YAML lives beside each service so you can tweak build flags without touching Terraform.)

For a deeper dive into semantic versioning in pipelines, check my **"Ship It Like SemVer"** AWS blog post.

---

## Security & Secrets

- Secrets (e.g., Neo4j password, SMTP, Pusher keys) are **never** baked into Terraform state. Everything sensitive is stored in Secret Manager (and referenced by resource ID).
- Every Cloud Run service owns a dedicated **service account** (with the smallest IAM role needed (e.g., Pub/Sub publisher or subscriber)).
- Firewall rules (e.g., for SSH (via IAP), internal traffic (for Neo4j), etc.) are configured (e.g., via the `vpc` module) to restrict access.
- (Optional) A VPC Service Controls perimeter (e.g., "regular" or "bridge") (via the `security/service_perimeter` module) can be used to restrict access to GCP services.

---


## Code Review Findings & Proposed Fixes

This section summarizes issues found during a manual review and the recommended remediation path.

1. **Hard-coded provider credential path (fixed).**
   *Issue:* a user-specific absolute path was previously hard-coded in the Google provider block.
   *Fix applied:* provider now uses optional `credentials_file` input and otherwise falls back to ADC.

2. **Terraform state backup tracked in git (fixed).**
   *Issue:* `GCP/terraform.tfstate.backup` was committed, which can leak resource metadata and secret references.
   *Fix applied:* removed tracked backup file and expanded `.gitignore` with `*.tfstate*` patterns.

3. **Public API invoker policy (recommended follow-up).**
   *Issue:* `google_cloud_run_service_iam_member.public_access` grants `roles/run.invoker` to `allUsers` for the API module.
   *Proposed fix:* restrict invokers to authenticated identities (IAP, Auth0-aware gateway, or a dedicated service account) and add an explicit toggle variable such as `allow_unauthenticated_api = false` by default.

## Neo4j Schema Management

The `neo4j‑db` module (using a startup script (e.g., "install‑neo4j.sh")) downloads a CQL file (e.g., "create‑indexes‑and‑constraints.cql") (from your repository (or a public URL)) and runs it (using `neo4j‑admin run`) at startup. Update the file (or its URL) to change the schema (e.g., create indexes and constraints).

---

## Cleaning Up

```bash
terraform destroy
```

Terraform tears down every resource (including Artifact Registry images). (Neo4j Aura lives outside GCP, so kill that instance from the Neo4j console if you spun up a temporary cluster.)

---

## Contributing

Pull requests are welcome. Open an issue for big changes so we can chat about the design first. All code is formatted with **`terraform fmt`** and validated by **`tflint`** on push.

---

## License

Apache‑2.0. See [LICENSE.md](LICENSE.md) for the boring details.
