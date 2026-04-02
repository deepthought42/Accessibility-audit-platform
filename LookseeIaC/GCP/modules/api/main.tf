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

        # Add environment variables from secrets
        env {
          name = "NEO4J_PASSWORD"
          value_from {
            secret_key_ref {
              name = "neo4j-password"
              key  = "latest"
            }
          }
        }

        env {
          name = "NEO4J_USERNAME"
          value_from {
            secret_key_ref {
              name = "neo4j-username"
              key  = "latest"
            }
          }
        }

        env {
          name = "NEO4J_BOLT_URI"
          value_from {
            secret_key_ref {
              name = "neo4j-bolt-uri"
              key  = "latest"
            }
          }
        }

        env {
          name = "NEO4J_DB_NAME"
          value_from {
            secret_key_ref {
              name = "neo4j-db-name"
              key  = "latest"
            }
          }
        }
        
        dynamic "env" {
          for_each = var.pubsub_topics
          content {
            name  = env.key
            value = env.value
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
