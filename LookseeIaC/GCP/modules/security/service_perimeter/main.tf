locals {
  # Format: {env}-{service}-{type}-perimeter
  # Examples: 
  # prod-payment-regular-perimeter
  # staging-auth-bridge-perimeter
  perimeter_title = "${var.environment}-${var.service_name}-${var.perimeter_type}-perimeter"
}

resource "google_access_context_manager_service_perimeter" "perimeter" {
  parent = "accessPolicies/${var.access_policy_id}"
  name   = "accessPolicies/${var.access_policy_id}/servicePerimeters/${local.perimeter_title}"
  title  = local.perimeter_title

  status {
    resources = var.project_numbers != null ? [for project in var.project_numbers : "projects/${project}"] : []

    restricted_services = var.restricted_services
    
    vpc_accessible_services {
      enable_restriction = true
      allowed_services  = var.allowed_services
    }

    access_levels = var.access_level_names
  }

  dynamic "spec" {
    for_each = var.use_explicit_dry_run_spec ? [1] : []
    content {
      resources = var.project_numbers != null ? [for project in var.project_numbers : "projects/${project}"] : []

      restricted_services = var.restricted_services
      
      vpc_accessible_services {
        enable_restriction = true
        allowed_services  = var.allowed_services
      }

      access_levels = var.access_level_names
    }
  }
} 