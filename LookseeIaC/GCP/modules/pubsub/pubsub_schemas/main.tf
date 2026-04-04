resource "google_pubsub_schema" "url_message_schema" {
  project    = var.project_id
  name       = "url-message-schema-${var.schema_version}"
  type       = "AVRO"
  definition = file("${path.module}/../schemas/avro/url_message.avsc")

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_pubsub_schema" "page_built_message_schema" {
  project    = var.project_id
  name       = "page-built-message-schema-${var.schema_version}"
  type       = "AVRO"
  definition = file("${path.module}/../schemas/avro/page_built_message.avsc")

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_pubsub_schema" "page_audit_message_schema" {
  project    = var.project_id
  name       = "page-audit-message-schema-${var.schema_version}"
  type       = "AVRO"
  definition = file("${path.module}/../schemas/avro/page_audit_message.avsc")

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_pubsub_schema" "verified_journey_message_schema" {
  project    = var.project_id
  name       = "verified-journey-message-schema-${var.schema_version}"
  type       = "AVRO"
  definition = file("${path.module}/../schemas/avro/verified_journey_message.avsc")

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_pubsub_schema" "discarded_journey_message_schema" {
  project    = var.project_id
  name       = "discarded-journey-message-schema-${var.schema_version}"
  type       = "AVRO"
  definition = file("${path.module}/../schemas/avro/discarded_journey_message.avsc")

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_pubsub_schema" "journey_candidate_message_schema" {
  project    = var.project_id
  name       = "journey-candidate-message-schema-${var.schema_version}"
  type       = "AVRO"
  definition = file("${path.module}/../schemas/avro/journey_candidate_message.avsc")

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_pubsub_schema" "audit_progress_update_schema" {
  project    = var.project_id
  name       = "audit-progress-update-schema-${var.schema_version}"
  type       = "AVRO"
  definition = file("${path.module}/../schemas/avro/audit_progress_update.avsc")

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_pubsub_schema" "audit_error_schema" {
  project    = var.project_id
  name       = "audit-error-schema-${var.schema_version}"
  type       = "AVRO"
  definition = file("${path.module}/../schemas/avro/audit_error.avsc")

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_pubsub_schema" "journey_completion_cleanup_message_schema" {
  project    = var.project_id
  name       = "journey-completion-cleanup-message-schema-${var.schema_version}"
  type       = "AVRO"
  definition = file("${path.module}/../schemas/avro/journey_completion_cleanup_message.avsc")

  lifecycle {
    create_before_destroy = true
  }
}
