import {enableModule, createSite, deleteSite, deleteUser, setNodeProperty, getJahiaVersion, revokeRoles, installConfig} from '@jahia/cypress';
import {publishAndWaitJobEnding} from '@jahia/cypress/dist/utils/PublicationAndWorkflowHelper';
import {waitAndFillKeycloakLoginForm, initiateSamlLogin} from '../support/helper';
import {compare} from 'compare-versions';

describe('Login via SAML on Private Site', () => {
    const siteKey = 'samlTestSite';
    const home = `/sites/${siteKey}/home`;
    const siteRoot = `/sites/${siteKey}`;

    const kcUrl = 'http://keycloak:8080';
    const kcUsername = 'blachance8';
    const kcPassword = 'password';

    before(() => {
        deleteSite(siteKey);
        deleteUser(kcUsername);
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
        publishAndWaitJobEnding(home, ['en']);

        installConfig('samlLogin/org.jahia.modules.auth-samlTestSite.cfg');

        // Make the site and home page private by revoking guest access
        revokeRoles(siteRoot, ['reader'], 'guest', 'USER');
        revokeRoles(home, ['reader'], 'guest', 'USER');
        publishAndWaitJobEnding(home, ['en']);
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('Anonymous user should not be able to access private site', () => {
        cy.clearAllCookies();
        cy.visit(`${home}`, {failOnStatusCode: false});

        getJahiaVersion().then(jahiaVersion => {
            console.log(jahiaVersion);
            if (compare(jahiaVersion.release.replace('-SNAPSHOT', ''), '8.2', '<')) {
                // On Jahia 8.1 there is no 404 page but a redirect to local login page
                cy.get('#loginForm').should('exist');
            } else {
                // Verify that the user gets a 404 error
                cy.get('body').should('satisfy', $body => {
                    const bodyText = $body.text().toLowerCase();
                    return bodyText.includes('page not found');
                });
            }
        });
    });

    it('User should be able to login using SAML authentication on private site', () => {
        cy.clearAllCookies();
        cy.setLocale('en-EN');
        cy.setLanguageHeaders('en-EN');
        cy.reload();

        initiateSamlLogin({siteKey: siteKey});
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        cy.log('Verify user is logged in and can access private site');

        // Should now be able to access the private site
        cy.url().should('contain', `/sites/${siteKey}`);
        cy.get('body').should('contain', kcUsername);
        cy.title().should('equal', 'SAML Private Test Site');
    });
});
