# SAML Authentication Setup

## Keycloak server

- Need to access URLs with named hostnames to make it work correctly in your local env (e.g. map to 127.0.0.1 in your `/etc/hosts`):
```
# keycloak testing saml-auth-valve
127.0.0.1   keycloak
127.0.0.1   jahia
```
- Start by running `docker-compose up -d keycloak`
  - also starts ldap://ldap-server
- Admin console url http://keycloak:8080/admin
  - login `admin/admin`
  - SAML authentication realm: `realm-idp`
    - Make sure you switch to the correct realm after login
  - External port needs to be same as docker internal port (8080) for generating `idp-metadata.xml`, which means we need to map jahia server to a different port (8081)
  - Note: there is a bug on the latest keycloak console version where clicking action buttons (e.g. save button) does not show toast notifications
    - To fix, need to execute this code in the browser console: `crypto.randomUUID = () => Math.floor(Math.random() * 294893849384)` (random large long integer number)
- Keycloak client ID (aka Relying Party identifier): `jahia-saml-client`
- To export any keycloak config changes:
  1. Get bash access to keycloak server: `docker exec -u root -it keycloak bash`
  2. Run export: `/opt/keycloak/bin/kc.sh export --dir /opt/keycloak/data/import --realm realm-idp`
  3. This will export files to a mapped volume folder in your local cypress test directory `volumes/keycloak`
  4. Saved config files will then get imported when container is started with command `start-dev --import-realm` (as part of docker compose file config)

## Jahia SAML Authentication setup

This section explains some of the values specified in the sample config `cypress/fixtures/samlLogin/org.jahia.modules.auth-samlTestSite.cfg` and where they are defined in the keycloak server.

### Relying Party Identifier

This is the client ID `jahia-saml-client` for the client created in `realm-idp`

### Keystore-related fields

  * derived from `realm-idp > clients > jahia-saml-client > keys`
  * Export signing keys using a JKS archive format with fields `Key Alias`, `Key password`, and `Store pasword` and import into corresponding Jahia SAML fields

### Identity provider metadata

  * Can be accessed from realm-idp > Realm settings > Endpoints > SAML 2.0 Identity Provider metadata and save as xml

In order to fully authenticate, you'll also need to include `login` SAML attribute as part of the SAML response. This is done by creating Client scope `saml-scope` and adding the `username-saml-mapper` attribute mapper (already included in `realm-idp`). This is then added as default client scope in `jahia-saml-client` (found under Clients > jahia-saml-client > Client scopes)

## Setup SAML server steps

- Create IDP Realm
- Sync LDAP user federation
  - LDAP setup
  - import users
- Create client
  - Create scope
    - create mapper
  - Create client

## Links

* [Keycloak docker repository](https://quay.io/repository/keycloak/keycloak)
* [LDAP browser - multiplatform](https://directory.apache.org/studio/)
