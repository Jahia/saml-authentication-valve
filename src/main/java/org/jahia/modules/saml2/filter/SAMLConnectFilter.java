/*
 * Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.saml2.filter;

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.filters.AbstractServletFilter;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.modules.saml2.helper.SAMLSiteHelper;
import org.jahia.utils.ClassLoaderUtils;
import org.opensaml.core.config.InitializationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.exception.http.OkAction;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.exception.http.SeeOtherAction;
import org.pac4j.saml.client.SAML2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
@Component(immediate = true, service = AbstractServletFilter.class)
public class SAMLConnectFilter extends AbstractServletFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAMLConnectFilter.class);
    private static final String REDIRECT = "redirect";

    @Reference
    private SettingsService settingsService;
    @Reference
    private SAML2Util util;

    @Override
    public void init(FilterConfig filterConfig) {
        LOGGER.debug("Initializing SAMLConnectFilter...");
    }

    @Activate
    public void activate() {
        LOGGER.debug("Activating SAMLConnectFilter...");
        setMatchAllUrls(false);
        setUrlPatterns(new String[]{"*.saml"});
        setFilterName("SAMLConnectFilter");
        setOrder(-870f);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        LOGGER.debug("Executing SAMLConnectFilter.doFilter()...");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();
        if (requestURI.endsWith("connect.saml")) {
            LOGGER.debug("SAMLConnectFilter.doFilter() matches request URI: {}", requestURI);
            final String siteKey = SAMLSiteHelper.findSiteKeyForRequest(httpRequest);
            if (siteKey != null) {
                boolean redirected = ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(), () -> {
                    // Storing redirect url into cookie to be used when the request is send from IDP to continue the access to the secure resource
                    final String redirectParam = httpRequest.getParameter(REDIRECT);
                    if (redirectParam != null) {
                        final Cookie cookie = new Cookie(REDIRECT, redirectParam.replaceAll("\n\r", ""));
                        String contextPath = httpRequest.getContextPath();
                        if (StringUtils.isEmpty(contextPath)) {
                            contextPath = "/";
                        }
                        cookie.setPath(contextPath);
                        cookie.setSecure(httpRequest.isSecure());
                        httpResponse.addCookie(cookie);
                    }
                    final String siteParam = httpRequest.getParameter(SAML2Constants.SITE);
                    if (siteParam != null) {
                        httpResponse.addCookie(new Cookie(siteKey, siteParam.replaceAll("\n\r", "")));
                    }
                    final SAML2Client client = util.getSAML2Client(settingsService, httpRequest, siteKey);
                    JEEContext webContext = new JEEContext(httpRequest, httpResponse);
                    final Optional<RedirectionAction> action = client.getRedirectionAction(webContext);
                    if (action.isPresent()) {
                        RedirectionAction redirectionAction = action.get();
                        try {
                            if (redirectionAction instanceof OkAction) {
                                httpResponse.getWriter().append(((OkAction) redirectionAction).getContent());
                            } else if (redirectionAction instanceof SeeOtherAction) {
                                httpResponse.sendRedirect(((SeeOtherAction) redirectionAction).getLocation());
                            } else if (redirectionAction instanceof FoundAction) {
                                httpResponse.sendRedirect(((FoundAction) redirectionAction).getLocation());
                            }
                            httpResponse.getWriter().flush();
                            return true;
                        } catch (IOException e) {
                            LOGGER.error("Cannot send response", e);
                        }
                        LOGGER.info(redirectionAction.getMessage());
                    } else {
                        LOGGER.warn("No SAML redirection");
                    }
                    return false;
                });
                if (redirected) {
                    LOGGER.debug("SAMLConnectFilter request redirected to SSO");
                    return;
                }
            } else {
                LOGGER.error("No site found (param or servername based), cannot proceed with SAML authentication");
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        LOGGER.debug("Destroying SAMLConnectFilter...");
    }
}

