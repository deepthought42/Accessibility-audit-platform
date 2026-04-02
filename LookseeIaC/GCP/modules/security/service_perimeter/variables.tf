variable "access_policy_id" {
  description = "The ID of the access policy this perimeter belongs to"
  type        = string
}

variable "perimeter_name" {
  description = "Name of the service perimeter"
  type        = string
}

variable "project_numbers" {
  description = "List of project numbers to include in the perimeter"
  type        = list(string)
  default     = null
}

variable "restricted_services" {
  description = "List of GCP services to restrict"
  type        = list(string)
  default     = []
}

variable "allowed_services" {
  description = "List of GCP services to allow within the VPC"
  type        = list(string)
  default     = []
}

variable "access_level_names" {
  description = "List of access level resource names to include in the perimeter"
  type        = list(string)
  default     = []
}

variable "use_explicit_dry_run_spec" {
  description = "Whether to use explicit dry-run spec"
  type        = bool
  default     = false
}

variable "labels" {
  description = "A map of labels to apply to the service perimeter"
  type        = map(string)
  default     = {}
}

variable "environment" {
  description = "Environment identifier (e.g., prod, staging, dev)"
  type        = string
}

variable "service_name" {
  description = "Name of the service or application this perimeter protects"
  type        = string
}

variable "perimeter_type" {
  description = "Type of perimeter (e.g., regular, bridge)"
  type        = string
  default     = "regular"

  validation {
    condition     = contains(["regular", "bridge"], var.perimeter_type)
    error_message = "Perimeter type must be either 'regular' or 'bridge'."
  }
} 