---
saml-authentication-valve: patch
---

Changed SAML2 login so a site accepts an identity provider's response only when the assertion it carries is signed. Sites that have not made an explicit choice in SAML2 Settings are covered by this requirement. If your identity provider signs only the response envelope, either configure it to sign the assertion, or clear Requires signed assertions in Administration > Sites > Jahia Authentication > SAML2 Settings. A new Requires signed responses option lets you also require a signature on the response envelope; it stays off unless you turn it on.
