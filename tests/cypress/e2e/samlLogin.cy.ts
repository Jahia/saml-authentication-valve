import {enableModule, createSite, deleteSite, setNodeProperty} from '@jahia/cypress';
import {publishAndWaitJobEnding} from '@jahia/cypress/dist/utils/PublicationAndWorkflowHelper';

describe('Login via SAML', () => {
    const siteKey = 'samlTestSite';
    const buttonName = 'my-saml-button';

    const home = `/sites/${siteKey}/home`;
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
        setNodeProperty(home, 'jcr:title', 'SAML Test Site', 'en');
        setNodeProperty(home, 'jcr:title', 'SAML Test Site FR', 'fr');
        setNodeProperty(home, 'jcr:title', 'SAML Test Site DE', 'de');
        publishAndWaitJobEnding(home, ['en', 'fr', 'de']);
    });

    after(() => {
        // deleteSite(siteKey);
    });

    it('User should be able to add SAML button and publish', () => {
        installConfig('samlLogin/org.jahia.modules.auth-samlTestSite.cfg');
        createSamlButton(buttonName);
        publishAndWaitJobEnding(home, ['en']);
    });

    /* Wait/retry until site is published */
    it('User should be able to login using SAML authentication', () => {
        cy.visit('sites/samlTestSite/home.html');
        cy.get(`input[value="${buttonName}"]`).should('exist').and('be.visible').click();
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        cy.log('Verify user is logged in');
        cy.get('body').should('contain', 'blachance8');
        cy.get(`input[value="${buttonName}"]`).should('not.exist'); // Logged in
    });

    it.skip('User should be able to login using SAML authentication in FR', () => {
        cy.clearAllCookies();
        cy.visit('/fr/sites/samlTestSite/home.html', {
            headers: {
                'Accept-Language': 'fr'
            }
        });
        cy.title().should('include', 'SAML Test Site FR');
        cy.get(`input[value="${buttonName}"]`).should('exist').and('be.visible').click();
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        cy.log('Verify user is logged in');
        cy.get('body').should('contain', 'blachance8');
        cy.get(`input[value="${buttonName}"]`).should('not.exist'); // Logged in
        cy.title().should('include', 'SAML Test Site FR');
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
            variables: {homePath: home, name}
        }).should(res => {
            expect(res?.data?.jcr.addNode.addChild.uuid, `Created SAML button ${name}`).to.be.not.undefined;
        });
    }
});
