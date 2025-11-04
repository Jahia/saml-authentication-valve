import {enableModule, createSite, deleteSite, setNodeProperty} from '@jahia/cypress';
import {publishAndWaitJobEnding} from '@jahia/cypress/dist/utils/PublicationAndWorkflowHelper';

describe('SAML Open Redirect Protection', () => {
    const siteKey = 'samlTestSite';
    const home = `/sites/${siteKey}/home`;
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
        const maliciousRedirect = 'http://evil.com/steal-data';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {
            failOnStatusCode: false,
            timeout: 30000
        });

        performKeycloakLogin(kcUsername, kcPassword);

        // Verify user is redirected to the safe default location (site home) instead of malicious URL
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'evil.com');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
    });

    it('Should block external URL redirects with HTTPS protocol', () => {
        // Attempt to use an external HTTPS URL as redirect parameter
        const maliciousRedirect = 'https://malicious-site.com/phishing';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {
            failOnStatusCode: false,
            timeout: 30000
        });

        performKeycloakLogin(kcUsername, kcPassword);

        // Verify user is redirected to the safe default location instead of malicious URL
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'malicious-site.com');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
    });

    it('Should block protocol-relative URLs (//)', () => {
        // Attempt to use a protocol-relative URL as redirect parameter
        const maliciousRedirect = '//attacker.com/evil-page';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {
            failOnStatusCode: false,
            timeout: 30000
        });

        performKeycloakLogin(kcUsername, kcPassword);

        // Verify user is redirected to the safe default location
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'attacker.com');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
    });

    it('Should block URLs with potential XSS payloads', () => {
        // Attempt to use a URL with XSS payload as redirect parameter
        const maliciousRedirect = '/sites/test?param=<script>alert("xss")</script>';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {
            failOnStatusCode: false,
            timeout: 30000
        });

        performKeycloakLogin(kcUsername, kcPassword);

        // Verify user is redirected to the safe default location
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', '<script>');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
    });

    it('Should allow safe local URL redirects', () => {
        // Use a safe local URL as redirect parameter
        const safeRedirect = `/sites/${siteKey}/home.html`;
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(safeRedirect)}`, {
            failOnStatusCode: false,
            timeout: 30000
        });

        performKeycloakLogin(kcUsername, kcPassword);

        // Verify user is redirected to the specified safe URL
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}/home`);
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
        cy.title().should('equal', 'SAML Open Redirect Test Site');
    });

    it('Should block FTP protocol URLs', () => {
        // Attempt to use an FTP URL as redirect parameter
        const maliciousRedirect = 'ftp://malicious-ftp.com/file.txt';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {
            failOnStatusCode: false,
            timeout: 30000
        });

        performKeycloakLogin(kcUsername, kcPassword);

        // Verify user is redirected to the safe default location
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'ftp://');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
    });

    it('Should block JavaScript protocol URLs', () => {
        // Attempt to use a JavaScript URL as redirect parameter
        // eslint-disable-next-line no-script-url
        const maliciousRedirect = 'javascript:alert("XSS")';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {
            failOnStatusCode: false,
            timeout: 30000
        });

        performKeycloakLogin(kcUsername, kcPassword);

        // Verify user is redirected to the safe default location
        cy.url({timeout: 15000}).should('contain', `/sites/${siteKey}`);
        // eslint-disable-next-line no-script-url
        cy.url().should('not.contain', 'javascript:');
        cy.get('body', {timeout: 10000}).should('contain', 'blachance8');
    });

    /**
     * Call keycloak login page and populate form.
     */
    function performKeycloakLogin(username: string, password: string) {
        cy.origin('http://keycloak:8080', () => {
            cy.get('#username', {timeout: 10000}).should('be.visible').type(username);
            cy.get('#password', {timeout: 10000}).should('be.visible').type(password);
            cy.get('input[type="submit"]', {timeout: 10000}).should('be.visible').click();
        });
    }

    /**
     * @param configFilePath config file path relative to fixtures folder
     */
    function installConfig(configFilePath) {
        return cy.runProvisioningScript(
            {fileContent: `- installConfiguration: "${configFilePath}"`, type: 'application/yaml'},
            [{fileName: `${configFilePath}`, type: 'text/plain'}]
        );
    }
});
