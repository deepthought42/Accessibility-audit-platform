variable "project_id" {
  description = "The GCP project ID"
}

variable "region" {
  description = "The region to deploy to"
}

variable "environment" {
  description = "Environment (prod, dev, etc)"
}

variable "image" {
  description = "Container image to deploy"
  type        = string
  default     = "docker.io/selenium/standalone-chrome:3.141.59"
}

variable "service_name" {
  description = "Name of the Cloud Run service"
  default     = "selenium standalone chrome"
}

variable "service_account_email" {
  description = "The email address of the service account to use for the Cloud Run service"
  type        = string
  default     = "service-account@project-id.iam.gserviceaccount.com"
}

variable "vpc_connector_name" {
  description = "Name of the VPC connector"
  type        = string
  default     = "vpc-connector"
}

variable "port" {
  description = "The port to run the service on"
  type        = number
  default     = 4444
}

variable "memory_allocation" {
  description = "Memory allocated for cloud run instance"
  type        = string
}

variable "cpu_allocation" {
  description = "CPU allocated for cloud run instance"
  type        = string
}