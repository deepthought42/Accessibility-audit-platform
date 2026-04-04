# This module creates a PubSub subscription that pushes messages to a Cloud Run service

# Dead-letter topic for failed messages
resource "google_pubsub_topic" "dead_letter" {
  name    = "${var.subscription_name}-dlq"
  project = var.project_id
}

resource "google_pubsub_subscription" "subscription" {
  name    = var.subscription_name
  topic   = var.topic_id
  project = var.project_id

  ack_deadline_seconds       = 600
  message_retention_duration = "604800s"

  push_config {
    push_endpoint = var.push_endpoint

    oidc_token {
      service_account_email = var.service_account_email
    }
  }

  dead_letter_policy {
    dead_letter_topic     = google_pubsub_topic.dead_letter.id
    max_delivery_attempts = 10
  }

  retry_policy {
    minimum_backoff = "10s"
    maximum_backoff = "600s"
  }
}

# Allow the PubSub service agent to publish to the dead-letter topic
resource "google_pubsub_topic_iam_member" "dlq_publisher" {
  topic   = google_pubsub_topic.dead_letter.name
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:service-${data.google_project.project.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
  project = var.project_id
}

# Allow the PubSub service agent to acknowledge messages from the subscription
resource "google_pubsub_subscription_iam_member" "dlq_subscriber" {
  subscription = google_pubsub_subscription.subscription.name
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:service-${data.google_project.project.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
  project      = var.project_id
}

data "google_project" "project" {
  project_id = var.project_id
}
