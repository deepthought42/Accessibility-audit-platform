# Pub/Sub Push Subscription Terraform Module

This Terraform module creates a Google Cloud Pub/Sub push subscription and (optionally) manages IAM permissions for a service account to publish messages to the subscription. It is designed to be used as part of a larger infrastructure-as-code setup on Google Cloud Platform (GCP).

## Features

- Creates a Pub/Sub push subscription.
- (Optional) Grants IAM roles to a service account for publishing to the subscription.
- Configurable push endpoint and authentication.

## Usage

```hcl
module "push_subscription" {
  source = "./modules/atoms/pubsub/push_subscription"

  project_id             = "your-gcp-project-id"
  topic_name             = "your-pubsub-topic"
  subscription_name      = "your-push-subscription"
  push_endpoint          = "https://your-cloud-run-service.run.app"
  service_account_email  = "your-service-account@your-project.iam.gserviceaccount.com" # Optional
}
```

## Input Variables

| Name                   | Description                                                      | Type     | Default | Required |
|------------------------|------------------------------------------------------------------|----------|---------|:--------:|
| `project_id`           | The GCP project ID.                                              | string   | n/a     |   yes    |
| `topic_name`           | The name of the Pub/Sub topic to subscribe to.                   | string   | n/a     |   yes    |
| `subscription_name`    | The name of the Pub/Sub subscription to create.                  | string   | n/a     |   yes    |
| `push_endpoint`        | The endpoint URL to which messages should be pushed.             | string   | n/a     |   yes    |
| `service_account_email`| (Optional) Service account email to grant Pub/Sub publisher role.| string   | `""`    |    no    |

## Outputs

| Name                | Description                                 |
|---------------------|---------------------------------------------|
| `subscription_name` | The name of the created Pub/Sub subscription|
| `subscription_id`   | The ID of the created Pub/Sub subscription  |

## IAM Permissions

If you want to allow a service account to publish messages to the subscription, uncomment and configure the IAM resource in `iam.tf`:

```hcl
resource "google_pubsub_subscription_iam_member" "pubsub_iam" {
  project      = var.project_id
  subscription = google_pubsub_subscription.subscription.name
  role         = "roles/pubsub.publisher"
  member       = "serviceAccount:${var.service_account_email}"
}
```

## Example

See the [Usage](#usage) section above for a basic example.

## Requirements

- Terraform >= 0.13
- Google Provider >= 3.5

## License

MIT License