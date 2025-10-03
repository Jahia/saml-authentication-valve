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

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.utils.LanguageCodeConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Jerome Blanchard
 */
public class SAMLHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAMLHelper.class);
    private static final String REDIRECT = "redirect";
    private static final String PREFERRED_LANGUAGE = "preferredLanguage";

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

    /**
     * Store redirect URL and preferred language in cookies for use after SAML authentication
     */
    public static void storeAuthenticationContext(HttpServletRequest request, HttpServletResponse response, String siteKey) {
        String contextPath = request.getContextPath();
        if (StringUtils.isEmpty(contextPath)) {
            contextPath = "/";
        }

        // Store redirect URL if provided
        final String redirectParam = request.getParameter(REDIRECT);
        if (redirectParam != null) {
            final Cookie redirectCookie = new Cookie(REDIRECT, redirectParam.replaceAll("\n\r", ""));
            redirectCookie.setPath(contextPath);
            redirectCookie.setSecure(request.isSecure());
            response.addCookie(redirectCookie);
        }

        // Store preferred language from Accept-Language header
        String acceptLanguage = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (acceptLanguage != null) {
            final String cookieValue = Base64.getEncoder().encodeToString(acceptLanguage.replaceAll("\n\r", "").getBytes(StandardCharsets.UTF_8));
            final Cookie langCookie = new Cookie(PREFERRED_LANGUAGE, cookieValue);
            langCookie.setPath(contextPath);
            langCookie.setSecure(request.isSecure());
            response.addCookie(langCookie);
        }

        // Store site parameter if provided
        final String siteParam = request.getParameter(SAML2Constants.SITE);
        if (siteParam != null) {
            final Cookie siteCookie = new Cookie(siteKey, siteParam.replaceAll("\n\r", ""));
            siteCookie.setPath(contextPath);
            siteCookie.setSecure(request.isSecure());
            response.addCookie(siteCookie);
        }
    }

    /**
     * Retrieve redirection URL with proper locale from stored authentication context
     */
    public static String getRedirectionUrl(HttpServletRequest request, String siteKey, SAML2Util util, SettingsService settingsService) {
        String redirection = util.getCookieValue(request, REDIRECT);
        if (StringUtils.isEmpty(redirection)) {
            // Resolve locale from preferred language cookie first, then from request
            Locale locale = resolveLocale(request, siteKey, util);

            redirection = request.getContextPath() + (locale != null ? "/" + locale : "") +
                    settingsService.getSettings(siteKey).getValues("Saml").getProperty(SAML2Constants.POST_LOGIN_PATH);

            if (StringUtils.isEmpty(redirection)) {
                // default value
                redirection = "/";
            }
        }

        return redirection + (redirection.contains("?") ? "&" : "?") + "site=" + siteKey;
    }

    /**
     * Resolve the appropriate locale from stored cookies and request context
     */
    private static Locale resolveLocale(HttpServletRequest request, String siteKey, SAML2Util util) {
        Locale locale = null;
        try {
            locale = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Locale>() {
                @Override public Locale doInJCR(JCRSessionWrapper session) {
                    try {
                        Locale locale = null;
                        JahiaSite siteByKey = sitesService.getSiteByKey(siteKey, session);
                        if (siteByKey == null) {
                            return null;
                        }
                        locale = LanguageCodeConverters.languageCodeToLocale(siteByKey.getDefaultLanguage());
                        List<Locale> languagesAsLocales = siteByKey.getLanguagesAsLocales();

                        // Check if we have a preferred language cookie from the initial request
                        String encodedPreferredLanguage = util.getCookieValue(request, PREFERRED_LANGUAGE);
                        if (encodedPreferredLanguage != null) {
                            String preferredLanguage = new String(Base64.getDecoder().decode(encodedPreferredLanguage), StandardCharsets.UTF_8);
                            locale = parsePreferredLanguage(preferredLanguage, languagesAsLocales, locale);
                        }

                        // Fallback to request locales if cookie didn't match
                        if (locale.equals(LanguageCodeConverters.languageCodeToLocale(siteByKey.getDefaultLanguage()))) {
                            locale = resolveFromRequestLocales(request, languagesAsLocales, locale);
                        }
                        return locale;
                    } catch (RepositoryException e) {
                        return null;
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.warn("Error while setting the locale in SAML authentication", e);
        }
        return locale;
    }

    /**
     * Parse the Accept-Language header format and find the best matching locale
     */
    private static Locale parsePreferredLanguage(String preferredLanguage, List<Locale> supportedLocales, Locale defaultLocale) {
        // Parse the Accept-Language header format (e.g., "fr-FR,fr;q=0.9,en;q=0.8")
        String[] languages = preferredLanguage.split(",");
        for (String lang : languages) {
            String langCode = lang.split(";")[0].trim(); // Remove quality factor
            try {
                Locale preferredLocale = LanguageCodeConverters.languageCodeToLocale(langCode);
                if (supportedLocales.contains(preferredLocale)) {
                    return preferredLocale;
                }
                // Try with just the language part (fr instead of fr-FR)
                String simpleLang = langCode.split("-")[0];
                preferredLocale = LanguageCodeConverters.languageCodeToLocale(simpleLang);
                if (supportedLocales.contains(preferredLocale)) {
                    return preferredLocale;
                }
            } catch (Exception e) {
                LOGGER.debug("Could not parse language code: {}", langCode);
            }
        }
        return defaultLocale;
    }

    /**
     * Fallback to request locales if preferred language cookie didn't match
     */
    private static Locale resolveFromRequestLocales(HttpServletRequest request, List<Locale> supportedLocales, Locale defaultLocale) {
        Enumeration<Locale> requestLocales = request.getLocales();
        while (requestLocales.hasMoreElements()) {
            Locale requestLocale = requestLocales.nextElement();
            if (supportedLocales.contains(requestLocale)) {
                return requestLocale;
            }
        }
        return defaultLocale;
    }
}
