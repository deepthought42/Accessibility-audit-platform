variable "project_id" {
  description = "The project ID where the subscription will be created"
  type        = string
}

variable "environment" {
  description = "Environment (dev, prod, etc)"
  type        = string
  default     = "dev"
}

variable "service_name" {
  description = "Name of the service"
  type        = string
}

variable "subscription_name" {
  description = "Name of the PubSub subscription"
  type        = string
}

variable "topic_id" {
  description = "The ID of the PubSub topic to subscribe to"
  type        = string
}

variable "push_endpoint" {
  description = "The URL of the Cloud Run service that will receive messages"
  type        = string
}

variable "service_account_email" {
  description = "The email of the service account used for authentication"
  type        = string
}