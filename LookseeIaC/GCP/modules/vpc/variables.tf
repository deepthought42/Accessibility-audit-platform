# Variables file reference
variable "project_id" {
  description = "The ID of the GCP project"
  type        = string
}

variable "region" {
  description = "The region where resources will be created"
  type        = string
  default     = "us-central1"
}

variable "environment" {
  description = "The environment (dev, prod, etc)"
  type        = string
}

variable "vpc_name" {
  description = "Name of the VPC network"
  type        = string
  default     = "custom-vpc"
}

variable "labels" {
  description = "Environment labels"
  type        = map(string)
}

variable "subnet_name" {
  description = "Name of the subnet"
  type        = string
  default     = "private-subnet-1"
}

variable "subnet_cidr" {
  description = "CIDR range for the subnet"
  type        = string
  default     = "10.0.0.0/24"
}

variable "ssh_source_ranges" {
  description = "CIDR ranges for standard SSH firewall (external)"
  type        = list(string)
  default     = []
}

variable "iap_ssh_iam_member" {
  description = "IAM member for IAP SSH firewall"
  type        = string
  default     = ""
}
