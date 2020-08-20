package org.jahia.modules.saml2.admin;

import org.opensaml.saml.common.xml.SAMLConstants;

import java.util.Map;

public final class SAML2Settings {
    private String previousSiteKey;
    private String siteKey;
    private SAML2SettingsService saml2SettingsService;
    private boolean enabled = false;
    private String identityProviderMetadata;
    private String incomingTargetUrl = "/home.samlCallback.do";
    private String keyStore;
    private String keyStoreAlias = "saml2clientconfiguration";
    private String keyStorePass = "changeit";
    private String privateKeyPass = "changeit";
    private String postLoginPath = "/";
    private String relyingPartyIdentifier;
    private Long maximumAuthenticationLifetime = 86400L;
    private boolean forceAuth = false;
    private boolean passive = false;
    private boolean signAuthnRequest = true;
    private boolean requireSignedAssertions = false;
    private String bindingType = SAMLConstants.SAML2_POST_BINDING_URI;
    private String mapperName;

    public void init() {
        saml2SettingsService.registerServerSettings(this);
        this.previousSiteKey = siteKey;
    }

    public void destroy() {
        saml2SettingsService.removeServerSettings(getSiteKey());
    }

    @SuppressWarnings("java:S1172")
    public void update(Map<String, Object> map) {
        if (previousSiteKey != null) {
            saml2SettingsService.removeServerSettings(previousSiteKey);
        }
        init();
    }

    public String getSiteKey() {
        return siteKey;
    }

    public void setSiteKey(String siteKey) {
        this.siteKey = siteKey;
    }

    public String getIdentityProviderMetadata() {
        return identityProviderMetadata;
    }

    public void setIdentityProviderMetadata(final String identityProviderMetadata) {
        this.identityProviderMetadata = identityProviderMetadata;
    }

    public String getRelyingPartyIdentifier() {
        return relyingPartyIdentifier;
    }

    public void setRelyingPartyIdentifier(final String relyingPartyIdentifier) {
        this.relyingPartyIdentifier = relyingPartyIdentifier;
    }

    public String getIncomingTargetUrl() {
        return incomingTargetUrl;
    }

    public void setIncomingTargetUrl(final String incomingTargetUrl) {
        this.incomingTargetUrl = incomingTargetUrl;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(final String keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStoreAlias() {
        return keyStoreAlias;
    }

    public void setKeyStoreAlias(String keyStoreAlias) {
        this.keyStoreAlias = keyStoreAlias;
    }

    public String getKeyStorePass() {
        return keyStorePass;
    }

    public void setKeyStorePass(final String keyStorePass) {
        this.keyStorePass = keyStorePass;
    }

    public String getPrivateKeyPass() {
        return privateKeyPass;
    }

    public void setPrivateKeyPass(final String privateKeyPass) {
        this.privateKeyPass = privateKeyPass;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getPostLoginPath() {
        return postLoginPath;
    }

    public void setPostLoginPath(String postLoginPath) {
        this.postLoginPath = postLoginPath;
    }

    public Long getMaximumAuthenticationLifetime() {
        return maximumAuthenticationLifetime;
    }

    public void setMaximumAuthenticationLifetime(Long maximumAuthenticationLifetime) {
        this.maximumAuthenticationLifetime = maximumAuthenticationLifetime;
    }

    public boolean isForceAuth() {
        return forceAuth;
    }

    public void setForceAuth(boolean forceAuth) {
        this.forceAuth = forceAuth;
    }

    public boolean isPassive() {
        return passive;
    }

    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    public boolean isSignAuthnRequest() {
        return signAuthnRequest;
    }

    public void setSignAuthnRequest(boolean signAuthnRequest) {
        this.signAuthnRequest = signAuthnRequest;
    }

    public boolean isRequireSignedAssertions() {
        return requireSignedAssertions;
    }

    public void setRequireSignedAssertions(boolean requireSignedAssertions) {
        this.requireSignedAssertions = requireSignedAssertions;
    }

    public String getBindingType() {
        return bindingType;
    }

    public void setBindingType(String bindingType) {
        this.bindingType = bindingType;
    }

    public String getMapperName() {
        return mapperName;
    }

    public void setMapperName(String mapperName) {
        this.mapperName = mapperName;
    }

    public void setSaml2SettingsService(SAML2SettingsService saml2SettingsService) {
        this.saml2SettingsService = saml2SettingsService;
    }

}

