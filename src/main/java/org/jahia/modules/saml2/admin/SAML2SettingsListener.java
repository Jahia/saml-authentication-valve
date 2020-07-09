package org.jahia.modules.saml2.admin;

import org.jahia.modules.saml2.SAML2Constants;
import org.jahia.modules.saml2.admin.SAML2SettingsChangedListener.SAML2SettingsChangedEvent;
import org.jahia.services.content.DefaultEventListener;
import org.jahia.services.content.ExternalEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

/**
 * Listener to propagate settings info among cluster nodes
 */
public final class SAML2SettingsListener extends DefaultEventListener implements ExternalEventListener, ApplicationEventPublisherAware {

    private static final Logger logger = LoggerFactory.getLogger(SAML2SettingsListener.class);
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public int getEventTypes() {
        return Event.PROPERTY_ADDED + Event.PROPERTY_CHANGED + Event.PROPERTY_REMOVED + Event.NODE_ADDED + Event.NODE_REMOVED;
    }

    @Override
    public String[] getNodeTypes() {
        return new String[]{SAML2Constants.SETTINGS_NODE_TYPE};
    }

    @Override
    public void onEvent(final EventIterator events) {
        if (logger.isDebugEnabled()) {
            logger.debug("Firing off SAML2SettingsChangedEvent");
        }
        applicationEventPublisher.publishEvent(new SAML2SettingsChangedEvent(events));
    }

    public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
