package org.jahia.modules.saml2.internal;

public final class SAML2Constants {

    public static final String SITEKEY = "siteKey";
    public static final String ENABLED = "enabled";
    public static final String IDENTITY_PROVIDER_METADATA = "identityProviderMetadata";
    public static final String INCOMING_TARGET_URL = "incomingTargetUrl";
    public static final String KEY_STORE = "keyStore";
    public static final String KEY_STORE_TYPE = "keyStoreType";
    public static final String KEY_STORE_ALIAS = "keyStoreAlias";
    public static final String KEY_STORE_PASS = "keyStorePass";
    public static final String PRIVATE_KEY_PASS = "privateKeyPass";
    public static final String RELYING_PARTY_IDENTIFIER = "relyingPartyIdentifier";
    public static final String SERVER_LOCATION = "serverLocation";
    public static final String POST_LOGIN_PATH = "postLoginPath";
    public static final String MAPPER_NAME = "mapperName";
    public static final String MAPPER_ID_FIELD = "mapperIdField";
    /**
     * The property carrying the NameID of the assertion. SAML states the subject of an assertion in
     * its NameID, and the attributes beside it are whatever the identity provider was configured to
     * send.
     */
    public static final String NAME_ID = "nameID";
    public static final String MAXIMUM_AUTHENTICATION_LIFETIME = "maximumAuthenticationLifetime";
    public static final String FORCE_AUTH = "forceAuth";
    public static final String PASSIVE = "passive";
    public static final String SIGN_AUTH_REQUEST = "signAuthnRequest";
    public static final String BINDING_TYPE = "bindingType";
    public static final String REQUIRES_SIGNED_ASSERTIONS = "requireSignedAssertions";
    public static final String REQUIRES_SIGNED_RESPONSES = "requireSignedResponses";

    public static final String REDIRECT = "redirect";
    public static final String SITE = "site";

    private SAML2Constants() {
    }
}
