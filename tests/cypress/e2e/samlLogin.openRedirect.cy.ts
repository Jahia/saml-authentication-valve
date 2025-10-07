import {enableModule, createSite, deleteSite, setNodeProperty} from '@jahia/cypress';
import {publishAndWaitJobEnding} from '@jahia/cypress/dist/utils/PublicationAndWorkflowHelper';

describe('SAML Open Redirect Protection', () => {
    const siteKey = 'samlOpenRedirectTestSite';
    const home = `/sites/${siteKey}/home`;

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

    it('Should block external URL redirects with HTTP protocol', () => {
        cy.clearAllCookies();

        // Attempt to use an external HTTP URL as redirect parameter
        const maliciousRedirect = 'http://evil.com/steal-data';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {failOnStatusCode: false});

        // Complete SAML authentication
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        // Verify user is redirected to the safe default location (site home) instead of malicious URL
        cy.url().should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'evil.com');
        cy.get('body').should('contain', 'blachance8');
    });

    it('Should block external URL redirects with HTTPS protocol', () => {
        cy.clearAllCookies();

        // Attempt to use an external HTTPS URL as redirect parameter
        const maliciousRedirect = 'https://malicious-site.com/phishing';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {failOnStatusCode: false});

        // Complete SAML authentication
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        // Verify user is redirected to the safe default location instead of malicious URL
        cy.url().should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'malicious-site.com');
        cy.get('body').should('contain', 'blachance8');
    });

    it('Should block protocol-relative URLs (//)', () => {
        cy.clearAllCookies();

        // Attempt to use a protocol-relative URL as redirect parameter
        const maliciousRedirect = '//attacker.com/evil-page';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {failOnStatusCode: false});

        // Complete SAML authentication
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        // Verify user is redirected to the safe default location
        cy.url().should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'attacker.com');
        cy.get('body').should('contain', 'blachance8');
    });

    it('Should block URLs with path traversal attempts', () => {
        cy.clearAllCookies();

        // Attempt to use a URL with path traversal as redirect parameter
        const maliciousRedirect = '/sites/other-site/../../../admin/sensitive-data';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {failOnStatusCode: false});

        // Complete SAML authentication
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        // Verify user is redirected to the safe default location
        cy.url().should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', '../');
        cy.get('body').should('contain', 'blachance8');
    });

    it('Should block URLs with potential XSS payloads', () => {
        cy.clearAllCookies();

        // Attempt to use a URL with XSS payload as redirect parameter
        const maliciousRedirect = '/sites/test?param=<script>alert("xss")</script>';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {failOnStatusCode: false});

        // Complete SAML authentication
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        // Verify user is redirected to the safe default location
        cy.url().should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', '<script>');
        cy.get('body').should('contain', 'blachance8');
    });

    it('Should allow safe local URL redirects', () => {
        cy.clearAllCookies();

        // Use a safe local URL as redirect parameter
        const safeRedirect = `/sites/${siteKey}/home`;
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(safeRedirect)}`, {failOnStatusCode: false});

        // Complete SAML authentication
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        // Verify user is redirected to the specified safe URL
        cy.url().should('contain', `/sites/${siteKey}/home`);
        cy.get('body').should('contain', 'blachance8');
        cy.title().should('equal', 'SAML Open Redirect Test Site');
    });

    it('Should block FTP protocol URLs', () => {
        cy.clearAllCookies();

        // Attempt to use an FTP URL as redirect parameter
        const maliciousRedirect = 'ftp://malicious-ftp.com/file.txt';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {failOnStatusCode: false});

        // Complete SAML authentication
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        // Verify user is redirected to the safe default location
        cy.url().should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'ftp://');
        cy.get('body').should('contain', 'blachance8');
    });

    it('Should block JavaScript protocol URLs', () => {
        cy.clearAllCookies();

        // Attempt to use a JavaScript URL as redirect parameter
        const maliciousRedirect = 'javascript:alert("XSS")';
        cy.visit(`/connect.saml?siteKey=${siteKey}&redirect=${encodeURIComponent(maliciousRedirect)}`, {failOnStatusCode: false});

        // Complete SAML authentication
        cy.get('#username').should('be.visible').type('blachance8');
        cy.get('#password').should('be.visible').type('password');
        cy.get('input[type="submit"]').should('be.visible').click();

        // Verify user is redirected to the safe default location
        cy.url().should('contain', `/sites/${siteKey}`);
        cy.url().should('not.contain', 'javascript:');
        cy.get('body').should('contain', 'blachance8');
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

    function deleteUser(userPath) {
        cy.apollo({
            mutationFile: 'samlLogin/deleteUser.graphql',
            variables: {userPath}
        }).should(res => {
            expect(res?.data?.jcr?.deleteNode, `Deleted user at ${userPath}`).to.be.true;
        });
    }
});
