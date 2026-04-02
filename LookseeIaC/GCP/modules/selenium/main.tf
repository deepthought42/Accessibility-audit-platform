# Cloud Run service
resource "google_cloud_run_service" "selenium_standalone_chrome" {
  name     = var.service_name
  location = var.region

  template {
    spec {
      containers {
        # Using the latest image from Artifact Registry
        image = var.image

        ports {
          container_port = var.port
        }

        resources {
          limits = {
            memory = var.memory_allocation
            cpu    = var.cpu_allocation
          }
        }
      }
    }
  }

  # Use the latest revision
  metadata {
    annotations = {
      "run.googleapis.com/client-name" = "terraform"
    }
  }

  # Configure traffic to latest revision
  traffic {
    percent         = 100
    latest_revision = true
  }
}