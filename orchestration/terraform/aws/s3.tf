# -----------------------------------------------------------------------------
# State Storage (AWS S3)
# -----------------------------------------------------------------------------

terraform {
  backend "s3" {
    bucket = "im-terraform-state"
    key = "fragnet-terraform"
    region = "eu-central-1"
    dynamodb_table = "fragnet-terraform"
  }
}
