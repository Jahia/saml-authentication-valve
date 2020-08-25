package org.jahia.modules.saml2.admin;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.jahia.modules.jahiaoauth.service.MapperService;
import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.osgi.FrameworkService;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.tools.files.FileUpload;
import org.jahia.utils.ClassLoaderUtils;
import org.json.JSONObject;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.Run;
import pl.touk.throwing.ThrowingFunction;
import pl.touk.throwing.ThrowingSupplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SAML2SettingsAction extends Action {

    private static final Logger logger = LoggerFactory.getLogger(SAML2SettingsAction.class);
    private final Map<String, String> bindings;
    private final Map<String, String> keyStoreTypes;
    private SAML2SettingsService saml2SettingsService;

    public SAML2SettingsAction() {
        bindings = new LinkedHashMap<>();
        bindings.put(SAMLConstants.SAML2_POST_BINDING_URI, "POST");
        bindings.put(SAMLConstants.SAML2_REDIRECT_BINDING_URI, "Redirect");
        bindings.put(SAMLConstants.SAML2_ARTIFACT_BINDING_URI, "Artifact");
        bindings.put(SAMLConstants.SAML2_SOAP11_BINDING_URI, "SOAP");
        bindings.put(SAMLConstants.SAML2_PAOS_BINDING_URI, "PAOS");
        bindings.put(SAMLConstants.SAML2_POST_SIMPLE_SIGN_BINDING_URI, "POST-SimpleSign");

        keyStoreTypes = new LinkedHashMap<>();
        keyStoreTypes.put("JKS", "JKS");
        keyStoreTypes.put("JCEKS", "JCEKS");
        keyStoreTypes.put("PKCS12", "PKCS12");
        keyStoreTypes.put("PKCS12S2", "PKCS12S2");
    }

    @Override
    public ActionResult doExecute(final HttpServletRequest request, final RenderContext renderContext, final Resource resource, final JCRSessionWrapper session, Map<String, List<String>> parameters, final URLResolver urlResolver) throws Exception {
        try {
            SAML2Settings serverSettings;
            final String siteKey = renderContext.getSite().getSiteKey();
            // if payload has content, it means an update.
            if (parameters.get(SAML2Constants.ENABLED) != null) {
                final SAML2Settings oldSettings = saml2SettingsService.getSettings(siteKey);
                serverSettings = ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(),() ->
                    saveSettings(parameters, (FileUpload) request.getAttribute("fileUpload"), siteKey, oldSettings)
                );
            } else {
                serverSettings = saml2SettingsService.getSettings(siteKey);
                if (serverSettings == null) {
                    serverSettings = saml2SettingsService.createSAML2Settings(siteKey);
                }
            }

            final JSONObject resp = new JSONObject();
            resp.put(SAML2Constants.ENABLED, serverSettings.getEnabled());
            resp.put(SAML2Constants.RELYING_PARTY_IDENTIFIER, serverSettings.getRelyingPartyIdentifier());
            resp.put(SAML2Constants.INCOMING_TARGET_URL, serverSettings.getIncomingTargetUrl());
            resp.put(SAML2Constants.KEY_STORE_TYPE, serverSettings.getKeyStoreType());
            resp.put(SAML2Constants.KEY_STORE_ALIAS, serverSettings.getKeyStoreAlias());
            resp.put(SAML2Constants.KEY_STORE_PASS, serverSettings.getKeyStorePass());
            resp.put(SAML2Constants.MAXIMUM_AUTHENTICATION_LIFETIME, serverSettings.getMaximumAuthenticationLifetime());
            resp.put(SAML2Constants.PRIVATE_KEY_PASS, serverSettings.getPrivateKeyPass());
            resp.put(SAML2Constants.POST_LOGIN_PATH, serverSettings.getPostLoginPath());
            resp.put(SAML2Constants.FORCE_AUTH, serverSettings.isForceAuth());
            resp.put(SAML2Constants.PASSIVE, serverSettings.isPassive());
            resp.put(SAML2Constants.REQUIRES_SIGNED_ASSERTIONS, serverSettings.isRequireSignedAssertions());
            resp.put(SAML2Constants.SIGN_AUTH_REQUEST, serverSettings.isSignAuthnRequest());
            resp.put(SAML2Constants.BINDING_TYPE, serverSettings.getBindingType());
            resp.put(SAML2Constants.MAPPER_NAME, serverSettings.getMapperName());
            resp.put("availableMappers", getMapperNames());
            resp.put("availableBindings", bindings);
            resp.put("availableKeyStoreTypes", keyStoreTypes);

            return new ActionResult(HttpServletResponse.SC_OK, null, resp);
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            if (logger.isDebugEnabled()) {
                logger.debug("error while saving settings", e);
            }
            if (e.getMessage() != null) {
                error.put("error", e.getMessage());
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    error.put("error", e.getMessage() + " - " + e.getCause().getMessage());
                }
            } else {
                error.put("error", "Error when saving");
            }
            error.put("type", e.getClass().getSimpleName());
            return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, error);
        }
    }

    public Map<String, String> getMapperNames() {
        try {
            Map<String, String> mappers = new LinkedHashMap<>();
            mappers.put("default", "SAML ID");
            mappers.putAll(FrameworkService.getBundleContext().getServiceReferences(MapperService.class, null).stream().filter(ref -> ref.getProperty(JahiaOAuthConstants.MAPPER_SERVICE_NAME) != null).map(ref -> (String) ref.getProperty(JahiaOAuthConstants.MAPPER_SERVICE_NAME)).collect(Collectors.toMap(Function.identity(), Function.identity())));
            return mappers;
        } catch (InvalidSyntaxException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    private SAML2Settings saveSettings(Map<String, List<String>> parameters, FileUpload fup, String siteKey, SAML2Settings oldSettings) {
        SAML2Settings serverSettings;
        try {
            serverSettings = oldSettings != null ? oldSettings : saml2SettingsService.createSAML2Settings(siteKey);
            setProperty(parameters, SAML2Constants.ENABLED, s -> serverSettings.setEnabled(Boolean.parseBoolean(s)));
            setFile(fup, SAML2Constants.IDENTITY_PROVIDER_METADATA, serverSettings::setIdentityProviderMetadataFile);
            setProperty(parameters, SAML2Constants.RELYING_PARTY_IDENTIFIER, serverSettings::setRelyingPartyIdentifier);
            setProperty(parameters, SAML2Constants.INCOMING_TARGET_URL, serverSettings::setIncomingTargetUrl);
            setFile(fup, SAML2Constants.KEY_STORE, serverSettings::setKeyStoreFile);
            setProperty(parameters, SAML2Constants.KEY_STORE_TYPE, serverSettings::setKeyStoreType);
            setProperty(parameters, SAML2Constants.KEY_STORE_ALIAS, serverSettings::setKeyStoreAlias);
            setProperty(parameters, SAML2Constants.KEY_STORE_PASS, serverSettings::setKeyStorePass);
            setProperty(parameters, SAML2Constants.PRIVATE_KEY_PASS, serverSettings::setPrivateKeyPass);
            setProperty(parameters, SAML2Constants.POST_LOGIN_PATH, serverSettings::setPostLoginPath);
            setProperty(parameters, SAML2Constants.MAXIMUM_AUTHENTICATION_LIFETIME, s -> serverSettings.setMaximumAuthenticationLifetime(Long.parseLong(s)));
            setProperty(parameters, SAML2Constants.FORCE_AUTH, s -> serverSettings.setForceAuth(Boolean.parseBoolean(s)));
            setProperty(parameters, SAML2Constants.PASSIVE, s -> serverSettings.setPassive(Boolean.parseBoolean(s)));
            setProperty(parameters, SAML2Constants.REQUIRES_SIGNED_ASSERTIONS, s -> serverSettings.setRequireSignedAssertions(Boolean.parseBoolean(s)));
            setProperty(parameters, SAML2Constants.SIGN_AUTH_REQUEST, s -> serverSettings.setSignAuthnRequest(Boolean.parseBoolean(s)));
            setProperty(parameters, SAML2Constants.BINDING_TYPE, serverSettings::setBindingType);
            setProperty(parameters, SAML2Constants.MAPPER_NAME, serverSettings::setMapperName);
            saml2SettingsService.saveSAML2Settings(serverSettings);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return serverSettings;
    }

    private void setProperty(final Map<String, List<String>> settings, final String propertyName, Consumer<String> c) {
        if (settings.containsKey(propertyName)) {
            String s = settings.get(propertyName).iterator().next();
            c.accept(s);
        }
    }

    private void setFile(final FileUpload fup, final String propertyName, Consumer<File> c) throws IOException {
        if (fup.getFileItems().containsKey(propertyName)) {
            File s = fup.getFileItems().get(propertyName).getStoreLocation();
            c.accept(s);
        }
    }

    public void setSaml2SettingsService(final SAML2SettingsService saml2SettingsService) {
        this.saml2SettingsService = saml2SettingsService;
    }
}
