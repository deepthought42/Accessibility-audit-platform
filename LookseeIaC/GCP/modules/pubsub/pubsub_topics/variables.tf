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