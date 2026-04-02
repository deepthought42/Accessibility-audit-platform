resource "google_pubsub_topic" "url_topic" {
  name    = var.url_topic_name
  project = var.project_id

  # Optional: Add labels if you want to organize/identify your resources
  labels = var.labels

  message_storage_policy {
    allowed_persistence_regions = [
      var.region
    ]
  }
}

resource "google_pubsub_topic" "page_created_topic" {
  name    = var.page_created_topic_name
  project = var.project_id

  # Optional: Add labels if you want to organize/identify your resources
  labels = var.labels

  message_storage_policy {
    allowed_persistence_regions = [
      var.region
    ]
  }
}

resource "google_pubsub_topic" "page_audit_topic" {
  name    = var.page_audit_topic_name
  project = var.project_id

  # Optional: Add labels if you want to organize/identify your resources
  labels = var.labels

  message_storage_policy {
    allowed_persistence_regions = [
      var.region
    ]
  }
}

resource "google_pubsub_topic" "journey_verified_topic" {
  name    = var.journey_verified_topic_name
  project = var.project_id

  # Optional: Add labels if you want to organize/identify your resources
  labels = var.labels

  message_storage_policy {
    allowed_persistence_regions = [
      var.region
    ]
}
}

resource "google_pubsub_topic" "journey_discarded_topic" {
  name    = var.journey_discarded_topic_name
  project = var.project_id

  # Optional: Add labels if you want to organize/identify your resources
  labels = var.labels

  message_storage_policy {
    allowed_persistence_regions = [
      var.region
    ]
  }
}

resource "google_pubsub_topic" "journey_candidate_topic" {
  name    = var.journey_candidate_topic_name
  project = var.project_id

  # Optional: Add labels if you want to organize/identify your resources
  labels = var.labels

  message_storage_policy {
    allowed_persistence_regions = [
      var.region
    ]
  }
}

resource "google_pubsub_topic" "audit_update_topic" {
  name    = var.audit_update_topic_name
  project = var.project_id

  # Optional: Add labels if you want to organize/identify your resources
  labels = var.labels

  message_storage_policy {
    allowed_persistence_regions = [
      var.region
    ]
  }
}

resource "google_pubsub_topic" "journey_completion_cleanup_topic" {
  name    = var.journey_completion_cleanup_topic_name
  project = var.project_id

  # Optional: Add labels if you want to organize/identify your resources
  labels = var.labels

  message_storage_policy {
    allowed_persistence_regions = [
      var.region
    ]
  }
}

resource "google_pubsub_topic" "audit_error_topic" {
  name    = var.audit_error_topic_name
  project = var.project_id

  # Optional: Add labels if you want to organize/identify your resources
  labels = var.labels

  message_storage_policy {
    allowed_persistence_regions = [
      var.region
    ]
  }
}