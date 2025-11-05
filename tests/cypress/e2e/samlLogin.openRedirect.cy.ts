import {enableModule, createSite, deleteSite, installConfig} from '@jahia/cypress';
import {publishAndWaitJobEnding} from '@jahia/cypress/dist/utils/PublicationAndWorkflowHelper';
import {initiateSamlLogin, waitAndFillKeycloakLoginForm} from '../support/helper';

describe('SAML Open Redirect Protection', () => {
    const siteKey = 'samlTestSite';
    const home = `/sites/${siteKey}/home`;

    const kcUrl = 'http://keycloak:8080';
    const kcUsername = 'blachance8';
    const kcPassword = 'password';

    before(() => {
        deleteSite(siteKey);
        createSite(siteKey, {
            languages: 'en',
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
        setNodeProperty(home, 'jcr:title', 'SAML Open Redirect Test Site', 'en');
        publishAndWaitJobEnding(home, ['en']);

        installConfig('samlLogin/org.jahia.modules.auth-samlTestSite.cfg');
    });

    after(() => {
        deleteSite(siteKey);
    });

    // Add beforeEach to ensure clean state
    beforeEach(() => {
        cy.clearAllCookies();
        cy.clearAllLocalStorage();
        cy.clearAllSessionStorage();
        // Wait a bit to ensure previous test cleanup is complete
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(500);
    });

    it('Should block external URL redirects with HTTP protocol', () => {
        // Attempt to use an external HTTP URL as redirect parameter
        initiateSamlLogin({siteKey: siteKey, redirect: 'http://evil.com/steal-data'});
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        // Verify user is redirected to the safe default location (site home) instead of malicious URL
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'evil.com');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
    });

    it('Should block external URL redirects with HTTPS protocol', () => {
        // Attempt to use an external HTTPS URL as redirect parameter
        initiateSamlLogin({siteKey: siteKey, redirect: 'https://malicious-site.com/phishing'});
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        // Verify user is redirected to the safe default location instead of malicious URL
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'malicious-site.com');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
    });

    it('Should block protocol-relative URLs (//)', () => {
        // Attempt to use a protocol-relative URL as redirect parameter
        initiateSamlLogin({siteKey: siteKey, redirect: '//attacker.com/evil-page'});
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        // Verify user is redirected to the safe default location
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'attacker.com');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
    });

    it('Should block URLs with potential XSS payloads', () => {
        // Attempt to use a URL with XSS payload as redirect parameter
        initiateSamlLogin({siteKey: siteKey, redirect: '/sites/test?param=<script>alert("xss")</script>'});
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        // Verify user is redirected to the safe default location
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', '<script>');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
    });

    it('Should allow safe local URL redirects', () => {
        // Use a safe local URL as redirect parameter
        initiateSamlLogin({siteKey: siteKey, redirect: `/sites/${siteKey}/home.html`});
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        // Verify user is redirected to the specified safe URL
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}/home`);
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
        cy.title().should('equal', 'SAML Open Redirect Test Site');
    });

    it('Should block FTP protocol URLs', () => {
        // Attempt to use an FTP URL as redirect parameter
        initiateSamlLogin({siteKey: siteKey, redirect: 'ftp://malicious-ftp.com/file.txt'});
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        // Verify user is redirected to the safe default location
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'ftp://');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
    });

    it('Should block JavaScript protocol URLs', () => {
        // Attempt to use a JavaScript URL as redirect parameter
        // eslint-disable-next-line no-script-url
        initiateSamlLogin({siteKey: siteKey, redirect: 'javascript:alert("XSS")'});
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        // Verify user is redirected to the safe default location
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}`);
        // eslint-disable-next-line no-script-url
        cy.url().should('not.contain', 'javascript:');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
    });
});
