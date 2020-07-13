package org.jahia.modules.saml2.admin;

import org.apache.commons.io.FileUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.tools.files.FileUpload;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class SAML2SettingsAction extends Action {

    private static final Logger logger = LoggerFactory.getLogger(SAML2SettingsAction.class);
    private SAML2SettingsService saml2SettingsService;

    @Override
    public ActionResult doExecute(final HttpServletRequest request, final RenderContext renderContext, final Resource resource, final JCRSessionWrapper session, Map<String, List<String>> parameters, final URLResolver urlResolver) throws Exception {
        try {
            FileUpload fup = (FileUpload) request.getAttribute("fileUpload");
            final SAML2Settings serverSettings;
            final String siteKey = renderContext.getSite().getSiteKey();
            // if payload has content, it means an update.
            if (parameters.get(SAML2Constants.ENABLED) != null) {
                final SAML2Settings oldSettings = saml2SettingsService.getSettings(siteKey);
                serverSettings = saveSettings(parameters, fup, siteKey, oldSettings);
            } else {
                serverSettings = saml2SettingsService.getSettings(siteKey);
            }

            final JSONObject resp = new JSONObject();
            if (serverSettings != null) {
                resp.put(SAML2Constants.ENABLED, serverSettings.getEnabled());
                resp.put(SAML2Constants.RELYING_PARTY_IDENTIFIER, serverSettings.getRelyingPartyIdentifier());
                resp.put(SAML2Constants.INCOMING_TARGET_URL, serverSettings.getIncomingTargetUrl());
                resp.put(SAML2Constants.KEY_STORE_PASS, serverSettings.getKeyStorePass());
                resp.put(SAML2Constants.MAXIMUM_AUTHENTICATION_LIFETIME, serverSettings.getMaximumAuthenticationLifetime());
                resp.put(SAML2Constants.PRIVATE_KEY_PASS, serverSettings.getPrivateKeyPass());
                resp.put(SAML2Constants.POST_LOGIN_PATH, serverSettings.getPostLoginPath());
            }
            resp.put("noConf", serverSettings == null);
            return new ActionResult(HttpServletResponse.SC_OK, null, resp);
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            if (logger.isDebugEnabled()) {
                logger.debug("error while saving settings", e);
            }
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, error);
        }
    }

    private SAML2Settings saveSettings(Map<String, List<String>> parameters, FileUpload fup, String siteKey, SAML2Settings oldSettings) throws IOException {
        SAML2Settings serverSettings;
        serverSettings = saml2SettingsService.createSAML2Settings(siteKey);
        serverSettings.setEnabled(Boolean.parseBoolean(getSettingOrDefault(parameters, SAML2Constants.ENABLED, Boolean.toString(oldSettings != null && oldSettings.getEnabled()))));
        if (fup.getFileItems().containsKey(SAML2Constants.IDENTITY_PROVIDER_METADATA)) {
            serverSettings.setIdentityProviderMetadata(Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(fup.getFileItems().get(SAML2Constants.IDENTITY_PROVIDER_METADATA).getStoreLocation())));
        } else if (oldSettings != null) {
            serverSettings.setIdentityProviderMetadata(oldSettings.getIdentityProviderMetadata());
        }
        serverSettings.setRelyingPartyIdentifier(getSettingOrDefault(parameters, SAML2Constants.RELYING_PARTY_IDENTIFIER, (oldSettings != null ? oldSettings.getRelyingPartyIdentifier() : "")));
        serverSettings.setIncomingTargetUrl(getSettingOrDefault(parameters, SAML2Constants.INCOMING_TARGET_URL, (oldSettings != null ? oldSettings.getIncomingTargetUrl() : "")));
        if (fup.getFileItems().containsKey(SAML2Constants.KEY_STORE)) {
            serverSettings.setKeyStore(Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(fup.getFileItems().get(SAML2Constants.KEY_STORE).getStoreLocation())));
        } else if (oldSettings != null) {
            serverSettings.setKeyStore(oldSettings.getKeyStore());
        }
        serverSettings.setKeyStorePass(getSettingOrDefault(parameters, SAML2Constants.KEY_STORE_PASS, (oldSettings != null ? oldSettings.getKeyStorePass() : "")));
        serverSettings.setPrivateKeyPass(getSettingOrDefault(parameters, SAML2Constants.PRIVATE_KEY_PASS, (oldSettings != null ? oldSettings.getPrivateKeyPass() : "")));
        serverSettings.setPostLoginPath(getSettingOrDefault(parameters, SAML2Constants.POST_LOGIN_PATH, (oldSettings != null ? oldSettings.getPostLoginPath() : "")));
        serverSettings.setMaximumAuthenticationLifetime(Long.parseLong(getSettingOrDefault(parameters, SAML2Constants.MAXIMUM_AUTHENTICATION_LIFETIME, Long.toString(oldSettings != null ? oldSettings.getMaximumAuthenticationLifetime() : 0))));
        saml2SettingsService.saveSAML2Settings(serverSettings);
        return serverSettings;
    }

    private String getSettingOrDefault(final Map<String, List<String>> settings, final String propertyName, final String defaultValue) {
        return settings.getOrDefault(propertyName, Collections.singletonList(defaultValue)).iterator().next();
    }

    public void setSaml2SettingsService(final SAML2SettingsService saml2SettingsService) {
        this.saml2SettingsService = saml2SettingsService;
    }
}
