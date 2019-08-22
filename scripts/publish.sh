#!/bin/bash

# A script used by Travis
#
# If you're a user then execute from the project root,
# e.g. ./scripts/publish.sh

LATEST_TAG='latest'
export FRAGNET_IMAGE_TAG="${TRAVIS_TAG:-$LATEST_TAG}"
echo "FRAGNET_IMAGE_TAG is $FRAGNET_IMAGE_TAG"

# Login to docker.io
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin docker.io

./gradlew buildDockerImage -x test
docker push squonk/fragnet-services:"$FRAGNET_IMAGE_TAG"

# If this is not 'latest'
# then also tag and push new latest images...

if [[ "$FRAGNET_IMAGE_TAG" != "$LATEST_TAG" ]]
then
  docker tag squonk/fragnet-services:"$FRAGNET_IMAGE_TAG" squonk/fragnet-services:"$LATEST_TAG"
  docker push squonk/fragnet-services:"$LATEST_TAG"
fi
