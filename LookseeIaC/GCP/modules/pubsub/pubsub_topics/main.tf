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

  dynamic "schema_settings" {
    for_each = var.url_schema_config.schema != "" ? [var.url_schema_config] : []
    content {
      schema             = schema_settings.value.schema
      encoding           = schema_settings.value.encoding
      first_revision_id  = schema_settings.value.first_revision_id != "" ? schema_settings.value.first_revision_id : null
      last_revision_id   = schema_settings.value.last_revision_id != "" ? schema_settings.value.last_revision_id : null
    }
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

  dynamic "schema_settings" {
    for_each = var.page_created_schema_config.schema != "" ? [var.page_created_schema_config] : []
    content {
      schema             = schema_settings.value.schema
      encoding           = schema_settings.value.encoding
      first_revision_id  = schema_settings.value.first_revision_id != "" ? schema_settings.value.first_revision_id : null
      last_revision_id   = schema_settings.value.last_revision_id != "" ? schema_settings.value.last_revision_id : null
    }
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

  dynamic "schema_settings" {
    for_each = var.page_audit_schema_config.schema != "" ? [var.page_audit_schema_config] : []
    content {
      schema             = schema_settings.value.schema
      encoding           = schema_settings.value.encoding
      first_revision_id  = schema_settings.value.first_revision_id != "" ? schema_settings.value.first_revision_id : null
      last_revision_id   = schema_settings.value.last_revision_id != "" ? schema_settings.value.last_revision_id : null
    }
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

  dynamic "schema_settings" {
    for_each = var.journey_verified_schema_config.schema != "" ? [var.journey_verified_schema_config] : []
    content {
      schema             = schema_settings.value.schema
      encoding           = schema_settings.value.encoding
      first_revision_id  = schema_settings.value.first_revision_id != "" ? schema_settings.value.first_revision_id : null
      last_revision_id   = schema_settings.value.last_revision_id != "" ? schema_settings.value.last_revision_id : null
    }
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

  dynamic "schema_settings" {
    for_each = var.journey_discarded_schema_config.schema != "" ? [var.journey_discarded_schema_config] : []
    content {
      schema             = schema_settings.value.schema
      encoding           = schema_settings.value.encoding
      first_revision_id  = schema_settings.value.first_revision_id != "" ? schema_settings.value.first_revision_id : null
      last_revision_id   = schema_settings.value.last_revision_id != "" ? schema_settings.value.last_revision_id : null
    }
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

  dynamic "schema_settings" {
    for_each = var.journey_candidate_schema_config.schema != "" ? [var.journey_candidate_schema_config] : []
    content {
      schema             = schema_settings.value.schema
      encoding           = schema_settings.value.encoding
      first_revision_id  = schema_settings.value.first_revision_id != "" ? schema_settings.value.first_revision_id : null
      last_revision_id   = schema_settings.value.last_revision_id != "" ? schema_settings.value.last_revision_id : null
    }
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

  dynamic "schema_settings" {
    for_each = var.audit_update_schema_config.schema != "" ? [var.audit_update_schema_config] : []
    content {
      schema             = schema_settings.value.schema
      encoding           = schema_settings.value.encoding
      first_revision_id  = schema_settings.value.first_revision_id != "" ? schema_settings.value.first_revision_id : null
      last_revision_id   = schema_settings.value.last_revision_id != "" ? schema_settings.value.last_revision_id : null
    }
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

  dynamic "schema_settings" {
    for_each = var.journey_completion_cleanup_schema_config.schema != "" ? [var.journey_completion_cleanup_schema_config] : []
    content {
      schema             = schema_settings.value.schema
      encoding           = schema_settings.value.encoding
      first_revision_id  = schema_settings.value.first_revision_id != "" ? schema_settings.value.first_revision_id : null
      last_revision_id   = schema_settings.value.last_revision_id != "" ? schema_settings.value.last_revision_id : null
    }
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

  dynamic "schema_settings" {
    for_each = var.audit_error_schema_config.schema != "" ? [var.audit_error_schema_config] : []
    content {
      schema             = schema_settings.value.schema
      encoding           = schema_settings.value.encoding
      first_revision_id  = schema_settings.value.first_revision_id != "" ? schema_settings.value.first_revision_id : null
      last_revision_id   = schema_settings.value.last_revision_id != "" ? schema_settings.value.last_revision_id : null
    }
  }
}

# ---------------------------------------------------------------------------
# audit_error monitor subscription
#
# Services publish application-level errors to the audit_error topic but no
# Cloud Run service consumes them, so failed messages were silently dropped
# once retention expired and there was no alerting on accumulation. This pull
# subscription gives the topic a single durable consumer (held for 7 days),
# which lets Cloud Monitoring alert on oldest_unacked_message_age and lets
# operators inspect failures via `gcloud pubsub subscriptions pull`.
#
# When the dedicated dlq-handler service from Wave 1 of the architecture plan
# lands, this can be converted to a push subscription that targets it.
# ---------------------------------------------------------------------------
resource "google_pubsub_subscription" "audit_error_monitor" {
  name    = "${var.audit_error_topic_name}-monitor"
  topic   = google_pubsub_topic.audit_error_topic.name
  project = var.project_id
  labels  = var.labels

  ack_deadline_seconds       = 60
  message_retention_duration = "604800s" # 7 days
  retain_acked_messages      = false
  enable_message_ordering    = false

  expiration_policy {
    ttl = "" # never expire
  }
}
