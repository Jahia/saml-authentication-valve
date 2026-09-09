---
# Allowed version bumps: patch, minor, major
saml-authentication-valve: patch
---

Dropped the unused velocity-tools library from the module. It carried commons-beanutils 1.9.4, which is reported as vulnerable (CVE-2025-48734), along with commons-digester3 and json-simple. Nothing in the module or in the SAML libraries it embeds ever called them, so no behaviour changes.
