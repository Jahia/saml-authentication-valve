package org.jahia.modules.saml2.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.utils.ClassLoaderUtils;
import org.opensaml.core.config.InitializationService;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.exception.http.OkAction;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.exception.http.SeeOtherAction;
import org.pac4j.saml.client.SAML2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;

public class ConnectToSAML extends Action {
    private static final Logger logger = LoggerFactory.getLogger(ConnectToSAML.class);

    private static final String REDIRECT = "redirect";

    private SettingsService settingsService;
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
                final Cookie cookie = new Cookie(REDIRECT, redirectParam.replaceAll("\n\r", ""));
                String contextPath = request.getContextPath();
                if (StringUtils.isEmpty(contextPath)) {
                    contextPath = "/";
                }
                cookie.setPath(contextPath);
                cookie.setSecure(request.isSecure());
                response.addCookie(cookie);
            }
            final String siteParam = request.getParameter(SAML2Constants.SITE);
            if (siteParam != null) {
                response.addCookie(new Cookie(siteKey, siteParam.replaceAll("\n\r", "")));
            }
            final SAML2Client client = util.getSAML2Client(settingsService, request, siteKey);
            JEEContext webContext = new JEEContext(request, response);
            final Optional<RedirectionAction> action = client.getRedirectionAction(webContext);
            if (action.isPresent()) {
                RedirectionAction redirectionAction = action.get();
                try {
                    if (redirectionAction instanceof OkAction) {
                        response.getWriter().append(((OkAction)redirectionAction).getContent());
                    } else if (redirectionAction instanceof SeeOtherAction) {
                        response.sendRedirect(((SeeOtherAction)redirectionAction).getLocation());
                    } else if (redirectionAction instanceof FoundAction) {
                        response.sendRedirect(((FoundAction)redirectionAction).getLocation());
                    }
                    response.getWriter().flush();
                } catch (IOException e) {
                    logger.error("Cannot send response", e);
                }
                logger.info(redirectionAction.getMessage());
            } else {
                logger.warn("No SAML redirection");
            }
            return true;
        });
    }

    public void setSettingsService(SettingsService saml2SettingsService) {
        this.settingsService = saml2SettingsService;
    }

    public void setUtil(SAML2Util util) {
        this.util = util;
    }
}
