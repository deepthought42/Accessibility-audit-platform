# output the public IP address of the instance
output "public_ip" {
  description = "The public IP address of the instance"
  value       = google_compute_instance.neo4j.network_interface[0].access_config[0].nat_ip
}

# output the private IP address of the instance
output "private_ip" {
  description = "The private IP address of the instance"
  value       = google_compute_instance.neo4j.network_interface[0].network_ip
}

output "neo4j_ip" {
  description = "The IP address of the neo4j instance"
  value = google_compute_instance.neo4j.network_interface[0].access_config[0].nat_ip
}

output "neo4j_instance_sa_email" {
  description = "The service account email"
  value = google_service_account.neo4j_instance_sa.email
}

output "neo4j_bolt_uri_secret_id" {
  description = "The ID of the neo4j bolt URI secret"
  value       = google_secret_manager_secret.neo4j_bolt_uri.id
}

output "neo4j_bolt_uri_secret_name" {
  description = "The name of the neo4j bolt URI secret"
  value       = google_secret_manager_secret.neo4j_bolt_uri.secret_id
}

output "neo4j_bolt_uri_secret_version" {
  description = "The version of the neo4j bolt uri secret"
  value = google_secret_manager_secret_version.neo4j_bolt_uri_version.name
}

output "instance_name" {
  description = "The name of the instance"
  value = google_compute_instance.neo4j.name
}