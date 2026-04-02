resource "google_service_account" "neo4j_instance_sa" {
  project      = var.project_id
  account_id   = "neo4j-instance-sa"
  display_name = "Service Account for Neo4j Instance"
}

resource "google_project_iam_member" "neo4j_logging" {
  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${google_service_account.neo4j_instance_sa.email}"
}

resource "google_project_iam_member" "neo4j_monitoring" {
  project = var.project_id
  role    = "roles/monitoring.metricWriter"
  member  = "serviceAccount:${google_service_account.neo4j_instance_sa.email}"
}

resource "google_project_iam_member" "neo4j_storage_viewer" {
  project = var.project_id
  role    = "roles/storage.objectViewer"
  member  = "serviceAccount:${google_service_account.neo4j_instance_sa.email}"
}

resource "google_project_iam_member" "neo4j_iap_tunnel_user" {
  project = var.project_id
  role    = "roles/iap.tunnelResourceAccessor"
  member  = "serviceAccount:${google_service_account.neo4j_instance_sa.email}"
}


# SECRET MANAGER
resource "google_secret_manager_secret_iam_member" "neo4j_bolt_uri_secret_accessor" {
  secret_id = google_secret_manager_secret.neo4j_bolt_uri.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

# Neo4j Bolt URI Secret
resource "google_secret_manager_secret" "neo4j_bolt_uri" {
  secret_id = "neo4j-bolt-uri"
  project   = var.project_id

  labels = {
    environment = var.environment
  }

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "neo4j_bolt_uri_version" {
  secret         = google_secret_manager_secret.neo4j_bolt_uri.id
  secret_data_wo = "bolt://${google_compute_instance.neo4j.network_interface[0].network_ip}:7687"
}
