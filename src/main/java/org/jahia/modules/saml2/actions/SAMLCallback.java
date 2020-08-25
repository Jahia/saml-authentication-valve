package org.jahia.modules.saml2.actions;

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.modules.saml2.admin.SAML2Settings;
import org.jahia.modules.saml2.admin.SAML2SettingsService;
import org.jahia.modules.saml2.utils.JCRConstants;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.utils.ClassLoaderUtils;
import org.opensaml.core.config.InitializationService;
import org.pac4j.core.context.J2EContext;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.pac4j.saml.exceptions.SAMLException;
import org.pac4j.saml.profile.SAML2Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants.*;

public class SAMLCallback extends Action {
    private static final Logger logger = LoggerFactory.getLogger(ConnectToSAML.class);
    private static final String REDIRECT = "redirect";

    private SAML2SettingsService saml2SettingsService;
    private SAML2Util util;

    private JahiaOAuthService jahiaOAuthService;

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        String siteKey = renderContext.getSite().getSiteKey();
        try {
            ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(), () -> {
                final SAML2Client client = util.getSAML2Client(saml2SettingsService, httpServletRequest, siteKey);
                final J2EContext webContext = new J2EContext(httpServletRequest, renderContext.getResponse());
                final SAML2Credentials saml2Credentials = client.getCredentials(webContext);
                final SAML2Profile saml2Profile = client.getUserProfile(saml2Credentials, webContext);

                SAML2Settings settings = saml2SettingsService.getSettings(siteKey);

                Map<String, Object> properties = getMapperResult(saml2Profile);
                jahiaOAuthService.executeMapper(httpServletRequest.getSession().getId(), settings.getMapperName(), properties);

                return true;
            });
        } catch (SAMLException e) {
            logger.warn("Cannot log in user : {}", e.getMessage());
        }
        String url = retrieveRedirectUrl(httpServletRequest, siteKey);
        return new ActionResult(HttpServletResponse.SC_OK, url, true, null);
    }

    /**
     * properties for new user.
     */
    private Map<String, Object> getMapperResult(SAML2Profile saml2Profile) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(JahiaOAuthConstants.SSO_LOGIN, saml2Profile.getId());
        properties.put(JCRConstants.USER_PROPERTY_EMAIL, getValue(saml2Profile.getEmail(), "email"));
        if (saml2Profile.getFamilyName() != null) {
            properties.put(JCRConstants.USER_PROPERTY_LASTNAME, getValue(saml2Profile.getFamilyName(), "string"));
        }
        if (saml2Profile.getFirstName() != null) {
            properties.put(JCRConstants.USER_PROPERTY_FIRSTNAME, getValue(saml2Profile.getFirstName(), "string"));
        }

        return properties;
    }

    private Map<String, Object> getValue(String value, String type) {
        Map<String, Object> m = new HashMap<>();
        m.put(PROPERTY_VALUE, value);
        m.put(PROPERTY_VALUE_TYPE, type);
        return m;
    }

    /**
     * Gets the redirection URL from the cookie, if not set takes the value is taken from the site settings
     *
     * @param request : the http request
     * @return the redirection URL
     */
    private String retrieveRedirectUrl(HttpServletRequest request, String siteKey) {
        String redirection = util.getCookieValue(request, REDIRECT);
        if (StringUtils.isEmpty(redirection)) {
            redirection = request.getContextPath() + saml2SettingsService.getSettings(siteKey).getPostLoginPath();
            if (StringUtils.isEmpty(redirection)) {
                // default value
                redirection = "/";
            }
        }

        return redirection + "?site=" + siteKey;
    }

    public void setJahiaOAuthService(JahiaOAuthService jahiaOAuthService) {
        this.jahiaOAuthService = jahiaOAuthService;
    }

    public void setSaml2SettingsService(SAML2SettingsService saml2SettingsService) {
        this.saml2SettingsService = saml2SettingsService;
    }

    public void setUtil(SAML2Util util) {
        this.util = util;
    }
}
