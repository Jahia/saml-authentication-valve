package org.jahia.modules.saml2.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The one place this module writes a cookie.
 * <p>
 * Every cookie of a SAML flow carries the same three marks, and they are written here so a cookie
 * added later carries them too.
 * <ul>
 * <li>{@code HttpOnly}, because no script of a page has business reading a value of the flow.</li>
 * <li>{@code SameSite=None}, because the identity provider posts its response from its own page. A
 * browser withholds any other cookie on that cross-site POST, and a value the callback reads has to
 * arrive with it.</li>
 * <li>{@code Secure}, because a browser refuses {@code SameSite=None} without it. A deployment
 * therefore serves SAML over HTTPS, which a browser also requires of the POST itself.</li>
 * </ul>
 * The header is written by hand because the servlet {@code Cookie} of this container states no
 * {@code SameSite}.
 */
public final class SamlCookies {

    /** A flow that takes longer than this is not answered. */
    public static final int MAX_AGE_SECONDS = 600;

    private SamlCookies() {
        // Utility class
    }

    public static void write(HttpServletRequest request, HttpServletResponse response, String name,
            String value, int maxAgeSeconds) {
        String path = request.getContextPath().isEmpty() ? "/" : request.getContextPath();
        response.addHeader("Set-Cookie", String.format(
                "%s=%s; Path=%s; Max-Age=%d; HttpOnly; Secure; SameSite=None",
                name, value, path, maxAgeSeconds));
    }
}
