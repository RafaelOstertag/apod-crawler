#!/bin/sh

set -eu

VERSION=$(mvn -B -q help:evaluate -Dexpression='project.version' -DforceStdout=true)

if echo "${VERSION}" | grep -E -- '-SNAPSHOT$' >/dev/null 2>&1
then
  echo "Cannot build docker image for snapshot versionss" >&2
  exit 1
fi

curl \
  -f \
  --header "Content-Type: application/json" \
  --header "Circle-Token: $CIRCLE_CI_TOKEN" \
  --request POST \
  --data "{\"branch\":\"master\", \"parameters\":{\"version\": \"${VERSION}\"}}" \
  https://circleci.com/api/v2/project/github/RafaelOstertag/apod-crawler-docker/pipeline
