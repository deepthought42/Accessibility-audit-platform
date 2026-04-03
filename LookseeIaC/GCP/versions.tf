terraform {
  required_version = ">= 1.6"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 5.0"
    }
  }

  backend "gcs" {
    # All values must be provided at init time via -backend-config flags or
    # a backend config file so that no bucket names are committed to the repo.
    #
    # Example:
    #   terraform init \
    #     -backend-config="bucket=my-tf-state-bucket" \
    #     -backend-config="prefix=looksee/dev"
    #
    # In GitHub Actions, pass these via environment or step inputs:
    #   env:
    #     TF_CLI_ARGS_init: "-backend-config=bucket=${{ secrets.TF_STATE_BUCKET }} -backend-config=prefix=looksee/${{ github.ref_name }}"
  }
}
