import {enableModule, createSite, deleteSite, installConfig, setNodeProperty} from '@jahia/cypress';
import {publishAndWaitJobEnding} from '@jahia/cypress/dist/utils/PublicationAndWorkflowHelper';
import {initiateSamlLogin, waitAndFillKeycloakLoginForm} from '../support/helper';

describe('SAML Open Redirect Protection', () => {
    const siteKey = 'samlTestSite';
    const home = `/sites/${siteKey}/home`;
    const TEST_CASES = [
        {testName: 'Should block external URL redirects with HTTP protocol', redirect: 'http://evil.com/steal-data', shouldContain: `/sites/${siteKey}`, shouldNotContain: 'evil.com'},
        {testName: 'Should block external URL redirects with HTTPS protocol', redirect: 'https://malicious-site.com/phishing', shouldContain: `/sites/${siteKey}`, shouldNotContain: 'malicious-site.com'},
        {testName: 'Should block protocol-relative URLs (//)', redirect: '//attacker.com/evil-page', shouldContain: `/sites/${siteKey}`, shouldNotContain: 'attacker.com'},
        {testName: 'Should block URLs with potential XSS payloads', redirect: '/sites/test?param=<script>alert("xss")</script>', shouldContain: `/sites/${siteKey}`, shouldNotContain: 'xss'},
        {testName: 'Should allow safe local URL redirects', redirect: `/sites/${siteKey}/home.html`, shouldContain: `/sites/${siteKey}/home`, expectedTitle: 'SAML Open Redirect Test Site'},
        {testName: 'Should block FTP protocol URLs', redirect: 'ftp://malicious-ftp.com/file.txt', shouldContain: `/sites/${siteKey}`, shouldNotContain: 'malicious-ftp.com'},
        // eslint-disable-next-line no-script-url
        {testName: 'Should block JavaScript protocol URLs', redirect: 'javascript:alert("XSS")', shouldContain: `/sites/${siteKey}`, shouldNotContain: 'javascript:'}
    ];

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
        cy.clearAllLocalStorage();
        cy.clearAllSessionStorage();
        // Wait a bit to ensure previous test cleanup is complete
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(500);
    });

    TEST_CASES.forEach(({testName, redirect, shouldContain, shouldNotContain}) => {
        it(testName, () => {
            initiateSamlLogin({siteKey: siteKey, redirect});
            waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

            // Verify user is redirected to the expected location
            cy.url({timeout: 15000}).should('contain', shouldContain);
            cy.url().should('not.contain', shouldNotContain);
            cy.get('body', {timeout: 10000}).should('contain', kcUsername);
        });
    });
});
