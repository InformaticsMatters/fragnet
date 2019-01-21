# -----------------------------------------------------------------------------
# Mandatory Parameters (must be defined externally)
# -----------------------------------------------------------------------------

# ENVIRONMENT VARIABLES
# Define these secrets as environment variables
#
# - TF_VAR_aws_access_key                 Your AWS access token
# - TF_VAR_aws_secret_key                 Your AWS secret key

variable "aws_access_key" {}
variable "aws_secret_key" {}

# -----------------------------------------------------------------------------
# Default Parameters (can be changed via command-line or ENV)
# -----------------------------------------------------------------------------

variable "aws_key_name" {
  description = "The name of the Key Pair, as known by AWS"
  default = "abc-im"
}

variable "aws_region" {
  description = "EC2 Region for the Cluster"
  default = "us-east-1"
}

variable "aws_vpc_id" {
  description = "Pre-existing EC2 VPC for the Cluster"
  default = "vpc-8ea077ea"
}

variable "aws_subnet_id" {
  description = "Pre-existing EC2 VSubnet for the Cluster"
  default = "subnet-21acf40a"
}

variable "aws_zone" {
  description = "Pre-existing EC2 Zone for the Cluster"
  default = "us-east-1e"
}

variable "amis" {
  description = "AMIs by Region"
  type = "map"
  default = {
    us-east-1 = "ami-9887c6e7" # Centos Linux 7
  }
}

variable "data_volume_size" {
  description = "Size of graph data volume (Gi)"
  default = "300"
}

variable "ansible_dir" {
  description = "Relative path to Ansible (for inventory rendering)"
  default = "../../ansible"
}