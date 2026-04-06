variable "project_id" {
  description = "The ID of the GCP project"
  type        = string
}

variable "schema_version" {
  description = "Version suffix appended to schema resource names (e.g. 'v1'). Changing this creates new schema resources, enabling safe schema evolution via create_before_destroy lifecycle."
  type        = string
  default     = "v1"
}
