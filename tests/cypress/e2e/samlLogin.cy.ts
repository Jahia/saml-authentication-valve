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
        deleteSite(siteKey);
    });

    it('User should be able to add SAML button and publish', () => {
        installConfig('samlLogin/org.jahia.modules.auth-samlTestSite.cfg');
        createSamlButton(buttonName);
        publishAndWaitJobEnding(home, ['en']);
    });

    it('User should be able to login using SAML authentication', () => {
        cy.clearAllCookies();
        cy.clearAllLocalStorage();
        cy.clearAllSessionStorage();

        // Delete user and wait for confirmation
        deleteUser('/users/fj/ac/bj/blachance8');
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(1000); // Wait for user deletion to complete

        cy.setLocale('en-EN');
        cy.setLanguageHeaders('en-EN');

        cy.visit('/', {
            onBeforeLoad: win => {
                // Ensure clean state
                win.sessionStorage.clear();
                win.localStorage.clear();
            }
        });

        cy.title().should('equal', 'SAML Test Site');
        cy.get(`input[value="${buttonName}"]`, {timeout: 10000}).should('exist').and('be.visible').click();

        // Wait for Keycloak login page to be fully loaded
        cy.origin('http://keycloak:8080', () => {
            cy.get('#username', {timeout: 10000}).should('be.visible').type('blachance8');
            cy.get('#password', {timeout: 10000}).should('be.visible').type('password');
            cy.get('input[type="submit"]', {timeout: 10000}).should('be.visible').click();
        });

        cy.log('Verify user is logged in');
        // Wait for redirect back to Jahia
        cy.url({timeout: 15000}).should('include', '/sites/samlTestSite');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
        cy.get(`input[value="${buttonName}"]`).should('not.exist'); // Logged in
        cy.title().should('equal', 'SAML Test Site');
    });

    it('User should be able to login using SAML authentication in FR', () => {
        cy.clearAllCookies();
        cy.clearAllLocalStorage();
        cy.clearAllSessionStorage();

        // Delete user and wait for confirmation
        deleteUser('/users/fj/ac/bj/blachance8');
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(1000); // Wait for user deletion to complete

        cy.setLocale('fr-FR');
        cy.setLanguageHeaders('fr-FR');

        cy.visit('/', {
            onBeforeLoad: win => {
                // Ensure clean state
                win.sessionStorage.clear();
                win.localStorage.clear();
            }
        });

        cy.title().should('equal', 'SAML Test Site FR');
        cy.get(`input[value="${buttonName}"]`, {timeout: 10000}).should('exist').and('be.visible').click();

        // Wait for Keycloak login page to be fully loaded
        cy.origin('http://keycloak:8080', () => {
            cy.get('#username', {timeout: 10000}).should('be.visible').type('blachance8');
            cy.get('#password', {timeout: 10000}).should('be.visible').type('password');
            cy.get('input[type="submit"]', {timeout: 10000}).should('be.visible').click();
        });

        cy.log('Verify user is logged in');
        // Wait for redirect back to Jahia
        cy.url({timeout: 15000}).should('include', '/sites/samlTestSite');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
        cy.get(`input[value="${buttonName}"]`).should('not.exist'); // Logged in
        cy.title().should('equal', 'SAML Test Site FR');
    });

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

    function deleteUser(userPath) {
        cy.apollo({
            mutationFile: 'samlLogin/deleteUser.graphql',
            variables: {userPath}
        }).then(() => {
            cy.log(`User ${userPath} deletion requested`);
        });
    }
});
