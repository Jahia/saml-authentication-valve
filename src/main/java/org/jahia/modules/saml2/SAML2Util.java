package org.jahia.modules.saml2;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.ClassLoaderUtils;
import org.opensaml.core.config.InitializationService;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public final class SAML2Util {

    private final HashMap<String, SAML2Client> clients = new HashMap<>();

    public String getAssertionConsumerServiceUrl(final HttpServletRequest request, final String incoming) {
        String serverName = request.getHeader("X-Forwarded-Server");
        if (StringUtils.isEmpty(serverName)) {
            serverName = request.getServerName();
        }

        try {
            URL url = new URL(request.getScheme(), serverName, request.getLocalPort(), request.getContextPath() + incoming);
            return url.toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get saml client.
     *
     * @param settingsService
     * @param request
     * @return
     */
    public SAML2Client getSAML2Client(final SettingsService settingsService, final HttpServletRequest request, String siteKey) {
        final SAML2Client client;
        if (clients.containsKey(siteKey)) {
            client = clients.get(siteKey);
        } else {
            final ConnectorConfig saml2Settings = settingsService.getConnectorConfig(siteKey, "Saml");
            client = initSAMLClient(saml2Settings, request);
            clients.put(siteKey, client);
        }
        return client;
    }

    public String getCookieValue(final HttpServletRequest request, final String name) {
        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Method to reset SAMLClient so that a new state {@link SAML2Client} can be generated, when it is requested the
     * next time.
     */
    public void resetClient(String siteKey) {
        clients.remove(siteKey);
    }

    public SAML2ClientConfiguration getSAML2ClientConfiguration(ConnectorConfig saml2Settings) {
        final SAML2ClientConfiguration saml2ClientConfiguration = new SAML2ClientConfiguration();

        saml2ClientConfiguration.setMaximumAuthenticationLifetime(Integer.parseInt(saml2Settings.getProperty(SAML2Constants.MAXIMUM_AUTHENTICATION_LIFETIME)));
        saml2ClientConfiguration.setIdentityProviderMetadataResource(new ByteArrayResource(saml2Settings.getBinaryProperty(SAML2Constants.IDENTITY_PROVIDER_METADATA)));
        saml2ClientConfiguration.setServiceProviderEntityId(saml2Settings.getProperty(SAML2Constants.RELYING_PARTY_IDENTIFIER));
        if (saml2Settings.getProperty(SAML2Constants.KEY_STORE) != null) {
            saml2ClientConfiguration.setKeystoreResource(new ByteArrayResource(saml2Settings.getBinaryProperty(SAML2Constants.KEY_STORE)));
        }
        saml2ClientConfiguration.setKeystoreType(saml2Settings.getProperty(SAML2Constants.KEY_STORE_TYPE));
        if (StringUtils.isNotEmpty(saml2Settings.getProperty(SAML2Constants.KEY_STORE_ALIAS))) {
            saml2ClientConfiguration.setKeystoreAlias(saml2Settings.getProperty(SAML2Constants.KEY_STORE_ALIAS));
        }
        saml2ClientConfiguration.setKeystorePassword(saml2Settings.getProperty(SAML2Constants.KEY_STORE_PASS));
        saml2ClientConfiguration.setPrivateKeyPassword(saml2Settings.getProperty(SAML2Constants.PRIVATE_KEY_PASS));
        saml2ClientConfiguration.setServiceProviderMetadataResource(new FileSystemResource(getSamlFileName(saml2Settings.getSiteKey(), "sp-metadata.xml")));
        saml2ClientConfiguration.setForceAuth(saml2Settings.getBooleanProperty(SAML2Constants.FORCE_AUTH));
        saml2ClientConfiguration.setPassive(saml2Settings.getBooleanProperty(SAML2Constants.PASSIVE));
        saml2ClientConfiguration.setAuthnRequestSigned(saml2Settings.getBooleanProperty(SAML2Constants.SIGN_AUTH_REQUEST));
        saml2ClientConfiguration.setWantsAssertionsSigned(saml2Settings.getBooleanProperty(SAML2Constants.REQUIRES_SIGNED_ASSERTIONS));
        saml2ClientConfiguration.setDestinationBindingType(saml2Settings.getProperty(SAML2Constants.BINDING_TYPE));

        return saml2ClientConfiguration;
    }

    /**
     * New method to Initializing saml client.
     *
     * @param saml2Settings
     * @param request
     */
    private SAML2Client initSAMLClient(ConnectorConfig saml2Settings, HttpServletRequest request) {
        return initSAMLClient(getSAML2ClientConfiguration(saml2Settings), getAssertionConsumerServiceUrl(request, saml2Settings.getProperty(SAML2Constants.INCOMING_TARGET_URL)));
    }

    private SAML2Client initSAMLClient(SAML2ClientConfiguration saml2ClientConfiguration, String callbackUrl) {
        return ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(), () -> {
            try {
                final File spMetadataFile = saml2ClientConfiguration.getServiceProviderMetadataResource().getFile();
                if (spMetadataFile.exists()) {
                    spMetadataFile.delete();
                }
            } catch (IOException e) {
                throw new TechnicalException("Cannot udpate SP Metadata file", e);
            }

            final SAML2Client client = new SAML2Client(saml2ClientConfiguration);
            client.setCallbackUrl(callbackUrl);
            try {
                client.init();
            } catch (NullPointerException e) {
                // Check if we have an NPE in DOMMetadataResolver, meaning we get an unknown XML element
                if (e.getStackTrace().length > 0 && e.getStackTrace()[0].getClassName().equals("org.opensaml.saml.metadata.resolver.impl.DOMMetadataResolver")) {
                    throw new TechnicalException("Error parsing idp Metadata - Invalid XML file", e);
                }
                throw e;
            }
            return client;
        });
    }

    public void validateSettings(ConnectorConfig settings) throws IOException {
        if (settings.getBinaryProperty(SAML2Constants.KEY_STORE) == null) {
            settings.getValues().setBinaryProperty(SAML2Constants.KEY_STORE, generateKeyStore(settings));
        }

        initSAMLClient(getSAML2ClientConfiguration(settings), "/");
    }

    private byte[] generateKeyStore(ConnectorConfig settings) throws IOException {
        File samlFileName = new File(getSamlFileName(settings.getSiteKey(), "keystore.jks"));
        samlFileName.getParentFile().mkdirs();
        SAML2ClientConfiguration saml2ClientConfiguration = getSAML2ClientConfiguration(settings);
        saml2ClientConfiguration.setKeystoreResource(new FileSystemResource(samlFileName));
        initSAMLClient(saml2ClientConfiguration, "/");
        byte[] s = FileUtils.readFileToByteArray(samlFileName);
        samlFileName.delete();
        return s;

    }

    private String getSamlFileName(String siteKey, String filename) {
        return SettingsBean.getInstance().getJahiaVarDiskPath() + "/saml/" + siteKey + "." + filename;
    }
}
