variable "project_id" {
  description = "The ID of the project"
  type        = string
}

variable "region" {
  description = "The region of the project"
  type        = string
}

variable "labels" {
  description = "A map of labels to apply to the topic"
  type        = map(string)
}

variable "service_account_email" {
  description = "The email of the service account to use for the topic"
  type        = string
}

variable "url_topic_name" {
  description = "The name of the PubSub topic"
  type        = string
}

variable "page_created_topic_name" {
  description = "The name of the PubSub topic"
  type        = string
}

variable "page_audit_topic_name" {
  description = "The name of the PubSub topic"
  type        = string
}

variable "journey_verified_topic_name" {
  description = "The name of the PubSub topic"
  type        = string
}

variable "journey_discarded_topic_name" {
  description = "The name of the PubSub topic"
  type        = string
}

variable "journey_candidate_topic_name" {
  description = "The name of the PubSub topic"
  type        = string
}

variable "audit_update_topic_name" {
  description = "The name of the PubSub topic"
  type        = string
}

variable "journey_completion_cleanup_topic_name" {
  description = "The name of the PubSub topic"
  type        = string
}

variable "audit_error_topic_name" {
  description = "The name of the PubSub topic"
  type        = string
}

# Schema IDs for topic validation (empty string = no schema attached)

variable "url_schema_id" {
  description = "The ID of the Avro schema for the URL topic"
  type        = string
  default     = ""
}

variable "page_created_schema_id" {
  description = "The ID of the Avro schema for the page_created topic"
  type        = string
  default     = ""
}

variable "page_audit_schema_id" {
  description = "The ID of the Avro schema for the page_audit topic"
  type        = string
  default     = ""
}

variable "journey_verified_schema_id" {
  description = "The ID of the Avro schema for the journey_verified topic"
  type        = string
  default     = ""
}

variable "journey_discarded_schema_id" {
  description = "The ID of the Avro schema for the journey_discarded topic"
  type        = string
  default     = ""
}

variable "journey_candidate_schema_id" {
  description = "The ID of the Avro schema for the journey_candidate topic"
  type        = string
  default     = ""
}

variable "audit_update_schema_id" {
  description = "The ID of the Avro schema for the audit_update topic"
  type        = string
  default     = ""
}

variable "journey_completion_cleanup_schema_id" {
  description = "The ID of the Avro schema for the journey_completion_cleanup topic"
  type        = string
  default     = ""
}

variable "audit_error_schema_id" {
  description = "The ID of the Avro schema for the audit_error topic"
  type        = string
  default     = ""
}