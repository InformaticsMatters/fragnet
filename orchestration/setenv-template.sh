# Copy as 'setenv.sh'
# then 'source setenv.sh' before orchestrating.

# AWS credentials
export AWS_ACCESS_KEY_ID="SetMe"
export AWS_SECRET_ACCESS_KEY="SetMe"
# The region of the S3 bucket
export AWS_DEFAULT_REGION=eu-west-1

# the location of the Keycloak server. e.g. "https://squonk.it/auth"
export KEYCLOAK_SERVER_URL="SetMe"

# the public key of the keycloak realm
export KEYCLOAK_PUBLIC_KEY="SetMe"

# the client secret for fragnet-search client in the realm
export KEYCLOAK_SECRET="SetMe"

# the neo4j user password
export NEO4J_PASSWORD="SetMe"

# Variables used to test the service.
# We just need an authorised user (and password).
# Then run 'test-fragnet' playbook.
export FRAGNET_USERNAME="SetMe"
export FRAGNET_PASSWORD="SetMe"

# The fragnet-search certificate bundle password
# and certbot registration email.
export FRAGNET_PFX_PASSWORD="SetMe"
export FRAGNET_CERTBOT_EMAIL="SetMe"
