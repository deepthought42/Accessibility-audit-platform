output "service_url" {
  description = "URL of the selenium instance"
  value = google_cloud_run_service.selenium_standalone_chrome.status[0].url
}

output "service_name" {
  description = "Name of the selenium instance"
  value = google_cloud_run_service.selenium_standalone_chrome.name
}