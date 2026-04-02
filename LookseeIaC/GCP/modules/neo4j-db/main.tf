resource "google_compute_instance" "neo4j" {
  name         = "neo4j-community"
  machine_type = var.machine_type
  zone         = var.zone
  project      = var.project_id

  boot_disk {
    initialize_params {
      image = var.image
      size  = var.disk_size
    }
  }

  network_interface {
    network    = var.vpc_network_name  # e.g., "default" or your custom VPC
    subnetwork = var.subnet_name       # optional, if using custom subnet

    # No external IP, so only accessible via VPC
    access_config {
      # Omit this block to prevent external IP assignment
    }
  }

  # Optionally, set tags for firewall rules
  tags = concat(var.tags, ["iap-ssh"])

  metadata = {
    enable-oslogin = "TRUE"
  }

  metadata_startup_script = file("${path.module}/install-neo4j.sh")
}

# Firewall rule to allow access only from within the VPC
resource "google_compute_firewall" "neo4j-internal" {
  name    = "neo4j-allow-internal"
  network = var.vpc_network_name

  allow {
    protocol = "tcp"
    ports    = ["7474", "7687"] # Neo4j HTTP and Bolt ports and ssh
  }

  source_ranges = var.source_ranges # Adjust to your VPC's CIDR
  target_tags   = var.tags
}

# 6. IAM binding for IAP Tunnel SSH
resource "google_iap_tunnel_instance_iam_member" "ssh" {
  project  = var.project_id
  zone     = var.zone
  instance = google_compute_instance.neo4j.name
  role     = "roles/iap.tunnelResourceAccessor"
  member   = "serviceAccount:${google_service_account.neo4j_instance_sa.email}"
}