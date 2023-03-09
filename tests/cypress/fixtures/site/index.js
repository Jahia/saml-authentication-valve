export function publishSite(siteKey, lang) {
    return cy.apollo({
        mutationFile: "site/publishSite.graphql",
        variables:{sitePath: `/sites/${siteKey}`, lang:[lang]}
    }).should((res) => {
        expect(res?.data?.jcr.mutateNode.publish).to.be.true;
    });
}

export function createSite(siteKey, modulesToDeploy = []) {
    cy.executeGroovy('site/createSite.groovy', {
        SITE_KEY: siteKey,
        MODULES_TO_DEPLOY: modulesToDeploy.join(',')
    });
}

export function deleteSite(siteKey) {
    cy.executeGroovy('site/deleteSite.groovy', {SITEKEY: siteKey});
}
