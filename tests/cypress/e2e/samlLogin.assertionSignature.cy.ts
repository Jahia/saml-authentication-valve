import {createSite, deleteSite, deleteUser, enableModule, getUserPath, installConfig, publishAndWaitJobEnding, setNodeProperty} from '@jahia/cypress';
import {initiateSamlLogin, waitAndFillKeycloakLoginForm} from '../support/helper';

// Both sites use the same identity provider, which signs the response envelope and leaves the
// assertion unsigned. Their configurations differ by `Saml.requireSignedAssertions` alone: the
// first leaves it out, the second sets it to false. So the outcome below is attributable to that
// setting and to nothing else in the flow.
describe('SAML login against an identity provider that leaves the assertion unsigned', () => {
    const kcUrl = 'http://keycloak:8080';
    const kcUsername = 'blachance8';
    const kcPassword = 'password';

    const SITES = [
        {siteKey: 'samlTestSiteUnsigned', title: 'SAML Assertion Signature Test Site'},
        {siteKey: 'samlTestSiteOptout', title: 'SAML Assertion Signature Opt-out Test Site'}
    ];

    before(() => {
        SITES.forEach(({siteKey, title}) => {
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
            setNodeProperty(`/sites/${siteKey}/home`, 'jcr:title', title, 'en');
            publishAndWaitJobEnding(`/sites/${siteKey}/home`, ['en']);

            installConfig(`samlLogin/org.jahia.modules.auth-${siteKey}.cfg`);
        });
    });

    after(() => {
        SITES.forEach(({siteKey}) => deleteSite(siteKey));
    });

    beforeEach(() => {
        cy.clearAllLocalStorage();
        cy.clearAllSessionStorage();
        deleteUser(kcUsername);
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(1000); // Wait for user deletion to complete
    });

    it('A site that has made no choice declares that it expects a signature on the assertion', () => {
        cy.request('/metadata.saml?siteKey=samlTestSiteUnsigned').then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('WantAssertionsSigned="true"');
        });
    });

    it('A site that has made no choice ends the flow at the callback', () => {
        initiateSamlLogin({siteKey: 'samlTestSiteUnsigned'});
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        // The identity provider returns its response to the callback and the flow ends there:
        // no redirection into the site, no account created, session still belongs to nobody.
        cy.url({timeout: 15000}).should('include', 'callback.saml');
        cy.url().should('not.contain', '/sites/samlTestSiteUnsigned/home.html');
        cy.get('body', {timeout: 10000}).should('not.contain', kcUsername);
        getUserPath(kcUsername).should(result => {
            expect(result?.data?.admin?.userAdmin?.user, `No account for ${kcUsername}`).to.be.null;
        });
    });

    it('A site configured with requireSignedAssertions = false completes the flow', () => {
        initiateSamlLogin({siteKey: 'samlTestSiteOptout'});
        waitAndFillKeycloakLoginForm(kcUrl, kcUsername, kcPassword);

        cy.url({timeout: 15000}).should('include', '/sites/samlTestSiteOptout/home.html');
        cy.get('body', {timeout: 10000}).should('contain', kcUsername);
        getUserPath(kcUsername).should(result => {
            expect(result?.data?.admin?.userAdmin?.user?.node?.path, `Account for ${kcUsername}`).to.contain(kcUsername);
        });
    });
});
