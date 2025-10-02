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
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.modules.saml2.helper.SAMLSiteHelper;
import org.jahia.utils.ClassLoaderUtils;
import org.opensaml.core.config.InitializationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.pac4j.saml.metadata.SAML2MetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Jerome Blanchard
 */
@Component(immediate = true, service = AbstractServletFilter.class)
public class SAMLMetadataFilter extends AbstractServletFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAMLMetadataFilter.class);

    @Reference
    private SettingsService settingsService;
    @Reference
    private SAML2Util util;

    @Override
    public void init(FilterConfig filterConfig) {
        LOGGER.debug("Initializing SAMLMetadataFilter...");
    }

    @Activate
    public void activate() {
        LOGGER.debug("Activating SAMLMetadataFilter...");
        setMatchAllUrls(false);
        setUrlPatterns(new String[]{"*.saml"});
        setFilterName("SAMLMetadataFilter");
        setOrder(-871f);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        LOGGER.debug("Executing SAMLMetadataFilter.doFilter()...");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();
        if (requestURI.endsWith("metadata.saml")) {
            LOGGER.debug("SAMLMetadataFilter.doFilter() matches URL {}", requestURI);
            final String siteKey = SAMLSiteHelper.findSiteKeyForRequest(httpRequest);
            if (siteKey != null) {
                boolean generated = ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(), () -> {
                    SAML2MetadataResolver metadataResolver = util.getSAML2Client(settingsService, httpRequest, siteKey).getServiceProviderMetadataResolver();
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
                    return;
                }
            } else {
                LOGGER.error("No site found (param or servername based), cannot proceed with SAML metadata generation");
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        LOGGER.debug("Destroying SAMLMetadataFilter...");
    }

}
