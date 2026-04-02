output "pubsub_subscription_id" {
  value       = google_pubsub_subscription.subscription.id
  description = "The ID of the PubSub push subscription"
}

output "pubsub_subscription_name" {
  value       = google_pubsub_subscription.subscription.name
  description = "The name of the PubSub push subscription"
}
