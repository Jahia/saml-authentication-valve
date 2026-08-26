import {createSite, deleteSite, enableModule, publishAndWaitJobEnding, setNodeProperty} from '@jahia/cypress';
import {createSamlButton} from '../support/helper';

/*
 * The samlLogin view places the node title into the value attribute of its submit button.
 *
 * The title below holds characters that are syntax inside an attribute, so a view that stopped
 * encoding its output would change the SHAPE of the markup rather than the visible label. That
 * is what the assertion measures.
 *
 * This spec needs no identity provider: the view renders for any visitor who is not logged in,
 * so it stays independent of the SAML round trip.
 *
 * The site uses templates-system, the supported default template set, whose own page template
 * declares the pagecontent area this view is placed in.
 */
const LABEL = 'Log in "now" & <fast>';

describe('SAML login button label rendering', () => {
    const siteKey = 'samlLabelRenderingSite';
    const home = `/sites/${siteKey}/home`;
    const buttonName = 'label-rendering-probe';

    before(() => {
        deleteSite(siteKey);
        createSite(siteKey, {
            languages: 'en',
            locale: 'en',
            serverName: 'localhost',
            templateSet: 'templates-system'
        });
        [
            'saml-authentication-valve',
            'jahia-authentication'
        ].forEach(moduleName => {
            enableModule(moduleName, siteKey);
        });
        createSamlButton(home, buttonName);
        setNodeProperty(`${home}/pagecontent/${buttonName}`, 'jcr:title', LABEL, 'en');
        publishAndWaitJobEnding(home, ['en']);
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('keeps a configured title inside the value attribute of the submit button', () => {
        // The view renders for a visitor who is not logged in, so an authenticated probe would
        // see no button at all and the assertion would be vacuous rather than failing.
        cy.logout();
        cy.clearCookies();
        cy.visit(`/cms/render/live/en${home}.html`);
        cy.get('form[action$=".connect.saml"] input[type="submit"]').should($submit => {
            expect($submit.attr('value'), 'value attribute').to.equal(LABEL);
            expect($submit[0].getAttributeNames(), 'attributes carried by the submit button')
                .to.have.members(['type', 'value']);
        });
    });
});
