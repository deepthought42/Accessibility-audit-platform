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
      timeout_seconds = 600
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

        # ----------------------------------------------------------------
        # Observability defaults (Wave 2 of architecture review)
        # Enable Stackdriver metric export and OpenTelemetry tracing for
        # every service deployed via this module. Sampler ratio is low by
        # default to keep cost predictable; override per-service via
        # var.otel_traces_sampler_arg.
        # ----------------------------------------------------------------
        env {
          name  = "MANAGEMENT_METRICS_STACKDRIVER_ENABLED"
          value = "true"
        }
        env {
          name  = "SPRING_CLOUD_GCP_PROJECT_ID"
          value = var.project_id
        }
        env {
          name  = "OTEL_SERVICE_NAME"
          value = var.service_name
        }
        env {
          name  = "OTEL_EXPORTER_OTLP_ENDPOINT"
          value = var.otel_exporter_otlp_endpoint
        }
        env {
          name  = "OTEL_TRACES_SAMPLER"
          value = "parentbased_traceidratio"
        }
        env {
          name  = "OTEL_TRACES_SAMPLER_ARG"
          value = tostring(var.otel_traces_sampler_arg)
        }
        env {
          name  = "OTEL_RESOURCE_ATTRIBUTES"
          value = "service.name=${var.service_name},deployment.environment=${var.environment}"
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
        "autoscaling.knative.dev/maxScale"        = tostring(var.max_instances)
        "autoscaling.knative.dev/minScale"        = tostring(var.min_instances)
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