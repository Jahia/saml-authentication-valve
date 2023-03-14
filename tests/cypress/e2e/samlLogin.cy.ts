import {publishSite, createSite, deleteSite} from '../fixtures/site';

describe('Login via SAML', () => {
    const siteKey = 'samlTestSite';
    const buttonName = 'my-saml-button';

    before(() => {
        createSite(siteKey, [
            'saml-authentication-valve',
            'jahia-authentication',
            'jcr-auth-provider'
        ]);
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('User should be able to add SAML button and publish', () => {
        installConfig('samlLogin/org.jahia.modules.auth-samlTestSite.cfg');
        createSamlButton(buttonName);
        publishSite(siteKey, 'en');
    });

    /* Wait/retry until site is published */
    it('User should be able to login using SAML authentication', {retries: 5}, () => {
        cy.visit('sites/samlTestSite/home.html');
        cy.get(`input[value="${buttonName}"]`).should('exist').and('be.visible').click();
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        cy.log('Verify user is logged in');
        cy.get('body').should('contain', 'blachance8');
        cy.get(`input[value="${buttonName}"]`).should('not.exist'); // Logged in
    });

    /**
     * @param configFilePath config file path relative to fixtures folder
     */
    function installConfig(configFilePath) {
        return cy.runProvisioningScript(
            {fileContent: `- installConfiguration: "${configFilePath}"`, type: 'application/yaml'},
            [{fileName: `${configFilePath}`, type: 'text/plain'}]
        );
    }

    function createSamlButton(name) {
        cy.apollo({
            mutationFile: 'samlLogin/createSamlButton.graphql',
            variables: {homePath: `/sites/${siteKey}/home`, name}
        }).should(res => {
            expect(res?.data?.jcr.addNode.addChild.uuid, `Created SAML button ${name}`).to.be.not.undefined;
        });
    }
});
