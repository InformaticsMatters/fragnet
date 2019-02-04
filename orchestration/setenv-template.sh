# Copy as 'setenv.sh'
# then 'source setenv.sh' before orchestrating.

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
