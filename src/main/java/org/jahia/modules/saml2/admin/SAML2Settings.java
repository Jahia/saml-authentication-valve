package org.jahia.modules.saml2.admin;

import java.util.Map;

public final class SAML2Settings {
    private String previousSiteKey;
    private String siteKey;
    private SAML2SettingsService saml2SettingsService;
    private boolean enabled;
    private String identityProviderMetadata;
    private String incomingTargetUrl;
    private String keyStore;
    private String keyStorePass;
    private String postLoginPath;
    private String privateKeyPass;
    private String relyingPartyIdentifier;
    private Long maximumAuthenticationLifetime;

    public void init() {
        System.out.println("register: "+ siteKey);
        saml2SettingsService.registerServerSettings(this);
        this.previousSiteKey = siteKey;
    }

    public void destroy() {
        saml2SettingsService.removeServerSettings(getSiteKey());
    }

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

    public void setSaml2SettingsService(SAML2SettingsService saml2SettingsService) {
        this.saml2SettingsService = saml2SettingsService;
    }
}

