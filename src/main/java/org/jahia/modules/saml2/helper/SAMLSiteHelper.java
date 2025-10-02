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
package org.jahia.modules.saml2.helper;

import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
public class SAMLSiteHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAMLSiteHelper.class);

    private static final JahiaSitesService sitesService = JahiaSitesService.getInstance();

    public static JahiaSite getSiteByKey(String siteKey) {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<JahiaSite>() {
                @Override public JahiaSite doInJCR(JCRSessionWrapper session) {
                    try {
                        return sitesService.getSiteByKey(siteKey, session);
                    } catch (RepositoryException e) {
                        LOGGER.error("Cannot find site for key {}", siteKey);
                        return null;
                    }
                }
            });
        } catch (RepositoryException e) {
            LOGGER.error("Cannot find site for key {}", siteKey, e);
            return null;
        }
    }

    /**
     * We do not use URLResolver strategies to determine the site key (aka parsing the path to extract /sites/siteKey/**) to avoid
     * code duplication and because it seems not relevant to do such processing here.
     * Only the following strategies are implemented in that order:
     * - siteKey request parameter
     * - the only site in case of a Jahia single site instance
     * - server name resolution for multi-site instances
     */
    public static String findSiteKeyForRequest(HttpServletRequest request) {
        String siteKey = request.getParameter(SAML2Constants.SITEKEY);
        if (siteKey == null) {
            LOGGER.info("No site key provided, trying to guess using server name");
            try {
                siteKey = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<String>() {
                    @Override public String doInJCR(JCRSessionWrapper session) {
                        try {
                            List<JCRSiteNode> sites = sitesService.getSitesNodeList(session);
                            if (sites.size() == 1) {
                                return sites.get(0).getSiteKey();
                            }
                            Optional<String> siteKey = sites.stream()
                                    .filter(site -> site.getServerName().equals(request.getServerName()))
                                    .map(JCRSiteNode::getSiteKey).findFirst();
                            if (siteKey.isPresent()) {
                                return siteKey.get();
                            }
                            LOGGER.error("Unable to determine site key for server name {}, check your configuration", request.getServerName());
                            return null;
                        } catch (RepositoryException e) {
                            LOGGER.error("Error while trying to determine site key from server name {}", request.getServerName(), e);
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
