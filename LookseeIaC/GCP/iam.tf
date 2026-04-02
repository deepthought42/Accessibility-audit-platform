# Create service account for PubSub to invoke Cloud Run
resource "google_service_account" "pubsub_sa" {
  project      = var.project_id
  account_id   = "pubsub-sa"
  display_name = "Service Account for PubSub to invoke Cloud Run"
}

# Create topic-level IAM bindings for the service account
resource "google_pubsub_topic_iam_member" "topic_publisher" {
  for_each = toset([
    module.pubsub_topics.url_topic_name,
    module.pubsub_topics.page_created_topic_name,
    module.pubsub_topics.page_audit_topic_name,
  module.pubsub_topics.journey_verified_topic_name])

  project = var.project_id
  topic   = each.value
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.pubsub_sa.email}"
}

resource "google_service_account" "cloud_run_sa" {
  project      = var.project_id
  account_id   = "cloud-run-sa"
  display_name = "Service Account for Cloud Run"
}
