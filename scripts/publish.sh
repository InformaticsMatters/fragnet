#!/bin/bash

# A script used by Travis
#
# If you're a user then execute from the project root,
# e.g. ./scripts/publish.sh

set -eo pipefail

# Version number regex.
# Used to test strings against the pattern "N.N[.N]",
# i.e. a 2 or 3-field version string of digits separated by periods.
V_REGEX='^([0-9]+\.){1,2}[0-9]+$'

export FRAGNET_IMAGE_TAG="${TRAVIS_TAG:-latest}"
echo "FRAGNET_IMAGE_TAG is $FRAGNET_IMAGE_TAG"

# Login to docker.io
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin docker.io

./gradlew buildDockerImage -x test
docker push squonk/fragnet-services:"$FRAGNET_IMAGE_TAG"

# If this is not 'latest'
# then also tag and push new 'latest'
# and (if it's a formal tag) 'stable' images...

if [[ $FRAGNET_IMAGE_TAG != "latest" ]]
then
  docker tag squonk/fragnet-services:"$FRAGNET_IMAGE_TAG" squonk/fragnet-services:latest
  docker push squonk/fragnet-services:latest
  if [[ $FRAGNET_IMAGE_TAG =~ $V_REGEX ]]
  then
    docker tag squonk/fragnet-services:"$FRAGNET_IMAGE_TAG" squonk/fragnet-services:stable
    docker push squonk/fragnet-services:stable
  fi
fi
