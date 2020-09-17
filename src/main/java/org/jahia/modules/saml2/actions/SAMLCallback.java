package org.jahia.modules.saml2.actions;

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.jahiaauth.service.MapperConfig;
import org.jahia.modules.jahiaauth.service.Mapping;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.JahiaAuthException;
import org.jahia.modules.jahiaauth.service.JahiaAuthMapperService;
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
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.pac4j.saml.exceptions.SAMLException;
import org.pac4j.saml.profile.SAML2Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jahia.modules.jahiaauth.service.JahiaAuthConstants.PROPERTY_VALUE;
import static org.jahia.modules.jahiaauth.service.JahiaAuthConstants.PROPERTY_VALUE_TYPE;

public class SAMLCallback extends Action {
    private static final Logger logger = LoggerFactory.getLogger(SAMLCallback.class);
    private static final String REDIRECT = "redirect";

    private SAML2SettingsService saml2SettingsService;
    private SAML2Util util;

    private JahiaAuthMapperService jahiaAuthMapperService;

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

                try {
                    MapperConfig mapperConfig = new MapperConfig(settings.getMapperName());
                    mapperConfig.setMappings(Arrays.asList(
                            new Mapping("id", JahiaAuthConstants.SSO_LOGIN),
                            new Mapping(CommonProfileDefinition.EMAIL, JCRConstants.USER_PROPERTY_EMAIL),
                            new Mapping(CommonProfileDefinition.FAMILY_NAME, JCRConstants.USER_PROPERTY_LASTNAME),
                            new Mapping(CommonProfileDefinition.FIRST_NAME, JCRConstants.USER_PROPERTY_FIRSTNAME)
                    ));

                    jahiaAuthMapperService.executeMapper(httpServletRequest.getSession().getId(), mapperConfig, properties);
                } catch (JahiaAuthException e) {
                    return false;
                }

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
        Map<String, Object> properties = new HashMap<>(saml2Profile.getAttributes());
        properties.put("id", saml2Profile.getId());
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

    public void setJahiaAuthMapperService(JahiaAuthMapperService jahiaAuthMapperService) {
        this.jahiaAuthMapperService = jahiaAuthMapperService;
    }

    public void setSaml2SettingsService(SAML2SettingsService saml2SettingsService) {
        this.saml2SettingsService = saml2SettingsService;
    }

    public void setUtil(SAML2Util util) {
        this.util = util;
    }
}
