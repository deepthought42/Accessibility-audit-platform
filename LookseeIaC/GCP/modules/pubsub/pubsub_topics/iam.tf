
resource "google_pubsub_topic_iam_binding" "url_topic_publisher" {
  project = var.project_id
  topic   = google_pubsub_topic.url_topic.name
  role    = "roles/pubsub.publisher"
  members = [
    "serviceAccount:${var.service_account_email}"
  ]
}

resource "google_pubsub_topic_iam_binding" "page_created_topic_publisher" {
  project = var.project_id
  topic   = google_pubsub_topic.page_created_topic.name
  role    = "roles/pubsub.publisher"
  members = [
    "serviceAccount:${var.service_account_email}"
  ]
} 

resource "google_pubsub_topic_iam_binding" "page_audit_topic_publisher" {
  project = var.project_id
  topic   = google_pubsub_topic.page_audit_topic.name
  role    = "roles/pubsub.publisher"
  members = [
    "serviceAccount:${var.service_account_email}"
  ]
}

resource "google_pubsub_topic_iam_binding" "journey_verified_topic_publisher" {
  project = var.project_id
  topic   = google_pubsub_topic.journey_verified_topic.name
  role    = "roles/pubsub.publisher"
  members = [
    "serviceAccount:${var.service_account_email}"
  ]
}

resource "google_pubsub_topic_iam_binding" "journey_discarded_topic_publisher" {
  project = var.project_id
  topic   = google_pubsub_topic.journey_discarded_topic.name
  role    = "roles/pubsub.publisher"
  members = [
    "serviceAccount:${var.service_account_email}"
  ]
}

resource "google_pubsub_topic_iam_binding" "journey_candidate_topic_publisher" {
  project = var.project_id
  topic   = google_pubsub_topic.journey_candidate_topic.name  
  role    = "roles/pubsub.publisher"
  members = [
    "serviceAccount:${var.service_account_email}"
  ]
}

resource "google_pubsub_topic_iam_binding" "audit_update_topic_publisher" {
  project = var.project_id
  topic   = google_pubsub_topic.audit_update_topic.name
  role    = "roles/pubsub.publisher"
  members = [
    "serviceAccount:${var.service_account_email}"
  ]
}

resource "google_pubsub_topic_iam_binding" "journey_completion_cleanup_topic_publisher" {
  project = var.project_id
  topic   = google_pubsub_topic.journey_completion_cleanup_topic.name
  role    = "roles/pubsub.publisher"
  members = [
    "serviceAccount:${var.service_account_email}"
  ]
}

resource "google_pubsub_topic_iam_binding" "audit_error_topic_publisher" {
  project = var.project_id
  topic   = google_pubsub_topic.audit_error_topic.name
  role    = "roles/pubsub.publisher"
  members = [
    "serviceAccount:${var.service_account_email}"
  ]
}
