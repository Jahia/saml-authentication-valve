import {createSite, deleteSite, deleteUser, enableModule, installConfig, publishAndWaitJobEnding, setNodeProperty} from '@jahia/cypress';
import {initiateSamlLogin, waitAndFillKeycloakLoginForm} from '../support/helper';

// The identity provider used here signs the response envelope and leaves the assertion unsigned.
// The site configuration makes no explicit choice about assertion signatures.
// The signed-assertion path is covered by samlLogin.cy.ts.
describe('SAML login against an identity provider that leaves the assertion unsigned', () => {
    const siteKey = 'samlTestSiteUnsigned';
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
        setNodeProperty(home, 'jcr:title', 'SAML Assertion Signature Test Site', 'en');
        publishAndWaitJobEnding(home, ['en']);

        installConfig(`samlLogin/org.jahia.modules.auth-${siteKey}.cfg`);
    });

    after(() => {
        deleteSite(siteKey);
    });

    beforeEach(() => {
        cy.clearAllLocalStorage();
        cy.clearAllSessionStorage();
        deleteUser(kcUsername);
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(1000); // Wait for user deletion to complete
    });

    it('The site declares that it expects a signature on the assertion', () => {
        cy.request(`/metadata.saml?siteKey=${siteKey}`).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('WantAssertionsSigned="true"');
        });
    });

    it('The flow stops at the callback and leaves the session anonymous', () => {
        initiateSamlLogin({siteKey: siteKey});
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        // The identity provider returns its response to the callback, and the flow ends there:
        // no redirection into the site, and the session still belongs to nobody.
        cy.url({timeout: 15000}).should('include', 'callback.saml');
        cy.get('body', {timeout: 10000}).should('not.contain', kcUsername);
    });
});
