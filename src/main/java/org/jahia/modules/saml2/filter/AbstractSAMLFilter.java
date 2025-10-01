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
import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Jerome Blanchard
 */
public abstract class AbstractSAMLFilter extends AbstractServletFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSAMLFilter.class);

    private final JahiaSitesService sitesService = JahiaSitesService.getInstance();

    protected JahiaSite getSiteByKey(String siteKey) {
        try {
            JahiaSite site = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<JahiaSite>() {
                @Override public JahiaSite doInJCR(JCRSessionWrapper session) {
                    try {
                        JahiaSite site = sitesService.getSiteByKey(siteKey, session);
                        if (site == null) {
                            LOGGER.error("Cannot find site for key {}, loading default site", siteKey);
                            site = sitesService.getDefaultSite();
                        }
                        return site;
                    } catch (RepositoryException e) {
                        return null;
                    }
                }
            });
            return site;
        } catch (RepositoryException e) {
            LOGGER.error("Cannot find site for key {}", siteKey, e);
            return null;
        }
    }

    protected String getSiteKey(HttpServletRequest request) {
        String siteKey = request.getParameter(SAML2Constants.SITEKEY);
        if (siteKey == null) {
            LOGGER.info("No site key provided, trying to guess using server name");
            try {
                siteKey = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<String>() {
                    @Override public String doInJCR(JCRSessionWrapper session) {
                        try {
                            JahiaSite site = sitesService.getSiteByServerName(request.getServerName(), session);
                            if (site == null) {
                                LOGGER.error("Cannot find site for server name {}, loading default site", request.getServerName());
                                site = sitesService.getDefaultSite();
                            }
                            return site.getSiteKey();
                        } catch (RepositoryException e) {
                            return null;
                        }
                    }
                });
            } catch (RepositoryException e) {
                LOGGER.error("Cannot find site for server name {}", request.getServerName(), e);
            }
        }
        return siteKey;
    }
}
