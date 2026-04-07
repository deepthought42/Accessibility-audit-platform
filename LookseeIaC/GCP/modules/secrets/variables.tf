variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "environment" {
  description = "The environment"
  type        = string
}

variable "service_account_email" {
  description = "Service account email"
  type        = string
}

variable "neo4j_password" {
  description = "Password for Neo4j database"
  type        = string
  sensitive   = true
}

variable "neo4j_username" {
  description = "Username for Neo4j database"
  type        = string
  default     = "neo4j"
}

variable "neo4j_db_name" {
  description = "Database name for Neo4j"
  type        = string
  default     = "neo4j"
}


variable "pusher_app_id" {
  description = "Pusher application ID"
  type        = string
  default     = "1149968"
}

variable "pusher_key" {
  description = "Pusher key"
  type        = string
  sensitive   = true
}

variable "pusher_cluster" {
  description = "Pusher cluster"
  type        = string
  sensitive   = true
}

variable "pusher_secret" {
  description = "Pusher secret"
  type        = string
  sensitive   = true
}

variable "smtp_password" {
  description = "SMTP password"
  type        = string
  sensitive   = true
}

variable "smtp_username" {
  description = "SMTP username"
  type        = string
  sensitive   = true
}

variable "auth0_client_id" {
  description = "Auth0 client ID"
  type        = string
  sensitive   = true
}

variable "auth0_client_secret" {
  description = "Auth0 client secret"
  type        = string
  sensitive   = true
}

variable "auth0_domain" {
  description = "Auth0 domain"
  type        = string
  sensitive   = true
}

variable "auth0_audience" {
  description = "Auth0 audience"
  type        = string
  sensitive   = true
}

variable "gcp_api_key" {
  description = "Google Cloud API key (PageSpeed Insights / Vision API)"
  type        = string
  sensitive   = true
  default     = ""
}

variable "integrations_encryption_key" {
  description = "AES-GCM encryption key for integration configurations"
  type        = string
  sensitive   = true
  default     = ""
}

variable "smtp_host" {
  description = "SMTP host for outbound email"
  type        = string
  default     = ""
}

# Wave 5.3 of architecture review: Stripe / Segment / SendGrid secrets.
variable "stripe_secret_key" {
  description = "Stripe API secret key (sk_live_... or sk_test_...). Empty disables the secret."
  type        = string
  default     = ""
  sensitive   = true
}

variable "stripe_webhook_secret" {
  description = "Stripe webhook signing secret (whsec_...). Empty disables the secret."
  type        = string
  default     = ""
  sensitive   = true
}

variable "stripe_price_ids" {
  description = "JSON document mapping plan names to Stripe price IDs. Empty disables the secret."
  type        = string
  default     = ""
  sensitive   = true
}

variable "segment_write_key" {
  description = "Segment server-side write key. Empty disables the secret."
  type        = string
  default     = ""
  sensitive   = true
}

variable "sendgrid_api_key" {
  description = "SendGrid API key for transactional email. Empty disables the secret."
  type        = string
  default     = ""
  sensitive   = true
}