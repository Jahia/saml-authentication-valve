package org.jahia.modules.saml2.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.saml2.SAML2Util;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.utils.ClassLoaderUtils;
import org.opensaml.core.config.InitializationService;
import org.pac4j.saml.metadata.SAML2MetadataResolver;
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

            SAML2MetadataResolver metadataResolver = util.getSAML2Client(settingsService, renderContext.getRequest(), siteKey).getServiceProviderMetadataResolver();

            try {
                renderContext.getResponse().getWriter().append(metadataResolver.getMetadata());
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
