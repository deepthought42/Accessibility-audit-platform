# Create Cloud Run service
resource "google_cloud_run_service" "service" {
  name     = var.service_name
  location = var.region
  project  = var.project_id

  metadata {
    annotations = {
      "run.googleapis.com/ingress" = "internal"
    }
  }

  template {
    spec {
      timeout_seconds = 120
      containers {
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

        # Add selenium URLs as environment variable
        dynamic "env" {
          for_each = length(var.selenium_urls) > 0 ? [1] : []
          content {
            name  = "SELENIUM_URLS"
            value = join(",", var.selenium_urls)
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
            memory = var.memory_limit
            cpu    = var.cpu_limit
          }

          requests = {
            memory = var.memory_allocation
            cpu    = var.cpu_allocation
          }
        }
      }
      
      service_account_name = var.service_account_email
    }
    metadata {
      annotations = {
        "run.googleapis.com/vpc-access-connector" = var.vpc_connector_name
        "run.googleapis.com/vpc-access-egress"    = var.vpc_egress
        "run.googleapis.com/client-name"          = "terraform"
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }
}

module "pubsub_subscription" {
  source                = "../pubsub/push_subscription"
  project_id            = var.project_id
  subscription_name     = "${var.service_name}-subscription"
  topic_id              = var.topic_id
  push_endpoint         = google_cloud_run_service.service.status[0].url
  service_account_email = var.service_account_email
  environment           = var.environment
  service_name          = var.service_name
  region                = var.region
}