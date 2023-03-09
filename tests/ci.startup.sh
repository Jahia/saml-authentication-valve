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

# WORKAROUND: It seems keycloak could not reach ldap server the first time its started
# even with a delay between ldap server and keycloak
# Workaround is to start it the first time and let it error out, remove the container and restart again.
docker-compose up -d keycloak
sleep 45
docker stop keycloak; docker rm keycloak
sleep 15

docker-compose up -d --renew-anon-volumes --remove-orphans --force-recreate jahia

if [[ $1 != "notests" ]]; then
    echo "$(date +'%d %B %Y - %k:%M') [TESTS] == Starting cypress tests =="
    docker-compose up --abort-on-container-exit --renew-anon-volumes cypress
fi

echo " == Printing keycloak server logs"
docker logs keycloak
