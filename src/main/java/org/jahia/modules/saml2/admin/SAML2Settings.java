package org.jahia.modules.saml2.admin;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

public final class SAML2Settings {

    private static final Logger logger = LoggerFactory.getLogger(SAML2Settings.class);
    public static final String SITES = "/sites/";
    private final String siteKey;
    private final SAML2Util util;
    private boolean enabled;
    private String identityProviderPath;
    private String incomingTargetUrl;
    private String keyStoreLocation;
    private String keyStorePass;
    private String postLoginPath;
    private String privateKeyPass;
    private String relyingPartyIdentifier;
    private String spMetaDataLocation;
    private Double maximumAuthenticationLifetime;

    public SAML2Settings(final String siteKey, SAML2Util util) {
        this.siteKey = siteKey;
        // TODO: should be getting the value for this property from the JCR
        this.enabled = true;
        this.util = util;
    }

    public void remove() {
        try {
            // store settings
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {
                public Boolean doInJCR(final JCRSessionWrapper session) throws RepositoryException {
                    final JCRNodeWrapper settingsNode = session.getNode(SITES + siteKey + "/" + SAML2Constants.SETTINGS_NODE_NAME);
                    settingsNode.remove();
                    session.save();
                    return Boolean.TRUE;
                }
            });
        } catch (RepositoryException e) {
            logger.error("Error deleting context server settings into the repository.", e);
        }
    }

    public boolean load() {
        try {
            // read default settings
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, null, new JCRCallback<Boolean>() {
                public Boolean doInJCR(final JCRSessionWrapper session) throws RepositoryException {
                    if (session.nodeExists(SITES + siteKey) && session.nodeExists(SITES + siteKey + "/" + SAML2Constants.SETTINGS_NODE_NAME)) {
                        load(session.getNode(SITES + siteKey + "/" + SAML2Constants.SETTINGS_NODE_NAME));
                        return Boolean.TRUE;
                    }
                    return Boolean.FALSE;
                }
            });
        } catch (RepositoryException e) {
            logger.error("Error reading settings from the repository.", e);
        }
        return false;
    }

    private void load(JCRNodeWrapper settingsNode) throws RepositoryException {
        if (settingsNode.hasProperty(SAML2Constants.SETTINGS_SAML2_IDENTITY_PROVIDER_URL)) {
            identityProviderPath = settingsNode.getProperty(SAML2Constants.SETTINGS_SAML2_IDENTITY_PROVIDER_URL).getString();
        }
        if (settingsNode.hasProperty(SAML2Constants.SETTINGS_SAML2_RELYING_PARTY_IDENTIFIER)) {
            relyingPartyIdentifier = settingsNode.getProperty(SAML2Constants.SETTINGS_SAML2_RELYING_PARTY_IDENTIFIER).getString();
        }
        if (settingsNode.hasProperty(SAML2Constants.SETTINGS_SAML2_INCOMMING_TARGET_URL)) {
            incomingTargetUrl = settingsNode.getProperty(SAML2Constants.SETTINGS_SAML2_INCOMMING_TARGET_URL).getString();
        }
        if (settingsNode.hasProperty(SAML2Constants.SETTINGS_SAML2_SP_META_DATA_LOCATION)) {
            spMetaDataLocation = settingsNode.getProperty(SAML2Constants.SETTINGS_SAML2_SP_META_DATA_LOCATION).getString();
        }
        if (settingsNode.hasProperty(SAML2Constants.SETTINGS_SAML2_KEY_STORE_LOCATION)) {
            keyStoreLocation = settingsNode.getProperty(SAML2Constants.SETTINGS_SAML2_KEY_STORE_LOCATION).getString();
        }
        if (settingsNode.hasProperty(SAML2Constants.SETTINGS_SAML2_PRIVATE_KEY_PASS)) {
            privateKeyPass = settingsNode.getProperty(SAML2Constants.SETTINGS_SAML2_PRIVATE_KEY_PASS).getString();
        }
        if (settingsNode.hasProperty(SAML2Constants.SETTINGS_SAML2_KEY_STORE_PASS)) {
            keyStorePass = settingsNode.getProperty(SAML2Constants.SETTINGS_SAML2_KEY_STORE_PASS).getString();
        }
        if (settingsNode.hasProperty(SAML2Constants.SETTINGS_SAML2_POST_LOGIN_PATH)) {
            postLoginPath = settingsNode.getProperty(SAML2Constants.SETTINGS_SAML2_POST_LOGIN_PATH).getString();
        }
        if (settingsNode.hasProperty(SAML2Constants.SETTINGS_SAML2_MAXIMUM_AUTHENTICATION_LIFETIME)) {
            maximumAuthenticationLifetime = settingsNode.getProperty(SAML2Constants.SETTINGS_SAML2_MAXIMUM_AUTHENTICATION_LIFETIME).getDouble();
        }
    }

    public void store() {
        try {
            // store default props
            JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, null, new JCRCallback<Boolean>() {
                public Boolean doInJCR(final JCRSessionWrapper session) throws RepositoryException {
                    final JCRNodeWrapper settingsNode;
                    if (session.nodeExists(SITES + siteKey + "/" + SAML2Constants.SETTINGS_NODE_NAME)) {
                        settingsNode = session.getNode(SITES + siteKey + "/" + SAML2Constants.SETTINGS_NODE_NAME);
                    } else {
                        settingsNode = session.getNode(SITES + siteKey).addNode(SAML2Constants.SETTINGS_NODE_NAME, SAML2Constants.SETTINGS_NODE_TYPE);
                    }

                    boolean doSave = setProperty(settingsNode, SAML2Constants.SETTINGS_SAML2_IDENTITY_PROVIDER_URL, getIdentityProviderPath());
                    doSave |= setProperty(settingsNode, SAML2Constants.SETTINGS_SAML2_RELYING_PARTY_IDENTIFIER, getRelyingPartyIdentifier());
                    doSave |= setProperty(settingsNode, SAML2Constants.SETTINGS_SAML2_INCOMMING_TARGET_URL, getIncomingTargetUrl());
                    doSave |= setProperty(settingsNode, SAML2Constants.SETTINGS_SAML2_SP_META_DATA_LOCATION, getSpMetaDataLocation());
                    doSave |= setProperty(settingsNode, SAML2Constants.SETTINGS_SAML2_KEY_STORE_LOCATION, getKeyStoreLocation());
                    doSave |= setProperty(settingsNode, SAML2Constants.SETTINGS_SAML2_KEY_STORE_PASS, getKeyStorePass());
                    doSave |= setProperty(settingsNode, SAML2Constants.SETTINGS_SAML2_PRIVATE_KEY_PASS, getPrivateKeyPass());
                    doSave |= setProperty(settingsNode, SAML2Constants.SETTINGS_SAML2_POST_LOGIN_PATH, getPostLoginPath());
                    doSave |= setProperty(settingsNode, SAML2Constants.SETTINGS_SAML2_MAXIMUM_AUTHENTICATION_LIFETIME, String.valueOf(getMaximumAuthenticationLifetime()));

                    if (doSave) {
                        session.save();
                    }
                    return Boolean.TRUE;
                }
            });
        } catch (RepositoryException e) {
            logger.error("Error storing settings into the repository.", e);
        }
        util.resetClient(siteKey);
    }

    private boolean setProperty(final JCRNodeWrapper node, final String propertyName, final String value) throws RepositoryException {
        if ((!node.hasProperty(propertyName) && StringUtils.isNotEmpty(value)) || (node.hasProperty(propertyName) && !StringUtils.equals(node.getProperty(propertyName).getString(), value))) {
            if (StringUtils.isEmpty(value)) {
                node.getProperty(propertyName).remove();
            } else {
                node.setProperty(propertyName, value);
            }
            return true;
        }
        return false;
    }

    public String getIdentityProviderPath() {
        return identityProviderPath;
    }

    public void setIdentityProviderPath(final String identityProviderPath) {
        this.identityProviderPath = identityProviderPath;
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

    public String getSpMetaDataLocation() {
        return spMetaDataLocation;
    }

    public void setSpMetaDataLocation(final String spMetaDataLocation) {
        this.spMetaDataLocation = spMetaDataLocation;
    }

    public String getKeyStoreLocation() {
        return keyStoreLocation;
    }

    public void setKeyStoreLocation(final String keyStoreLocation) {
        this.keyStoreLocation = keyStoreLocation;
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

    public Double getMaximumAuthenticationLifetime() {
        return maximumAuthenticationLifetime;
    }

    public void setMaximumAuthenticationLifetime(Double maximumAuthenticationLifetime) {
        this.maximumAuthenticationLifetime = maximumAuthenticationLifetime;
    }
}
