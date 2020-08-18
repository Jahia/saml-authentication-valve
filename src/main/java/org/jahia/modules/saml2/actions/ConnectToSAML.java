package org.jahia.modules.saml2.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.modules.saml2.admin.SAML2SettingsService;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.utils.ClassLoaderUtils;
import org.opensaml.core.config.InitializationService;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.saml.client.SAML2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ConnectToSAML extends Action {
    private static final Logger logger = LoggerFactory.getLogger(ConnectToSAML.class);

    private static final String REDIRECT = "redirect";

    private SAML2SettingsService saml2SettingsService;
    private SAML2Util util;

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        redirect(httpServletRequest, renderContext.getResponse(), renderContext.getSite().getSiteKey());
        return ActionResult.OK;
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

    public void setSaml2SettingsService(SAML2SettingsService saml2SettingsService) {
        this.saml2SettingsService = saml2SettingsService;
    }

    public void setUtil(SAML2Util util) {
        this.util = util;
    }
}
