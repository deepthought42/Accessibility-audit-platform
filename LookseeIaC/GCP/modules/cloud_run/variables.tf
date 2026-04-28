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
  description = "Map of environment variables sourced from Secret Manager. Value is [secret_name, version]."
  type        = map(list(string))
  default     = {}
}

variable "plain_environment_variables" {
  description = "Map of plain (non-secret) environment variables. Use environment_variables for secret references."
  type        = map(string)
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

variable "max_instances" {
  description = "Maximum number of Cloud Run instances"
  type        = number
  default     = 100
}

variable "min_instances" {
  description = "Minimum number of Cloud Run instances"
  type        = number
  default     = 0
}

# ----------------------------------------------------------------------------
# Observability variables (Wave 2 of architecture review)
# ----------------------------------------------------------------------------
variable "otel_exporter_otlp_endpoint" {
  description = "OTLP endpoint for OpenTelemetry trace export. Defaults to Google Cloud Trace's regional OTLP endpoint."
  type        = string
  default     = "https://telemetry.googleapis.com:443"
}

variable "otel_traces_sampler_arg" {
  description = "Trace sampling ratio (0.0-1.0). 0.1 = 10% of traces sampled."
  type        = number
  default     = 0.1

  validation {
    condition     = var.otel_traces_sampler_arg >= 0 && var.otel_traces_sampler_arg <= 1
    error_message = "otel_traces_sampler_arg must be between 0.0 and 1.0."
  }
}

# ----------------------------------------------------------------------------
# Image tag pinning (Wave 5.3 of architecture review)
# Forbid `latest` so production deployments are always reproducible.
# ----------------------------------------------------------------------------
variable "image_tag" {
  description = "Container image tag (semver). The :latest tag is forbidden in production."
  type        = string
  default     = ""

  validation {
    condition     = var.image_tag != "latest"
    error_message = "image_tag=latest is forbidden. Pass an explicit semver tag from the service's docker-ci-release.yml output."
  }
}
