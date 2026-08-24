package org.jahia.modules.saml2.internal;

import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorPropertyInfo;
import org.jahia.modules.jahiaauth.service.ConnectorService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.saml.profile.SAML2Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component(service = ConnectorService.class, immediate = true, property = "connectorServiceName=Saml")
public class SamlConnector implements ConnectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SamlConnector.class);

    @Reference
    private SAML2Util util;

    @Override
    public List<ConnectorPropertyInfo> getAvailableProperties() {
        LOGGER.debug("Get available properties");
        List<ConnectorPropertyInfo> array = new ArrayList<>();
        array.add(new ConnectorPropertyInfo(SAML2Constants.NAME_ID, "string"));
        array.add(new ConnectorPropertyInfo("login", "string"));
        new CommonProfileDefinition<SAML2Profile>().getPrimaryAttributes().forEach(s -> array.add(new ConnectorPropertyInfo(s, s.equals(CommonProfileDefinition.EMAIL) ? "email" : "string")));
        return array;
    }

    @Override
    public String getVerifiedSubjectProperty() {
        // SAML states the subject of an assertion in its NameID, and the signature covers it. An
        // attribute beside it carries whatever the identity provider was configured to send.
        return SAML2Constants.NAME_ID;
    }

    public void validateSettings(ConnectorConfig settings) throws IOException {
        LOGGER.debug("Validating settings for Saml connector");
        util.validateSettings(settings);
        util.resetClient(settings.getSiteKey());
    }

}
