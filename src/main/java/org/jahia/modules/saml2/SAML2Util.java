package org.jahia.modules.saml2;

import org.apache.commons.lang.StringUtils;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.saml2.admin.SAML2Settings;
import org.jahia.modules.saml2.admin.SAML2SettingsService;
import org.jahia.settings.SettingsBean;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
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
        for (final Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
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
        saml2ClientConfiguration.setKeystoreResource(new ByteArrayResource(Base64.getDecoder().decode(saml2Settings.getKeyStore())));
        saml2ClientConfiguration.setKeystorePassword(saml2Settings.getKeyStorePass());
        saml2ClientConfiguration.setPrivateKeyPassword(saml2Settings.getPrivateKeyPass());
        saml2ClientConfiguration.setServiceProviderMetadataResource(new FileSystemResource(SettingsBean.getInstance().getJahiaVarDiskPath() + "/saml/SAMLSPMetadata."+saml2Settings.getSiteKey()+".xml"));

        return saml2ClientConfiguration;
    }

    /**
     * New method to Initializing saml client.
     *
     * @param saml2Settings
     * @param request
     */
    private SAML2Client initSAMLClient(SAML2Settings saml2Settings, HttpServletRequest request) {
        String spMetaDataLocation = SettingsBean.getInstance().getJahiaVarDiskPath() + "/saml/SAMLSPMetadata."+saml2Settings.getSiteKey()+".xml";
        final File spMetadataFile = new File(spMetaDataLocation);
        if (spMetadataFile.exists()) {
            spMetadataFile.delete();
        }

        final SAML2Client client = new SAML2Client(getSAML2ClientConfiguration(saml2Settings));
        client.setCallbackUrl(getAssertionConsumerServiceUrl(request, saml2Settings.getIncomingTargetUrl()));
        return client;
    }
}
