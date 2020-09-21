package org.jahia.modules.saml2.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.utils.ClassLoaderUtils;
import org.opensaml.core.config.InitializationService;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import org.pac4j.saml.crypto.KeyStoreCredentialProvider;
import org.pac4j.saml.metadata.SAML2MetadataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public final class MetadataAction extends Action {
    private static final Logger logger = LoggerFactory.getLogger(MetadataAction.class);

    private SettingsService settingsService;
    private SAML2Util util;

    @Override
    public ActionResult doExecute(final HttpServletRequest req, final RenderContext renderContext, final Resource resource, final JCRSessionWrapper session, final Map<String, List<String>> parameters, final URLResolver urlResolver) throws Exception {
        if (renderContext.getSite() == null) {
            return ActionResult.OK;
        }
        return ClassLoaderUtils.executeWith(InitializationService.class.getClassLoader(), () -> {
            final String siteKey = renderContext.getSite().getSiteKey();
            final ConnectorConfig saml2Settings = settingsService.getConnectorConfig(siteKey, "Saml");

            final SAML2ClientConfiguration saml2ClientConfiguration = util.getSAML2ClientConfiguration(saml2Settings);
            final KeyStoreCredentialProvider keyStoreCredentialProvider = new KeyStoreCredentialProvider(saml2ClientConfiguration);
            final SAML2MetadataGenerator saml2MetadataGenerator = new SAML2MetadataGenerator(null);
            saml2MetadataGenerator.setEntityId(saml2Settings.getProperty(SAML2Constants.RELYING_PARTY_IDENTIFIER));
            saml2MetadataGenerator.setAssertionConsumerServiceUrl(util.getAssertionConsumerServiceUrl(req, saml2Settings.getProperty(SAML2Constants.INCOMING_TARGET_URL)));
            saml2MetadataGenerator.setCredentialProvider(keyStoreCredentialProvider);

            try {
                renderContext.getResponse().getWriter().append(saml2MetadataGenerator.getMetadata(saml2MetadataGenerator.buildEntityDescriptor()));
            } catch (Exception e) {
                logger.error("Error when getting metadata", e);
                return ActionResult.INTERNAL_ERROR;
            }
            return ActionResult.OK;
        });
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setUtil(SAML2Util util) {
        this.util = util;
    }
}
