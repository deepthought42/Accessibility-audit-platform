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

# Schema configuration for topic validation.
# Each variable is an object with:
#   - schema:             The full schema resource ID (required, empty string = no schema attached)
#   - encoding:           Message encoding format, "JSON" or "BINARY" (default: "JSON")
#   - first_revision_id:  Oldest schema revision accepted by the topic (optional, empty = no constraint)
#   - last_revision_id:   Newest schema revision accepted by the topic (optional, empty = no constraint)

variable "url_schema_config" {
  description = "Schema configuration for the URL topic"
  type = object({
    schema             = string
    encoding           = optional(string, "JSON")
    first_revision_id  = optional(string, "")
    last_revision_id   = optional(string, "")
  })
  default = {
    schema             = ""
    encoding           = "JSON"
    first_revision_id  = ""
    last_revision_id   = ""
  }
}

variable "page_created_schema_config" {
  description = "Schema configuration for the page_created topic"
  type = object({
    schema             = string
    encoding           = optional(string, "JSON")
    first_revision_id  = optional(string, "")
    last_revision_id   = optional(string, "")
  })
  default = {
    schema             = ""
    encoding           = "JSON"
    first_revision_id  = ""
    last_revision_id   = ""
  }
}

variable "page_audit_schema_config" {
  description = "Schema configuration for the page_audit topic"
  type = object({
    schema             = string
    encoding           = optional(string, "JSON")
    first_revision_id  = optional(string, "")
    last_revision_id   = optional(string, "")
  })
  default = {
    schema             = ""
    encoding           = "JSON"
    first_revision_id  = ""
    last_revision_id   = ""
  }
}

variable "journey_verified_schema_config" {
  description = "Schema configuration for the journey_verified topic"
  type = object({
    schema             = string
    encoding           = optional(string, "JSON")
    first_revision_id  = optional(string, "")
    last_revision_id   = optional(string, "")
  })
  default = {
    schema             = ""
    encoding           = "JSON"
    first_revision_id  = ""
    last_revision_id   = ""
  }
}

variable "journey_discarded_schema_config" {
  description = "Schema configuration for the journey_discarded topic"
  type = object({
    schema             = string
    encoding           = optional(string, "JSON")
    first_revision_id  = optional(string, "")
    last_revision_id   = optional(string, "")
  })
  default = {
    schema             = ""
    encoding           = "JSON"
    first_revision_id  = ""
    last_revision_id   = ""
  }
}

variable "journey_candidate_schema_config" {
  description = "Schema configuration for the journey_candidate topic"
  type = object({
    schema             = string
    encoding           = optional(string, "JSON")
    first_revision_id  = optional(string, "")
    last_revision_id   = optional(string, "")
  })
  default = {
    schema             = ""
    encoding           = "JSON"
    first_revision_id  = ""
    last_revision_id   = ""
  }
}

variable "audit_update_schema_config" {
  description = "Schema configuration for the audit_update topic"
  type = object({
    schema             = string
    encoding           = optional(string, "JSON")
    first_revision_id  = optional(string, "")
    last_revision_id   = optional(string, "")
  })
  default = {
    schema             = ""
    encoding           = "JSON"
    first_revision_id  = ""
    last_revision_id   = ""
  }
}

variable "journey_completion_cleanup_schema_config" {
  description = "Schema configuration for the journey_completion_cleanup topic"
  type = object({
    schema             = string
    encoding           = optional(string, "JSON")
    first_revision_id  = optional(string, "")
    last_revision_id   = optional(string, "")
  })
  default = {
    schema             = ""
    encoding           = "JSON"
    first_revision_id  = ""
    last_revision_id   = ""
  }
}

variable "audit_error_schema_config" {
  description = "Schema configuration for the audit_error topic"
  type = object({
    schema             = string
    encoding           = optional(string, "JSON")
    first_revision_id  = optional(string, "")
    last_revision_id   = optional(string, "")
  })
  default = {
    schema             = ""
    encoding           = "JSON"
    first_revision_id  = ""
    last_revision_id   = ""
  }
}