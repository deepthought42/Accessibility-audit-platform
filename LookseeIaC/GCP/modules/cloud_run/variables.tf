variable "project_id" {
  description = "The ID of the project where resources will be created"
  type        = string
}

variable "region" {
  description = "The region where resources will be created"
  type        = string
}

variable "environment" {
  description = "The environment (dev, prod, etc)"
  type        = string
}

variable "service_name" {
  description = "Name of the Cloud Run service"
  type        = string
}

variable "image" {
  description = "Container image to deploy"
  type        = string
}

variable "pubsub_topics" {
  description = "Map of PubSub topic names to publish messages to"
  type        = map(string)
  default     = {}
}

variable "service_account_email" {
  description = "Email of the service account for the Cloud Run service"
  type        = string
}

variable "labels" {
  description = "Environment labels"
  type        = map(string)
}

variable "topic_id" {
  description = "The ID of the PubSub topic to subscribe to"
  type        = string
}

variable "vpc_connector_name" {
  description = "The name of the VPC connector to use"
  type        = string
}

variable "port" {
  description = "The port to run the service on"
  type        = number
  default     = 8080
}

variable "memory_allocation" {
  description = "Memory allocated for cloud run"
  type        = string
  default     = "500M"
}

variable "cpu_allocation" {

  description = "CPU allocation for cloud run"
  type        = string
  default     = "1"
}

variable "memory_limit" {
  description = "Memory limit for cloud run"
  type        = string
  default     = "4Gi"
}

variable "cpu_limit" {
  description = "CPU limit for cloud run"
  type        = string
  default     = "2"
}

variable "environment_variables" {
  description = "Map of environment variables to set"
  type        = map(list(string))
  default     = {}
}

variable "vpc_egress" {
  description = "The egress of the VPC connector"
  type        = string
  default     = "all-traffic"
}

variable "selenium_urls" {
  description = "List of Selenium instance URLs"
  type        = list(string)
  default     = []
}
