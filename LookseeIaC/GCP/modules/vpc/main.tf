# Create the VPC network
resource "google_compute_network" "vpc" {
  name                    = var.vpc_name
  description            = "WebCrawler VPC network"
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "custom_subnet" {
  name                     = var.subnet_name
  ip_cidr_range            = var.subnet_cidr
  region                   = var.region
  network                  = google_compute_network.vpc.id
}

resource "google_vpc_access_connector" "connector" {
  name   = "cloud-run-connector"
  region = var.region
  network = google_compute_network.vpc.name
  ip_cidr_range = "10.8.0.0/28"
  min_instances = 2
  max_instances = 3
  machine_type = "e2-micro"
}

# Firewall: IAP SSH
resource "google_compute_firewall" "ssh_iap" {
  name    = "${var.vpc_name}-allow-iap-ssh"
  network = google_compute_network.vpc.name

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }
  source_ranges = ["35.235.240.0/20"]
  target_tags   = ["iap-ssh"]
}
