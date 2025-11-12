import {enableModule, createSite, deleteSite, deleteUser, setNodeProperty, installConfig, publishAndWaitJobEnding} from '@jahia/cypress';
import {createSamlButton, initiateSamlLogin, waitAndFillKeycloakLoginForm} from '../support/helper';

describe('Login via SAML', () => {
    const siteKey = 'samlTestSite';
    const home = `/sites/${siteKey}/home`;
    const buttonName = 'my-saml-button';

    const kcUrl = 'http://keycloak:8080';
    const kcUsername = 'blachance8';
    const kcPassword = 'password';

    const TEST_CASES = [
        {language: 'en', locale: 'en-EN', title: 'SAML Test Site EN'},
        {language: 'fr', locale: 'fr-FR', title: 'SAML Test Site FR'},
        {language: 'de', locale: 'de-DE', title: 'SAML Test Site DE'}
    ];

    before(() => {
        deleteSite(siteKey);
        createSite(siteKey, {
            languages: TEST_CASES.map(c => c.language).join(','),
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
        TEST_CASES.forEach(({language, title}) => {
            setNodeProperty(home, 'jcr:title', title, language);
        });
        publishAndWaitJobEnding(home, TEST_CASES.map(c => c.language));
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('User should be able to add SAML button and publish', () => {
        installConfig('samlLogin/org.jahia.modules.auth-samlTestSite.cfg');
        createSamlButton(home, buttonName);
        publishAndWaitJobEnding(home, ['en']);
    });

    TEST_CASES.forEach(({language, locale, title}) => {
        it('User should be able to login using SAML authentication in ' + language, () => {
            cy.clearAllLocalStorage();
            cy.clearAllSessionStorage();

            deleteUser(kcUsername);
            // eslint-disable-next-line cypress/no-unnecessary-waiting
            cy.wait(1000); // Wait for user deletion to complete

            cy.setLocale(locale);
            cy.setLanguageHeaders(locale);

            cy.visit('/', {
                onBeforeLoad: win => {
                    // Ensure clean state
                    win.sessionStorage.clear();
                    win.localStorage.clear();
                }
            });

            cy.log('Initiate SAML login flow');
            cy.title().should('equal', title);
            initiateSamlLogin({buttonName: buttonName}); // Should redirect to keycloak login page
            waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

            cy.log('Verify user is logged in');
            // Wait for redirect back to Jahia
            cy.url({timeout: 15000}).should('include', '/sites/' + siteKey);
            cy.get('body', {timeout: 10000}).should('contain', kcUsername);
            cy.get(`input[value="${buttonName}"]`).should('not.exist'); // Logged in
            cy.title().should('equal', title);
        });
    });
});
