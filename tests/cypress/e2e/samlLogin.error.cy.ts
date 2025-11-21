import {createSite, deleteSite, enableModule, publishAndWaitJobEnding, setNodeProperty} from '@jahia/cypress';

describe('Login via SAML on Private Site', () => {
    const siteKey = 'samlTestSiteError';
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
        setNodeProperty(home, 'jcr:title', 'SAML Private Test Site Without SAML config', 'en');
        publishAndWaitJobEnding(home, ['en']);
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('Trying to connect on a site without SAML config must raise an HTTP 500 error', () => {
        const url = `/connect.saml?siteKey=${siteKey}`;
        cy.request({url, failOnStatusCode: false}).then(response => {
            expect(response.status).to.eq(500);
        });
    });

    it('Trying to connect without giving a sitekey  must raise an HTTP 400 error', () => {
        const url = '/connect.saml';
        cy.request({url, failOnStatusCode: false}).then(response => {
            expect(response.status).to.eq(400);
        });
    });

    it('Trying a callback on a site without SAML config must raise an HTTP 500 error', () => {
        const url = `/callback.saml?siteKey=${siteKey}`;
        cy.request({url, failOnStatusCode: false}).then(response => {
            expect(response.status).to.eq(500);
        });
    });

    it('Trying a callback without giving a sitekey  must raise an HTTP 400 error', () => {
        const url = '/callback.saml';
        cy.request({url, failOnStatusCode: false}).then(response => {
            expect(response.status).to.eq(400);
        });
    });

    it('Trying to get metadata on a site without SAML config must raise an HTTP 500 error', () => {
        const url = `/metadata.saml?siteKey=${siteKey}`;
        cy.request({url, failOnStatusCode: false}).then(response => {
            expect(response.status).to.eq(500);
        });
    });

    it('Trying to get metadata without giving a sitekey  must raise an HTTP 400 error', () => {
        const url = '/metadata.saml';
        cy.request({url, failOnStatusCode: false}).then(response => {
            expect(response.status).to.eq(400);
        });
    });
});
