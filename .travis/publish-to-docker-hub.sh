#!/bin/bash
set -e

cd `dirname $0`/..

DOCKER_ORG=expediadotcom
DOCKER_IMAGE_NAME=haystack-agent

docker build -t $(DOCKER_IMAGE_NAME) -f docker/Dockerfile .

QUALIFIED_DOCKER_IMAGE_NAME=$DOCKER_ORG/$DOCKER_IMAGE_NAME
echo "DOCKER_ORG=$DOCKER_ORG, DOCKER_IMAGE_NAME=$DOCKER_IMAGE_NAME, QUALIFIED_DOCKER_IMAGE_NAME=$QUALIFIED_DOCKER_IMAGE_NAME"
echo "BRANCH=$TRAVIS_BRANCH, TAG=$TRAVIS_TAG, SHA=$TRAVIS_COMMIT"

# login
docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD

# Add tags
if [[ $TRAVIS_TAG =~ ([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    echo "pushing the released haystack agent to docker hub"

    unset MAJOR MINOR PATCH
    MAJOR="${BASH_REMATCH[1]}"
    MINOR="${BASH_REMATCH[2]}"
    PATCH="${BASH_REMATCH[3]}"

    # for tag, add MAJOR, MAJOR.MINOR, MAJOR.MINOR.PATCH and latest as tag
    docker tag $DOCKER_IMAGE_NAME $QUALIFIED_DOCKER_IMAGE_NAME:$MAJOR
    docker tag $DOCKER_IMAGE_NAME $QUALIFIED_DOCKER_IMAGE_NAME:$MAJOR.$MINOR
    docker tag $DOCKER_IMAGE_NAME $QUALIFIED_DOCKER_IMAGE_NAME:$MAJOR.$MINOR.$PATCH
    docker tag $DOCKER_IMAGE_NAME $QUALIFIED_DOCKER_IMAGE_NAME:latest

    # publish image with tags
    docker push $QUALIFIED_DOCKER_IMAGE_NAME

elif [[ "$TRAVIS_BRANCH" == "master" ]]; then
    echo "pushing the snapshot version of haystack agent to docker hub"

    # TODO: for 'master' branch, add SHA as tag for now, ideally it should be project version like 1.0.0-SNAPSHOT
    docker tag $DOCKER_IMAGE_NAME $QUALIFIED_DOCKER_IMAGE_NAME:$TRAVIS_COMMIT

    # publish image with tags
    docker push $QUALIFIED_DOCKER_IMAGE_NAME
fi
