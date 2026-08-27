---
# Allowed version bumps: patch, minor, major
saml-authentication-valve: patch
---

Marked the two cookies that carry SAML sign-in flow state as server-only, so a page script can no longer read them. Only the server reads them, so no behaviour changes.
