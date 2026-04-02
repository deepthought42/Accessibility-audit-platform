resource "google_cloud_run_service" "journey-executor" {
  name     = "journey-executor"
  location = "us-central1"

  template {
    spec {
      containers {
        image = "us-docker.pkg.dev/cloudrun/container/hello"
        startup_probe {
          initial_delay_seconds = 0
          timeout_seconds = 1
          period_seconds = 5
          failure_threshold = 20
          tcp_socket {
            port = 8080
          }
        }
        liveness_probe {
          http_get {
            path = "/"
          }
        }
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }

  lifecycle {
    ignore_changes = [
      metadata.0.annotations,
    ]
  }
}


resource "google_pubsub_topic" "PageBuilt" {
  name         = "PageBuilt"
  kms_key_name = google_kms_crypto_key.crypto_key.id
}

resource "google_kms_crypto_key" "crypto_key" {
  name     = "page-built-key"
  key_ring = google_kms_key_ring.key_ring.id
}

resource "google_kms_key_ring" "key_ring" {
  name     = "page-built-keyring"
  location = "global"
}