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
package org.jahia.modules.saml2.internal;

import org.jahia.bin.filters.AbstractServletFilter;
import org.jahia.modules.jahiaauth.service.*;
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
import org.pac4j.core.profile.BasicUserProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.pac4j.saml.exceptions.SAMLException;
import org.pac4j.saml.metadata.SAML2MetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
@Component(immediate = true, service = AbstractServletFilter.class)
public class SAML2Filter extends AbstractServletFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SAML2Filter.class);

    @Reference
    private SAML2Util util;
    @Reference
    private SettingsService settingsService;
    @Reference
    private JahiaAuthMapperService jahiaAuthMapperService;

    @Override
    public void init(FilterConfig filterConfig) {
        LOGGER.debug("Initializing SAML2Filter...");
    }

    @Activate
    public void activate() {
        LOGGER.debug("Activating SAML2Filter...");
        setMatchAllUrls(false);
        setUrlPatterns(new String[]{"*.saml"});
        setFilterName("SAML2Filter");
        setOrder(-872f);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        LOGGER.debug("Executing SAML2Filter.doFilter()...");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();
        if (requestURI.endsWith("callback.saml")) {
            handleCallback(httpRequest, httpResponse);
            return;
        } else if (requestURI.endsWith("connect.saml")) {
            handleConnect(httpRequest, httpResponse);
            return;
        } else if (requestURI.endsWith("metadata.saml")) {
            handleMetadata(httpRequest, httpResponse);
            return;
        }
        chain.doFilter(request, response);
    }

    private void handleCallback(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        LOGGER.debug("SAML2Filter.handleCallback() matches URL {}", httpRequest.getRequestURI());
        String siteKey = util.findSiteKeyForRequest(httpRequest);
        if (siteKey != null) {
            try {
                boolean redirect = ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(), () -> {
                    final SAML2Client client = util.getSAML2Client(httpRequest, siteKey);
                    final JEEContext webContext = new JEEContext(httpRequest, httpResponse);
                    final Optional<SAML2Credentials> saml2Credentials = client.getCredentials(webContext);
                    final Optional<UserProfile> saml2Profile = saml2Credentials.flatMap(c -> client.getUserProfile(c, webContext));

                    if (saml2Profile.isPresent()) {
                        Map<String, Object> properties = getMapperResult((BasicUserProfile) saml2Profile.get());
                        ConnectorConfig config = settingsService.getConnectorConfig(siteKey, "Saml");
                        for (MapperConfig mapper : config.getMappers()) {
                            try {
                                jahiaAuthMapperService.executeMapper(httpRequest.getSession().getId(), mapper, properties);
                            } catch (JahiaAuthException e) {
                                LOGGER.warn("Cannot log in user : {}", e.getMessage());
                                return false;
                            }
                        }
                        jahiaAuthMapperService.executeConnectorResultProcessors(config, properties);
                        return true;
                    }
                    LOGGER.warn("Cannot log in user : saml2Profile is not present");
                    return false;
                });
                if (redirect) {
                    String redirection = util.getRedirectionUrl(httpRequest, siteKey);
                    LOGGER.debug("Redirecting to {}", redirection);
                    httpResponse.sendRedirect(redirection);
                } else {
                    httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to handle SSO callback");
                }
            } catch (SAMLException e) {
                LOGGER.warn("Unable to handle SAML callback : {}", e.getMessage());
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while trying to login");
            }
        } else {
            LOGGER.error("No site found (param or servername based), cannot proceed with SAML authentication");
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to proceed with SAML authentication");
        }
    }

    private void handleConnect(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        LOGGER.debug("SAML2Filter.handleConnect() matches request URI: {}", httpRequest.getRequestURI());
        final String siteKey = util.findSiteKeyForRequest(httpRequest);
        if (siteKey != null) {
            boolean redirected = ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(), () -> {
                // Store authentication context (redirect URL, site param) in cookies
                util.storeAuthenticationContext(httpRequest, httpResponse, siteKey);

                final SAML2Client client = util.getSAML2Client(httpRequest, siteKey);
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
                } else {
                    LOGGER.warn("No SAML redirection");
                }
                return false;
            });
            if (redirected) {
                LOGGER.debug("SAMLConnectFilter request redirected to SSO");
            } else {
                httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to redirect to SSO");
            }
        } else {
            LOGGER.error("No site found (param or servername based), cannot proceed with SAML connect");
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to proceed with SAML authentication");
        }
    }

    private void handleMetadata(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        LOGGER.debug("SAML2Filter.handleMetadata() matches URL {}", httpRequest.getRequestURI());
        final String siteKey = util.findSiteKeyForRequest(httpRequest);
        if (siteKey != null) {
            boolean generated = ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(), () -> {
                SAML2MetadataResolver metadataResolver = util.getSAML2Client(httpRequest, siteKey).getServiceProviderMetadataResolver();
                try {
                    httpResponse.getWriter().append(metadataResolver.getMetadata());
                    return true;
                } catch (Exception e) {
                    LOGGER.error("Error when getting metadata", e);
                    return false;
                }
            });
            if (generated) {
                LOGGER.debug("SAML2 metadata successfully generated");
            } else {
                httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to generate SAML metadata");
            }
        } else {
            LOGGER.error("No site found (param or servername based), cannot proceed with SAML metadata generation");
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to proceed with SAML authentication");
        }
    }

    @Override
    public void destroy() {
        LOGGER.debug("Destroying SAML2Filter...");
    }

    /**
     * properties for new user.
     */
    private Map<String, Object> getMapperResult(BasicUserProfile saml2Profile) {
        Map<String, Object> properties = new HashMap<>();
        for (Map.Entry<String, Object> entry : saml2Profile.getAttributes().entrySet()) {
            if (entry.getValue() instanceof List) {
                final List<?> l = (List<?>) entry.getValue();
                if (l.size() == 1) {
                    properties.put(entry.getKey(), l.get(0));
                } else {
                    properties.put(entry.getKey(), entry.getValue());
                }
            } else {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        return properties;
    }
}
