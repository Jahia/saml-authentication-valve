import {enableModule, createSite, deleteSite, setNodeProperty} from '@jahia/cypress';
import {publishAndWaitJobEnding} from '@jahia/cypress/dist/utils/PublicationAndWorkflowHelper';

describe('Login via SAML on Private Site', () => {
    const siteKey = 'samlTestSite';

    const home = `/sites/${siteKey}/home`;
    const siteRoot = `/sites/${siteKey}`;

    before(() => {
        deleteSite(siteKey);
        createSite(siteKey, {
            languages: 'en,fr,de',
            locale: 'en',
            serverName: 'localhost',
            templateSet: 'samples-bootstrap-templates'
        });
        [
            'saml-authentication-valve',
            'jahia-authentication',
            'jcr-auth-provider'
        ].forEach(moduleName => {
            enableModule(moduleName, siteKey);
        });
        setNodeProperty(home, 'jcr:title', 'SAML Private Test Site', 'en');
        setNodeProperty(home, 'jcr:title', 'Site privé SAML Test', 'fr');
        setNodeProperty(home, 'jcr:title', 'SAML Private Test Site DE', 'de');
        publishAndWaitJobEnding(home, ['en', 'fr', 'de']);

        installConfig('samlLogin/org.jahia.modules.auth-samlTestSite.cfg');

        // Make the site and home page private by revoking guest access
        revokeGuestAccess(siteRoot);
        revokeGuestAccess(home);
        publishAndWaitJobEnding(home, ['en', 'fr', 'de']);
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('Anonymous user should not be able to access private site', () => {
        cy.clearAllCookies();
        cy.visit(`/sites/${siteKey}/home.html`, {failOnStatusCode: false});

        // Verify that the user gets a 404 error
        cy.get('body').should('satisfy', ($body) => {
            const bodyText = $body.text().toLowerCase();
            return bodyText.includes('page not found');
        });
    });

    it('User should be able to login using SAML authentication on private site', () => {
        cy.clearAllCookies();

        // Try to initiate the SAML Auth process - should be redirected to login
        cy.visit(`/sites/${siteKey}/connect.saml?siteKey=${siteKey}`, {failOnStatusCode: false});

        // Fill in credentials on IdP
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        cy.log('Verify user is logged in and can access private site');

        // Should now be able to access the private site
        cy.url().should('contain', `/sites/${siteKey}`);
        cy.get('body').should('contain', 'blachance8');
        cy.title().should('equal', 'SAML Private Test Site');
    });

    it('User should be able to access private site in different languages after SAML login', () => {
        cy.clearAllCookies();

        // Try to visit the French version of the private site
        cy.visit(`/sites/${siteKey}/connect.saml?siteKey=${siteKey}&locale=fr`, {failOnStatusCode: false});

        // Fill in credentials on IdP
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        // Should be able to access the French version
        cy.url().should('contain', `/sites/${siteKey}`);
        cy.get('body').should('contain', 'blachance8');
        cy.title().should('equal', 'Site privé SAML Test');
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

    function revokeGuestAccess(nodePath) {
        cy.apollo({
            mutationFile: 'samlLogin/revokeGuestAccess.graphql',
            variables: {nodePath}
        }).should(res => {
            expect(res?.data?.jcr?.mutateNode, `Revoked guest access for ${nodePath}`).to.be.not.undefined;
        });
    }
});
