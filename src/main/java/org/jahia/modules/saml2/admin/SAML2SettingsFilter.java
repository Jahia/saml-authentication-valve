package org.jahia.modules.saml2.admin;

import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SAML2SettingsFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(SAML2SettingsFilter.class);
    private SAML2SettingsService saml2SettingsService;

    @Override
    public String prepare(final RenderContext renderContext, final Resource resource, final RenderChain chain) throws Exception {

        final SAML2Settings saml2Settings = saml2SettingsService.getSettings(renderContext.getSite().getSiteKey());
        renderContext.getRequest().setAttribute("saml2HasSettings", saml2Settings != null);

        final String language = renderContext.getUILocale().getLanguage();
        if (logger.isDebugEnabled()) {
            logger.debug("langugage is {}", language);
        }
        if (saml2SettingsService.getResourceBundleName() != null) {
            renderContext.getRequest().setAttribute("i18nJavaScriptFile", "i18n/" + saml2SettingsService.getResourceBundleName() + (saml2SettingsService.getSupportedLocales().contains(language) ? ("_" + language) : "") + ".js");
        }
        return null;
    }

    public void setSaml2SettingsService(final SAML2SettingsService saml2SettingsService) {
        this.saml2SettingsService = saml2SettingsService;
    }
}
