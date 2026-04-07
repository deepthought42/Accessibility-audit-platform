# Notification channel for alert emails
resource "google_monitoring_notification_channel" "email" {
  display_name = "Email Notification Channel"
  type         = "email"
  project      = var.project_id

  labels = {
    email_address = var.notification_email
  }
}

# Alert policy: DLQ message count > 0
resource "google_monitoring_alert_policy" "dlq_messages" {
  display_name = "Dead-Letter Queue Messages Detected"
  project      = var.project_id
  combiner     = "OR"

  conditions {
    display_name = "DLQ topic has undelivered messages"

    condition_threshold {
      filter          = "resource.type=\"pubsub_topic\" AND metric.type=\"pubsub.googleapis.com/topic/send_message_operation_count\" AND resource.label.topic_id=monitoring.regex.full_match(\".*-dlq\")"
      comparison      = "COMPARISON_GT"
      threshold_value = 0
      duration        = "60s"

      aggregations {
        alignment_period   = "300s"
        per_series_aligner = "ALIGN_SUM"
      }
    }
  }

  notification_channels = [google_monitoring_notification_channel.email.name]

  alert_strategy {
    auto_close = "604800s"
  }
}

# Alert policy: audit_error topic has unacked messages on its monitor sub.
# Pairs with the audit_error_monitor pull subscription created in the
# pubsub_topics module. Fires if any application-level error message goes
# untriaged for more than 5 minutes.
resource "google_monitoring_alert_policy" "audit_error_unacked" {
  display_name = "audit_error topic has unacked messages"
  project      = var.project_id
  combiner     = "OR"

  conditions {
    display_name = "audit_error_monitor oldest unacked message > 5m"

    condition_threshold {
      filter = join(" AND ", [
        "resource.type=\"pubsub_subscription\"",
        "metric.type=\"pubsub.googleapis.com/subscription/oldest_unacked_message_age\"",
        "resource.label.subscription_id=monitoring.regex.full_match(\".*audit_error.*-monitor\")",
      ])
      comparison      = "COMPARISON_GT"
      threshold_value = 300
      duration        = "60s"

      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_MAX"
      }
    }
  }

  notification_channels = [google_monitoring_notification_channel.email.name]

  alert_strategy {
    auto_close = "604800s"
  }
}

# Alert policy: Cloud Run error rate > 5%
resource "google_monitoring_alert_policy" "cloud_run_error_rate" {
  display_name = "Cloud Run Error Rate > 5%"
  project      = var.project_id
  combiner     = "OR"

  conditions {
    display_name = "Cloud Run 5xx error rate exceeds 5%"

    condition_threshold {
      filter          = "resource.type=\"cloud_run_revision\" AND metric.type=\"run.googleapis.com/request_count\" AND metric.label.response_code_class=\"5xx\""
      comparison      = "COMPARISON_GT"
      threshold_value = 5
      duration        = "300s"

      aggregations {
        alignment_period     = "300s"
        per_series_aligner   = "ALIGN_RATE"
        cross_series_reducer = "REDUCE_SUM"
        group_by_fields      = ["resource.label.service_name"]
      }
    }
  }

  notification_channels = [google_monitoring_notification_channel.email.name]

  alert_strategy {
    auto_close = "604800s"
  }
}
