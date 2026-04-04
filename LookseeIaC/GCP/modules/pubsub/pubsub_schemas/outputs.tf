output "url_message_schema_id" {
  description = "The ID of the UrlMessage PubSub schema"
  value       = google_pubsub_schema.url_message_schema.id
}

output "page_built_message_schema_id" {
  description = "The ID of the PageBuiltMessage PubSub schema"
  value       = google_pubsub_schema.page_built_message_schema.id
}

output "page_audit_message_schema_id" {
  description = "The ID of the PageAuditMessage PubSub schema"
  value       = google_pubsub_schema.page_audit_message_schema.id
}

output "verified_journey_message_schema_id" {
  description = "The ID of the VerifiedJourneyMessage PubSub schema"
  value       = google_pubsub_schema.verified_journey_message_schema.id
}

output "discarded_journey_message_schema_id" {
  description = "The ID of the DiscardedJourneyMessage PubSub schema"
  value       = google_pubsub_schema.discarded_journey_message_schema.id
}

output "journey_candidate_message_schema_id" {
  description = "The ID of the JourneyCandidateMessage PubSub schema"
  value       = google_pubsub_schema.journey_candidate_message_schema.id
}

output "audit_progress_update_schema_id" {
  description = "The ID of the AuditProgressUpdate PubSub schema"
  value       = google_pubsub_schema.audit_progress_update_schema.id
}

output "audit_error_schema_id" {
  description = "The ID of the AuditError PubSub schema"
  value       = google_pubsub_schema.audit_error_schema.id
}

output "journey_completion_cleanup_message_schema_id" {
  description = "The ID of the JourneyCompletionCleanupMessage PubSub schema"
  value       = google_pubsub_schema.journey_completion_cleanup_message_schema.id
}
