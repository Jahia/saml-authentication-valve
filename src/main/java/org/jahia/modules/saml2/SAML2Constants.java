package org.jahia.modules.saml2;

public final class SAML2Constants {

    public static final String ENABLED = "enabled";
    public static final String IDENTITY_PROVIDER_METADATA = "identityProviderMetadata";
    public static final String INCOMING_TARGET_URL = "incomingTargetUrl";
    public static final String KEY_STORE = "keyStore";
    public static final String KEY_STORE_PASS = "keyStorePass";
    public static final String PRIVATE_KEY_PASS = "privateKeyPass";
    public static final String RELYING_PARTY_IDENTIFIER = "relyingPartyIdentifier";
    public static final String POST_LOGIN_PATH = "loginSuccessPath";
    public static final String MAXIMUM_AUTHENTICATION_LIFETIME = "maximumAuthenticationLifetime";
    public static final String SETTINGS_NODE_NAME = "saml2-settings";
    public static final String SETTINGS_NODE_TYPE = "saml2nt:settings";
    public static final String SETTINGS_SAML2 = "saml2:";
    public static final String SETTINGS_SAML2_IDENTITY_PROVIDER_METADATA = SETTINGS_SAML2 + IDENTITY_PROVIDER_METADATA;
    public static final String SETTINGS_SAML2_INCOMMING_TARGET_URL = SETTINGS_SAML2 + INCOMING_TARGET_URL;
    public static final String SETTINGS_SAML2_KEY_STORE = SETTINGS_SAML2 + KEY_STORE;
    public static final String SETTINGS_SAML2_KEY_STORE_PASS = SETTINGS_SAML2 + KEY_STORE_PASS;
    public static final String SETTINGS_SAML2_MAXIMUM_AUTHENTICATION_LIFETIME = SETTINGS_SAML2 + MAXIMUM_AUTHENTICATION_LIFETIME;
    public static final String SETTINGS_SAML2_POST_LOGIN_PATH = SETTINGS_SAML2 + POST_LOGIN_PATH;
    public static final String SETTINGS_SAML2_PRIVATE_KEY_PASS = SETTINGS_SAML2 + PRIVATE_KEY_PASS;
    public static final String SETTINGS_SAML2_RELYING_PARTY_IDENTIFIER = SETTINGS_SAML2 + RELYING_PARTY_IDENTIFIER;
    public static final String SITE = "site";

    private SAML2Constants() {
    }
}
