package org.jahia.modules.saml2.valve;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.modules.saml2.admin.SAML2SettingsService;
import org.jahia.modules.saml2.utils.JCRConstants;
import org.jahia.params.valves.AuthValveContext;
import org.jahia.params.valves.AutoRegisteredBaseAuthValve;
import org.jahia.params.valves.LoginEngineAuthValveImpl;
import org.jahia.pipelines.PipelineException;
import org.jahia.pipelines.valves.ValveContext;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.seo.urlrewrite.ServerNameToSiteMapper;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.utils.ClassLoaderUtils;
import org.opensaml.core.config.InitializationService;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.pac4j.saml.profile.SAML2Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public final class AuthenticationValve extends AutoRegisteredBaseAuthValve {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationValve.class);
    private static final String REDIRECT = "redirect";
    private SAML2SettingsService saml2SettingsService;
    private JahiaUserManagerService jahiaUserManagerService;
    private SAML2Util util;

    @Override
    public void invoke(final Object context, final ValveContext valveContext) throws PipelineException {
        final AuthValveContext authContext = (AuthValveContext) context;
        final HttpServletRequest request = authContext.getRequest();
        final HttpServletResponse response = authContext.getResponse();

        // Get siteKey from servername
        final String siteKey = ServerNameToSiteMapper.getSiteKeyByServerName(request);

        if (!StringUtils.isEmpty(siteKey)) {
            try {
                final JahiaSitesService siteService = JahiaSitesService.getInstance();
                final JahiaSite site = siteService.getSiteByKey(siteKey, JCRSessionFactory.getInstance().getCurrentSystemSession(null, null, null));
                final List<String> installedModules = site.getInstalledModules();
                if (installedModules.contains("saml-authentication-valve") && handleSAMLRequest(request, response, siteKey, valveContext, authContext)) {
                    return;
                }
            } catch (RepositoryException ex) {
                logger.error(String.format("Impossible to check if the SAML is enabled for %s", siteKey), ex);
            }
        }

        valveContext.invokeNext(authContext);
    }

    private boolean handleSAMLRequest(HttpServletRequest request, HttpServletResponse response, String siteKey, ValveContext valveContext, AuthValveContext authContext) {
        if (request.getRequestURI().equals(request.getContextPath() + "/cms/login")) {
            redirect(request, response, siteKey);
            return true;
        }

        if (request.getRequestURI().equals(request.getContextPath() + saml2SettingsService.getSettings(siteKey).getIncomingTargetUrl())) {
            //TODO This should rather be done in an action, like oauth providers
            login(request, response, siteKey, valveContext, authContext);
            return true;
        }

        return false;
    }

    private void login(HttpServletRequest request, HttpServletResponse response, String siteKey, ValveContext valveContext, AuthValveContext authContext) {
        ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(), () -> {
            final SAML2Client client = util.getSAML2Client(saml2SettingsService, request, siteKey);
            final J2EContext webContext = new J2EContext(request, response);
            final SAML2Credentials saml2Credentials = client.getCredentials(webContext);
            final SAML2Profile saml2Profile = client.getUserProfile(saml2Credentials, webContext);

            Map<String, Object> properties = getMapperResult(saml2Profile);

            //TODO From there, we could reuse jahia-oauth module to handle user mapping
            final String email = (String) properties.get(JCRConstants.USER_PROPERTY_EMAIL);
            logger.debug("email of SAML Profile: {}", email);

            try {
                if (StringUtils.isNotEmpty(email)) {
                    JahiaUser jahiaUser = processSSOUserInJcr(properties, siteKey);
                    if (jahiaUser.isAccountLocked()) {
                        logger.info("Login failed. Account is locked for user {}", email);
                        valveContext.invokeNext(authContext);
                        return false;
                    }
                    authContext.getSessionFactory().setCurrentUser(jahiaUser);
                    request.getSession().setAttribute(Constants.SESSION_USER, jahiaUser);

                    request.setAttribute(LoginEngineAuthValveImpl.VALVE_RESULT, LoginEngineAuthValveImpl.OK);

                    // Get the redirection URL from the cookie, if not set takes the value is taken from the site settings
                    String redirection = retrieveRedirectUrl(request, siteKey);
                    response.sendRedirect(redirection);
                } else {
                    valveContext.invokeNext(authContext);
                }
            } catch (RepositoryException | PipelineException | IOException e) {
                logger.error("Cannot login user", e);
            }
            return true;
        });
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, String siteKey) {
        ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(), () -> {
            // Storing redirect url into cookie to be used when the request is send from IDP to continue the
            // access to the secure resource
            final String redirectParam = request.getParameter(REDIRECT);
            if (redirectParam != null) {
                response.addCookie(new Cookie(REDIRECT, redirectParam.replaceAll("\n\r", "")));
            }
            final String siteParam = request.getParameter(SAML2Constants.SITE);
            if (siteParam != null) {
                response.addCookie(new Cookie(siteKey, siteParam.replaceAll("\n\r", "")));
            }
            final SAML2Client client = util.getSAML2Client(saml2SettingsService, request, siteKey);
            final J2EContext webContext = new J2EContext(request, response);
            final HttpAction action = client.redirect(webContext);
            try {
                response.getWriter().flush();
            } catch (IOException e) {
                logger.error("Cannot send response", e);
            }
            logger.info(action.getMessage());
            return true;
        });
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

    public void setSaml2SettingsService(final SAML2SettingsService saml2SettingsService) {
        this.saml2SettingsService = saml2SettingsService;
    }

    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    public void setUtil(SAML2Util util) {
        this.util = util;
    }
}
