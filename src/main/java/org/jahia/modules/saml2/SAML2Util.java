package org.jahia.modules.saml2;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.saml2.admin.SAML2Settings;
import org.jahia.modules.saml2.admin.SAML2SettingsService;
import org.jahia.settings.SettingsBean;
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
import java.util.Base64;
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
     * @param saml2SettingsService
     * @param request
     * @return
     */
    public SAML2Client getSAML2Client(final SAML2SettingsService saml2SettingsService, final HttpServletRequest request, String siteKey) {
        final SAML2Client client;
        if (clients.containsKey(siteKey)) {
            client = clients.get(siteKey);
        } else {
            final SAML2Settings saml2Settings = saml2SettingsService.getSettings(siteKey);
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

    public SAML2ClientConfiguration getSAML2ClientConfiguration(SAML2Settings saml2Settings) {
        final SAML2ClientConfiguration saml2ClientConfiguration = new SAML2ClientConfiguration();

        saml2ClientConfiguration.setMaximumAuthenticationLifetime(saml2Settings.getMaximumAuthenticationLifetime().intValue());
        saml2ClientConfiguration.setIdentityProviderMetadataResource(new ByteArrayResource(Base64.getDecoder().decode(saml2Settings.getIdentityProviderMetadata())));
        saml2ClientConfiguration.setServiceProviderEntityId(saml2Settings.getRelyingPartyIdentifier());
        if (saml2Settings.getKeyStore() != null) {
            saml2ClientConfiguration.setKeystoreResource(new ByteArrayResource(Base64.getDecoder().decode(saml2Settings.getKeyStore())));
        }
        saml2ClientConfiguration.setKeystoreType(saml2Settings.getKeyStoreType());
        if (StringUtils.isNotEmpty(saml2Settings.getKeyStoreAlias())) {
            saml2ClientConfiguration.setKeystoreAlias(saml2Settings.getKeyStoreAlias());
        }
        saml2ClientConfiguration.setKeystorePassword(saml2Settings.getKeyStorePass());
        saml2ClientConfiguration.setPrivateKeyPassword(saml2Settings.getPrivateKeyPass());
        saml2ClientConfiguration.setServiceProviderMetadataResource(new FileSystemResource(getSamlFileName(saml2Settings.getSiteKey(), "sp-metadata.xml")));
        saml2ClientConfiguration.setForceAuth(saml2Settings.isForceAuth());
        saml2ClientConfiguration.setPassive(saml2Settings.isPassive());
        saml2ClientConfiguration.setAuthnRequestSigned(saml2Settings.isSignAuthnRequest());
        saml2ClientConfiguration.setWantsAssertionsSigned(saml2Settings.isRequireSignedAssertions());
        saml2ClientConfiguration.setDestinationBindingType(saml2Settings.getBindingType());

        return saml2ClientConfiguration;
    }

    /**
     * New method to Initializing saml client.
     *
     * @param saml2Settings
     * @param request
     */
    private SAML2Client initSAMLClient(SAML2Settings saml2Settings, HttpServletRequest request) {
        return initSAMLClient(getSAML2ClientConfiguration(saml2Settings), getAssertionConsumerServiceUrl(request, saml2Settings.getIncomingTargetUrl()));
    }

    private SAML2Client initSAMLClient(SAML2ClientConfiguration saml2ClientConfiguration, String callbackUrl) {
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
    }



    public void validateSettings(SAML2Settings settings) throws IOException {
        if (settings.getIdentityProviderMetadataFile() != null) {
            settings.setIdentityProviderMetadata(Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(settings.getIdentityProviderMetadataFile())));
            settings.setIdentityProviderMetadataFile(null);
        }

        if (settings.getKeyStoreFile() != null) {
            settings.setKeyStore(Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(settings.getKeyStoreFile())));
            settings.setKeyStoreFile(null);
        } else if (settings.getKeyStore() == null) {
            settings.setKeyStore(generateKeyStore(settings));
        }

        initSAMLClient(getSAML2ClientConfiguration(settings), "/");
    }

    private String generateKeyStore(SAML2Settings settings) throws IOException {
        File samlFileName = new File(getSamlFileName(settings.getSiteKey(), "keystore.jks"));
        SAML2ClientConfiguration saml2ClientConfiguration = getSAML2ClientConfiguration(settings);
        saml2ClientConfiguration.setKeystoreResource(new FileSystemResource(samlFileName));
        initSAMLClient(saml2ClientConfiguration, "/");
        String s = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(samlFileName));
        samlFileName.delete();
        return s;

    }

    private String getSamlFileName(String siteKey, String filename) {
        return SettingsBean.getInstance().getJahiaVarDiskPath() + "/saml/" + siteKey + "." + filename;
    }
}
