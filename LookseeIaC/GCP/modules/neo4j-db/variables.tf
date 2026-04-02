variable "project_id" {
    description = "The project ID"
    type        = string
}

variable "region" {
    description = "The region"
    type        = string
    default     = "us-central1"
}

variable "zone" {
    description = "The zone"
    type        = string
    default     = "us-central1-a"
}

variable "vpc_network_name" {
    description = "The name of the VPC network"
    type        = string
}

variable "subnet_name" {
    description = "The name of the subnet"
    type        = string
}

variable "machine_type" {
    description = "The machine type"
    type        = string
    default     = "e2-medium"
}

variable "image" {
    description = "The image"
    type        = string
}

variable "disk_size" {
    description = "The disk size"
    type        = number
    default     = 20
}

variable "disk_type" {
    description = "The disk type"
    type        = string
    default     = "pd-ssd"
}

variable "source_ranges" {
    description = "The source ranges"
    type        = list(string)
    default     = ["10.0.0.0/8", "0.0.0.0/0"]
}

variable "target_tags" {
    description = "The target tags"
    type        = list(string)
    default     = ["iap-tunnel"]
}


variable "tags" {
    description = "The tags"
    type        = list(string)
    default     = ["neo4j"]
}

variable "neo4j_password" {
    description = "Initial password for the neo4j admin user"
    type        = string
    sensitive   = true
}

variable "neo4j_username" {
    description = "The username for the neo4j user"
    type        = string
}

variable "neo4j_db_name" {
    description = "The name of the neo4j database"
    type        = string
}

variable "service_account_email" {
    description = "The service account email"
    type        = string
}

variable "environment" {
    description = "The environment"
    type        = string
}