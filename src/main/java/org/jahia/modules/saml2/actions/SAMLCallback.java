package org.jahia.modules.saml2.actions;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.modules.saml2.admin.SAML2SettingsService;
import org.jahia.modules.saml2.utils.JCRConstants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.utils.ClassLoaderUtils;
import org.opensaml.core.config.InitializationService;
import org.pac4j.core.context.J2EContext;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.pac4j.saml.profile.SAML2Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class SAMLCallback extends Action {
    private static final String REDIRECT = "redirect";
    private static final Logger logger = LoggerFactory.getLogger(SAMLCallback.class);

    private SAML2SettingsService saml2SettingsService;
    private JahiaUserManagerService jahiaUserManagerService;
    private SAML2Util util;

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        String siteKey = renderContext.getSite().getSiteKey();
        ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(), () -> {
            final SAML2Client client = util.getSAML2Client(saml2SettingsService, httpServletRequest, siteKey);
            final J2EContext webContext = new J2EContext(httpServletRequest, renderContext.getResponse());
            final SAML2Credentials saml2Credentials = client.getCredentials(webContext);
            final SAML2Profile saml2Profile = client.getUserProfile(saml2Credentials, webContext);

            Map<String, Object> properties = getMapperResult(saml2Profile);
            executeMapper(httpServletRequest, renderContext, siteKey, properties);

            return true;
        });

        String url = retrieveRedirectUrl(httpServletRequest, siteKey);
        return new ActionResult(HttpServletResponse.SC_OK, url, true, null);
    }

    private void executeMapper(HttpServletRequest httpServletRequest, RenderContext renderContext, String siteKey, Map<String, Object> properties) {
        final String email = (String) properties.get(JCRConstants.USER_PROPERTY_EMAIL);
        logger.debug("email of SAML Profile: {}", email);

        try {
            if (StringUtils.isNotEmpty(email)) {
                JahiaUser jahiaUser = processSSOUserInJcr(properties, siteKey);
                if (jahiaUser.isAccountLocked()) {
                    logger.info("Login failed. Account is locked for user {}", email);
                    return;
                }
                httpServletRequest.getSession().setAttribute(Constants.SESSION_USER, jahiaUser);
            }
        } catch (RepositoryException e) {
            logger.error("Cannot login user", e);
        }
    }

    /**
     * Update user is exist or create a new user for site with sso profile properties.
     *
     * @throws RepositoryException
     */
    private JahiaUser processSSOUserInJcr(Map<String, Object> mapperResult, String siteKey) throws RepositoryException {
        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentSystemSession(null, null, null);
        JCRUserNode ssoUserNode;
        final String userId = (String) mapperResult.get(JCRConstants.USER_PROPERTY_EMAIL);

        if (jahiaUserManagerService.userExists(userId, siteKey)) {
            ssoUserNode = jahiaUserManagerService.lookupUser(userId, siteKey, session);
            JCRNodeWrapper jcrNodeWrapper = ssoUserNode.getDecoratedNode();
            boolean isUpdated = updateUserProperties(jcrNodeWrapper, mapperResult);
            //saving session if any property is updated for user.
            if (isUpdated) {
                session.save();
            }
        } else {
            Properties properties = new Properties();
            ssoUserNode = jahiaUserManagerService.createUser(userId, siteKey, "SHA-1:*", properties, session);
            updateUserProperties(ssoUserNode, mapperResult);

            session.save();
        }
        return ssoUserNode.getJahiaUser();
    }


    /**
     * properties for new user.
     */
    private Map<String, Object> getMapperResult(SAML2Profile saml2Profile) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(JCRConstants.USER_PROPERTY_EMAIL, saml2Profile.getEmail());
        properties.put(JCRConstants.USER_PROPERTY_LASTNAME, saml2Profile.getFamilyName());
        properties.put(JCRConstants.USER_PROPERTY_FIRSTNAME, saml2Profile.getFirstName());
        return properties;
    }

    /**
     * Update properties for existing user node.
     *
     * @param jcrNodeWrapper
     * @param mapperResult
     * @throws RepositoryException
     */
    private boolean updateUserProperties(JCRNodeWrapper jcrNodeWrapper, Map<String, Object> mapperResult) throws RepositoryException {
        boolean isUpdated = false;
        for (Map.Entry<String, Object> entry : mapperResult.entrySet()) {
            if (Objects.isNull(jcrNodeWrapper.getPropertyAsString(entry.getKey())) || !jcrNodeWrapper.getPropertyAsString(entry.getKey()).equals(entry.getValue())) {
                jcrNodeWrapper.setProperty(entry.getKey(), (String) entry.getValue());
                isUpdated = true;
            }
        }

        return isUpdated;
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

        return redirection;
    }

    public void setSaml2SettingsService(SAML2SettingsService saml2SettingsService) {
        this.saml2SettingsService = saml2SettingsService;
    }

    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    public void setUtil(SAML2Util util) {
        this.util = util;
    }
}
