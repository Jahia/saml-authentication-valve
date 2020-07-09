package org.jahia.modules.saml2.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.modules.saml2.admin.SAML2Settings;
import org.jahia.modules.saml2.admin.SAML2SettingsService;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import org.pac4j.saml.crypto.KeyStoreCredentialProvider;
import org.pac4j.saml.metadata.SAML2MetadataGenerator;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public final class MetadataAction extends Action {

    private SAML2SettingsService saml2SettingsService;
    private SAML2Util util;

    @Override
    public ActionResult doExecute(final HttpServletRequest req, final RenderContext renderContext, final Resource resource, final JCRSessionWrapper session, final Map<String, List<String>> parameters, final URLResolver urlResolver) throws Exception {
        if (renderContext.getSite() == null) {
            return ActionResult.OK;
        }
        util.initialize(() -> {
            final String siteKey = renderContext.getSite().getSiteKey();
            final SAML2Settings saml2Settings = saml2SettingsService.getSettings(siteKey);

            final SAML2ClientConfiguration saml2ClientConfiguration = util.getSAML2ClientConfiguration(saml2Settings);
            final KeyStoreCredentialProvider keyStoreCredentialProvider = new KeyStoreCredentialProvider(saml2ClientConfiguration);
            final SAML2MetadataGenerator saml2MetadataGenerator = new SAML2MetadataGenerator(null);
            saml2MetadataGenerator.setEntityId(saml2Settings.getRelyingPartyIdentifier());
            saml2MetadataGenerator.setAssertionConsumerServiceUrl(util.getAssertionConsumerServiceUrl(req, saml2Settings.getIncomingTargetUrl()));
            saml2MetadataGenerator.setCredentialProvider(keyStoreCredentialProvider);

            renderContext.getResponse().getWriter().append(saml2MetadataGenerator.getMetadata(saml2MetadataGenerator.buildEntityDescriptor()));

        });
        return ActionResult.OK;
    }

    public void setSaml2SettingsService(SAML2SettingsService saml2SettingsService) {
        this.saml2SettingsService = saml2SettingsService;
    }

    public void setUtil(SAML2Util util) {
        this.util = util;
    }
}
