# Security analyses

## `vex.cdx.json`

A CycloneDX VEX document recording the vulnerabilities that Dependency Track reports against this
module's SBOM and that we have analysed as not affecting it. Each entry carries the reasoning, so the
analysis can be reviewed rather than taken on trust.

The build pipeline does not consume this file: `.github/workflows/on-merge.yml` uploads the SBOM to
Dependency Track, and nothing uploads the VEX. It is kept here because the analysis has to be applied
again for every Dependency Track project version — the SBOM is uploaded with the branch name as the
version — and because a reviewer should be able to see why a finding was dismissed.

Applying it, either way:

- **In the UI** — open the project in Dependency Track, go to *Audit Vulnerabilities*, select the
  finding, then set *Analysis* to `NOT_AFFECTED` with justification *Code Not Reachable*, paste the
  `detail` text from the VEX entry, and suppress the finding.
- **Through the API** — with the project's UUID and an API key that holds `VULNERABILITY_ANALYSIS`:

  ```bash
  curl -X POST "https://${DT_HOST}/api/v1/vex" \
       -H "X-Api-Key: ${DT_APIKEY}" \
       -F "project=${DT_PROJECT_UUID}" \
       -F "vex=@.security/vex.cdx.json"
  ```

The pac4j entry is a holding position: the migration that removes the finding for real is tracked in
[#200](https://github.com/Jahia/saml-authentication-valve/issues/200).

Whenever a dependency is upgraded or removed, revisit the entries here: an analysis written against
one version of a library says nothing about the next one.
