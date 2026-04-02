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









