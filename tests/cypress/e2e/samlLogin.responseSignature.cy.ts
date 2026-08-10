import {createSite, deleteSite, deleteUser, enableModule, getUserPath, installConfig, publishAndWaitJobEnding, setNodeProperty} from '@jahia/cypress';
import {initiateSamlLogin, waitAndFillKeycloakLoginForm} from '../support/helper';

// `jahia-saml-client` signs both the response envelope and the assertion, so a site that asks for a
// signed envelope on top of the assertion requirement completes the login against it.
//
// The refusal direction is not covered here: it needs an identity provider that signs the assertion
// and leaves the envelope unsigned, and neither client in the test realm is configured that way.
describe('SAML login for a site that requires a signed response envelope', () => {
    const siteKey = 'samlTestSiteEnvelope';
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
        setNodeProperty(home, 'jcr:title', 'SAML Response Signature Test Site', 'en');
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

    it('A provider that signs both the envelope and the assertion completes the flow', () => {
        initiateSamlLogin({siteKey: siteKey});
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        cy.url({timeout: 15000}).should('include', `/sites/${siteKey}/home.html`);
        cy.get('body', {timeout: 10000}).should('contain', kcUsername);
        getUserPath(kcUsername).should(result => {
            expect(result?.data?.admin?.userAdmin?.user?.node?.path, `Account for ${kcUsername}`).to.contain(kcUsername);
        });
    });
});
