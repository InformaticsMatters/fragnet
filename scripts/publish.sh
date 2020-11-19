#!/bin/bash

# A script used by Travis
#
# If you're a user then execute from the project root,
# e.g. ./scripts/publish.sh

export FRAGNET_IMAGE_TAG="${TRAVIS_TAG:-latest}"
echo "FRAGNET_IMAGE_TAG is $FRAGNET_IMAGE_TAG"

# Login to docker.io
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin docker.io

./gradlew buildDockerImages -x test
docker push squonk/fragnet-depict:"$FRAGNET_IMAGE_TAG"
docker push squonk/fragnet-search:"$FRAGNET_IMAGE_TAG"

# If this is not 'latest'
# then also tag and push new 'latest' and 'stable' images...

if [[ "$FRAGNET_IMAGE_TAG" != "latest" ]]
then
  docker tag squonk/fragnet-depict:"$FRAGNET_IMAGE_TAG" squonk/fragnet-depict:latest
  docker push squonk/fragnet-depict:latest
  docker tag squonk/fragnet-depict:"$FRAGNET_IMAGE_TAG" squonk/fragnet-depict:stable
  docker push squonk/fragnet-depict:stable

  docker tag squonk/fragnet-search:"$FRAGNET_IMAGE_TAG" squonk/fragnet-search:latest
  docker push squonk/fragnet-search:latest
  docker tag squonk/fragnet-search:"$FRAGNET_IMAGE_TAG" squonk/fragnet-search:stable
  docker push squonk/fragnet-search:stable
fi
