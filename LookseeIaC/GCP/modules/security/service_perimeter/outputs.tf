output "perimeter_name" {
  description = "The name of the service perimeter"
  value       = google_access_context_manager_service_perimeter.perimeter.name
}

output "perimeter_id" {
  description = "The ID of the service perimeter"
  value       = google_access_context_manager_service_perimeter.perimeter.id
} 