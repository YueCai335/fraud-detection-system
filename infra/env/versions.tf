terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # Bucket created by infra/bootstrap. S3-native locking (no DynamoDB table needed).
  backend "s3" {
    bucket       = "fraud-detection-tfstate-039066033099"
    key          = "env/terraform.tfstate"
    region       = "ca-central-1"
    use_lockfile = true
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project   = "fraud-detection-system"
      ManagedBy = "terraform"
    }
  }
}
