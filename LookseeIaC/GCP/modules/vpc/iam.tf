# Create a service account for VPC management
resource "google_service_account" "vpc_service_account" {
  account_id   = "vpc-service-account"
  display_name = "VPC Service Account"
  description  = "Service account for VPC management"
}

# Assign necessary IAM roles to the service account
resource "google_project_iam_member" "vpc_admin" {
  project = var.project_id
  role    = "roles/compute.networkAdmin"
  member  = "serviceAccount:${google_service_account.vpc_service_account.email}"
}