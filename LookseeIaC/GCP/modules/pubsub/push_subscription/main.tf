# This module creates a PubSub subscription that pushes messages to a Cloud Run service
resource "google_pubsub_subscription" "subscription" {
  name    = var.subscription_name
  topic   = var.topic_id
  project = var.project_id

  push_config {
    push_endpoint = var.push_endpoint
    
    oidc_token {
      service_account_email = var.service_account_email
    }
  }

  retry_policy {
    minimum_backoff = "10s"
    maximum_backoff = "600s"
  }
}