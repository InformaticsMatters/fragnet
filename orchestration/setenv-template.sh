# Copy as 'setenv.sh'
# then 'source setenv.sh' before orchestrating.

# AWS credentials for Terraform
export TF_VAR_aws_access_key="SetMe"
export TF_VAR_aws_secret_key="SetMe"
export TF_VAR_aws_key_name="SetMe"

# AWS credentials
export AWS_ACCESS_KEY_ID="${TF_VAR_aws_access_key}"
export AWS_SECRET_ACCESS_KEY="${TF_VAR_aws_secret_key}"
# The region of the S3 bucket
export AWS_DEFAULT_REGION=eu-west-1

# OpenShift credentials
export OS_ADMIN="SetMe"
export OS_ADMIN_PASSWORD="SetMe"
export OS_USER="SetMe"
export OS_USER_PASSWORD="SetMe"

# Keycloak material
# the location of the Keycloak server. e.g. "https://squonk.it/auth"
export KEYCLOAK_SERVER_URL="SetMe"
export KEYCLOAK_ADMIN="SetMe"
export KEYCLOAK_ADMIN_PASSWORD="SetMe"
export KEYCLOAK_PUBLIC_KEY="SetMe"
export KEYCLOAK_SECRET="SetMe"
export KEYCLOAK_REALM="SetMe"

# the neo4j user password
export NEO4J_SERVER="SetMe"
export NEO4J_USER="SetMe"
export NEO4J_PASSWORD="SetMe"

# Variables used to test the service.
# We just need an authorised user (and password).
# Then run 'test-fragnet' playbook.
export FRAGNET_USERNAME="SetMe"
export FRAGNET_PASSWORD="SetMe"
export FRAGNET_CLIENT_SECRET="SetMe"

# The fragnet-search certificate bundle password
# and certbot registration email.
export FRAGNET_PFX_PASSWORD="SetMe"
export FRAGNET_CERTBOT_EMAIL="SetMe"
