resource "google_secret_manager_secret_iam_member" "neo4j_password_secret_accessor" {
  secret_id = google_secret_manager_secret.neo4j_password.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "neo4j_username_secret_accessor" {
  secret_id = google_secret_manager_secret.neo4j_username.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "neo4j_db_name_secret_accessor" {
  secret_id = google_secret_manager_secret.neo4j_db_name.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "pusher_app_id_secret_accessor" {
  secret_id = google_secret_manager_secret.pusher_app_id.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "pusher_cluster_secret_accessor" {
  secret_id = google_secret_manager_secret.pusher_cluster.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "pusher_key_secret_accessor" {
  secret_id = google_secret_manager_secret.pusher_key.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "pusher_secret_secret_accessor" {
  secret_id = google_secret_manager_secret.pusher_secret.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}


resource "google_secret_manager_secret_iam_member" "smtp_password_secret_accessor" {
  secret_id = google_secret_manager_secret.smtp_password.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "smtp_username_secret_accessor" {
  secret_id = google_secret_manager_secret.smtp_username.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "auth0_client_id_secret_accessor" {
  secret_id = google_secret_manager_secret.auth0_client_id.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "auth0_client_secret_secret_accessor" {
  secret_id = google_secret_manager_secret.auth0_client_secret.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "auth0_domain_secret_accessor" {
  secret_id = google_secret_manager_secret.auth0_domain.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "auth0_audience_secret_accessor" {
  secret_id = google_secret_manager_secret.auth0_audience.name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "gcp_api_key_secret_accessor" {
  count     = var.gcp_api_key != "" ? 1 : 0
  secret_id = google_secret_manager_secret.gcp_api_key[0].name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "integrations_encryption_key_secret_accessor" {
  count     = var.integrations_encryption_key != "" ? 1 : 0
  secret_id = google_secret_manager_secret.integrations_encryption_key[0].name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}

resource "google_secret_manager_secret_iam_member" "smtp_host_secret_accessor" {
  count     = var.smtp_host != "" ? 1 : 0
  secret_id = google_secret_manager_secret.smtp_host[0].name
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
}









