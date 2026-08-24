package org.jahia.modules.saml2.internal;

import org.pac4j.core.context.WebContext;
import org.pac4j.saml.store.SAMLMessageStore;
import org.pac4j.saml.store.SAMLMessageStoreFactory;

/**
 * Hands pac4j a {@link FlowCookieMessageStore} for the request in hand.
 * <p>
 * pac4j defaults to a factory that answers no store, and its {@code InResponseTo} comparison is
 * skipped when the store is absent.
 */
public class FlowCookieMessageStoreFactory implements SAMLMessageStoreFactory {

    @Override
    public SAMLMessageStore getMessageStore(WebContext context) {
        return new FlowCookieMessageStore(context);
    }
}
