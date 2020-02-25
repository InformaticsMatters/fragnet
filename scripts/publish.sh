#!/bin/bash

# A script used by Travis
#
# If you're a user then execute from the project root,
# e.g. ./scripts/publish.sh

export FRAGNET_IMAGE_TAG="${TRAVIS_TAG:-latest}"
echo "FRAGNET_IMAGE_TAG is $FRAGNET_IMAGE_TAG"

# Login to docker.io
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin docker.io

./gradlew buildDockerImage -x test
docker push squonk/fragnet-services:"$FRAGNET_IMAGE_TAG"

# If this is not 'latest'
# then also tag and push new 'latest' and 'stable' images...

if [[ "$FRAGNET_IMAGE_TAG" != "latest" ]]
then
  docker tag squonk/fragnet-services:"$FRAGNET_IMAGE_TAG" squonk/fragnet-services:latest
  docker push squonk/fragnet-services:latest
  docker tag squonk/fragnet-services:"$FRAGNET_IMAGE_TAG" squonk/fragnet-services:stable
  docker push squonk/fragnet-services:stable
fi
