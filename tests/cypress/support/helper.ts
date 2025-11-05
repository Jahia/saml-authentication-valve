export function createSamlButton(home, name) {
    cy.apollo({
        mutationFile: 'samlLogin/createSamlButton.graphql',
        variables: {homePath: home, name}
    }).should(res => {
        expect(res?.data?.jcr.addNode.addChild.uuid, `Created SAML button ${name}`).to.be.not.undefined;
    });
}

export function waitAndFillKeycloakLoginForm(keycloakUrl: string, username: string, password: string) {
    cy.origin(keycloakUrl, {args: {username, password}}, ({username: user, password: pass}) => {
        cy.get('#username', {timeout: 10000}).should('be.visible').type(user);
        cy.get('#password', {timeout: 10000}).should('be.visible').type(pass);
        cy.get('input[type="submit"]', {timeout: 10000}).should('be.visible').click();
    });
}

export interface SamlLoginOptions {
    siteKey?: string;
    redirect?: string;
    buttonName?: string;
    failOnStatusCode?: boolean;
    timeout?: number;
}

export function initiateSamlLogin(options: SamlLoginOptions): void {
    const {
        siteKey,
        redirect,
        buttonName,
        failOnStatusCode = false,
        timeout = 30000
    } = options;

    if (buttonName) {
        // Click method: clic on the saml login button to initiate login
        cy.get(`input[value="${buttonName}"]`, {timeout: 10000})
            .should('exist')
            .and('be.visible')
            .click();
    } else {
        // Direct visit method (default)
        let url = `/connect.saml?siteKey=${siteKey}`;

        if (redirect) {
            url += `&redirect=${encodeURIComponent(redirect)}`;
        }

        cy.visit(url, {
            failOnStatusCode,
            timeout
        });
    }
}
