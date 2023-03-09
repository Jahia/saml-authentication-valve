import org.apache.commons.lang3.StringUtils
import org.jahia.services.sites.JahiaSitesService
import org.jahia.services.sites.SiteCreationInfo

JahiaSitesService sitesService = JahiaSitesService.getInstance();
if (sitesService.getSiteByKey("SITEKEY") == null) {

    def modulesToDeploy = "MODULES_TO_DEPLOY"

    SiteCreationInfo.Builder builder = SiteCreationInfo.builder()
            .siteKey("SITE_KEY")
            .serverName("localhost")
            .title("SITE_KEY")
            .templateSet("TEMPLATE_SET")
            .locale("en")

    if (StringUtils.isNotBlank(modulesToDeploy)) {
        builder.modulesToDeploy(StringUtils.split(modulesToDeploy, ","))
    }

    sitesService.addSite(builder.build())
}
