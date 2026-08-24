---
# Allowed version bumps: patch, minor, major
saml-authentication-valve: major
---

Changed SAML sign-ins so a response is accepted when it answers an authentication request this service provider issued for that browser.

Every cookie the module writes is marked HttpOnly, Secure and SameSite=None. A deployment therefore serves SAML over HTTPS, which a browser already requires of the response the identity provider posts.

The account a sign-in resolves is read from the NameID of the assertion, which the signature covers.
