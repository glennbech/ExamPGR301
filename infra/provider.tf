terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "5.26.0"
    }
  }
  backend "s3" {
    bucket = "pgr301-2021-terraform-state"
    key    = "candidate2043bucketkey/apprunner-another-new-state.state"
    region = "eu-north-1"
  }
}
