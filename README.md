# SAML Authentication Valve
This community module (not supported by Jahia) is meant to activate SAML on one or multiples Jahia websites.


### 1. Keystore Generation

Create a keystore: 
```sh
keytool -genkeypair -alias jahiakeystorealias -keypass changeit -keystore sp.jks -storepass changeit -keyalg RSA -keysize 2048 -validity 3650
```

#### :warning: Important Input Step :warning:
- What is your first and last name?: ***jahia.server.name***

***This value must match your Jahia site domain name***

## Valve Configuration

#### Identity Provider MetaData file
The Identity Provider Metadata file is provided by the Identity Provider (IdP), for example Shibboleth or Google. This XML file must be uploaded here.

#### Relying Party Identifier
The value is arbitrary.

#### Incoming Target Url
This is the URL when the Idp will return the SAML response. Its default value is /home.samlCallback.do

#### Keystore, Alias, Password of the Keystore and Password of the Private Key
Those value must match the one defined when creating the server key and certificate.

#### Redirect after successful login
This is the DX relative ULR where the user will be redirect after successfully authentication. (For example /home.html)

#### Maximum authentication lifetime
The maximum age of the authentication on the IdP. User will be asked to reauthenticate if the session on the IdP is older than the specified time. 

#### Force authentication
If set, authentication will be asked everytime even if the user has already a session on the IdP.

#### Passive
The user will transparently log in, without any interaction. User will be authenticated only if the IdP is able to do it without asking the user.

#### Sign authentication request
Sign the request sent to the IdP.

#### Requires signed assertions
Will only accept signed assertions from the IdP.

#### Binding type
SAML binding type used to communicate iwth IdP.

#### User mapper
How to map the user data (can create a user in JCR or LDAP)

## Configuration example

- Identity Provider MetaData :`GoogleIDPMetadata-jahia.com.xml`
- Relying Party Identifier: `test.jahia.com`
- Incoming Target Url: `/home.samlCallback.do`
- Keystore: `sp.jks`
- Key Store Pass: `changeit`
- Private Key Pass: `changeit`
- Redirect path after successful login: `/`
- Maximum authentication lifetime: `2073600`
- User mapper : `jcrOauthProvider`
