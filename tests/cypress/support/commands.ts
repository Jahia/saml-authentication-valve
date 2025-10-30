import 'cypress-wait-until';

declare global {
    interface Chainable {
        setLocale(locale: string): Chainable<void>;
        setLanguageHeaders(locale: string): Chainable<void>;
        getBrowserLocale(): Chainable<string>;
        testLocaleFormatting(locale: string): Chainable<void>;
        clearAllLocalStorage(): Chainable<void>;
        clearAllSessionStorage(): Chainable<void>;
    }
}

// Set browser locale using Chrome DevTools Protocol
Cypress.Commands.add('setLocale', (locale: string) => {
    cy.log(`Setting browser locale to: ${locale}`);

    if (Cypress.browser.family === 'chromium') {
        // Set locale override
        Cypress.automation('remote:debugger:protocol', {
            command: 'Emulation.setLocaleOverride',
            params: {
                locale: locale
            }
        });

        // Set user agent with proper Accept-Language header
        Cypress.automation('remote:debugger:protocol', {
            command: 'Network.setUserAgentOverride',
            params: {
                userAgent: navigator.userAgent,
                acceptLanguage: `${locale},${locale.split('-')[0]},en-US;q=0.9,en;q=0.8`
            }
        });
    }
});

// Intercept all requests to add Accept-Language header
Cypress.Commands.add('setLanguageHeaders', (locale: string) => {
    cy.intercept('**/*', req => {
        req.headers['Accept-Language'] = `${locale},${locale.split('-')[0]},en-US;q=0.9,en;q=0.8`;
    });
});

// Get current browser locale for verification
Cypress.Commands.add('getBrowserLocale', () => {
    return cy.window().then(win => {
        return win.navigator.language;
    });
});

// Test locale formatting to verify it's working
Cypress.Commands.add('testLocaleFormatting', (expectedLocale: string) => {
    cy.window().then(win => {
        const testDate = new Date('2023-12-25');
        const formattedDate = testDate.toLocaleDateString();
        cy.log(`Date formatted as: ${formattedDate} for locale: ${expectedLocale}`);

        // Log browser language info
        cy.log(`Navigator language: ${win.navigator.language}`);
        cy.log(`Navigator languages: ${win.navigator.languages}`);
    });
});

// Clear all localStorage across all domains
Cypress.Commands.add('clearAllLocalStorage', () => {
    cy.window().then((win) => {
        win.localStorage.clear();
    });
    // Also clear for the base URL
    cy.clearLocalStorage();
});

// Clear all sessionStorage across all domains
Cypress.Commands.add('clearAllSessionStorage', () => {
    cy.window().then((win) => {
        win.sessionStorage.clear();
    });
});

export {};
