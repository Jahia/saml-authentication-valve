package org.jahia.modules.saml2.internal;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.saml.store.SAMLMessageStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opensaml.saml.saml2.core.RequestAbstractType;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Remembers the identifier of the authentication request a browser started, in a cookie of that
 * browser.
 * <p>
 * pac4j compares the {@code InResponseTo} of a response against this store, and skips the comparison
 * when no store exists. A response then signs a user in whether or not it answers a request this
 * service provider issued.
 * <p>
 * The store is a cookie rather than the session for one measured reason. The identity provider posts
 * the response from its own page, which is a cross-site POST, and a browser withholds the session
 * cookie on one of those. A store on the session is therefore unreachable exactly when the response
 * arrives. The cookie below carries {@code SameSite=None}, so the browser sends it, and {@code Secure}
 * because a browser refuses that combination without it.
 * <p>
 * Nothing is held on the server, so a cluster shares nothing and needs nothing. The cookie is cleared
 * when the response it answers arrives, which makes the binding single use.
 * <p>
 * The value is the request itself, deflated and encoded, and it carries no signature. What the store
 * answers is whether this browser started the request the response names. A browser holds no cookie of
 * another browser, and a request a visitor writes for themselves is one they started. A signature
 * would guard neither.
 * <p>
 * The whole request is kept rather than its identifier alone, because pac4j reads the endpoint and the
 * binding off it to check that the response came back where the request asked. A rebuilt object
 * carrying only the fields pac4j reads today would answer null to a field a later version reads, and
 * would do so in silence.
 */
public class FlowCookieMessageStore implements SAMLMessageStore {

    static final String COOKIE_NAME = "jahia-saml-flow";
    /** An authentication request that takes longer than this is not answered. */
    static final int MAX_AGE_SECONDS = 600;

    private static final Logger logger = LoggerFactory.getLogger(FlowCookieMessageStore.class);

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public FlowCookieMessageStore(WebContext context) {
        JEEContext jeeContext = (JEEContext) context;
        this.request = jeeContext.getNativeRequest();
        this.response = jeeContext.getNativeResponse();
    }

    @Override
    public void set(String requestId, XMLObject message) {
        try {
            write(encode(message), MAX_AGE_SECONDS);
        } catch (Exception e) {
            logger.error("Cannot remember the authentication request, so its response will be refused", e);
        }
    }

    @Override
    public Optional<XMLObject> get(String requestId) {
        String stored = read();
        if (stored == null || stored.isEmpty()) {
            logger.warn("The response names a request this browser did not start");
            return Optional.empty();
        }
        XMLObject started;
        try {
            started = decode(stored);
        } catch (Exception e) {
            logger.warn("The remembered authentication request cannot be read", e);
            return Optional.empty();
        }
        if (!(started instanceof RequestAbstractType)
                || !((RequestAbstractType) started).getID().equals(requestId)) {
            logger.warn("The response names a request other than the one this browser started");
            return Optional.empty();
        }
        return Optional.of(started);
    }

    private static String encode(XMLObject message) throws Exception {
        String xml = SerializeSupport.nodeToString(XMLObjectSupport.marshall(message));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater =
                     new DeflaterOutputStream(bytes, new Deflater(Deflater.BEST_COMPRESSION, true))) {
            deflater.write(xml.getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
    }

    private static XMLObject decode(String value) throws Exception {
        byte[] deflated = Base64.getUrlDecoder().decode(value);
        try (InflaterInputStream inflater = new InflaterInputStream(
                new ByteArrayInputStream(deflated), new java.util.zip.Inflater(true))) {
            return XMLObjectSupport.unmarshallFromInputStream(
                    XMLObjectProviderRegistrySupport.getParserPool(), inflater);
        }
    }

    @Override
    public void remove(String requestId) {
        write("", 0);
    }

    private String read() {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void write(String value, int maxAge) {
        SamlCookies.write(request, response, COOKIE_NAME, value, maxAge);
    }
}
