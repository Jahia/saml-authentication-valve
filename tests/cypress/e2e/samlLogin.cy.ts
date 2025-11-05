import {enableModule, createSite, deleteSite, deleteUser, setNodeProperty} from '@jahia/cypress';
import {publishAndWaitJobEnding} from '@jahia/cypress/dist/utils/PublicationAndWorkflowHelper';
import {installConfig, createSamlButton, initiateSamlLogin, waitAndFillKeycloakLoginForm} from '../support/helper';

describe('Login via SAML', () => {
    const siteKey = 'samlTestSite';
    const home = `/sites/${siteKey}/home`;
    const buttonName = 'my-saml-button';

    const kcUrl = 'http://keycloak:8080';
    const kcUsername = 'blachance8';
    const kcPassword = 'password';

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
        createSamlButton(home, buttonName);
        publishAndWaitJobEnding(home, ['en']);
    });

    it('User should be able to login using SAML authentication', () => {
        cy.clearAllCookies();
        cy.clearAllLocalStorage();
        cy.clearAllSessionStorage();

        deleteUser('blachance8');
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

        cy.log('Initiate SAML login flow');
        cy.title().should('equal', 'SAML Test Site');
        initiateSamlLogin({buttonName: buttonName}); // Should redirect to keycloak login page
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

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

        deleteUser('blachance8');
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

        cy.log('Initiate SAML login flow');
        cy.title().should('equal', 'SAML Test Site FR');
        initiateSamlLogin({buttonName: buttonName}); // Should redirect to keycloak login page
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        cy.log('Verify user is logged in');
        // Wait for redirect back to Jahia
        cy.url({timeout: 15000}).should('include', '/sites/samlTestSite');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
        cy.get(`input[value="${buttonName}"]`).should('not.exist'); // Logged in
        cy.title().should('equal', 'SAML Test Site FR');
    });
});
