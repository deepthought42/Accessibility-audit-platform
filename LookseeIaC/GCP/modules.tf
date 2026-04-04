locals {
  resource_labels = merge(var.labels, {
    environment = var.environment
    application = "crawler"
  })
}

provider "google" {
  project     = var.project_id
  region      = var.region
  credentials = var.credentials_file != null && var.credentials_file != "" ? file(var.credentials_file) : null
}

# VPC module
module "vpc" {
  source = "./modules/vpc"

  project_id  = var.project_id
  environment = var.environment
  labels      = var.labels
  ssh_source_ranges = ["10.42.0.0/16"]
  subnet_cidr = "10.0.0.0/24"
}


module "pubsub_schemas" {
  source     = "./modules/pubsub/pubsub_schemas"
  project_id = var.project_id
}

module "pubsub_topics" {
  source                                = "./modules/pubsub/pubsub_topics"
  project_id                            = var.project_id
  region                                = var.region
  labels                                = var.labels
  service_account_email                 = google_service_account.pubsub_sa.email
  url_topic_name                        = "url"
  page_created_topic_name               = "page_created"
  page_audit_topic_name                 = "page_audit"
  journey_verified_topic_name           = "journey_verified"
  journey_discarded_topic_name          = "journey_discarded"
  audit_error_topic_name                = "audit_error"
  audit_update_topic_name               = "audit_update"
  journey_candidate_topic_name          = "journey_candidate"
  journey_completion_cleanup_topic_name = "journey_completion_cleanup"

  # Attach versioned schemas to simple topics (no complex nested objects).
  # Each config object supports optional revision pinning via first_revision_id / last_revision_id
  # to control which schema revisions a topic accepts during rolling upgrades.
  url_schema_config = {
    schema = module.pubsub_schemas.url_message_schema_id
  }
  page_created_schema_config = {
    schema = module.pubsub_schemas.page_built_message_schema_id
  }
  page_audit_schema_config = {
    schema = module.pubsub_schemas.page_audit_message_schema_id
  }
  audit_update_schema_config = {
    schema = module.pubsub_schemas.audit_progress_update_schema_id
  }
  audit_error_schema_config = {
    schema = module.pubsub_schemas.audit_error_schema_id
  }

  # Journey topics: schemas are created but not attached yet.
  # The Journey object uses polymorphic Jackson serialization that requires
  # validation testing before enabling broker-level schema enforcement.
  # To attach, uncomment and set:
  # journey_verified_schema_config = {
  #   schema = module.pubsub_schemas.verified_journey_message_schema_id
  # }
  # journey_discarded_schema_config = {
  #   schema = module.pubsub_schemas.discarded_journey_message_schema_id
  # }
  # journey_candidate_schema_config = {
  #   schema = module.pubsub_schemas.journey_candidate_message_schema_id
  # }

  depends_on = [module.pubsub_schemas]
}

variable "environment_variables" {
  description = "Map of environment variables to set"
  type        = map(string)
  default     = {}
}
# Secrets module
module "secrets" {
  source                = "./modules/secrets"
  project_id            = var.project_id
  environment           = var.environment
  service_account_email = google_service_account.cloud_run_sa.email

  neo4j_password = var.neo4j_password
  neo4j_db_name  = var.neo4j_db_name
  neo4j_username = var.neo4j_username

  smtp_password = var.smtp_password
  smtp_username = var.smtp_username

  pusher_key     = var.pusher_key
  pusher_app_id  = var.pusher_app_id
  pusher_cluster = var.pusher_cluster
  pusher_secret  = var.pusher_secret

  auth0_client_id     = var.auth0_client_id
  auth0_client_secret = var.auth0_client_secret
  auth0_domain        = var.auth0_domain
  auth0_audience      = var.auth0_audience

  gcp_api_key                 = var.gcp_api_key
  integrations_encryption_key = var.integrations_encryption_key
  smtp_host                   = var.smtp_host

  depends_on = [module.neo4j_db]
}






# PUBSUB PERIMETER
#module "pubsub_perimeter" {
#  source              = "./modules/security/service_perimeter"
#  environment         = var.environment
#  service_name        = var.service_vpcname
#  access_policy_id    = var.access_policy_id
#  perimeter_name      = "crawler-perimeter"
#  project_numbers     = [data.google_project.project.number]
#  restricted_services = ["pubsub.googleapis.com"]
#  allowed_services    = ["pubsub.googleapis.com"]
#  access_level_names  = [var.vpc_access_level]
#}


###############################
#
#  Neo4j DB module
#
###############################
module "neo4j_db" {
  source                = "./modules/neo4j-db"
  project_id            = var.project_id
  vpc_network_name      = module.vpc.vpc_name
  image                 = "ubuntu-os-cloud/ubuntu-2204-lts"
  subnet_name           = module.vpc.subnet_name
  region                = var.region
  zone                  = "us-central1-a"
  machine_type          = "e2-medium"
  disk_size             = 50
  source_ranges         = ["10.0.0.0/8"]
  tags                  = ["neo4j"]
  neo4j_password        = var.neo4j_password
  neo4j_username        = var.neo4j_username
  neo4j_db_name         = var.neo4j_db_name
  environment           = var.environment
  service_account_email = google_service_account.cloud_run_sa.email
}

###############################
#
#  Cloud Run modules
#
###############################

# API module
module "api" {
  source                = "./modules/api"
  project_id            = var.project_id
  region                = var.region
  environment           = var.environment
  service_name          = "api"
  service_account_email = google_service_account.cloud_run_sa.email
  vpc_connector_name    = module.vpc.vpc_connector_name
  url_topic_name        = module.pubsub_topics.url_topic_name
  pubsub_topics = {
    "pubsub.url_topic" : module.pubsub_topics.url_topic_name,
    "pubsub.discarded_journey_topic" : module.pubsub_topics.journey_discarded_topic_name,
    "pubsub.error_topic" : module.pubsub_topics.audit_error_topic_name
  }
  environment_variables = merge(
    {
      "spring.data.neo4j.database" : [module.secrets.neo4j_db_name_secret_name, "latest"],
      "spring.data.neo4j.username" : [module.secrets.neo4j_username_secret_name, "latest"],
      "spring.data.neo4j.password" : [module.secrets.neo4j_password_secret_name, "latest"],
      "spring.data.neo4j.uri" : [module.neo4j_db.neo4j_bolt_uri_secret_name, "latest"],
      "auth0.clientId" : [module.secrets.auth0_client_id_secret_name, "latest"],
      "auth0.clientSecret" : [module.secrets.auth0_client_secret_secret_name, "latest"],
      "auth0.domain" : [module.secrets.auth0_domain_secret_name, "latest"],
      "auth0.audience" : [module.secrets.auth0_audience_secret_name, "latest"],
      "smtp.username" : [module.secrets.smtp_username_secret_name, "latest"],
      "smtp.password" : [module.secrets.smtp_password_secret_name, "latest"],
    },
    var.gcp_api_key != "" ? {
      "gcp.api.key" : [module.secrets.gcp_api_key_secret_name, "latest"],
    } : {},
    var.integrations_encryption_key != "" ? {
      "integrations.encryption.key" : [module.secrets.integrations_encryption_key_secret_name, "latest"],
    } : {},
    var.smtp_host != "" ? {
      "spring.mail.host" : [module.secrets.smtp_host_secret_name, "latest"],
    } : {},
  )
  memory_allocation = "1Gi"
  depends_on        = [module.neo4j_db, module.secrets]
}

# Page Builder Cloud Run module
module "page_builder_cloud_run" {
  source                = "./modules/cloud_run"
  project_id            = var.project_id
  environment           = var.environment
  service_name          = "page-builder"
  image                 = var.page_builder_image
  region                = var.region
  topic_id              = module.pubsub_topics.url_topic_id
  labels                = { "environment" = var.environment, "application" = "page-builder" }
  service_account_email = google_service_account.cloud_run_sa.email
  cpu_allocation        = "2"
  memory_allocation     = "4Gi"
  memory_limit          = "8Gi"
  cpu_limit             = "4"
  pubsub_topics = {
    "pubsub.page_built" : module.pubsub_topics.page_created_topic_name,
    "pubsub.page_audit_topic" : module.pubsub_topics.page_audit_topic_name,
    "pubsub.journey_verified" : module.pubsub_topics.journey_verified_topic_name,
    "pubsub.error_topic" : module.pubsub_topics.audit_error_topic_name,
    "spring.cloud.gcp.project-id" : var.project_id,
    "spring.cloud.gcp.region" : var.region
  }
  environment_variables = merge(
    {
      "spring.data.neo4j.database" : [module.secrets.neo4j_db_name_secret_name, "latest"],
      "spring.data.neo4j.username" : [module.secrets.neo4j_username_secret_name, "latest"],
      "spring.data.neo4j.password" : [module.secrets.neo4j_password_secret_name, "latest"],
      "spring.data.neo4j.uri" : [module.neo4j_db.neo4j_bolt_uri_secret_name, "latest"],
    },
    var.gcp_api_key != "" ? {
      "gcp.api.key" : [module.secrets.gcp_api_key_secret_name, "latest"],
    } : {},
  )

  vpc_connector_name = module.vpc.vpc_connector_name
  vpc_egress         = "private-ranges-only"
  selenium_urls      = local.selenium_urls
  depends_on         = [module.selenium_chrome_cloud_run]
}

module "audit_manager_cloud_run" {
  source                = "./modules/cloud_run"
  project_id            = var.project_id
  environment           = var.environment
  service_name          = "audit-manager"
  image                 = var.audit_manager_image
  region                = var.region
  topic_id              = module.pubsub_topics.page_created_topic_id
  labels                = { "environment" = var.environment, "application" = "audit-manager" }
  service_account_email = google_service_account.cloud_run_sa.email
  pubsub_topics = {
    "pubsub.audit_update" : module.pubsub_topics.audit_update_topic_name,
    "pubsub.page_audit_topic" : module.pubsub_topics.page_audit_topic_name,
    "pubsub.error_topic" : module.pubsub_topics.audit_error_topic_name,
    "spring.cloud.gcp.project-id" : var.project_id,
    "spring.cloud.gcp.region" : var.region
  }
  environment_variables = {
    "spring.data.neo4j.database" : [module.secrets.neo4j_db_name_secret_name, "latest"],
    "spring.data.neo4j.username" : [module.secrets.neo4j_username_secret_name, "latest"],
    "spring.data.neo4j.password" : [module.secrets.neo4j_password_secret_name, "latest"],
    "spring.data.neo4j.uri" : [module.neo4j_db.neo4j_bolt_uri_secret_name, "latest"],
  }
  vpc_connector_name = module.vpc.vpc_connector_name
}

module "audit_service_cloud_run" {
  source                = "./modules/cloud_run"
  project_id            = var.project_id
  environment           = var.environment
  service_name          = "audit-service"
  image                 = var.audit_service_image
  region                = var.region
  topic_id              = module.pubsub_topics.page_audit_topic_id
  labels                = { "environment" = var.environment, "application" = "audit-service" }
  service_account_email = google_service_account.cloud_run_sa.email
  pubsub_topics = {
    "pubsub.audit_update" : module.pubsub_topics.audit_update_topic_name,
    "pubsub.error_topic" : module.pubsub_topics.audit_error_topic_name,
    "spring.cloud.gcp.project-id" : var.project_id,
    "spring.cloud.gcp.region" : var.region
  }
  environment_variables = {
    "spring.data.neo4j.database" : [module.secrets.neo4j_db_name_secret_name, "latest"],
    "spring.data.neo4j.username" : [module.secrets.neo4j_username_secret_name, "latest"],
    "spring.data.neo4j.password" : [module.secrets.neo4j_password_secret_name, "latest"],
    "spring.data.neo4j.uri" : [module.neo4j_db.neo4j_bolt_uri_secret_name, "latest"],
    "pusher.key" : [module.secrets.pusher_key_secret_name, "latest"],
    "pusher.appId" : [module.secrets.pusher_app_id_secret_name, "latest"],
    "pusher.cluster" : [module.secrets.pusher_cluster_secret_name, "latest"],
    "pusher.secret" : [module.secrets.pusher_secret_name, "latest"]
  }
  vpc_connector_name = module.vpc.vpc_connector_name
  memory_allocation  = "2Gi"
  memory_limit       = "4Gi"
}


module "journey_executor_cloud_run" {
  source                = "./modules/cloud_run"
  project_id            = var.project_id
  environment           = var.environment
  service_name          = "journey-executor"
  image                 = var.journey_executor_image
  region                = var.region
  topic_id              = module.pubsub_topics.journey_candidate_topic_id
  labels                = { "environment" = var.environment, "application" = "journey-executor" }
  service_account_email = google_service_account.cloud_run_sa.email
  memory_allocation     = "2Gi"
  memory_limit          = "4Gi"
  pubsub_topics = {
    "pubsub.page_built" : module.pubsub_topics.page_created_topic_name,
    "pubsub.journey_verified" : module.pubsub_topics.journey_verified_topic_name,
    "pubsub.discarded_journey_topic" : module.pubsub_topics.journey_discarded_topic_name,
    "pubsub.error_topic" : module.pubsub_topics.audit_error_topic_name,
    "spring.cloud.gcp.project-id" : var.project_id,
    "spring.cloud.gcp.region" : var.region
  }
  environment_variables = {
    "spring.data.neo4j.database" : [module.secrets.neo4j_db_name_secret_name, "latest"],
    "spring.data.neo4j.username" : [module.secrets.neo4j_username_secret_name, "latest"],
    "spring.data.neo4j.password" : [module.secrets.neo4j_password_secret_name, "latest"],
    "spring.data.neo4j.uri" : [module.neo4j_db.neo4j_bolt_uri_secret_name, "latest"],
  }

  vpc_connector_name = module.vpc.vpc_connector_name
  selenium_urls      = local.selenium_urls
  depends_on         = [module.selenium_chrome_cloud_run]
}

module "journey_expander_cloud_run" {
  source                = "./modules/cloud_run"
  project_id            = var.project_id
  environment           = var.environment
  service_name          = "journey-expander"
  image                 = var.journey_expander_image
  region                = var.region
  topic_id              = module.pubsub_topics.journey_verified_topic_id
  labels                = { "environment" = var.environment, "application" = "journey-expander" }
  service_account_email = google_service_account.cloud_run_sa.email
  pubsub_topics = {
    "pubsub.page_built" : module.pubsub_topics.page_created_topic_name,
    "pubsub.journey_verified" : module.pubsub_topics.journey_verified_topic_name,
    "pubsub.journey_candidate" : module.pubsub_topics.journey_candidate_topic_name,
    "pubsub.error_topic" : module.pubsub_topics.audit_error_topic_name,
    "spring.cloud.gcp.project-id" : var.project_id,
    "spring.cloud.gcp.region" : var.region
  }
  environment_variables = {
    "spring.data.neo4j.database" : [module.secrets.neo4j_db_name_secret_name, "latest"],
    "spring.data.neo4j.username" : [module.secrets.neo4j_username_secret_name, "latest"],
    "spring.data.neo4j.password" : [module.secrets.neo4j_password_secret_name, "latest"],
    "spring.data.neo4j.uri" : [module.neo4j_db.neo4j_bolt_uri_secret_name, "latest"],
  }
  vpc_connector_name = module.vpc.vpc_connector_name
}

module "content_audit_cloud_run" {
  source                = "./modules/cloud_run"
  project_id            = var.project_id
  environment           = var.environment
  service_name          = "content-audit"
  image                 = var.content_audit_image
  region                = var.region
  topic_id              = module.pubsub_topics.page_audit_topic_id
  labels                = { "environment" = var.environment, "application" = "content-audit" }
  service_account_email = google_service_account.cloud_run_sa.email
  pubsub_topics = {
    "pubsub.audit_update" : module.pubsub_topics.audit_update_topic_name,
    "pubsub.error_topic" : module.pubsub_topics.audit_error_topic_name,
    "spring.cloud.gcp.project-id" : var.project_id,
    "spring.cloud.gcp.region" : var.region
  }
  environment_variables = {
    "spring.data.neo4j.database" : [module.secrets.neo4j_db_name_secret_name, "latest"],
    "spring.data.neo4j.username" : [module.secrets.neo4j_username_secret_name, "latest"],
    "spring.data.neo4j.password" : [module.secrets.neo4j_password_secret_name, "latest"],
    "spring.data.neo4j.uri" : [module.neo4j_db.neo4j_bolt_uri_secret_name, "latest"],
  }
  vpc_connector_name = module.vpc.vpc_connector_name
}

module "visual_design_audit_cloud_run" {
  source                = "./modules/cloud_run"
  project_id            = var.project_id
  environment           = var.environment
  service_name          = "visual-design-audit"
  image                 = var.visual_design_audit_image
  region                = var.region
  topic_id              = module.pubsub_topics.page_audit_topic_id
  labels                = { "environment" = var.environment, "application" = "visual-design-audit" }
  service_account_email = google_service_account.cloud_run_sa.email
  pubsub_topics = {
    "pubsub.audit_update" : module.pubsub_topics.audit_update_topic_name,
    "pubsub.error_topic" : module.pubsub_topics.audit_error_topic_name,
    "spring.cloud.gcp.project-id" : var.project_id,
    "spring.cloud.gcp.region" : var.region
  }
  environment_variables = {
    "spring.data.neo4j.database" : [module.secrets.neo4j_db_name_secret_name, "latest"],
    "spring.data.neo4j.username" : [module.secrets.neo4j_username_secret_name, "latest"],
    "spring.data.neo4j.password" : [module.secrets.neo4j_password_secret_name, "latest"],
    "spring.data.neo4j.uri" : [module.neo4j_db.neo4j_bolt_uri_secret_name, "latest"],
  }
  vpc_connector_name = module.vpc.vpc_connector_name
}

module "information_architecture_audit_cloud_run" {
  source                = "./modules/cloud_run"
  project_id            = var.project_id
  environment           = var.environment
  service_name          = "information-architecture-audit"
  image                 = var.information_architecture_audit_image
  region                = var.region
  topic_id              = module.pubsub_topics.page_audit_topic_id
  labels                = { "environment" = var.environment, "application" = "information-architecture-audit" }
  service_account_email = google_service_account.cloud_run_sa.email
  pubsub_topics = {
    "pubsub.audit_update" : module.pubsub_topics.audit_update_topic_name,
    "pubsub.error_topic" : module.pubsub_topics.audit_error_topic_name,
    "spring.cloud.gcp.project-id" : var.project_id,
    "spring.cloud.gcp.region" : var.region
  }
  environment_variables = {
    "spring.data.neo4j.database" : [module.secrets.neo4j_db_name_secret_name, "latest"],
    "spring.data.neo4j.username" : [module.secrets.neo4j_username_secret_name, "latest"],
    "spring.data.neo4j.password" : [module.secrets.neo4j_password_secret_name, "latest"],
    "spring.data.neo4j.uri" : [module.neo4j_db.neo4j_bolt_uri_secret_name, "latest"],
  }
  vpc_connector_name = module.vpc.vpc_connector_name
  memory_allocation  = "2Gi"
  memory_limit       = "4Gi"
}


# Selenium modules - Cloud Run (multiple instances)
module "selenium_chrome_cloud_run" {
  for_each = { for i in range(var.selenium_instance_count) : i => "selenium-chrome-${i + 1}" }
  
  source                = "./modules/selenium"
  project_id            = var.project_id
  environment           = var.environment
  service_name          = each.value
  image                 = var.selenium_image
  region                = var.region
  service_account_email = google_service_account.cloud_run_sa.email
  memory_allocation     = "2Gi"
  cpu_allocation        = "1"
}

locals {
  selenium_urls = [for m in module.selenium_chrome_cloud_run : m.service_url]
}