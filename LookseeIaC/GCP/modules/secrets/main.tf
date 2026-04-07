# Neo4j Password Secret
resource "google_secret_manager_secret" "neo4j_password" {
  secret_id = "neo4j-password"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "neo4j_password_version" {
  secret         = google_secret_manager_secret.neo4j_password.id
  secret_data_wo = var.neo4j_password
}

# Neo4j Username Secret
resource "google_secret_manager_secret" "neo4j_username" {
  secret_id = "neo4j-username"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "neo4j_username_version" {
  secret         = google_secret_manager_secret.neo4j_username.id
  secret_data_wo = var.neo4j_username
}

# Neo4j Production Database Name Secret
resource "google_secret_manager_secret" "neo4j_db_name" {
  secret_id = "neo4j-db-name"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "neo4j_db_name_version" {
  secret         = google_secret_manager_secret.neo4j_db_name.id
  secret_data_wo = var.neo4j_db_name
}

# Pusher App ID Secret
resource "google_secret_manager_secret" "pusher_app_id" {
  secret_id = "pusher-app-id"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "pusher_app_id_version" {
  secret         = google_secret_manager_secret.pusher_app_id.id
  secret_data_wo = var.pusher_app_id
}

# Pusher Production Cluster Secret
resource "google_secret_manager_secret" "pusher_cluster" {
  secret_id = "pusher-cluster"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "pusher_cluster_version" {
  secret         = google_secret_manager_secret.pusher_cluster.id
  secret_data_wo = var.pusher_cluster
}

# Pusher Key Secret
resource "google_secret_manager_secret" "pusher_key" {
  secret_id = "pusher-key"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "pusher_key_version" {
  secret         = google_secret_manager_secret.pusher_key.id
  secret_data_wo = var.pusher_key
}

# Pusher Key Secret
resource "google_secret_manager_secret" "pusher_secret" {
  secret_id = "pusher-secret"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "pusher_secret_version" {
  secret         = google_secret_manager_secret.pusher_secret.id
  secret_data_wo = var.pusher_secret
}

# SMTP Password Secret
resource "google_secret_manager_secret" "smtp_password" {
  secret_id = "smtp-password"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "smtp_password_version" {
  secret         = google_secret_manager_secret.smtp_password.id
  secret_data_wo = var.smtp_password
}

# SMTP Username Secret
resource "google_secret_manager_secret" "smtp_username" {
  secret_id = "smtp-username"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "smtp_username_version" {
  secret         = google_secret_manager_secret.smtp_username.id
  secret_data_wo = var.smtp_username
}

# Auth0 Client ID Secret
resource "google_secret_manager_secret" "auth0_client_id" {
  secret_id = "auth0-client-id"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "auth0_client_id_version" {
  secret         = google_secret_manager_secret.auth0_client_id.id
  secret_data_wo = var.auth0_client_id
}

# Auth0 Client Secret
resource "google_secret_manager_secret" "auth0_client_secret" {
  secret_id = "auth0-client-secret"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "auth0_client_secret_version" {
  secret         = google_secret_manager_secret.auth0_client_secret.id
  secret_data_wo = var.auth0_client_secret
}

# Auth0 Domain Secret
resource "google_secret_manager_secret" "auth0_domain" {
  secret_id = "auth0-domain"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "auth0_domain_version" {
  secret         = google_secret_manager_secret.auth0_domain.id
  secret_data_wo = var.auth0_domain
}

# Auth0 Audience Secret
resource "google_secret_manager_secret" "auth0_audience" {
  secret_id = "auth0-audience"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "auth0_audience_version" {
  secret         = google_secret_manager_secret.auth0_audience.id
  secret_data_wo = var.auth0_audience
}

# GCP API Key Secret
resource "google_secret_manager_secret" "gcp_api_key" {
  count     = var.gcp_api_key != "" ? 1 : 0
  secret_id = "gcp-api-key"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "gcp_api_key_version" {
  count          = var.gcp_api_key != "" ? 1 : 0
  secret         = google_secret_manager_secret.gcp_api_key[0].id
  secret_data_wo = var.gcp_api_key
}

# Integrations Encryption Key Secret
resource "google_secret_manager_secret" "integrations_encryption_key" {
  count     = var.integrations_encryption_key != "" ? 1 : 0
  secret_id = "integrations-encryption-key"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "integrations_encryption_key_version" {
  count          = var.integrations_encryption_key != "" ? 1 : 0
  secret         = google_secret_manager_secret.integrations_encryption_key[0].id
  secret_data_wo = var.integrations_encryption_key
}

# SMTP Host Secret
resource "google_secret_manager_secret" "smtp_host" {
  count     = var.smtp_host != "" ? 1 : 0
  secret_id = "smtp-host"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "smtp_host_version" {
  count          = var.smtp_host != "" ? 1 : 0
  secret         = google_secret_manager_secret.smtp_host[0].id
  secret_data_wo = var.smtp_host
}

# ============================================================================
# Wave 5.3 of architecture review: bring previously-manual application secrets
# into Terraform-managed Secret Manager so deployments are reproducible and
# all secret material is auditable from a single source.
# ============================================================================

# ---- Stripe ----
resource "google_secret_manager_secret" "stripe_secret_key" {
  count     = var.stripe_secret_key != "" ? 1 : 0
  secret_id = "stripe-secret-key"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "stripe_secret_key_version" {
  count          = var.stripe_secret_key != "" ? 1 : 0
  secret         = google_secret_manager_secret.stripe_secret_key[0].id
  secret_data_wo = var.stripe_secret_key
}

resource "google_secret_manager_secret" "stripe_webhook_secret" {
  count     = var.stripe_webhook_secret != "" ? 1 : 0
  secret_id = "stripe-webhook-secret"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "stripe_webhook_secret_version" {
  count          = var.stripe_webhook_secret != "" ? 1 : 0
  secret         = google_secret_manager_secret.stripe_webhook_secret[0].id
  secret_data_wo = var.stripe_webhook_secret
}

resource "google_secret_manager_secret" "stripe_price_ids" {
  count     = var.stripe_price_ids != "" ? 1 : 0
  secret_id = "stripe-price-ids"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "stripe_price_ids_version" {
  count          = var.stripe_price_ids != "" ? 1 : 0
  secret         = google_secret_manager_secret.stripe_price_ids[0].id
  secret_data_wo = var.stripe_price_ids
}

# ---- Segment ----
resource "google_secret_manager_secret" "segment_write_key" {
  count     = var.segment_write_key != "" ? 1 : 0
  secret_id = "segment-write-key"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "segment_write_key_version" {
  count          = var.segment_write_key != "" ? 1 : 0
  secret         = google_secret_manager_secret.segment_write_key[0].id
  secret_data_wo = var.segment_write_key
}

# ---- SendGrid ----
resource "google_secret_manager_secret" "sendgrid_api_key" {
  count     = var.sendgrid_api_key != "" ? 1 : 0
  secret_id = "sendgrid-api-key"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "sendgrid_api_key_version" {
  count          = var.sendgrid_api_key != "" ? 1 : 0
  secret         = google_secret_manager_secret.sendgrid_api_key[0].id
  secret_data_wo = var.sendgrid_api_key
}
