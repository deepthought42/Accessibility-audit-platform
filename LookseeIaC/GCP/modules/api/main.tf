# Cloud Run service
resource "google_cloud_run_service" "api" {
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

        dynamic "env" {
          for_each = var.pubsub_topics
          content {
            name  = env.key
            value = env.value
          }
        }

        # Add environment variables from secrets
        dynamic "env" {
          for_each = var.environment_variables
          content {
            name = env.key
            value_from {
              secret_key_ref {
                name = env.value[0]
                key  = env.value[1]
              }
            }
          }
        }

        resources {
          limits = {
            memory = var.memory_allocation
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

# IAM policy to make the service public
resource "google_cloud_run_service_iam_member" "public_access" {
  service  = google_cloud_run_service.api.name
  location = google_cloud_run_service.api.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}
