# Grant PubSub service account the IAM role to invoke the Cloud Run service
#resource "google_pubsub_subscription_iam_member" "pubsub_iam" {
#  project      = var.project_id
#  subscription = google_pubsub_subscription.subscription.name
#  role         = "roles/pubsub.publisher"
#  member       = "serviceAccount:${var.service_account_email}"
#}