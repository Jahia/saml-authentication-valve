#!/usr/bin/env bash

# This script controls the startup of the container environment
# It can be used as an alternative to having docker-compose up started by the CI environment

source ./set-env.sh

echo " == Printing the most important environment variables"
echo " MANIFEST: ${MANIFEST}"
echo " TESTS_IMAGE: ${TESTS_IMAGE}"
echo " JAHIA_IMAGE: ${JAHIA_IMAGE}"
echo " JAHIA_URL: ${JAHIA_URL}"

docker-compose pull jahia
docker-compose up -d ldap
sleep 30 # needs a bit of time for ldap to start before keycloak can connect
docker-compose up -d --renew-anon-volumes --remove-orphans --force-recreate jahia

if [[ $1 != "notests" ]]; then
    echo "$(date +'%d %B %Y - %k:%M') [TESTS] == Starting cypress tests =="
    docker-compose up --abort-on-container-exit --renew-anon-volumes cypress
fi

echo " == Printing keycloak server logs"
docker logs keycloak
