---
# Allowed version bumps: patch, minor, major
saml-authentication-valve: patch
---

The SAML connect endpoint no longer writes a per-site cookie that nothing reads. The site a sign-in resolves against still travels on the redirect the flow builds, so behaviour is unchanged unless your own code read that cookie.
