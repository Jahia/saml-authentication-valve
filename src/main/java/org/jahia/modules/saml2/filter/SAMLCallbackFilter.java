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

import org.jahia.bin.filters.AbstractServletFilter;
import org.jahia.modules.jahiaauth.service.*;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.utils.ClassLoaderUtils;
import org.opensaml.core.config.InitializationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.profile.BasicUserProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.pac4j.saml.exceptions.SAMLException;
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
public class SAMLCallbackFilter extends AbstractServletFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAMLCallbackFilter.class);

    @Reference
    private SettingsService settingsService;
    @Reference
    private SAML2Util util;
    @Reference
    private JahiaAuthMapperService jahiaAuthMapperService;

    @Override
    public void init(FilterConfig filterConfig) {
        LOGGER.debug("Initializing SAMLCallbackFilter...");
    }

    @Activate
    public void activate() {
        LOGGER.debug("Activating SAMLCallbackFilter...");
        setMatchAllUrls(false);
        setUrlPatterns(new String[]{"*.saml"});
        setFilterName("SAMLCallbackFilter");
        setOrder(-872f);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        LOGGER.debug("Executing SAMLCallbackFilter.doFilter()...");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();
        if (requestURI.endsWith("callback.saml")) {
            LOGGER.debug("SAMLCallbackFilter.doFilter() matches URL {}", requestURI);
            String siteKey = util.findSiteKeyForRequest(httpRequest);
            if (siteKey != null) {
                try {
                    boolean redirect = ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(), () -> {
                        final SAML2Client client = util.getSAML2Client(settingsService, httpRequest, siteKey);
                        final JEEContext webContext = new JEEContext(httpRequest, httpResponse);
                        final Optional<SAML2Credentials> saml2Credentials = client.getCredentials(webContext);
                        final Optional<UserProfile> saml2Profile = saml2Credentials.flatMap(c -> client.getUserProfile(c, webContext));

                        ConnectorConfig settings = settingsService.getConnectorConfig(siteKey, "Saml");

                        if (saml2Profile.isPresent()) {
                            Map<String, Object> properties = getMapperResult((BasicUserProfile) saml2Profile.get());

                            for (MapperConfig mapper : settings.getMappers()) {
                                try {
                                    jahiaAuthMapperService.executeMapper(httpRequest.getSession().getId(), mapper, properties);
                                } catch (JahiaAuthException e) {
                                    LOGGER.warn("Cannot log in user : {}", e.getMessage());
                                    return false;
                                }
                            }
                            ConnectorConfig config = settingsService.getConnectorConfig(siteKey, "Saml");
                            jahiaAuthMapperService.executeConnectorResultProcessors(config, properties);

                            return true;
                        }
                        LOGGER.warn("Cannot log in user : saml2Profile is not present");
                        return false;
                    });
                    if (redirect) {
                        String redirection = util.getRedirectionUrl(httpRequest, siteKey, util, settingsService);
                        LOGGER.info("Redirecting to {}, Headers[Accept-Language]: {}", redirection, httpRequest.getHeader("Accept-Language"));
                        httpResponse.sendRedirect(redirection);
                        return;
                    }
                } catch (SAMLException e) {
                    LOGGER.warn("Cannot log in user : {}", e.getMessage());
                }
            } else {
                LOGGER.error("No site found (param or servername based), cannot proceed with SAML authentication");
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        LOGGER.debug("Destroying SAMLCallbackFilter...");
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
