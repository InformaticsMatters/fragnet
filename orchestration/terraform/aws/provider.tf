# -----------------------------------------------------------------------------
# Various Terraform provider definitions
# -----------------------------------------------------------------------------

terraform {
  required_version = "0.11.11"
}

provider "aws" {
  access_key = "${var.aws_access_key}"
  secret_key = "${var.aws_secret_key}"
  region = "${var.aws_region}"
  version = "2.27.0"
}

provider "local" {
  version = "1.3.0"
}

provider "template" {
  version = "2.1.2"
}
